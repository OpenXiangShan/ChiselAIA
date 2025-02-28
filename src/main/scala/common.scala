/***************************************************************************************
* Copyright (c) 2024 Beijing Institute of Open Source Chip (BOSC)
*
* ChiselAIA is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

package aia

import chisel3._
import chisel3.util._
import freechips.rocketchip.amba._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.util._
import freechips.rocketchip.regmapper._

object pow2 { def apply(n: Int): Long = 1L << n }

class AXI4ToLite()(implicit p: Parameters) extends LazyModule { 
  // val node = AXI4AdapterNode( // identity
  //   masterFn = { mp => 
  //     val masters = Array.tabulate(65536 / 16) { i => 
  //       AXI4MasterParameters(
  //         name = "",
  //         id = IdRange(i * 16, i * 16 + 15)
  //       )
  //     }
  //     mp.copy(masters = masters)
  //   },
  //   slaveFn = { sp =>
  //     sp.copy(slaves = sp.slaves.map(s =>
  //       s.copy(address = s.address.map(a =>
  //         AddressSet(a.base, a.mask)))))}
  // )
  val node = AXI4AdapterNode( // identity
    masterFn = { mp => mp},
    slaveFn = { sp =>
      sp.copy(slaves = sp.slaves.map(s =>
        s.copy(address = s.address.map(a =>
          AddressSet(a.base, a.mask)))))}
  )
  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    (node.in zip node.out) foreach { case ((in, edgeIn), (out, edgeOut)) =>
      // out <> in
      dontTouch(in.aw.bits)
      dontTouch(in.ar.bits)
      dontTouch(in.w.bits)
      dontTouch(in.b.bits)
      dontTouch(in.r.bits)
      dontTouch(in.b.ready)
      dontTouch(in.ar.ready)
      // in.ar.bits.addr = in.ar.bits.addr.pad(32)
      // in.aw.bits.addr = in.aw.bits.addr.pad(32)
      val isAWValid = in.aw.valid
      val isARValid = in.ar.valid
      val sIDLE :: sWCH :: sBCH :: sRCH :: Nil =Enum(4)
      val state = RegInit(sIDLE)
      val next_state = WireInit(sIDLE)
      state := next_state
      
      val awpulse_l = (state === sIDLE) && (next_state === sWCH)
      val arpulse_l = (state === sIDLE) && (next_state === sRCH)
      val awchvld = isAWValid & in.w.valid
      val aw_l = RegEnable(in.aw.bits, awpulse_l)
      val w_l = RegEnable(in.w.bits, awpulse_l)
      // val b_l = RegEnable(in.b.bits, awpulse_l)
      val ar_l = RegEnable(in.ar.bits, arpulse_l)
      // al r_l = RegEnable(in.r.bits, awpulse_l)

      //======TODO======//
      val awcnt = RegInit(0.U(8.W)) // width of awcnt is same with awlen
      
      val addrAW = aw_l.addr
      val addrAR = ar_l.addr
      // only support little-endian (0x0 is active) msi write address check for AW,refer to riscv aia spec.
      val isValidAddressAW = (addrAW(11, 0) === 0.U)
      val isValidAddressAR = (addrAR(11, 0) === 0.U)  //

      val isValidAlignmentAW = (addrAW(1, 0) === 0.U)  // Example alignment check for AW
      val isValidAlignmentAR = (addrAR(1, 0) === 0.U)  // Example alignment check for AR
      // val isAccessingValidRegisterAW = (addrAW(11, 2) === 0.U)  // Example valid register check for AW
      // val isAccessingValidRegisterAR = (addrAR(11, 2) === 0.U)  // Example valid register check for AR

      val isWAligErr  = !((aw_l.size === 2.U) & (w_l.strb === 15.U) & isValidAlignmentAW)  // alignment with 4B.
      val isWCacheErr = (aw_l.cache(3,1)).orR  //non device
      val isWLockErr = aw_l.lock      // AMO access
      val isWburstErr = aw_l.burst(1)  //0'b10 or 0'b11 : wrap or reserved
      val isWCErr = isWAligErr | isWCacheErr | isWburstErr


      val isRAligErr  = !((ar_l.size === 2.U) & isValidAlignmentAR)
      val isRCacheErr = (ar_l.cache(3,1)).orR  //non device
      val isRLockErr = ar_l.lock      // AMO access
      val isRburstErr = ar_l.burst(1)  //0'b10 or 0'b11 : wrap or reserved
      val isRCErr = isRAligErr | isRCacheErr | isRburstErr
//      val isReservedAreaAccessAW = !(isAccessingValidRegisterAW) // Reserved area for AW
//      val isReservedAreaAccessAR = !(isAccessingValidRegisterAR) // Reserved area for AR

      val isillegalAW = (!isValidAddressAW) | isWCErr | isWLockErr
      val isillegalAR = (!isValidAlignmentAR) | isRCErr | isRLockErr

      in.r.bits.last := (state === sRCH) && (awcnt === ar_l.len) && in.r.ready && in.r.valid
      val awready = RegInit(true.B) // temp signal ,out.awready for the first data, true.B for other data transaction.
      val wready = RegInit(true.B) // temp signal
      val aw_last = RegInit(false.B)
      val w_last = RegInit(false.B)
      // aw_last: the last addr for burst,
      when (state === sWCH){
        when((awcnt === aw_l.len) & awready){  //may be pulse signal
          aw_last := true.B
        }
      }.otherwise{
        aw_last := false.B
      }
      when (state === sWCH){
        when(in.w.bits.last){
          w_last := true.B
        }
      }.otherwise{
        w_last := false.B
      }
      val isFinalBurst = aw_last & w_last // may be level signal ,the final data for a transaction
      val w2b_vld = (state === sWCH) & (isillegalAW | out.b.valid) & isFinalBurst
//      val rready = out.ar.ready //ready from downstream
      next_state := state
      switch(state){
        is(sIDLE) {
          when(awchvld){
            next_state := sWCH
          }.elsewhen(isARValid){
            next_state := sRCH
          }
        }
        is(sWCH) {
          when(w2b_vld.asBool){ // in.b.valid can be high,only when the last burst data done and the bvalid for data to downstream is high.
            next_state := sBCH
          }
        }
        is(sBCH) {
          when(in.b.ready){
            next_state := sIDLE
          }
        }
        is(sRCH) {
          when(in.r.bits.last){
            next_state := sIDLE
          }
        }
      }
      when(state === sWCH) {
        when((!isillegalAW) & (!awready) & out.aw.ready & in.aw.valid) { // only the first illegal data to downstream
          awready := true.B
        }.elsewhen(aw_last | (awready & (awcnt === aw_l.len))) {
          awready := false.B
        }.elsewhen(isillegalAW === true.B) {
          awready := true.B
        }
      }.otherwise {
        awready := false.B
      }

      when(state === sWCH) {
        when((!isillegalAW) & (!wready) & out.w.ready & in.w.valid) { //first data
          wready := true.B
        }.elsewhen(in.w.bits.last | w_last) {
          wready := false.B // legal or non first data
        }.elsewhen(isillegalAW === true.B) {
          wready := true.B
        }
      }.otherwise {
        wready := false.B
      }
      when(state === sWCH) {
        when(awcnt >= aw_l.len) { // arrive the max length of burst
          awcnt := awcnt
        }.elsewhen(awready) {
          awcnt := awcnt + 1.U
        }
      }.elsewhen(state === sRCH) {
        when(in.r.valid & in.r.ready) {
          awcnt := awcnt + 1.U
        }
      }.otherwise {
        awcnt := 0.U
      }
      // response for in
      val isFinaldly = RegInit(false.B)
      isFinaldly := isFinalBurst
      val isFinalris = isFinalBurst & (!isFinaldly)

      in.aw.ready := isFinalris
      in.w.ready := wready
      in.b.valid := state === sBCH
      in.b.bits.id := aw_l.id
      in.b.bits.resp := Cat(isWCErr, 0.U)
      val rvalid = RegInit(false.B)
      when(state === sRCH) {
        when(in.ar.valid) {
          rvalid := true.B
        }.elsewhen(in.r.bits.last) {
          rvalid := false.B
        }
      }.otherwise {
        rvalid := false.B
      }
      in.r.valid := rvalid
      in.r.bits.resp := Cat(isRCErr, 0.U)
      in.r.bits.id := ar_l.id
      in.r.bits.data := 0.U
      in.ar.ready := true.B

      // When either AW or AR is valid, perform address checks
      val m_awvalid = RegInit(false.B)
      when(awpulse_l) {
        m_awvalid := true.B
      }.elsewhen(out.aw.ready) {
        m_awvalid := false.B
      }.elsewhen(state === sIDLE) {
        m_awvalid := false.B
      }
      out.aw.valid := m_awvalid & (!isillegalAW)
      out.aw.bits.addr := aw_l.addr
      out.aw.bits.id := 0.U
      out.ar.bits.id := 0.U
      out.aw.bits.echo := aw_l.echo
      out.ar.bits.echo := ar_l.echo
      out.aw.bits.size := 2.U
      val m_wvalid = RegInit(false.B)
      when(awpulse_l) {
        m_wvalid := true.B
      }.elsewhen(out.w.ready) {
        m_wvalid := false.B
      }.elsewhen(state === sIDLE) {
        m_wvalid := false.B
      }
      out.w.valid := m_wvalid & (!isillegalAW)
      out.w.bits.strb := 15.U
      out.w.bits.data := w_l.data
      out.w.bits.last := 1.U
      val m_bready = RegInit(false.B)
      when((state === sBCH) && (next_state === sIDLE)) {
        m_bready := true.B
      }.otherwise {
        m_bready := false.B
      }
      out.b.ready := m_bready & (!isillegalAW)

      //else out signal is from the signals latched,for timing.
      }
  }
}


class AXI4Map(fn: AddressSet => BigInt)(implicit p: Parameters) extends LazyModule
{
  // val node = AXI4AdapterNode( // identity
  //   masterFn = { mp => 
  //     val masters = Array.tabulate(65536 / 16) { i => 
  //       AXI4MasterParameters(
  //         name = "",
  //         id = IdRange(i * 16, i * 16 + 15)
  //       )
  //     }
  //     mp.copy(masters = masters)
  //   },
  //   slaveFn = { sp =>
  //     sp.copy(slaves = sp.slaves.map(s =>
  //       s.copy(address = s.address.map(a =>
  //         AddressSet(fn(a), a.mask)))))}
  // )
  
  val node = AXI4AdapterNode(
    masterFn = { mp => mp },
    slaveFn = { sp =>
      sp.copy(slaves = sp.slaves.map(s =>
        s.copy(address = s.address.map(a =>
          AddressSet(fn(a), a.mask)))))}
  )

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    (node.in zip node.out) foreach { case ((in, edgeIn), (out, edgeOut)) =>
      out <> in
      val convert = edgeIn.slave.slaves.flatMap(_.address) zip edgeOut.slave.slaves.flatMap(_.address)
      def forward(x: UInt) =
        convert.map { case (i, o) => Mux(i.contains(x), o.base.U | (x & o.mask.U), 0.U) }.reduce(_ | _)
      def backward(x: UInt) =
        convert.map { case (i, o) => Mux(o.contains(x), i.base.U | (x & i.mask.U), 0.U) }.reduce(_ | _)

      out.aw.bits.addr := forward(in.aw.bits.addr)
      out.ar.bits.addr := forward(in.ar.bits.addr)
    }
  }
}


object AXI4Map
{
  def apply(fn: AddressSet => BigInt)(implicit p: Parameters): AXI4Node =
  {
    val map = LazyModule(new AXI4Map(fn))
    map.node
  }
  
}

/**
  * Convert AXI4 master to TileLink **without TLError**.
  *
  * You can use this adapter to connect external AXI4 masters to TileLink bus topology.
  *
  * Setting wcorrupt=true is insufficient to enable w.user.corrupt.
  * One must additionally list it in the AXI4 master's requestFields.
  *
  * @param wcorrupt enable AMBACorrupt in w.user
  */
class AXI4ToTLNoTLError(wcorrupt: Boolean)(implicit p: Parameters) extends LazyModule
{
  val node = AXI4ToTLNode(wcorrupt)

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    (node.in zip node.out) foreach { case ((in, edgeIn), (out, edgeOut)) =>
      val numIds = edgeIn.master.endId
      val beatBytes = edgeOut.manager.beatBytes
      val beatCountBits = AXI4Parameters.lenBits + (1 << AXI4Parameters.sizeBits) - 1
      val maxFlight = edgeIn.master.masters.map(_.maxFlight.get).max
      val logFlight = log2Ceil(maxFlight)
      val txnCountBits = log2Ceil(maxFlight+1) // wrap-around must not block b_allow
      val addedBits = logFlight + 1 // +1 for read vs. write source ID

      require (edgeIn.master.masters(0).aligned)
      edgeOut.manager.requireFifo()

      // // Look for an Error device to redirect bad requests
      // val errorDevs = edgeOut.manager.managers.filter(_.nodePath.last.lazyModule.className == "TLError")
      // require (!errorDevs.isEmpty, "There is no TLError reachable from AXI4ToTL. One must be instantiated.")
      // val errorDev = errorDevs.maxBy(_.maxTransfer)
      // val error = errorDev.address.head.base
      // require (errorDev.supportsPutPartial.contains(edgeOut.manager.maxTransfer),
      //   s"Error device supports ${errorDev.supportsPutPartial} PutPartial but must support ${edgeOut.manager.maxTransfer}")
      // require (errorDev.supportsGet.contains(edgeOut.manager.maxTransfer),
      //   s"Error device supports ${errorDev.supportsGet} Get but must support ${edgeOut.manager.maxTransfer}")

      val r_out = WireDefault(out.a)
      val r_size1 = in.ar.bits.bytes1()
      val r_size = OH1ToUInt(r_size1)
      val r_ok = edgeOut.manager.supportsGetSafe(in.ar.bits.addr, r_size)
      // val r_addr = Mux(r_ok, in.ar.bits.addr, error.U | in.ar.bits.addr(log2Up(beatBytes)-1, 0))
      val r_addr = in.ar.bits.addr
      val r_count = RegInit(VecInit.fill(numIds) { 0.U(txnCountBits.W) })
      val r_id = if (maxFlight == 1) {
        Cat(in.ar.bits.id, 0.U(1.W))
      } else {
        Cat(in.ar.bits.id, r_count(in.ar.bits.id)(logFlight-1,0), 0.U(1.W))
      }

      assert (!in.ar.valid || r_size1 === UIntToOH1(r_size, beatCountBits)) // because aligned
      in.ar.ready := r_out.ready
      r_out.valid := in.ar.valid
      r_out.bits :<= edgeOut.Get(r_id, r_addr, r_size)._2

      Connectable.waiveUnmatched(r_out.bits.user, in.ar.bits.user) match {
        case (lhs, rhs) => lhs.squeezeAll :<= rhs.squeezeAll
      }

      r_out.bits.user.lift(AMBAProt).foreach { rprot =>
        rprot.privileged :=  in.ar.bits.prot(0)
        rprot.secure     := !in.ar.bits.prot(1)
        rprot.fetch      :=  in.ar.bits.prot(2)
        rprot.bufferable :=  in.ar.bits.cache(0)
        rprot.modifiable :=  in.ar.bits.cache(1)
        rprot.readalloc  :=  in.ar.bits.cache(2)
        rprot.writealloc :=  in.ar.bits.cache(3)
      }

      val r_sel = UIntToOH(in.ar.bits.id, numIds)
      (r_sel.asBools zip r_count) foreach { case (s, r) =>
        when (in.ar.fire && s) { r := r + 1.U }
      }

      val w_out = WireDefault(out.a)
      val w_size1 = in.aw.bits.bytes1()
      val w_size = OH1ToUInt(w_size1)
      val w_ok = edgeOut.manager.supportsPutPartialSafe(in.aw.bits.addr, w_size)
      // val w_addr = Mux(w_ok, in.aw.bits.addr, error.U | in.aw.bits.addr(log2Up(beatBytes)-1, 0))
      val w_addr = in.aw.bits.addr
      val w_count = RegInit(VecInit.fill(numIds) { 0.U(txnCountBits.W) })
      val w_id = if (maxFlight == 1) {
        Cat(in.aw.bits.id, 1.U(1.W))
      } else {
        Cat(in.aw.bits.id, w_count(in.aw.bits.id)(logFlight-1,0), 1.U(1.W))
      }

      assert (!in.aw.valid || w_size1 === UIntToOH1(w_size, beatCountBits)) // because aligned
      assert (!in.aw.valid || in.aw.bits.len === 0.U || in.aw.bits.size === log2Ceil(beatBytes).U) // because aligned
      in.aw.ready := w_out.ready && in.w.valid && in.w.bits.last
      in.w.ready  := w_out.ready && in.aw.valid
      w_out.valid := in.aw.valid && in.w.valid
      w_out.bits :<= edgeOut.Put(w_id, w_addr, w_size, in.w.bits.data, in.w.bits.strb)._2
      in.w.bits.user.lift(AMBACorrupt).foreach { w_out.bits.corrupt := _ }

      Connectable.waiveUnmatched(w_out.bits.user, in.aw.bits.user) match {
        case (lhs, rhs) => lhs.squeezeAll :<= rhs.squeezeAll
      }

      w_out.bits.user.lift(AMBAProt).foreach { wprot =>
        wprot.privileged :=  in.aw.bits.prot(0)
        wprot.secure     := !in.aw.bits.prot(1)
        wprot.fetch      :=  in.aw.bits.prot(2)
        wprot.bufferable :=  in.aw.bits.cache(0)
        wprot.modifiable :=  in.aw.bits.cache(1)
        wprot.readalloc  :=  in.aw.bits.cache(2)
        wprot.writealloc :=  in.aw.bits.cache(3)
      }

      val w_sel = UIntToOH(in.aw.bits.id, numIds)
      (w_sel.asBools zip w_count) foreach { case (s, r) =>
        when (in.aw.fire && s) { r := r + 1.U }
      }

      TLArbiter(TLArbiter.roundRobin)(out.a, (0.U, r_out), (in.aw.bits.len, w_out))

      val ok_b  = WireDefault(in.b)
      val ok_r  = WireDefault(in.r)

      val d_resp = Mux(out.d.bits.denied || out.d.bits.corrupt, AXI4Parameters.RESP_SLVERR, AXI4Parameters.RESP_OKAY)
      val d_hasData = edgeOut.hasData(out.d.bits)
      val d_last = edgeOut.last(out.d)

      out.d.ready := Mux(d_hasData, ok_r.ready, ok_b.ready)
      ok_r.valid := out.d.valid && d_hasData
      ok_b.valid := out.d.valid && !d_hasData

      ok_r.bits.id   := out.d.bits.source >> addedBits
      ok_r.bits.data := out.d.bits.data
      ok_r.bits.resp := d_resp
      ok_r.bits.last := d_last
      ok_r.bits.user :<= out.d.bits.user

      // AXI4 needs irrevocable behaviour
      in.r :<>= Queue.irrevocable(ok_r, 1, flow=true)

      ok_b.bits.id   := out.d.bits.source >> addedBits
      ok_b.bits.resp := d_resp
      ok_b.bits.user :<= out.d.bits.user

      // AXI4 needs irrevocable behaviour
      val q_b = Queue.irrevocable(ok_b, 1, flow=true)

      // We need to prevent sending B valid before the last W beat is accepted
      // TileLink allows early acknowledgement of a write burst, but AXI does not.
      val b_count = RegInit(VecInit.fill(numIds) { 0.U(txnCountBits.W) })
      val b_allow = b_count(in.b.bits.id) =/= w_count(in.b.bits.id)
      val b_sel = UIntToOH(in.b.bits.id, numIds)

      (b_sel.asBools zip b_count) foreach { case (s, r) =>
        when (in.b.fire && s) { r := r + 1.U }
      }

      in.b.bits :<= q_b.bits
      in.b.valid := q_b.valid && b_allow
      q_b.ready := in.b.ready && b_allow

      // Unused channels
      out.b.ready := true.B
      out.c.valid := false.B
      out.e.valid := false.B
    }
  }
}

// object AXI4ToTL
object AXI4ToTLNoTLError
{
  def apply(wcorrupt: Boolean = true)(implicit p: Parameters) =
  {
    val axi42tl = LazyModule(new AXI4ToTLNoTLError(wcorrupt))
    axi42tl.node
  }
}

// modifications based on `rocket-chip/src/main/scala/tilelink/RegisterRouter.scala`
case class TLRegMapperNode(
  address:     Seq[AddressSet],
  beatBytes:   Int,
)(implicit valName: ValName) extends SinkNode(TLImp)(Seq(TLSlavePortParameters.v1(
  Seq(TLSlaveParameters.v1(
    address            = address,
    executable         = false,
    supportsGet        = TransferSizes(1, beatBytes),
    supportsPutPartial = TransferSizes(1, beatBytes),
    supportsPutFull    = TransferSizes(1, beatBytes),
    fifoId             = Some(0), // requests are handled in order
  )),
  beatBytes  = beatBytes,
  minLatency = 1,
))) with TLFormatNode {
  val size = 1 << log2Ceil(1 + address.map(_.max).max - address.map(_.base).min)
  require (size >= beatBytes)
  address.foreach { case a =>
    require (a.widen(size-1).base == address.head.widen(size-1).base,
      s"TLRegMapperNode addresses (${address}) must be aligned to its size ${size}")
  }

  def regmap(in: DecoupledIO[RegMapperInput], out: DecoupledIO[RegMapperOutput], backpress: Bool = true.B) = {
    val (bundleIn, edge) = this.in(0)
    val a = bundleIn.a
    val d = bundleIn.d

    in.bits.read  := a.bits.opcode === TLMessages.Get
    in.bits.index := edge.addr_hi(a.bits)
    in.bits.data  := a.bits.data
    in.bits.mask  := a.bits.mask
    Connectable.waiveUnmatched(in.bits.extra, a.bits.echo) match {
      case (lhs, rhs) => lhs :<= rhs
    }

    // copy a.bits.{source, size} to d.bits.{source, size}
    val sourceReg = RegInit(0.U.asTypeOf(a.bits.source))
    val sizeReg   = RegInit(0.U.asTypeOf(a.bits.size))
    when (a.valid) {
      sourceReg := a.bits.source
      sizeReg   := a.bits.size
    }

    // No flow control needed
    in.valid  := a.valid
    a.ready   := Mux(in.bits.read, in.ready, (backpress & in.ready))
    d.valid   := Mux(out.bits.read, out.valid, (backpress & out.valid))
    out.ready := d.ready

    // We must restore the size to enable width adapters to work
    d.bits := edge.AccessAck(toSource = sourceReg, lgSize = sizeReg)

    // avoid a Mux on the data bus by manually overriding two fields
    d.bits.data := out.bits.data
    Connectable.waiveUnmatched(d.bits.echo, out.bits.extra) match {
      case (lhs, rhs) => lhs :<= rhs
    }

    d.bits.opcode := Mux(out.bits.read, TLMessages.AccessAckData, TLMessages.AccessAck)

    // Tie off unused channels
    bundleIn.b.valid := false.B
    bundleIn.c.ready := true.B
    bundleIn.e.ready := true.B
  }
}

// modification based on `rocket-chip/src/main/scala/amba/axi4/RegisterRouter.scala`
case class AXI4RegMapperNode(
  address: AddressSet,
  beatBytes: Int = 4,
)(implicit valName: ValName) extends SinkNode(AXI4Imp)(Seq(AXI4SlavePortParameters(
  Seq(AXI4SlaveParameters(
    address       = Seq(address),
    executable    = false,
    supportsWrite = TransferSizes(1, beatBytes),
    supportsRead  = TransferSizes(1, beatBytes),
    interleavedId = Some(0))),
  beatBytes  = beatBytes,
  minLatency = 1,
))) {
  require (address.contiguous)

  // Calling this method causes the matching AXI4 bundle to be
  // configured to route all requests to the listed RegFields.
  //backpress: add by zhaohong for bus backpressure
  def regmap(in: DecoupledIO[RegMapperInput],out: DecoupledIO[RegMapperOutput], backpress: Bool = true.B) = {
    val (io, _) = this.in(0)
    val ar = io.ar
    val aw = io.aw
    val w  = io.w
    val r  = io.r
    val b  = io.b

    dontTouch(aw.bits)
    dontTouch(ar.bits)
    dontTouch(w.bits)
    dontTouch(b.bits)
    dontTouch(r.bits)
    
    // Prefer to execute reads first
    in.valid := ar.valid || (aw.valid && w.valid)
    ar.ready := in.ready
    aw.ready := backpress && in.ready && !ar.valid
    w .ready := backpress && in.ready && !ar.valid

    // copy {ar,aw}_bits.{echo,id} to {r,b}_bits.{echo,id}
    val arEchoReg = RegInit(0.U.asTypeOf(ar.bits.echo))
    val awEchoReg = RegInit(0.U.asTypeOf(aw.bits.echo))
    val arIdReg   = RegInit(0.U.asTypeOf(ar.bits.id))
    val awIdReg   = RegInit(0.U.asTypeOf(aw.bits.id))
    when (ar.valid) { arEchoReg := ar.bits.echo; arIdReg := ar.bits.id }
    when (aw.valid) { awEchoReg := aw.bits.echo; awIdReg := aw.bits.id }

    val addr = Mux(ar.valid, ar.bits.addr, aw.bits.addr)
    val mask = MaskGen(ar.bits.addr, ar.bits.size, beatBytes)

    in.bits.read  := ar.valid
    in.bits.index := addr >> log2Ceil(beatBytes)
    in.bits.data  := w.bits.data
    in.bits.mask  := Mux(ar.valid, mask, w.bits.strb)

    // No flow control needed
    out.ready := Mux(out.bits.read, r.ready, b.ready)
    r.valid := out.valid &&  out.bits.read
    b.valid := backpress && out.valid && !out.bits.read // backpressure for write operation.

    r.bits.id   := arIdReg
    r.bits.data := out.bits.data
    r.bits.last := true.B
    r.bits.resp := AXI4Parameters.RESP_OKAY
    r.bits.echo :<= arEchoReg

    b.bits.id   := awIdReg
    b.bits.resp := AXI4Parameters.RESP_OKAY
    b.bits.echo :<= awEchoReg
  }
}