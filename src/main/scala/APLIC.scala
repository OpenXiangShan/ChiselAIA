//MC{hide}
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
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.regmapper._
import utility._

case class APLICParams(
  //MC APLIC接收的中断源数量的对数。
  //MC 默认值7表示APLIC支持最多128（2^7）个中断源。
  //MC **注意**：`aplicIntSrcWidth`必须小于`imsicIntSrcWidth`，
  //MC 因为APLIC的中断源将被转换为MSI，
  //MC 而APLIC转换成的MSI是IMSIC中断源的子集。
  //MC （Logarithm of number of interrupt sources to APLIC:
  //MC The default 7 means APLIC support at most 128 (2^7) interrupt sources.
  //MC **Note**: `aplicIntSrcWidth` must be **less than** `imsicIntSrcWidth`,
  //MC as APLIC interrupt sources are converted to MSIs,
  //MC which are a subset of IMSIC's interrupt sources）：
  //MC{visible}
  aplicIntSrcWidth: Int = 7,
  imsicIntSrcWidth: Int = 9,
  //MC 👉 APLIC域的基地址（Base address of APLIC domains）:
  baseAddr: Long = 0x19960000L,
  //MC **注意**：下述中括号内的变量与AIA规范中的一致（第3.6节：用于多个中断文件的内存区域排列）。
  //MC
  //MC **Note**: The following variables in bracket align with the AIA specification (Section 3.6: Memory Region Arrangement for Multiple Interrupt Files).
  //MC
  //MC 👉 每个组的成员数量（Number of members per group）[\\(h_{max}\\)]：
  membersNum      : Int  = 2           ,
  //MC 👉 所有IMSIC的机器态中断文件的基地址（Base address of machine-level interrupt files for all IMSICs）[\\(A\\)]：
  mBaseAddr       : Long = 0x61000000L ,
  //MC 👉 所有IMSIC的监管态和客户态中断文件的基地址（Base addr for supervisor-level and guest-level interrupt files for all IMSICs）[\\(B\\)]:
  sgBaseAddr      : Long = 0x82900000L ,
  //MC 👉 组的数量（Number of groups ）[\\(g_{max}\\)]:
  groupsNum       : Int  = 1           ,
  //MC 👉 客户中断文件的数量（Number of guest interrupt files）:
  geilen          : Int  = 4           ,
  //MC{hide}
  //MC{hide}
) {
  require(aplicIntSrcWidth <= 10, f"aplicIntSrcWidth=${aplicIntSrcWidth}, must not greater than log2(1024)=10, as there are at most 1023 sourcecfgs")
  lazy val intSrcNum: Int = pow2(aplicIntSrcWidth).toInt
  lazy val ixNum: Int = pow2(aplicIntSrcWidth).toInt / 32
  lazy val domainMemWidth : Int  = 14 // interrupt file memory region width: 14-bit width => 16KB size

  lazy val intFileMemWidth : Int  = 12        // interrupt file memory region width: 12-bit width => 4KB size
  // require(mStrideWidth >= intFileMemWidth)
  lazy val mStrideWidth    : Int  = intFileMemWidth // C: stride between each machine-level interrupt files
  // require(sgStrideWidth >= log2Ceil(geilen+1) + intFileMemWidth)
  lazy val sgStrideWidth   : Int = log2Ceil(geilen+1) + intFileMemWidth // D: stride between each supervisor- and guest-level interrupt files
  // require(groupStrideWidth >= k + math.max(mStrideWidth, sgStrideWidth))
  lazy val membersWidth    : Int = log2Ceil(membersNum) // k
  require((mBaseAddr & (pow2(membersWidth + mStrideWidth) -1)) == 0, "mBaseAddr should be aligned to a 2^(k+C)")
  lazy val groupStrideWidth: Int = membersWidth + math.max(mStrideWidth, sgStrideWidth) // E: stride between each interrupt file groups
  lazy val groupsWidth     : Int = log2Ceil(groupsNum) // j
  require((sgBaseAddr & (pow2(membersWidth + sgStrideWidth) - 1)) == 0, "sgBaseAddr should be aligned to a 2^(k+D)")
  require(( ((pow2(groupsWidth)-1) * pow2(groupStrideWidth)) & mBaseAddr ) == 0)
  require(( ((pow2(groupsWidth)-1) * pow2(groupStrideWidth)) & sgBaseAddr) == 0)

  def hartIndex_to_gh(hartIndex: Int): (Int, Int) = {
    val g = (hartIndex>>membersWidth) & (pow2(groupsWidth)-1)
    val h = hartIndex & (pow2(membersWidth)-1)
    (g.toInt, h.toInt)
  }
  def gh_to_hartIndex(g: Int, h: Int): Int = {
    (g<<membersWidth) | h
  }
}

class APLIC(
  params: APLICParams,
  beatBytes: Int = 4,
)(implicit p: Parameters) extends Module {
  println(f"APLICParams.membersWidth:      ${params.membersWidth    }%d")
  println(f"APLICParams.groupsWidth:       ${params.groupsWidth     }%d")
  println(f"APLICParams.membersNum:        ${params.membersNum      }%d")
  println(f"APLICParams.mBaseAddr:       0x${params.mBaseAddr       }%x")
  println(f"APLICParams.mStrideWidth:      ${params.mStrideWidth    }%d")
  println(f"APLICParams.sgBaseAddr:      0x${params.sgBaseAddr      }%x")
  println(f"APLICParams.sgStrideWidth:     ${params.sgStrideWidth   }%d")
  println(f"APLICParams.geilen:            ${params.geilen          }%d")
  println(f"APLICParams.groupsNum:         ${params.groupsNum       }%d")
  println(f"APLICParams.groupStrideWidth:  ${params.groupStrideWidth}%d")

  class Domain(
    baseAddr: Long, // base address for this aplic domain
    imsicBaseAddr: Long, // base address for imsic's interrupt files
    imsicMemberStrideWidth: Int, // C, D: stride between each interrupt files
    imsicGeilen: Int, // number of guest interrupt files, it is 0 for machine-level domain
  )(implicit p: Parameters) extends Module {
    override val desiredName = "Domain"
    class MSIBundle extends Bundle {
      val addr = Output(UInt(64.W))
      val data = Output(UInt(32.W))
    }
    object MSIBundle { def apply(a: UInt, d: UInt) = {
      val m = Wire(new MSIBundle); m.addr:=a; m.data:=d; m }}
    val io = IO(new Bundle {
      val msi = Decoupled(new MSIBundle)
      val ack = Input(Bool())
      val (regmapIn, regmapOut) = {
        val regmapParams = RegMapperParams(params.domainMemWidth-log2Up(beatBytes), beatBytes)
        ( Flipped(Decoupled(new RegMapperInput(regmapParams))),
          Decoupled(new RegMapperOutput(regmapParams)) )
    }})
    val intSrcs = IO(Input(Vec(params.intSrcNum, Bool())))
    val intSrcsDelegated = IO(Output(Vec(params.intSrcNum, Bool())))

    private val domaincfg = new Bundle {
      val high = 0x80.U(8.W)
      val IE   = RegInit(false.B)
      val DM   = true.B // only support MSI delivery mode
      val BE   = false.B // only support little endian
      val r = high<<24 | IE<<8 | DM<<2 | BE
      def w(data:UInt):Unit = IE := data(8)
    }
    private val sourcecfgs = new Bundle {
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
    private val ips = new IXs {
      def wBitUI(ui:UInt, bit:Bool): Unit = when (sourcecfgs.actives(ui)) {
        when (sourcecfgs.regs(ui).SM===sourcecfgs.level1 || sourcecfgs.regs(ui).SM===sourcecfgs.level0) {
          when (domaincfg.DM) {
            when (intSrcsRectified(ui)) { bits(ui):=bit }
          }.otherwise {/* Currently not support domaincfg.DM===0 */}
        }.otherwise { bits(ui):=bit }
      }
    }
    private val intSrcsRectified = Wire(Vec(params.intSrcNum, Bool()))
    private val ies = new IXs {
      def wBitUI(ui:UInt, bit:Bool): Unit = when (sourcecfgs.actives(ui)) {bits(ui):=bit}
    }
    private val genmsi = new Bundle {
      val HartIndex = RegInit(0.U(params.groupsWidth.W + params.membersWidth.W))
      val Busy      = RegInit(false.B)
      val EIID      = RegInit(0.U(params.imsicIntSrcWidth.W))
      def r = Mux(domaincfg.DM, HartIndex<<18 | Busy<<12 | EIID, 0.U)
      def w(data:UInt) = {
        when (domaincfg.DM && ~Busy) { HartIndex:=data(31,18); Busy:=true.B; EIID:=data(10,0); }
      }
    }
    private val targets = new Bundle {
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
      io.regmapOut <> RegMapper(beatBytes, 1, true, io.regmapIn,
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

    private val intSrcsSynced = RegNextN(intSrcs, 3)
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
    private val intSrcsTriggered = Wire(Vec(params.intSrcNum, Bool())); /*for debug*/dontTouch(intSrcsTriggered)
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
    private val topi = Wire(UInt(params.aplicIntSrcWidth.W)); /*for debug*/dontTouch(topi)
    topi := ParallelPriorityMux((
      (ips.bits0:+true.B) zip (ies.bits0:+true.B)
    ).zipWithIndex.map {
      case ((p: Bool, e: Bool), i: Int) => (p & e, i.U)
    })
    // send MSI
    locally {
      val idle :: waiting_ack :: Nil = Enum(2)
      val state = RegInit(idle)

      def getMSIAddr(HartIndex:UInt, guestID:UInt): UInt = {
        val groupID = if (params.groupsWidth == 0) 0.U
          else HartIndex(params.groupsWidth+params.membersWidth-1, params.membersWidth)
        val memberID = if (params.membersWidth == 0) 0.U
          else HartIndex(params.membersWidth-1, 0)
        // It is recommended to hardwire *msiaddrcfg* by the manual:
        // "For any given system, these addresses are fixed and should be hardwired into the APLIC if possible."
        imsicBaseAddr.U |
          (groupID<<params.groupStrideWidth) |
          (memberID<<imsicMemberStrideWidth) |
          (guestID<<params.intFileMemWidth)
      }
      val genmsiBits = MSIBundle(getMSIAddr(genmsi.HartIndex, 0.U), genmsi.EIID)
      val target = targets.regs(topi)
      val topiBits = MSIBundle(getMSIAddr(target.HartIndex, target.GuestIndex), target.EIID)

      // A pending extempore MSI (genmsi) should be sent by the APLIC with minimal delay.
      io.msi.bits := Mux(genmsi.Busy, genmsiBits, topiBits)
      io.msi.valid := (state===idle) && (genmsi.Busy || (domaincfg.IE && topi=/=0.U))

      switch (state) {
        is (idle) { when (io.msi.fire) { state := waiting_ack } }
        is (waiting_ack) { when (io.ack) { state := idle
          when (genmsi.Busy) { genmsi.Busy := false.B }
          .otherwise { ips.wBitUI(topi, false.B) }
      }}}
    }

    // delegate
    intSrcsDelegated := (sourcecfgs.regs zip intSrcs).map {case (r, i:Bool) => r.D&i}
  }

  // domain(0) is m domain, domain(1) is sg domain
  private val domains = Seq(Module(new Domain(
    params.baseAddr,
    params.mBaseAddr,
    params.mStrideWidth,
    0,
  )), Module(new Domain(
    params.baseAddr + pow2(params.domainMemWidth),
    params.sgBaseAddr,
    params.sgStrideWidth,
    params.geilen,
  )))
  val ios = domains.map(d => { val io = IO(d.io.cloneType); io <> d.io; io })

  val intSrcs = IO(Input(Vec(params.intSrcNum, Bool())))
  domains(0).intSrcs := intSrcs
  domains(1).intSrcs := domains(0).intSrcsDelegated
}

class TLAPLIC(
  params: APLICParams,
  beatBytes: Int = 4,
)(implicit p: Parameters) extends LazyModule {
  val fromCPU = LazyModule(new TLXbar).node
  val toIMSIC = LazyModule(new TLXbar).node
  private val domainFromCPUs = Seq(
    params.baseAddr, params.baseAddr + pow2(params.domainMemWidth) 
  ).map ( baseAddr => {
    val domainFromCPU = TLRegMapperNode(
      address = Seq(AddressSet(baseAddr, pow2(params.domainMemWidth)-1)),
      beatBytes = beatBytes)
    domainFromCPU := fromCPU; domainFromCPU
  })
  private val domainToIMSICs = (0 until 2).map (_ => {
    val domainToIMSIC = TLClientNode(Seq(TLMasterPortParameters.v1(Seq(TLMasterParameters.v1("toimsic", IdRange(0,16))))))
    toIMSIC := domainToIMSIC; domainToIMSIC
  })

  lazy val module = new Imp
  class Imp extends LazyModuleImp(this) {
    val intSrcs = IO(Input(Vec(params.intSrcNum, Bool())))
    private val aplic = Module(new APLIC(params, beatBytes))
    aplic.intSrcs := intSrcs

    (domainToIMSICs zip aplic.ios).map { case (domainToIMSIC, aplicIO) => {
      val (tl, edge) = domainToIMSIC.out.head
      val msi = aplicIO.msi
      val (_, bits) = edge.Put(0.U, msi.bits.addr, 2.U, msi.bits.data)
      tl.a.bits := bits
      tl.a.valid := msi.valid
      msi.ready := tl.a.ready
      tl.d.ready := true.B
      aplicIO.ack := tl.d.valid
    }}

    (domainFromCPUs zip aplic.ios).map { case (domainFromCPU, aplicIO) => {
      domainFromCPU.regmap(aplicIO.regmapIn, aplicIO.regmapOut)
    }}
  }
}

class AXI4APLIC(
  params: APLICParams,
  beatBytes: Int = 4,
)(implicit p: Parameters) extends LazyModule {
  val fromCPU = LazyModule(new AXI4Xbar).node
  val toIMSIC = LazyModule(new AXI4Xbar).node
  private val domainFromCPUs = Seq(
    params.baseAddr, params.baseAddr + pow2(params.domainMemWidth) 
  ).map ( baseAddr => {
    val domainFromCPU = AXI4RegMapperNode(
      address = AddressSet(baseAddr, pow2(params.domainMemWidth)-1),
      beatBytes = beatBytes)
    domainFromCPU := fromCPU; domainFromCPU
  })
  private val domainToIMSICs = (0 until 2).map (_ => {
    val domainToIMSIC = AXI4MasterNode(Seq(AXI4MasterPortParameters(Seq(AXI4MasterParameters("toimsic", IdRange(0,16))))))
    toIMSIC := domainToIMSIC; domainToIMSIC
  })

  lazy val module = new Imp
  class Imp extends LazyModuleImp(this) {
    val intSrcs = IO(Input(Vec(params.intSrcNum, Bool())))
    private val aplic = Module(new APLIC(params, beatBytes))
    aplic.intSrcs := intSrcs

    (domainToIMSICs zip aplic.ios).map { case (domainToIMSIC, aplicIO) => {
      val (axi, edge_params) = domainToIMSIC.out.head
      val msi = aplicIO.msi
      axi.aw.bits.addr := msi.bits.addr
      axi.aw.bits.size := 2.U
      axi.aw.valid := msi.valid
      axi.w.bits.data := msi.bits.data
      axi.w.bits.strb := MaskGen(msi.bits.addr, 2.U, beatBytes)
      axi.w.bits.last := true.B
      axi.w.valid := msi.valid
      msi.ready := axi.aw.ready & axi.w.ready
      axi.b.ready := true.B
      aplicIO.ack := axi.b.valid
    }}

    (domainFromCPUs zip aplic.ios).map { case (domainFromCPU, aplicIO) => {
      domainFromCPU.regmap(aplicIO.regmapIn, aplicIO.regmapOut)
    }}
  }
}
