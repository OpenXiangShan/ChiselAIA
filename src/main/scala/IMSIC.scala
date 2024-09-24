package aia

import chisel3._
import freechips.rocketchip.diplomacy._
import org.chipsalliance.cde.config.Parameters

// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage

class TLIMSIC()(implicit p: Parameters) extends LazyModule {
  lazy val module = new LazyModuleImp(this) {
  }
}

/**
 * Generate Verilog sources
 */
object TLIMSIC extends App {
  ChiselStage.emitSystemVerilogFile(
    (LazyModule(
      new TLIMSIC()(Parameters.empty)
    )).module,
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}
