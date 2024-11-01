/***************************************************************************************
* Copyright (c) 2024 Beijing Institute of Open Source Chip (BOSC)
*
* ChiselAIA is licensed under Mulan PSL v2.
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
  beatBytes: Int = 8,
)(implicit p: Parameters) extends LazyModule {

class Domain(
  baseAddr: Long, // base address for this aplic domain
  imsicBaseAddr: Long, // base address for imsic's interrupt files
  imsicMemberStrideWidth: Int, // C, D: stride between each interrupt files
  imsicGeilen: Int, // number of guest interrupt files, it is 0 for machine-level domain
)(implicit p: Parameters) extends LazyModule {
  override lazy val desiredName = "Domain"
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
      val regs = RegInit(VecInit.fill(params.intSrcNum)(0.U.asTypeOf(new Sourcecfg)))
      def rI(i:Int): UInt = regs(i).D<<10 | Mux(regs(i).D, 0.U, regs(i).SM)
      def wI(i:Int, data:UInt): Unit = {
        val D=data(10); val SM=data(2,0)
        regs(i).D := D
        regs(i).SM := Mux(D, 0.U, Mux(SM===reserved2||SM===reserved3, inactive, SM))
      }
      val actives = VecInit(regs.map(reg => ~reg.D && reg.SM=/=inactive))
    }
    abstract class IXs extends Bundle {
      protected val bits = RegInit(VecInit.fill(params.intSrcNum){false.B})
      val bits0 = VecInit(false.B +: bits.drop(1)) // bits0(0) is read-only 0
      def r32I(i:Int): UInt = Cat((0 until 32).map(j => rBitUI((i<<log2Ceil(32)|j).U)).reverse)
      def w32I(i:Int, d32:UInt): Unit = (0 until 32).map(j => wBitUI((i<<log2Ceil(32)|j).U, d32(j)))
      def rBitUI(ui:UInt): Bool = bits0(ui) & sourcecfgs.actives(ui)
      def wBitUI(ui:UInt, bit:Bool): Unit
    }
    val ips = new IXs {
      def wBitUI(ui:UInt, bit:Bool): Unit = when (sourcecfgs.actives(ui)) {
        when (sourcecfgs.regs(ui).SM===sourcecfgs.level1 || sourcecfgs.regs(ui).SM===sourcecfgs.level0) {
          when (domaincfg.DM) {
            when (intSrcsRectified(ui)) { bits(ui):=bit }
          }.otherwise {/* Currently not support domaincfg.DM===0 */}
        }.otherwise { bits(ui):=bit }
      }
    }
    val intSrcsRectified = Wire(Vec(params.intSrcNum, Bool()))
    val ies = new IXs {
      def wBitUI(ui:UInt, bit:Bool): Unit = when (sourcecfgs.actives(ui)) {bits(ui):=bit}
    }
    val genmsi = new Bundle {
      val HartIndex = RegInit(0.U(params.groupsWidth.W + params.membersWidth.W))
      val Busy      = RegInit(false.B)
      val EIID      = RegInit(0.U(params.imsicIntSrcWidth.W))
      def r = Mux(domaincfg.DM, HartIndex<<18 | Busy<<12 | EIID, 0.U)
      def w(data:UInt) = {
        when (domaincfg.DM && ~Busy) { HartIndex:=data(31,18); Busy:=true.B; EIID:=data(10,0); }
      }
    }
    val targets = new Bundle {
      class Target extends Bundle {
        val HartIndex  = UInt(params.groupsWidth.W + params.membersWidth.W)
        val GuestIndex = UInt(if (imsicGeilen==0) 0.W else log2Ceil(imsicGeilen).W)
        val EIID       = UInt(params.imsicIntSrcWidth.W)
      }
      val regs = RegInit(VecInit.fill(params.intSrcNum){0.U.asTypeOf(new Target)})
      def rI(i:Int): UInt = Mux(sourcecfgs.actives(i),
        regs(i).HartIndex<<18 | regs(i).GuestIndex<<12 | regs(i).EIID, 0.U
      )
      def wI(i:Int, data:UInt): Unit = {
        when (sourcecfgs.actives(i)) {
          regs(i).HartIndex := data(31,18)
          regs(i).GuestIndex := data(17,12)
          regs(i).EIID := data(10,0)
      }}
    }

    // Writing ips priorities:
    // * 3st: regmapped regs: including setips, setipnum, in_clrips, clripnum
    // * 2nd: intSrcsTriggered, which sets the corresponding ip
    // * 1rd: send MSI, which cleans the corresponding ip
    // For more details about how ip-writing priority is achieved,
    // see FIRRTL Specification's chapter Conditional Last Connect Semantics.
    locally {
      val intSrcsRectified32 = Wire(Vec(pow2(params.aplicIntSrcWidth-5).toInt, UInt(32.W)))
      intSrcsRectified32.zipWithIndex.map { case (rect32:UInt, i:Int) => {
        rect32 := Cat(intSrcsRectified.slice(i*32, i*32+32).reverse)
      }}
      def RWF_setixs(i:Int, ixs:IXs) = RegWriteFn((valid, data) => {
        when(valid) {ixs.w32I(i, ixs.r32I(i) | data)}; true.B })
      def RWF_setipnum = RegWriteFn((valid, data) => {
        when (valid && data=/=0.U) { ips.wBitUI(data(params.aplicIntSrcWidth-1,0), true.B) }; true.B })
      def RWF_setclrixnum(setclr:Bool, ixs:IXs) = RegWriteFn((valid, data) => {
        when (valid && data=/=0.U) { ixs.wBitUI(data(params.aplicIntSrcWidth-1,0), setclr) }; true.B })
      def RWF_clrixs(i:Int, ixs:IXs) = RegWriteFn((valid, data) => {
        when (valid) { ixs.w32I(i, ixs.r32I(i) & ~data) }; true.B })
      fromCPU.regmap(
        /*domaincfg*/   0x0000 -> Seq(RegField(32, domaincfg.r, RegWriteFn((v, d)=>{ when(v){domaincfg.w(d)}; true.B }))),
        /*sourcecfgs*/  0x0004 -> (1 until params.intSrcNum).map(i => RegField(32, sourcecfgs.rI(i),
          RegWriteFn((v, d)=>{ when(v){sourcecfgs.wI(i, d)}; true.B }))),
        /*mmsiaddrcfgh*/0x1BC4 -> Seq(RegField(32, 0x80000000L.U, RegWriteFn(():Unit))), // hardwired *msiaddrcfg* regs
        /*setips*/      0x1C00 -> (0 until params.ixNum).map(i => RegField(32, ips.r32I(i), RWF_setixs(i, ips))),
        /*setipnum*/    0x1CDC -> Seq(RegField(32, 0.U, RWF_setipnum)),
        /*in_clrips*/   0x1D00 -> (0 until params.ixNum).map(i => RegField(32, intSrcsRectified32(i), RWF_clrixs(i, ips))),
        /*clripnum*/    0x1DDC -> Seq(RegField(32, 0.U, RWF_setclrixnum(false.B, ips))),
        /*seties*/      0x1E00 -> (0 until params.ixNum).map(i => RegField(32, ies.r32I(i), RWF_setixs(i, ies))),
        /*setienum*/    0x1EDC -> Seq(RegField(32, 0.U, RWF_setclrixnum(true.B, ies))),
        /*clries*/      0x1F00 -> (0 until params.ixNum).map(i => RegField(32, 0.U, RWF_clrixs(i, ies))),
        /*clrienum*/    0x1FDC -> Seq(RegField(32, 0.U, RWF_setclrixnum(false.B, ies))),
        /*setipnum_le*/ 0x2000 -> Seq(RegField(32, 0.U, RWF_setipnum)),
        /*setipnum_be*/ 0x2004 -> Seq(RegField(32, 0.U, RegWriteFn(():Unit))), // setipnum_be not implemented
        /*genmsi*/      0x3000 -> Seq(RegField(32, genmsi.r, RegWriteFn((v,d)=>{ when(v){genmsi.w(d)}; true.B }))),
        /*targets*/     0x3004 -> (1 until params.intSrcNum).map(i => RegField(32, targets.rI(i),
          RegWriteFn((v, d)=>{ when(v){targets.wI(i, d)}; true.B }))),
      )
    }

    val intSrcsSynced = RegNextN(intSrcs, 3)
    intSrcsRectified(0) := false.B
    (1 until params.intSrcNum).map(i => {
      val (rect, sync, sm) = (intSrcsRectified(i), intSrcsSynced(i), sourcecfgs.regs(i).SM)
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
    intSrcsTriggered.zipWithIndex.map { case (trigger:Bool, i:Int) =>
      when(trigger) {ips.wBitUI(i.U, true.B)}
    }

    // The ":+ true.B" trick explain:
    //  Append true.B to handle the cornor case, where all bits in ip and ie are disabled.
    //  If do not append true.B, then we need to check whether the ip & ie are empty,
    //  otherwise, the returned topei will become the max index, that is 2^aplicIntSrcWidth-1
    //  [0,     2^aplicIntSrcWidth-1] :+ 2^aplicIntSrcWidth
    val topi = Wire(UInt(params.aplicIntSrcWidth.W)); /*for debug*/dontTouch(topi)
    topi := ParallelPriorityMux((
      (ips.bits0:+true.B) zip (ies.bits0:+true.B)
    ).zipWithIndex.map {
      case ((p: Bool, e: Bool), i: Int) => (p & e, i.U)
    })
    // send MSI
    locally {
      val (tl, edge) = toIMSIC.out(0)
      tl.d.ready := true.B
      def getMSIAddr(HartIndex:UInt, guestID:UInt): UInt = {
        val groupID = if (params.groupsWidth == 0) 0.U
          else HartIndex(params.groupsWidth+params.membersWidth-1, params.membersWidth)
        val memberID = HartIndex(params.membersWidth-1, 0)
        // It is recommended to hardwire *msiaddrcfg* by the manual:
        // "For any given system, these addresses are fixed and should be hardwired into the APLIC if possible."
        imsicBaseAddr.U |
          (groupID<<params.groupStrideWidth) |
          (memberID<<imsicMemberStrideWidth) |
          (guestID<<params.intFileMemWidth)
      }
      when (genmsi.Busy) {// A pending extempore MSI (genmsi) should be sent by the APLIC with minimal delay.
        val msiAddr = getMSIAddr(genmsi.HartIndex, 0.U)
        val (_, pfbits) = edge.Put(0.U, msiAddr, 2.U, genmsi.EIID)
        when (tl.a.ready) { genmsi.Busy := false.B }
        tl.a.bits := pfbits
        tl.a.valid := true.B
      }.elsewhen (domaincfg.IE && topi=/=0.U) {
        val target = targets.regs(topi)
        val msiAddr = getMSIAddr(target.HartIndex, target.GuestIndex)
        val (_, pfbits) = edge.Put(0.U, msiAddr, 2.U, target.EIID)
        // clear corresponding ip
        when (tl.a.ready) { ips.wBitUI(topi, false.B) }
        tl.a.bits := pfbits
        tl.a.valid := true.B
      }.otherwise {
        tl.a.valid := false.B
      }
    }

    // delegate
    intSrcsDelegated := (sourcecfgs.regs zip intSrcs).map {case (r, i:Bool) => r.D&i}
  }
}

  val mDomain = LazyModule(new Domain(
    params.baseAddr,
    params.mBaseAddr,
    params.mStrideWidth,
    0,
  ))
  val sgDomain = LazyModule(new Domain(
    params.baseAddr + pow2(params.domainMemWidth),
    params.sgBaseAddr,
    params.sgStrideWidth,
    params.geilen,
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
