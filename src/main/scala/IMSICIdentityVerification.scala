package aia

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import _root_.circt.stage.ChiselStage

class IMSICIdentityVerification extends Module {
  implicit val parameters: Parameters = Parameters.empty
  val params = IMSICParams(imsicIntSrcWidth = 8, EnableImsicAsyncBridge = false)
  val reggen = Module(new RegGen(params))
  val imsic = Module(new IMSIC(params))
  val request = IO(chiselTypeOf(reggen.regmapIOs.head._1))
  val response = IO(chiselTypeOf(reggen.regmapIOs.head._2))
  val supervisorRequest = IO(chiselTypeOf(reggen.regmapIOs(1)._1))
  val supervisorResponse = IO(chiselTypeOf(reggen.regmapIOs(1)._2))
  val fromCSR = IO(Input(new CSRToIMSICBundle(params)))
  val toCSR = IO(Output(new IMSICToCSRBundle(params)))
  val msiValid = IO(Output(Bool()))
  reggen.regmapIOs.head._1 <> request
  response <> reggen.regmapIOs.head._2
  reggen.regmapIOs(1)._1 <> supervisorRequest
  supervisorResponse <> reggen.regmapIOs(1)._2
  imsic.fromCSR := fromCSR
  toCSR := imsic.toCSR
  imsic.msiio.data := reggen.io.seteipnum
  imsic.msiio.vld_req := reggen.io.valid
  msiValid := reggen.io.valid
}

object VerifyIMSICIdentities extends App {
  ChiselStage.emitSystemVerilog(new IMSICIdentityVerification,
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info",
      "--split-verilog", "-o=/tmp/imsic-identity-verification/rtl"))
}
