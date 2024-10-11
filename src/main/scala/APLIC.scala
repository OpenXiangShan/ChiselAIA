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
    class Sourcecfg extends Bundle {
      val D = RegInit(false.B)
      val ChildIndex = RegInit(0.U(10.W))
      val SM        = RegInit(0.U(3.W))
      val List(inactive, detached, reserved2, reserved3, edge1, edge0, level1, level0) = Enum(8)
      val r = RegReadFn( D<<10 | Mux(D, ChildIndex, SM) )
      val w = RegWriteFn((valid, data) => {
        val i_D=data(10); val i_ChildIndex=data(9,0); val i_SM=data(2,0)
        when (valid) {
          D := i_D
          when (i_D) {
            ChildIndex := i_ChildIndex
          }.otherwise {
            SM := Mux(i_SM===reserved2 || i_SM===reserved3, inactive, i_SM)
          }
        }; true.B
      })
    }
    val sourcecfgs   = (0 until 1023).map {_=>new Sourcecfg} // TODO: parameterization
    val mmsiaddrcfg  = new Bundle {
      val Low_Base_PPN  = RegInit(0.U(32.W))
      val L             = RegInit(false.B)
      val HHXS          = RegInit(0.U(5.W))
      val LHXS          = RegInit(0.U(3.W))
      val HHXW          = RegInit(0.U(3.W))
      val LHXW          = RegInit(0.U(4.W))
      val High_Base_PPN = RegInit(0.U(12.W))
      val rl = RegReadFn(Low_Base_PPN)
      val wl = RegWriteFn((valid, data) => { when (valid && ~L) { Low_Base_PPN := data}; true.B })
      val rh = RegReadFn( L<<31 | HHXS<<24 | LHXS<<20 | HHXW<<16 | LHXW<<12 | High_Base_PPN )
      val wh = RegWriteFn((valid, data) => {
        when (valid && ~L) {
          L             := data(31)
          HHXS          := data(28,24)
          LHXS          := data(22,20)
          HHXW          := data(18,16)
          LHXW          := data(15,12)
          High_Base_PPN := data(11,0)
        }; true.B
      })
    }
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
      0x0004/*~0x0FFC*/ -> sourcecfgs.map(sourcecfg => RegField(32, sourcecfg.r, sourcecfg.w)),
      0x1BC0            -> Seq(RegField(32, mmsiaddrcfg.rl, mmsiaddrcfg.wl)),
      0x1BC4            -> Seq(RegField(32, mmsiaddrcfg.rh, mmsiaddrcfg.wh)),
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
