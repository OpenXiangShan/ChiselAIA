package aia

import chisel3._
import freechips.rocketchip.diplomacy._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.amba.axi4._
import _root_.circt.stage.ChiselStage

/**
 * A minimal AXI4 top for AXIRegIMSIC_WRAP whose upstream AXI ID space is wider than 16 bits.
 *
 * It exists so that test/imsic_axi_id can check that an access whose ID does not fit in 16 bits
 * (e.g. XiangShan's imsic_bus_top, where the CPU port is 0x10040) gets its response back with
 * the ID it used. With the raw AXI4SlaveNode/AXI4MasterNode(IdRange(0, 65536)) bridge the ID was
 * truncated to bit 15 and the response was routed to the wrong master.
 */
class AXI4IMSICIDTop(idBits: Int = 17)()(implicit p: Parameters) extends LazyModule {
  val master = AXI4MasterNode(Seq(AXI4MasterPortParameters(
    Seq(AXI4MasterParameters("testmaster", IdRange(0, 1 << idBits))))))

  val imsicParams = IMSICParams(mAddr = 0x61000000L, sgAddr = 0x82900000L, HasTEEIMSIC = false)
  val wrap = LazyModule(new AXIRegIMSIC_WRAP(imsicParams, beatBytes = 4))
  wrap.imsic_xbar1to2 := master

  lazy val module = new LazyModuleImp(this) {
    master.makeIOs()(ValName("master"))
    wrap.module.msiio <> DontCare
  }
}

object AXI4IMSICIDTop extends App {
  implicit val p: Parameters = Parameters.empty

  ChiselStage.emitSystemVerilog(
    LazyModule(new AXI4IMSICIDTop()).module,
    firtoolOpts = Array(
      "-disable-all-randomization",
      "-strip-debug-info",
      "--disable-annotation-unknown",
      "--lowering-options=explicitBitcast,disallowLocalVariables,disallowPortDeclSharing,locationInfoStyle=none",
      "--split-verilog", "-o=gen_imsic_axi_id",
    )
  )
}
