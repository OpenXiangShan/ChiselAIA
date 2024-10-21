/***************************************************************************************
* Copyright (c) 2024 Beijing Institute of Open Source Chip (BOSC)
*
* OpenAIA.scala is licensed under Mulan PSL v2.
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
import freechips.rocketchip.diplomacy._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.tilelink._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage

class OpenAIA()(implicit p: Parameters) extends LazyModule {
  val imsic_params = IMSICParams()
  val aplic_params = APLICParams()

  val imsic_fromCPU = TLClientNode(
    Seq(TLMasterPortParameters.v1(
      Seq(TLMasterParameters.v1("imsic_tl", IdRange(0, 16)))
  )))
  val imsic = LazyModule(new TLIMSIC(imsic_params)(Parameters.empty))
  imsic.fromCPU := imsic_fromCPU

  val aplic_fromCPU = TLClientNode(
    Seq(TLMasterPortParameters.v1(
      Seq(TLMasterParameters.v1("aplic_tl", IdRange(0, 16)))
  )))
  val aplic_toIMSIC = TLManagerNode(Seq(TLSlavePortParameters.v1(
    Seq(TLSlaveParameters.v1(
      address = Seq(
        AddressSet(imsic_params.mBaseAddr, pow2(imsic_params.groupStrideWidth + imsic_params.groupsWidth)-1),
        AddressSet(imsic_params.sgBaseAddr,pow2(imsic_params.groupStrideWidth + imsic_params.groupsWidth)-1),
      ),
      supportsPutFull = TransferSizes(1, 8),
    )),
    beatBytes = 8
  )))
  val aplic = LazyModule(new TLAPLIC(aplic_params, imsic_params)(Parameters.empty))
  aplic.fromCPU := aplic_fromCPU
  aplic_toIMSIC := aplic.toIMSIC

  lazy val module = new LazyModuleImp(this) {
    imsic_fromCPU.makeIOs()(ValName("intfile"))
    val toCSR = IO(Output(chiselTypeOf(imsic.module.toCSR)))
    val fromCSR = IO(Input(chiselTypeOf(imsic.module.fromCSR)))
    toCSR   <> imsic.module.toCSR
    fromCSR <> imsic.module.fromCSR

    aplic_fromCPU.makeIOs()(ValName("domain"))
    aplic_toIMSIC.makeIOs()(ValName("toimsic"))
    val intSrcs = IO(Input(chiselTypeOf(aplic.module.intSrcs)))
    intSrcs <> aplic.module.intSrcs
  }
}

/**
 * Generate Verilog sources
 */
object OpenAIA extends App {
  val top = DisableMonitors(p => LazyModule(
    new OpenAIA()(Parameters.empty))
  )(Parameters.empty)

  ChiselStage.emitSystemVerilog(
    top.module,
    args = Array("--dump-fir"),
    // more opts see: $CHISEL_FIRTOOL_PATH/firtool -h
    firtoolOpts = Array(
      "-disable-all-randomization",
      "-strip-debug-info",
      // without this, firtool will exit with error: Unhandled annotation
      "--disable-annotation-unknown",
      "--lowering-options=explicitBitcast,disallowLocalVariables,disallowPortDeclSharing,locationInfoStyle=none",
      "--split-verilog", "-o=gen",
    )
  )
}
