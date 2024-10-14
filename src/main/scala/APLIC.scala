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
    val sourcecfgs = new Bundle {
      private val regs = RegInit(VecInit.fill(1023){0.U(/*D*/1.W + /*ChildIndex, SM*/10.W)}) // TODO: parameterization
      class SourcecfgMeta(reg: UInt) {
        val D          = reg(10)
        val ChildIndex = reg(9,0)
        val SM         = reg(2,0)
        val List(inactive, detached, reserved2, reserved3, edge1, edge0, level1, level0) = Enum(8)
        val r = RegReadFn( D<<10 | Mux(D, ChildIndex, SM) )
        val w = RegWriteFn((valid, data) => {
          val i_D=data(10); val i_ChildIndex=data(9,0); val i_SM=data(2,0)
          when (valid) {
            reg := i_D<<10 | Mux(
              i_D,
              i_ChildIndex,
              Mux(i_SM===reserved2 || i_SM===reserved3, inactive, i_SM),
            )
          }; true.B
        })
        def is_active(): Bool = D || (~D && SM=/=inactive)
      }
      def apply(i: UInt) = new SourcecfgMeta(regs(i-1.U))
      def toSeq = regs.map (new SourcecfgMeta(_))
      val actives = Wire(Vec(32, UInt(32.W)))
      dontTouch(actives) // TODO: remove: for debug
      locally {
        val activeBools = false.B +: toSeq.map(_.is_active())
        actives.zipWithIndex.map{ case (active: UInt, i: Int) => {
          active := Cat(activeBools.slice(i*32, (i+1)*32).reverse)
        }}
      }
    }
    val msiaddrcfg = new Bundle {
      val L             = RegInit(false.B)
      val m = new Bundle {
        val Low_Base_PPN  = RegInit(0.U(32.W))
        val HHXS          = RegInit(0.U(5.W))
        val LHXS          = RegInit(0.U(3.W))
        val HHXW          = RegInit(0.U(3.W))
        val LHXW          = RegInit(0.U(4.W))
        val High_Base_PPN = RegInit(0.U(12.W))
        val lr = RegReadFn(Low_Base_PPN)
        val lw = RegWriteFn((valid, data) => { when (valid && ~L) { Low_Base_PPN := data}; true.B })
        val hr = RegReadFn( L<<31 | HHXS<<24 | LHXS<<20 | HHXW<<16 | LHXW<<12 | High_Base_PPN )
        val hw = RegWriteFn((valid, data) => {
          when (valid) {
            L := data(31)
            when (~L) {
              HHXS          := data(28,24)
              LHXS          := data(22,20)
              HHXW          := data(18,16)
              LHXW          := data(15,12)
              High_Base_PPN := data(11,0)
            }
          }; true.B
        })
      }
      val s = new Bundle {
        val Low_Base_PPN  = RegInit(0.U(32.W))
        val LHXS          = RegInit(0.U(3.W))
        val High_Base_PPN = RegInit(0.U(12.W))
        val lr = RegReadFn(Low_Base_PPN)
        val lw = RegWriteFn((valid, data) => { when (valid && ~L) { Low_Base_PPN := data}; true.B })
        val hr = RegReadFn( LHXS<<20 | High_Base_PPN )
        val hw = RegWriteFn((valid, data) => {
          when (valid && ~L) {
            LHXS          := data(22,20)
            High_Base_PPN := data(11,0)
          }; true.B
        })
      }
    }
    class IXs extends Bundle {
      // TODO: parameterization
      private val regs = RegInit(VecInit.fill(32){0.U(32.W)})
      class IxMeta(reg: UInt, active: UInt, bit0ReadOnlyZero: Boolean) {
        def r32() = reg
        def w32(d32: UInt) = {
          if (bit0ReadOnlyZero) reg := d32 & active & ~1.U(reg.getWidth.W)
          else                  reg := d32 & active
        }
      }
      def apply(i: UInt) = new IxMeta(regs(i), sourcecfgs.actives(i), i==0)
      def toSeq = regs.zipWithIndex.map { case (reg:UInt, i:Int) => new IxMeta(reg, sourcecfgs.actives(i), i==0) }
    }
    class Setixs(ixs: IXs) {
      class SetixMeta(ix: ixs.IxMeta) {
        val r = RegReadFn(ix.r32())
        val w = RegWriteFn((valid, data) => { when(valid) {ix.w32(data)}; true.B })
      }
      def apply(i: UInt) = new SetixMeta(ixs(i))
      def toSeq = ixs.toSeq.map( ix => new SetixMeta(ix) )
    }
    val ips = new IXs // internal regs
    // TODO: The pending
    // bit may also be set by a relevant write to a setip or setipnum register when the rectified input
    // value is high, but not when the rectified input value is low.
    val setips = new Setixs(ips)
    class Setixnum(ixs: IXs) {
      val r = RegReadFn(0.U(32.W)) // read zeros
      val w = RegWriteFn((valid, data) => {
        when (valid && data =/= 0.U && data <= 1023.U) { // TODO: parameterization
          val index = data(9,5); val offset = data(4,0); val ix = ixs(index)
          ix.w32(ix.r32() | UIntToOH(offset))
        }; true.B
      })
    }
    val setipnum = new Setixnum(ips)
    object in_clrips {
      class In_clripMeta(ip: ips.IxMeta) {
        val r = RegReadFn(0.U(32.W)) // TODO: returns the rectified input
        val w = RegWriteFn((valid, data) => {
          when (valid) { ip.w32( ip.r32() & ~data ) }; true.B
        })
      }
      def apply(i: UInt) = new In_clripMeta(ips(i))
      def toSeq = ips.toSeq.map ( ip => new In_clripMeta(ip) )
    }
    class Clrixnum(ixs: IXs) {
      val r = RegReadFn(0.U(32.W)) // read zeros
      val w = RegWriteFn((valid, data) => {
        when (valid && data =/= 0.U && data <= 1023.U) { // TODO: parameterization
          val index = data(9,5); val offset = data(4,0); val ix = ixs(index)
          ix.w32(ix.r32() & ~UIntToOH(offset))
        }; true.B
      })
    }
    val clripnum = new Clrixnum(ips)
    val ies = new IXs // internal regs
    val seties = new Setixs(ies)
    val setienum = new Setixnum(ies)
    object clries {
      class ClrieMeta(ie: ies.IxMeta) {
        val r = RegReadFn(0.U(32.W))
        val w = RegWriteFn((valid, data) => {
          when (valid) { ie.w32( ie.r32() & ~data ) }; true.B
        })
      }
      def apply(i: UInt) = new ClrieMeta(ies(i))
      def toSeq = ies.toSeq.map ( ie => new ClrieMeta(ie) )
    }
    val clrienum = new Clrixnum(ies)
    val genmsi       = RegInit(0.U(32.W)) // TODO: implement
    // TODO: parameterization
    val targets = new Bundle {
      private val regs = RegInit(VecInit.fill(1023){0.U(32.W)})
      class TargetMeta(reg: UInt) {
        val HartIndex  = reg(31,18)
        val GuestIndex = reg(17,12)
        val EIID       = reg(10,0)
        // TODO: For a machine-level domain, Guest Index is read-only zeros
        val r = RegReadFn(reg)
        val w = RegWriteFn(reg)
      }
      def apply(i: UInt) = new TargetMeta(regs(i-1.U))
      def toSeq = regs.map (new TargetMeta(_))
    }

    node.regmap(
      0x0000            -> Seq(RegField(32, domaincfg.r, domaincfg.w)),
      0x0004/*~0x0FFC*/ -> sourcecfgs.toSeq.map(sourcecfg => RegField(32, sourcecfg.r, sourcecfg.w)),
      0x1BC0            -> Seq(RegField(32, msiaddrcfg.m.lr, msiaddrcfg.m.lw)),
      0x1BC4            -> Seq(RegField(32, msiaddrcfg.m.hr, msiaddrcfg.m.hw)),
      0x1BC8            -> Seq(RegField(32, msiaddrcfg.s.lr, msiaddrcfg.s.lw)),
      0x1BCC            -> Seq(RegField(32, msiaddrcfg.s.hr, msiaddrcfg.s.hw)),
      0x1C00/*~0x1C7C*/ -> setips.toSeq.map ( setip => RegField(32, setip.r, setip.w) ),
      0x1CDC            -> Seq(RegField(32, setipnum.r, setipnum.w)),
      0x1D00/*~0x1D7C*/ -> in_clrips.toSeq.map(in_clrip => RegField(32, in_clrip.r, in_clrip.w)),
      0x1DDC            -> Seq(RegField(32, clripnum.r, clripnum.w)),
      0x1E00/*~0x1F7C*/ -> seties.toSeq.map( setie => RegField(32, setie.r, setie.w) ),
      0x1EDC            -> Seq(RegField(32, setienum.r, setienum.w)),
      0x1F00/*~0x1F7C*/ -> clries.toSeq.map( clrie => RegField(32, clrie.r, clrie.w)),
      0x1FDC            -> Seq(RegField(32, clrienum.r, clrienum.w)),
      0x2000            -> Seq(RegField(32, setipnum.r ,setipnum.w)),
      0x2004            -> Seq(RegField(32, 0.U(32.W), RegWriteFn(():Unit))), // setipnum_be not implemented
      0x3000            -> Seq(RegField(32, genmsi)),
      0x3004/*~0x3FFC*/ -> targets.toSeq.map( target => RegField(32, target.r, target.w)),
    )
  }
}
