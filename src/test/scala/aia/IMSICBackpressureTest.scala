package aia

import chisel3._
import chisel3.util._
import chiseltest._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.tilelink._
import org.chipsalliance.cde.config.Parameters
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

private class IMSICTLHarness(implicit parameters: Parameters) extends LazyModule {
  private val params = IMSICParams(geilen = 1, EnableImsicAsyncBridge = true)
  private val imsic = LazyModule(new TLRegIMSIC(params, seperateBus = true))
  private val clients = Seq.tabulate(2) { port =>
    val client = TLClientNode(Seq(TLMasterPortParameters.v1(Seq(
      TLMasterParameters.v1(name = s"msi_$port", sourceId = IdRange(0, 1))
    ))))
    imsic.fromMem(port) := client
    client
  }
  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val buses = clients.map { client =>
      val bus = IO(Flipped(chiselTypeOf(client.out.head._1)))
      client.out.head._1 <> bus
      bus
    }
    val msi = IO(Flipped(new MSITransBundle(params)))
    msi <> imsic.module.msiio
  }
}

private class IMSICAXIHarness(implicit parameters: Parameters) extends LazyModule {
  private val params = IMSICParams(geilen = 1, EnableImsicAsyncBridge = true)
  private val imsic = LazyModule(new AXIRegIMSIC(params, seperateBus = true))
  private val clients = Seq.tabulate(2) { port =>
    val client = AXI4MasterNode(Seq(AXI4MasterPortParameters(Seq(
      AXI4MasterParameters(name = s"msi_$port", id = IdRange(0, 1))
    ))))
    imsic.axi4tolite(port).node := client
    client
  }
  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val buses = clients.map { client =>
      val bus = IO(Flipped(chiselTypeOf(client.out.head._1)))
      client.out.head._1 <> bus
      bus
    }
    val msi = IO(Flipped(new MSITransBundle(params)))
    msi <> imsic.module.msiio
  }
}

private class IMSICRegGenHarness extends Module {
  implicit val parameters: Parameters = Parameters.empty
  private val params = IMSICParams(geilen = 1)
  private val reggen = Module(new RegGen(params))
  val requests = reggen.regmapIOs.map { case (request, response) =>
    val port = IO(Flipped(Decoupled(chiselTypeOf(request.bits))))
    request <> port
    response.ready := true.B
    port
  }
  val responses = IO(Output(Vec(2, Bool())))
  responses := VecInit(reggen.regmapIOs.map(_._2.fire))
  private val queue = Module(new Queue(UInt(params.MSI_INFO_WIDTH.W), 8))
  queue.io.enq.valid := reggen.io.valid
  queue.io.enq.bits := reggen.io.seteipnum
  reggen.io.ready := queue.io.enq.ready
  val events = IO(Decoupled(UInt(params.MSI_INFO_WIDTH.W)))
  events <> queue.io.deq
}

private class IMSICReceiverHarness(implicit parameters: Parameters)
    extends IMSIC(IMSICParams(imsicIntSrcWidth = 7, geilen = 1, EnableImsicAsyncBridge = true)) {
  val capturedFiles = IO(Output(UInt(3.W)))
  val capturedData = IO(Output(UInt(9.W)))
  capturedFiles := imsicGateWay.msi_valid_o
  capturedData := imsicGateWay.msi_data_o
}

class IMSICBackpressureTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "IMSIC MSI writes"

  it should "capture held asynchronous requests once and set the correct M/S/VS pending bits" in {
    implicit val parameters: Parameters = Parameters.empty
    test(new IMSICReceiverHarness).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      dut.fromCSR.addr.valid.poke(false.B)
      dut.fromCSR.addr.bits.addr.poke(0.U)
      dut.fromCSR.addr.bits.virt.poke(false.B)
      dut.fromCSR.addr.bits.priv.poke(PrivType.M)
      dut.fromCSR.vgein.poke(0.U)
      dut.fromCSR.wdata.valid.poke(false.B)
      dut.fromCSR.wdata.bits.op.poke(OpType.CSRRW)
      dut.fromCSR.wdata.bits.data.poke(0.U)
      dut.fromCSR.claims.foreach(_.poke(false.B))
      dut.msiio.vld_req.poke(false.B)
      dut.msiio.data.poke(0.U)
      val received = scala.collection.mutable.ArrayBuffer.empty[(BigInt, BigInt)]
      def step(cycles: Int): Unit = {
        for (cycle <- 0 until cycles) {
          if (dut.capturedFiles.peek().litValue != 0) {
            received += ((dut.capturedFiles.peek().litValue, dut.capturedData.peek().litValue))
          }
          dut.clock.step()
        }
      }
      step(5)
      for (identity <- 1 to 16; file <- 0 until 3) {
        dut.msiio.data.poke(((file << 7) | identity).U)
        dut.msiio.vld_req.poke(true.B)
        step(12)
        dut.msiio.vld_ack.expect(true.B)
        dut.msiio.vld_req.poke(false.B)
        step(12)
        dut.msiio.vld_ack.expect(false.B)
      }
      received.toSeq shouldBe (for (identity <- 1 to 16; file <- 0 until 3)
        yield (BigInt(1) << file, BigInt(identity)))
      for (file <- 0 until 3) {
        dut.fromCSR.addr.valid.poke(true.B)
        dut.fromCSR.addr.bits.addr.poke(0x80.U)
        dut.fromCSR.addr.bits.priv.poke(if (file == 0) PrivType.M else PrivType.S)
        dut.fromCSR.addr.bits.virt.poke((file == 2).B)
        dut.fromCSR.vgein.poke((if (file == 2) 1 else 0).U)
        step(3)
        dut.toCSR.rdata.valid.expect(true.B)
        dut.toCSR.rdata.bits.expect(((BigInt(1) << 17) - 2).U)
      }
    }
  }

  private class SlowReceiver(msi: MSITransBundle) {
    val received = scala.collection.mutable.ArrayBuffer.empty[BigInt]
    private var requestSync = List.fill(3)(false)
    private var ack = false
    private var activeData = Option.empty[BigInt]

    def drive(cycle: Int): Unit = {
      val request = msi.vld_req.peek().litToBoolean
      if (request) {
        val data = msi.data.peek().litValue
        activeData.foreach(_ shouldBe data)
        activeData = Some(data)
      } else {
        activeData = None
      }
      if (cycle % 11 == 0) {
        if (requestSync.last && !ack) received += msi.data.peek().litValue
        ack = requestSync.last
        requestSync = request :: requestSync.init
      }
      msi.vld_ack.poke(ack.B)
    }

    def check(writesPerPort: Int): Unit = {
      received.size shouldBe 2 * writesPerPort
      for (port <- 0 until 2) {
        received.filter(value => (value >> 9) == port).map(_ & 511).toSeq shouldBe
          (1 to writesPerPort).map(BigInt(_))
      }
    }
  }

  it should "backpressure full TL queues and deliver every MSI across slow four-phase handshakes" in {
    implicit val parameters: Parameters = Parameters.empty
    test(LazyModule(new IMSICTLHarness).module).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      val receiver = new SlowReceiver(dut.msi)
      val sent = Array.fill(2)(0)
      val completed = Array.fill(2)(0)
      val writesPerPort = 16
      var stalls = 0
      for (cycle <- 0 until 6000) {
        val pending = dut.buses.indices.map(port => sent(port) < writesPerPort && sent(port) == completed(port))
        for (port <- dut.buses.indices) {
          val bus = dut.buses(port)
          bus.a.valid.poke(pending(port).B)
          bus.a.bits.opcode.poke(TLMessages.PutFullData)
          bus.a.bits.param.poke(0.U)
          bus.a.bits.size.poke(2.U)
          bus.a.bits.source.poke(0.U)
          bus.a.bits.address.poke((if (port == 0) 0 else 0x10000).U)
          bus.a.bits.mask.poke(15.U)
          bus.a.bits.data.poke((sent(port) + 1).U)
          bus.a.bits.corrupt.poke(false.B)
          bus.d.ready.poke((cycle % 9 != 0).B)
        }
        for (port <- dut.buses.indices) {
          val bus = dut.buses(port)
          if (pending(port)) {
            if (bus.a.ready.peek().litToBoolean) sent(port) += 1 else stalls += 1
          }
          if (bus.d.valid.peek().litToBoolean && bus.d.ready.peek().litToBoolean) {
            bus.d.bits.denied.expect(false.B)
            completed(port) += 1
          }
        }
        receiver.drive(cycle)
        dut.clock.step()
      }
      stalls should be > 0
      sent.toSeq shouldBe Seq(writesPerPort, writesPerPort)
      completed.toSeq shouldBe sent.toSeq
      receiver.check(writesPerPort)
    }
  }

  it should "backpressure full AXI queues without losing or repeating MSI writes or B responses" in {
    implicit val parameters: Parameters = Parameters.empty
    test(LazyModule(new IMSICAXIHarness).module).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      val receiver = new SlowReceiver(dut.msi)
      val addresses = Array.fill(2)(0)
      val data = Array.fill(2)(0)
      val completed = Array.fill(2)(0)
      val writesPerPort = 16
      var stalls = 0
      for (cycle <- 0 until 6000) {
        for (port <- dut.buses.indices) {
          val bus = dut.buses(port)
          bus.ar.valid.poke(false.B)
          bus.r.ready.poke(true.B)
          bus.aw.valid.poke((addresses(port) < writesPerPort).B)
          bus.aw.bits.id.poke(0.U)
          bus.aw.bits.addr.poke((if (port == 0) 0 else 0x10000).U)
          bus.aw.bits.len.poke(0.U)
          bus.aw.bits.size.poke(2.U)
          bus.aw.bits.burst.poke(1.U)
          bus.aw.bits.lock.poke(0.U)
          bus.aw.bits.cache.poke(0.U)
          bus.aw.bits.prot.poke(0.U)
          bus.aw.bits.qos.poke(0.U)
          bus.w.valid.poke((data(port) < writesPerPort).B)
          bus.w.bits.data.poke((data(port) + 1).U)
          bus.w.bits.strb.poke(15.U)
          bus.w.bits.last.poke(true.B)
          bus.b.ready.poke((cycle % 9 == 0).B)
        }
        for (port <- dut.buses.indices) {
          val bus = dut.buses(port)
          if (addresses(port) < writesPerPort) {
            if (bus.aw.ready.peek().litToBoolean) addresses(port) += 1 else stalls += 1
          }
          if (data(port) < writesPerPort && bus.w.ready.peek().litToBoolean) data(port) += 1
          if (bus.b.valid.peek().litToBoolean && bus.b.ready.peek().litToBoolean) {
            bus.b.bits.resp.expect(0.U)
            completed(port) += 1
          }
        }
        receiver.drive(cycle)
        dut.clock.step()
      }
      stalls should be > 0
      addresses.toSeq shouldBe Seq(writesPerPort, writesPerPort)
      data.toSeq shouldBe addresses.toSeq
      completed.toSeq shouldBe addresses.toSeq
      receiver.check(writesPerPort)
    }
  }

  it should "retain an eleven-write burst while MSI delivery is blocked" in {
    test(new IMSICRegGenHarness) { dut =>
      var sent = 0
      var completed = 0
      var stalls = 0
      val received = scala.collection.mutable.ArrayBuffer.empty[BigInt]
      for (cycle <- 0 until 300) {
        for (port <- dut.requests.indices) {
          val request = dut.requests(port)
          request.valid.poke((port == 0 && sent < 11).B)
          request.bits.read.poke(false.B)
          request.bits.index.poke(0.U)
          request.bits.data.poke((sent + 1).U)
          request.bits.mask.poke(15.U)
        }
        val drain = cycle >= 80 && cycle % 7 == 0
        dut.events.ready.poke(drain.B)
        if (sent < 11) {
          if (dut.requests.head.ready.peek().litToBoolean) sent += 1 else stalls += 1
        }
        if (dut.responses.head.peek().litToBoolean) completed += 1
        if (drain && dut.events.valid.peek().litToBoolean) received += dut.events.bits.peek().litValue
        dut.clock.step()
      }
      sent shouldBe 11
      completed shouldBe 11
      received.toSeq shouldBe (1 to 11).map(BigInt(_))
      stalls should be > 0
      dut.events.ready.poke(true.B)
      dut.clock.step(10)
      dut.events.valid.expect(false.B)
    }
  }

  it should "retain concurrent M/S writes across a full FIFO and drain each exactly once" in {
    test(new IMSICRegGenHarness) { dut =>
      val sent = Array.fill(2)(0)
      val completed = Array.fill(2)(0)
      val received = scala.collection.mutable.ArrayBuffer.empty[BigInt]
      var blockedCycles = 0
      var cycles = 0
      val writesPerPort = 32
      while (cycles < 2500 && received.size < 2 * writesPerPort) {
        for (port <- dut.requests.indices) {
          val request = dut.requests(port)
          request.valid.poke((sent(port) < writesPerPort).B)
          request.bits.read.poke(false.B)
          request.bits.index.poke(0.U)
          request.bits.data.poke((sent(port) + 1).U)
          request.bits.mask.poke(15.U)
        }
        val drain = cycles >= 80 && cycles % 7 == 0
        dut.events.ready.poke(drain.B)
        for (port <- dut.requests.indices) {
          if (sent(port) < writesPerPort && dut.requests(port).ready.peek().litToBoolean) {
            sent(port) += 1
          }
          if (dut.responses(port).peek().litToBoolean) completed(port) += 1
        }
        if (cycles < 80 && dut.requests.forall(!_.ready.peek().litToBoolean)) blockedCycles += 1
        if (drain && dut.events.valid.peek().litToBoolean) received += dut.events.bits.peek().litValue
        dut.clock.step()
        cycles += 1
      }
      blockedCycles should be > 0
      sent.toSeq shouldBe Seq(writesPerPort, writesPerPort)
      completed.toSeq shouldBe sent.toSeq
      received.size shouldBe 2 * writesPerPort
      for (port <- dut.requests.indices) {
        received.filter(value => (value >> 9) == port).map(_ & 511).toSeq shouldBe
          (1 to writesPerPort).map(BigInt(_))
      }
      dut.requests.foreach(_.valid.poke(false.B))
      dut.events.ready.poke(true.B)
      dut.clock.step(20)
      dut.events.valid.expect(false.B)
    }
  }

  it should "complete reads without creating an MSI when event delivery is blocked" in {
    test(new IMSICRegGenHarness) { dut =>
      dut.events.ready.poke(false.B)
      dut.requests.foreach { request =>
        request.valid.poke(true.B)
        request.bits.read.poke(true.B)
        request.bits.index.poke(0.U)
        request.bits.data.poke(0.U)
        request.bits.mask.poke(15.U)
      }
      var completed = 0
      for (cycle <- 0 until 20) {
        completed += dut.responses.count(_.peek().litToBoolean)
        dut.events.valid.expect(false.B)
        dut.clock.step()
      }
      completed should be > 0
    }
  }
}
