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

class TLAPLIC(
  params: APLICParams,
  imsic_params: IMSICParams,
  beatBytes: Int = 8, // TODO: remove? and IMSIC's beatBytes
)(implicit p: Parameters) extends LazyModule {

class Domain(
  baseAddr: Long, // base address for this aplic domain
  imsicBaseAddr: Long, // base address for imsic's interrupt files
  imsicMemberStrideWidth: Int, // C, D: stride between each interrupt files
  imsicGeilen: Int, // number of guest interrupt files, it is 0 for machine-level domain
)(implicit p: Parameters) extends LazyModule {
  val fromCPU = TLRegisterNode(
    address = Seq(AddressSet(baseAddr, pow2(params.domainMemWidth)-1)),
    device = new SimpleDevice(
      "interrupt-controller",
      Seq(f"riscv,aplic,0x${baseAddr}%x")
    ),
    beatBytes = beatBytes,
    undefZero = true,
    concurrency = 1,
  )
  val toIMSIC = TLClientNode(Seq(TLMasterPortParameters.v1(
    Seq(TLMasterParameters.v1("toimsic", IdRange(0,16))),
  )))

  lazy val module = new Imp
  class Imp extends LazyModuleImp(this) {
    val intSrcs = IO(Input(Vec(params.intSrcNum, Bool())))
    val intSrcsDelegated = IO(Output(Vec(params.intSrcNum, Bool())))

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
      private val regs = RegInit(VecInit.fill(params.intSrcNum){0.U(/*D*/1.W + /*ChildIndex, SM*/10.W)})
      class SourcecfgMeta(reg: UInt) {
        val D          = reg(10)
        val ChildIndex = reg(9,0)
        val SM         = reg(2,0)
        val List(inactive, detached, reserved2, reserved3, edge1, edge0, level1, level0) = Enum(8)
        val r = RegReadFn( D<<10 | Mux(D, ChildIndex, SM) )
        // TODO: setting sourcecfg inactive will clear corresponding ip and ie
        // * If source is changed from inactive to an active mode, the interrupt source’s pending and enable bits remain zeros, unless set automatically for a reason specified later in this section or in Section 4.7.
        // * A write to a sourcecfg register will not by itself cause a pending bit to be cleared except when the source is made inactive.
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
        def is_active(): Bool = ~D && SM=/=inactive
      }
      def apply(i: UInt) = new SourcecfgMeta(regs(i-1.U))
      def toSeq = regs.map (new SourcecfgMeta(_))
      val actives = Wire(Vec(params.ixNum, UInt(32.W)))
      val activeBools = Wire(Vec(params.intSrcNum, Bool()))
      locally {
        val activeBoolsSeq = toSeq.map(_.is_active())
        activeBools.zipWithIndex.map { case (activeBool: Bool, i: Int) => {
          activeBool := activeBoolsSeq(i)
        }}
        val activeBoolsSeq1 = false.B +: activeBoolsSeq
        actives.zipWithIndex.map{ case (active: UInt, i: Int) => {
          active := Cat(activeBoolsSeq1.slice(i*32, (i+1)*32).reverse)
        }}
      }
      val DBools = Wire(Vec(params.intSrcNum, Bool()))
      (DBools zip toSeq).map {case (d:Bool, s:SourcecfgMeta) => d:=s.D}
    }
    class IXs extends Bundle {
      private val regs = RegInit(VecInit.fill(params.ixNum){0.U(32.W)})
      class IxMeta(reg: UInt, active: UInt, bit0ReadOnlyZero: Boolean) {
        def r32() = reg & active
        def w32(d32: UInt) = {
          if (bit0ReadOnlyZero) reg := d32 & active & ~1.U(reg.getWidth.W)
          else                  reg := d32 & active
        }
      }
      def apply(i: UInt) = new IxMeta(regs(i), sourcecfgs.actives(i), i==0)
      def toSeq = regs.zipWithIndex.map { case (reg:UInt, i:Int) => new IxMeta(reg, sourcecfgs.actives(i), i==0) }
      def toBools = Cat(regs.reverse).asBools
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
        when (valid && data =/= 0.U && data <= params.intSrcNum.U) {
          val index = data(9,5); val offset = data(4,0); val ix = ixs(index)
          ix.w32(ix.r32() | UIntToOH(offset))
        }; true.B
      })
    }
    val setipnum = new Setixnum(ips)
    val intSrcsRectified = Wire(Vec(params.intSrcNum, Bool()))
    object in_clrips {
      private val intSrcsRectified32 = Wire(Vec(pow2(params.intSrcWidth-5).toInt, UInt(32.W)))
      private val intSrcsRectified_0 = false.B +: intSrcsRectified
      intSrcsRectified32.zipWithIndex.map { case (rect32:UInt, i:Int) => {
        rect32 := Cat(intSrcsRectified_0.slice(i*32, i*32+32).reverse)
      }}
      class In_clripMeta(ip: ips.IxMeta, rects: UInt) {
        val r = RegReadFn(rects)
        val w = RegWriteFn((valid, data) => {
          when (valid) { ip.w32( ip.r32() & ~data ) }; true.B
        })
      }
      def apply(i: UInt) = new In_clripMeta(ips(i), intSrcsRectified32(i))
      def toSeq = ips.toSeq.zipWithIndex.map { case (ip:ips.IxMeta, i:Int) => new In_clripMeta(ip, intSrcsRectified32(i)) }
    }
    class Clrixnum(ixs: IXs) {
      val r = RegReadFn(0.U(32.W)) // read zeros
      val w = RegWriteFn((valid, data) => {
        when (valid && data =/= 0.U && data <= params.intSrcNum.U) {
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
    val targets = new Bundle {
      private val regs = RegInit(VecInit.fill(params.intSrcNum){0.U(32.W)})
      class TargetMeta(reg: UInt, active: Bool) {
        val HartIndex  = reg(31,18) // TODO: parameterization: width of (groupsNum + membersNum)
        val GuestIndex = reg(17,12) // TODO: parameterization: width of imsicGeilen
        val EIID       = reg(10,0) // TODO: parameterization: intSrcWidth
        // TODO: For a machine-level domain, Guest Index is read-only zeros
        val r = RegReadFn(Mux(active, reg, 0.U))
        val w = RegWriteFn((valid, data) => {
          when (valid && active) { reg := data }; true.B
        })
      }
      def apply(i: UInt) = new TargetMeta(regs(i-1.U), sourcecfgs.activeBools(i-1.U))
      def toSeq = (regs zip sourcecfgs.activeBools).map {
        case (reg:UInt, activeBool:UInt) => new TargetMeta(reg, activeBool)
      }
    }

    fromCPU.regmap(
      0x0000            -> Seq(RegField(32, domaincfg.r, domaincfg.w)),
      0x0004/*~0x0FFC*/ -> sourcecfgs.toSeq.map(sourcecfg => RegField(32, sourcecfg.r, sourcecfg.w)),
      0x1BC4            -> Seq(RegField(32, 0x80000000L.U, RegWriteFn(():Unit))), // hardwired *msiaddrcfg* regs
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

    val intSrcsSynced = RegNextN(intSrcs, 3)
    // TODO: For level sensitive intSrc:
    //       The pending bit may also be set by a relevant write to a setip or setipnum register when the rectified input value is high, but not when the rectified input value is low.
    (intSrcsRectified zip (intSrcsSynced zip sourcecfgs.toSeq)).map {
      case (rect, (intSrc, sourcecfg)) => {
        when      (sourcecfg.SM===sourcecfg.edge1 || sourcecfg.SM===sourcecfg.level1) {
          rect := intSrc
        }.elsewhen(sourcecfg.SM===sourcecfg.edge0 || sourcecfg.SM===sourcecfg.level0) {
          rect := !intSrc
        }.otherwise {
          rect := false.B
        }
      }
    }
    val intSrcsTriggered = Wire(Vec(params.intSrcNum, Bool())); /*for debug*/dontTouch(intSrcsTriggered)
    (intSrcsTriggered zip intSrcsRectified).map { case (trigger, rect) => {
      trigger := rect && !RegNext(rect)
    }}
    // TODO: may compete with mem mapped reg, thus causing lost info
    intSrcsTriggered.zipWithIndex.map { case (trigger:Bool, i:Int) =>
      when(trigger) {setipnum.w.fn(true.B, true.B, (i+1).U)}
    }

    // The ":+ true.B" trick explain:
    //  Append true.B to handle the cornor case, where all bits in ip and ie are disabled.
    //  If do not append true.B, then we need to check whether the ip & ie are empty,
    //  otherwise, the returned topei will become the max index, that is 2^aplicIntSrcWidth-1
    //  [0,     2^aplicIntSrcWidth-1] :+ 2^aplicIntSrcWidth
    val topi = Wire(UInt(params.intSrcWidth.W)); /*for debug*/dontTouch(topi)
    topi := ParallelPriorityMux((
      (ips.toBools:+true.B) zip (ies.toBools:+true.B)
    ).zipWithIndex.map {
      case ((p: Bool, e: Bool), i: Int) => (p & e, i.U)
    })
    // TODO: handle ready signal!
    // send MSI
    locally {
      val (tl, edge) = toIMSIC.out(0)
      when (domaincfg.IE && topi=/=0.U) {
        // It is recommended to hardwire *msiaddrcfg* by the manual:
        // "For any given system, these addresses are fixed and should be hardwired into the APLIC if possible."
        val target = targets(topi)
        val groupID = target.HartIndex(imsic_params.groupsWidth+imsic_params.membersWidth-1, imsic_params.membersWidth)
        val memberID = target.HartIndex(imsic_params.membersWidth-1, 0)
        val guestID = target.GuestIndex
        val msiAddr = imsicBaseAddr.U |
                      (groupID<<imsic_params.groupStrideWidth) |
                      (memberID<<imsicMemberStrideWidth) |
                      (guestID<<imsic_params.intFileMemWidth)
        val (_, pfbits) = edge.Put(0.U, msiAddr, 2.U, targets(topi).EIID)
        // clear corresponding ip
        // TODO: may compete with mem mapped reg, thus causing lost info
        clripnum.w.fn(true.B, true.B, topi)
        tl.a.bits := pfbits
        tl.a.valid := true.B
      }.otherwise {
        tl.a.valid := false.B
      }
    }

    // delegate
    intSrcsDelegated := (sourcecfgs.DBools zip intSrcs).map {case (d:Bool, i:Bool) => d&i}
  }
}

  val mDomain = LazyModule(new Domain(
    params.baseAddr,
    imsic_params.mBaseAddr,
    imsic_params.mStrideWidth,
    0,
  ))
  val sgDomain = LazyModule(new Domain(
    params.baseAddr + pow2(params.domainMemWidth),
    imsic_params.sgBaseAddr,
    imsic_params.sgStrideWidth,
    imsic_params.geilen,
  ))
  val fromCPU = LazyModule(new TLXbar).node
  val toIMSIC = LazyModule(new TLXbar).node
  mDomain.fromCPU := fromCPU
  sgDomain.fromCPU := fromCPU
  toIMSIC := mDomain.toIMSIC
  toIMSIC := sgDomain.toIMSIC

  lazy val module = new Imp
  class Imp extends LazyModuleImp(this) {
    val intSrcs = IO(Input(Vec(params.intSrcNum, Bool())))

    mDomain.module.intSrcs := intSrcs
    sgDomain.module.intSrcs := mDomain.module.intSrcsDelegated
  }
}
