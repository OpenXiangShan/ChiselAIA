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
  val mTLCNode = TLClientNode(
    Seq(TLMasterPortParameters.v1(
      Seq(TLMasterParameters.v1("m_tl", IdRange(0, 16)))
  )))
  val sgTLCNode = TLClientNode(
    Seq(TLMasterPortParameters.v1(
      Seq(TLMasterParameters.v1("sg_tl", IdRange(0, 16)))
  )))

  val imsic_params = IMSICParams()
  val aplic_params = APLICParams()

  val imsic = LazyModule(new TLIMSIC(imsic_params)(Parameters.empty))
  imsic.mTLNode := mTLCNode
  imsic.sgTLNode := sgTLCNode

  val aplicTLCNode = TLClientNode(
    Seq(TLMasterPortParameters.v1(
      Seq(TLMasterParameters.v1("aplic_tl", IdRange(0, 16)))
  )))
  val aplicTLMNode = TLManagerNode(Seq(TLSlavePortParameters.v1(
    Seq(TLSlaveParameters.v1(
      // TODO: parameterization
      address = Seq(
        AddressSet(imsic_params.mBaseAddr, pow2(imsic_params.groupStrideWidth + imsic_params.groupsWidth)-1),
        AddressSet(imsic_params.sgBaseAddr,pow2(imsic_params.groupStrideWidth + imsic_params.groupsWidth)-1),
      ), // TODO
      supportsPutFull = TransferSizes(1, 8),
    )),
    beatBytes = 8
  )))
  val aplic = LazyModule(new TLAPLIC(aplic_params, imsic_params)(Parameters.empty))
  aplic.node := aplicTLCNode
  aplicTLMNode := aplic.toIMSIC

  lazy val module = new LazyModuleImp(this) {
    mTLCNode.makeIOs()(ValName("m"))
    sgTLCNode.makeIOs()(ValName("sg"))
    val toCSR = IO(Output(chiselTypeOf(imsic.module.toCSR)))
    val fromCSR = IO(Input(chiselTypeOf(imsic.module.fromCSR)))
    toCSR   <> imsic.module.toCSR
    fromCSR <> imsic.module.fromCSR

    dontTouch(imsic.module.toCSR)
    dontTouch(imsic.module.fromCSR)

    aplicTLCNode.makeIOs()(ValName("domain"))
    val intSrcs = IO(Input(chiselTypeOf(aplic.module.intSrcs)))
    intSrcs <> aplic.module.intSrcs
    dontTouch(aplic.module.intSrcs)
    aplicTLMNode.makeIOs()(ValName("toimsic"))
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
