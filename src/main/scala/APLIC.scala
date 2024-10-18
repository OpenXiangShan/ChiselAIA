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
      val r = high<<24 | IE<<8 | DM<<2 | BE
      def w(data:UInt):Unit = IE := data(8)
    }
    val sourcecfgs = new Bundle {
      val List(inactive, detached, reserved2, reserved3, edge1, edge0, level1, level0) = Enum(8)
      class Sourcecfg extends Bundle {
        val D = Bool()
        // Currently only support one machine-level domain and one supervisor-level domain,
        // therefore, ChildIndex is not needed.
        val SM = UInt(3.W)
      }
      private val regs = RegInit(VecInit.fill(params.intSrcNum)(0.U.asTypeOf(new Sourcecfg)))
      def rI(i:Int): UInt = regs(i).D<<10 | Mux(regs(i).D, 0.U, regs(i).SM)
      def wI(i:Int, data:UInt): Unit = {
        val D=data(10); val SM=data(2,0)
        regs(i).D := D
        regs(i).SM := Mux(D, 0.U, Mux(SM===reserved2||SM===reserved3, inactive, SM))
      }
      val actives = VecInit(regs.map(reg => ~reg.D && reg.SM=/=inactive))
      val Ds = VecInit(regs.map(_.D))
      val SMs = VecInit(regs.map(_.SM))
    }
    class IXs extends Bundle {
      private val bits = RegInit(VecInit.fill(params.intSrcNum){false.B})
      val bits0 = VecInit(false.B +: bits.drop(1)) // bits0(0) is read-only 0
      // TODO: parameterization: 32, 5
      def r32I(i:Int): UInt = {
        val tmp = (0 until 32).map(j => {
          val index = i<<5|j
          bits0(index) & sourcecfgs.actives(index)
        })
        Cat(tmp.reverse)
      }
      def w32I(i:Int, d32:UInt): Unit = {
        (0 until 32).map(j => {
          val index = i<<5|j
          when (sourcecfgs.actives(index)) {bits(index):=d32(j)}
        })
      }
      def rBitUI(ui:UInt): Bool = bits0(ui) & sourcecfgs.actives(ui)
      def rBitI(i:Int):    Bool = bits0(i)  & sourcecfgs.actives(i)
      def wBitUI(ui:UInt, bit:Bool): Unit = when (sourcecfgs.actives(ui)) {bits(ui):=bit}
      def wBitI(i:Int, bit:Bool):    Unit = when (sourcecfgs.actives(i))  {bits(i):=bit}
    }
    val ips = new IXs // internal regs
    val intSrcsRectified = Wire(Vec(params.intSrcNum, Bool()))
    // TODO: move it to locally
    val intSrcsRectified32 = Wire(Vec(pow2(params.intSrcWidth-5).toInt, UInt(32.W)))
    intSrcsRectified32.zipWithIndex.map { case (rect32:UInt, i:Int) => {
      rect32 := Cat(intSrcsRectified.slice(i*32, i*32+32).reverse)
    }}
    val ies = new IXs // internal regs
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
      def apply(i: UInt) = new TargetMeta(regs(i), sourcecfgs.actives(i))
      def toSeq = (regs zip sourcecfgs.actives).map {
        case (reg:UInt, activeBool:UInt) => new TargetMeta(reg, activeBool)
      }
    }

    locally {
      def RWF_setixs(i:Int, ixs:IXs) = RegWriteFn((valid, data) => {
        when(valid) {ixs.w32I(i, data)}; true.B })
      // TODO: The pending
      // bit may also be set by a relevant write to a setip or setipnum register when the rectified input
      // value is high, but not when the rectified input value is low.
      def RWF_setipnum = RegWriteFn((valid, data) => {
        when (valid && data=/=0.U) { ips.wBitUI(data(params.intSrcWidth-1,0), true.B) }; true.B })
      def RWF_setclrixnum(setclr:Bool, ixs:IXs) = RegWriteFn((valid, data) => {
        when (valid && data=/=0.U) { ixs.wBitUI(data(params.intSrcWidth-1,0), setclr) }; true.B })
      def RWF_clrixs(i:Int, ixs:IXs) = RegWriteFn((valid, data) => {
        when (valid) { ixs.w32I(i, ixs.r32I(i) & ~data) }; true.B })
      fromCPU.regmap(
        /*domaincfg*/   0x0000 -> Seq(RegField(32, domaincfg.r, RegWriteFn((v, d)=>{ when(v){domaincfg.w(d)}; true.B }))),
        /*sourcecfgs*/  0x0004 -> (1 until params.intSrcNum).map(i => RegField(32, sourcecfgs.rI(i),
          RegWriteFn((v, d)=>{ when(v){sourcecfgs.wI(i, d)}; true.B }))),
        0x1BC4            -> Seq(RegField(32, 0x80000000L.U, RegWriteFn(():Unit))), // hardwired *msiaddrcfg* regs
        /*setips*/      0x1C00 -> (0 until params.ixNum).map(i => RegField(32, ips.r32I(i), RWF_setixs(i, ips))),
        /*setipnum*/    0x1CDC -> Seq(RegField(32, 0.U, RWF_setipnum)),
        /*in_clrips*/   0x1D00 -> (0 until params.ixNum).map(i => RegField(32, intSrcsRectified32(i), RWF_clrixs(i, ips))),
        /*clripnum*/    0x1DDC -> Seq(RegField(32, 0.U, RWF_setclrixnum(false.B, ips))),
        /*seties*/      0x1E00 -> (0 until params.ixNum).map(i => RegField(32, ies.r32I(i), RWF_setixs(i, ies))),
        /*setienum*/    0x1EDC -> Seq(RegField(32, 0.U, RWF_setclrixnum(true.B, ies))),
        /*clries*/      0x1F00 -> (0 until params.ixNum).map(i => RegField(32, 0.U, RWF_clrixs(i, ies))),
        /*clrienum*/    0x1FDC -> Seq(RegField(32, 0.U, RWF_setclrixnum(false.B, ies))),
        /*setipnum_le*/ 0x2000 -> Seq(RegField(32, 0.U, RWF_setipnum)),
        0x2004            -> Seq(RegField(32, 0.U(32.W), RegWriteFn(():Unit))), // setipnum_be not implemented
        0x3000            -> Seq(RegField(32, genmsi)),
        0x3004/*~0x3FFC*/ -> targets.toSeq.drop(1).map( target => RegField(32, target.r, target.w)),
      )
    }

    val intSrcsSynced = RegNextN(intSrcs, 3)
    // TODO: For level sensitive intSrc:
    //       The pending bit may also be set by a relevant write to a setip or setipnum register when the rectified input value is high, but not when the rectified input value is low.
    intSrcsRectified(0) := false.B
    (1 until params.intSrcNum).map(i => {
      val (rect, sync, sm) = (intSrcsRectified(i), intSrcsSynced(i), sourcecfgs.SMs(i))
      when      (sm===sourcecfgs.edge1 || sm===sourcecfgs.level1) {
        rect := sync
      }.elsewhen(sm===sourcecfgs.edge0 || sm===sourcecfgs.level0) {
        rect := !sync
      }.otherwise {
        rect := false.B
      }
    })
    val intSrcsTriggered = Wire(Vec(params.intSrcNum, Bool())); /*for debug*/dontTouch(intSrcsTriggered)
    (intSrcsTriggered zip intSrcsRectified).map { case (trigger, rect) => {
      trigger := rect && !RegNext(rect)
    }}
    // TODO: may compete with mem mapped reg, thus causing lost info
    intSrcsTriggered.zipWithIndex.map { case (trigger:Bool, i:Int) =>
      when(trigger) {ips.wBitI(i, true.B)}
    }

    // The ":+ true.B" trick explain:
    //  Append true.B to handle the cornor case, where all bits in ip and ie are disabled.
    //  If do not append true.B, then we need to check whether the ip & ie are empty,
    //  otherwise, the returned topei will become the max index, that is 2^aplicIntSrcWidth-1
    //  [0,     2^aplicIntSrcWidth-1] :+ 2^aplicIntSrcWidth
    val topi = Wire(UInt(params.intSrcWidth.W)); /*for debug*/dontTouch(topi)
    topi := ParallelPriorityMux((
      (ips.bits0:+true.B) zip (ies.bits0:+true.B)
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
        ips.wBitUI(topi, false.B)
        tl.a.bits := pfbits
        tl.a.valid := true.B
      }.otherwise {
        tl.a.valid := false.B
      }
    }

    // delegate
    intSrcsDelegated := (sourcecfgs.Ds zip intSrcs).map {case (d:Bool, i:Bool) => d&i}
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
