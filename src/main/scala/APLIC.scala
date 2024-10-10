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
import freechips.rocketchip.regmapper._
import xs.utils._

class TLAPLIC()(implicit p: Parameters) extends LazyModule {
  val node = TLRegisterNode(
    address = Seq(AddressSet(0x19960000L, 0x4000-1)), // TODO: parameterization
    device = new SimpleDevice(
      "interrupt-controller",
      Seq(f"riscv,aplic")
    ),
    beatBytes = 8, // TODO: parameterization
    undefZero = true,
    concurrency = 1,
  )

  lazy val module = new Imp
  class Imp extends LazyModuleImp(this) {
    val domaincfg = new Bundle {
      val high = 0x80.U(8.W)
      val IE   = RegInit(false.B)
      val DM   = true.B // only support MSI delivery mode
      val BE   = false.B // only support little endian
      val r = RegReadFn( high<<24 | IE<<8 | DM<<2 | BE )
      val w = RegWriteFn((valid, data) => {
        when (valid) { IE := data(8) }; true.B
      })
    }
    val sourcecfgs   = RegInit(VecInit.fill(1023){0.U(32.W)}) // TODO: parameterization
    val mmsiaddrcfg  = RegInit(0.U(32.W))
    val mmsiaddrcfgh = RegInit(0.U(32.W))
    val smsiaddrcfg  = RegInit(0.U(32.W))
    val smsiaddrcfgh = RegInit(0.U(32.W))
    val setips       = RegInit(VecInit.fill(32){0.U(32.W)}) // TODO: parameterization
    val setipnum     = RegInit(0.U(32.W))
    val in_clrips    = RegInit(VecInit.fill(32){0.U(32.W)}) // TODO: parameterization
    val clripnum     = RegInit(0.U(32.W))
    val seties       = RegInit(VecInit.fill(32){0.U(32.W)}) // TODO: parameterization
    val setienum     = RegInit(0.U(32.W))
    val clries       = RegInit(VecInit.fill(32){0.U(32.W)}) // TODO: parameterization
    val clrienum     = RegInit(0.U(32.W))
    val setipnum_le  = RegInit(0.U(32.W))
    val setipnum_be  = RegInit(0.U(32.W))
    val genmsi       = RegInit(0.U(32.W))
    val targets      = RegInit(VecInit.fill(1023){0.U(32.W)}) // TODO: parameterization
    node.regmap(
      0x0000            -> Seq(RegField(32, domaincfg.r, domaincfg.w)),
      0x0004/*~0x0FFC*/ -> sourcecfgs.map(RegField(32, _)),
      0x1BC0            -> Seq(RegField(32, mmsiaddrcfg)),
      0x1BC4            -> Seq(RegField(32, mmsiaddrcfgh)),
      0x1BC8            -> Seq(RegField(32, smsiaddrcfg)),
      0x1BCC            -> Seq(RegField(32, smsiaddrcfgh)),
      0x1C00/*~0x1C7C*/ -> setips.map(RegField(32, _)),
      0x1CDC            -> Seq(RegField(32, setipnum)),
      0x1D00/*~0x1D7C*/ -> in_clrips.map(RegField(32, _)),
      0x1DDC            -> Seq(RegField(32, clripnum)),
      0x1E00/*~0x1F7C*/ -> seties.map(RegField(32, _)),
      0x1EDC            -> Seq(RegField(32, setienum)),
      0x1F00/*~0x1F7C*/ -> clries.map(RegField(32, _)),
      0x1FDC            -> Seq(RegField(32, clrienum)),
      0x2000            -> Seq(RegField(32, setipnum_le)),
      0x2004            -> Seq(RegField(32, setipnum_be)),
      0x3000            -> Seq(RegField(32, genmsi)),
      0x3004/*~0x3FFC*/ -> targets.map(RegField(32, _)),
    )
  }
}
