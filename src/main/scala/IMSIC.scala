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
import freechips.rocketchip.prci.{ClockSinkDomain}
import freechips.rocketchip.util._
import xs.utils._

// RegMap that supports Default and Valid
object RegMapDV {
  def Unwritable = null
  def apply(addr: Int, reg: UInt, wfn: UInt => UInt = (x => x)) = (addr, (reg, wfn))
  def generate(default: UInt, mapping: Map[Int, (UInt, UInt => UInt)], raddr: UInt, rdata: UInt, rvalid: Bool,
    waddr: UInt, wen: Bool, wdata: UInt, wmask: UInt):Unit = {
    val chiselMapping = mapping.map { case (a, (r, w)) => (a.U, r, w) }
    val rdata_valid = WireDefault(0.U((rdata.getWidth+1).W))
    rdata_valid := LookupTreeDefault(raddr, Cat(default,false.B), chiselMapping.map { case (a, r, w) => (a, Cat(r,true.B)) })
    rdata := rdata_valid(rdata.getWidth, 1)
    rvalid := rdata_valid(0)
    chiselMapping.map { case (a, r, w) =>
      if (w != null) when (wen && waddr === a) { r := w(MaskData(r, wdata, wmask)) }
    }
  }
  def generate(default: UInt, mapping: Map[Int, (UInt, UInt => UInt)], addr: UInt, rdata: UInt, rvalid: Bool,
    wen: Bool, wdata: UInt, wmask: UInt):Unit = generate(default, mapping, addr, rdata, rvalid, addr, wen, wdata, wmask)
}

class TLIMSIC(
  params: IMSICParams,
  // According AIA manual:
  // This hart index may or may not related to the unique hart identifiers ("hart IDs") that the RISC-V Privileged Architecture assigns to harts.
  hartIndex: Int = 1,
  beatBytes: Int = 8,
)(implicit p: Parameters) extends LazyModule {
  val (groupID, memberID) = params.hartIndex_to_gh(hartIndex)
  require(groupID < params.groupsNum,    f"groupID ${groupID} should less than groupsNum ${params.groupsNum}")
  require(memberID < params.membersNum,  f"memberID ${memberID} should less than membersNum ${params.membersNum}")
  println(f"groupID:  0x${groupID }%x")
  println(f"memberID: 0x${memberID}%x")

  val device: SimpleDevice = new SimpleDevice(
    "interrupt-controller",
    Seq(f"riscv,imsic.${groupID}%d.${memberID}%d")
  )


  // addr for the machine-level interrupt file: g*2^E + A + h*2^C
  val mAddr = AddressSet(
    groupID * pow2(params.groupStrideWidth) + params.mBaseAddr + memberID * pow2(params.mStrideWidth),
    pow2(params.intFileMemWidth) - 1
  )
  // addr for the supervisor-level and guest-level interrupt files: g*2^E + B + h*2^D
  val sgAddr = AddressSet(
    groupID * pow2(params.groupStrideWidth) + params.sgBaseAddr + memberID * pow2(params.sgStrideWidth),
    pow2(params.intFileMemWidth) * pow2(log2Ceil(1+params.geilen)) - 1
  )
  println(f"mAddr:  [0x${mAddr.base }%x, 0x${mAddr.max }%x]")
  println(f"sgAddr: [0x${sgAddr.base}%x, 0x${sgAddr.max}%x]")

  val Seq(mTLNode, sgTLNode) = Seq(mAddr, sgAddr).map( addr => TLRegisterNode(
    address = Seq(addr),
    device = device,
    beatBytes = beatBytes,
    undefZero = true,
    concurrency = 1
  ))

  // Based on Xiangshan NewCSR
  object OpType extends ChiselEnum {
    val ILLEGAL = Value(0.U)
    val CSRRW   = Value(1.U)
    val CSRRS   = Value(2.U)
    val CSRRC   = Value(3.U)
  }
  object PrivType extends ChiselEnum {
    val U = Value(0.U)
    val S = Value(1.U)
    val M = Value(3.U)
  }
  class CSRToIMSICBundle extends Bundle {
    val addr = ValidIO(UInt(params.iselectWidth.W))
    val virt = Bool()
    val priv = PrivType()
    val vgein = UInt(params.vgeinWidth.W)
    val wdata = ValidIO(new Bundle {
      val op = OpType()
      val data = UInt(params.xlen.W)
    })
    val claims = Vec(params.privNum, Bool())
  }
  class IMSICToCSRBundle extends Bundle {
    val rdata = ValidIO(UInt(params.xlen.W))
    val illegal = Bool()
    val pendings = Vec(params.intFilesNum, Bool())
    val topeis  = Vec(params.privNum, UInt(32.W))
  }
  class IntFile extends Module {
    val fromCSR = IO(Input(new Bundle {
      val seteipnum = ValidIO(UInt(32.W))
      val addr = ValidIO(UInt(params.iselectWidth.W))
      val wdata = ValidIO(new Bundle {
        val op = OpType()
        val data = UInt(params.xlen.W)
      })
      val claim = Bool()
    }))
    val toCSR = IO(Output(new Bundle {
      val rdata = ValidIO(UInt(params.xlen.W))
      val illegal = Bool()
      val pending = Bool()
      val topei  = UInt(params.intSrcWidth.W)
    }))

    /// indirect CSRs
    val eidelivery = RegInit(0.U(params.xlen.W))
    val eithreshold = RegInit(0.U(params.xlen.W))
    val eips = RegInit(VecInit.fill(params.eixNum){0.U(params.xlen.W)})
    val eies = RegInit(VecInit.fill(params.eixNum){0.U(params.xlen.W)})

    val illegal_wdata_op = WireDefault(false.B)
    locally { // scope for xiselect CSR reg map
      val wdata = WireDefault(0.U(params.xlen.W))
      val wmask = WireDefault(0.U(params.xlen.W))
      when(fromCSR.wdata.valid) {
        switch(fromCSR.wdata.bits.op) {
          is(OpType.ILLEGAL) {
            illegal_wdata_op := true.B
          }
          is(OpType.CSRRW) {
            wdata := fromCSR.wdata.bits.data
            wmask := Fill(params.xlen, 1.U)
          }
          is(OpType.CSRRS) {
            wdata := Fill(params.xlen, 1.U)
            wmask := fromCSR.wdata.bits.data
          }
          is(OpType.CSRRC) {
            wdata := 0.U
            wmask := fromCSR.wdata.bits.data
          }
        }
      }
      def bit0ReadOnlyZero(x: UInt): UInt = { x & ~1.U(x.getWidth.W) }
      RegMapDV.generate(
        0.U,
        Map(
          RegMapDV(0x70, eidelivery),
          RegMapDV(0x72, eithreshold),
          RegMapDV(0x80, eips(0), bit0ReadOnlyZero),
          RegMapDV(0xC0, eies(0), bit0ReadOnlyZero),
        ) ++ eips.drop(1).zipWithIndex.map { case (eip: UInt, i: Int) =>
          RegMapDV(0x82+i*2, eip)
        } ++ eies.drop(1).zipWithIndex.map { case (eie: UInt, i: Int) =>
          RegMapDV(0xC2+i*2, eie)
        },
        /*raddr*/ fromCSR.addr.bits,
        /*rdata*/ toCSR.rdata.bits,
        /*rdata*/ toCSR.rdata.valid,
        /*waddr*/ fromCSR.addr.bits,
        /*wen  */ fromCSR.wdata.valid,
        /*wdata*/ wdata,
        /*wmask*/ wmask,
      )
      toCSR.illegal := RegNext(fromCSR.addr.valid) & Seq(
        ~toCSR.rdata.valid,
        illegal_wdata_op,
      ).reduce(_|_)
    } // end of scope for xiselect CSR reg map

    locally {
      val index  = fromCSR.seteipnum.bits(params.intSrcWidth-1, params.xlenWidth)
      val offset = fromCSR.seteipnum.bits(params.xlenWidth-1, 0)
      when ( fromCSR.seteipnum.valid & eies(index)(offset) ) {
        // set eips bit
        eips(index) := eips(index) | UIntToOH(offset)
      }
    }

    locally { // scope for xtopei
      // The ":+ true.B" trick explain:
      //  Append true.B to handle the cornor case, where all bits in eip and eie are disabled.
      //  If do not append true.B, then we need to check whether the eip & eie are empty,
      //  otherwise, the returned topei will become the max index, that is 2^intSrcWidth-1
      // Noted: the support max interrupt sources number = 2^intSrcWidth
      //              [0,     2^intSrcWidth-1] :+ 2^intSrcWidth
      val eipBools = Cat(eips.reverse).asBools :+ true.B
      val eieBools = Cat(eies.reverse).asBools :+ true.B
      def xtopei_filter(xeidelivery: UInt, xeithreshold: UInt, xtopei: UInt): UInt = {
        val tmp_xtopei = Mux(xeidelivery(0), xtopei, 0.U)
        // {
        //   all interrupts are enabled, when eithreshold == 0;
        //   interrupts, when i < eithreshold, are enabled;
        // } <=> interrupts, when i <= (eithreshold -1), are enabled
        Mux(tmp_xtopei <= (xeithreshold-1.U), tmp_xtopei, 0.U)
      }
      toCSR.topei := xtopei_filter(
        eidelivery,
        eithreshold,
        ParallelPriorityMux(
          (eipBools zip eieBools).zipWithIndex.map {
            case ((p: Bool, e: Bool), i: Int) => (p & e, i.U)
          }
        )
      )
    } // end of scope for xtopei
    toCSR.pending := toCSR.topei =/= 0.U

    when(fromCSR.claim) {
      val index  = toCSR.topei(params.intSrcWidth-1, params.xlenWidth)
      val offset = toCSR.topei(params.xlenWidth-1, 0)
      // clear the pending bit indexed by xtopei in xeip
      eips(index) := eips(index) & ~UIntToOH(offset)
    }
  }

  lazy val module = new Imp
  class Imp extends LazyModuleImp(this) {
    val toCSR = IO(Output(new IMSICToCSRBundle))
    val fromCSR = IO(Input(new CSRToIMSICBundle))

    val illegal_priv = WireDefault(false.B)
    val intFilesSelOH = WireDefault(0.U(params.intFilesNum.W))
    locally {
      val pv = Cat(fromCSR.priv.asUInt, fromCSR.virt)
      when      (pv === Cat(PrivType.M.asUInt, false.B)) { intFilesSelOH := UIntToOH(0.U) }
      .elsewhen (pv === Cat(PrivType.S.asUInt, false.B)) { intFilesSelOH := UIntToOH(1.U) }
      .elsewhen (pv === Cat(PrivType.S.asUInt,  true.B)) { intFilesSelOH := UIntToOH(2.U + fromCSR.vgein) }
      .otherwise { illegal_priv := true.B }
    }
    val topeis_forEachIntFiles = Wire(Vec(params.intFilesNum, UInt(params.intSrcWidth.W)))
    val illegals_forEachIntFiles = Wire(Vec(params.intFilesNum, Bool()))

    Seq((mTLNode,1), (sgTLNode,1+params.geilen)).zipWithIndex.map {
      case ((tlNode: TLRegisterNode, thisNodeintFilesNum: Int), nodei: Int)
    => {
      // TODO: directly access TL protocol, instead of use the regmap
      // thisNode_ii: index for intFiles in this node: S, G1, G2, ...
      val maps = (0 until thisNodeintFilesNum).map { thisNode_ii => {
        val ii = nodei + thisNode_ii
        val pi = if(ii>2) 2 else ii // index for privileges: M, S, VS.

        val seteipnum = RegInit(0.U(32.W))
        when (seteipnum =/= 0.U) {seteipnum := 0.U}

        def sel[T<:Data](old: Valid[T]): Valid[T] = {
          val new_ = Wire(Valid(chiselTypeOf(old.bits)))
          new_.bits := old.bits
          new_.valid := old.valid & intFilesSelOH(ii)
          new_
        }
        val intFile = Module(new IntFile)
        intFile.fromCSR.seteipnum.valid := seteipnum =/= 0.U
        intFile.fromCSR.seteipnum.bits  := seteipnum
        intFile.fromCSR.addr            := sel(fromCSR.addr)
        intFile.fromCSR.wdata           := sel(fromCSR.wdata)
        intFile.fromCSR.claim           := fromCSR.claims(pi) & intFilesSelOH(ii)
        toCSR.rdata                     := intFile.toCSR.rdata
        toCSR.pendings(ii)              := intFile.toCSR.pending
        topeis_forEachIntFiles(ii)      := intFile.toCSR.topei
        illegals_forEachIntFiles(ii)    := intFile.toCSR.illegal
        (thisNode_ii * pow2(params.intFileMemWidth).toInt -> Seq(RegField(32, seteipnum)))
      }}
      tlNode.regmap((maps): _*)
    }}

    locally {
      // Format of *topei:
      // * bits 26:16 Interrupt identity
      // * bits 10:0 Interrupt priority (same as identity)
      // * All other bit positions are zeros.
      // For detailed explainations of these memory region arguments,
      // please refer to the manual *The RISC-V Advanced Interrupt Architeture*: 3.9. Top external interrupt CSRs
      def wrap(topei: UInt): UInt = {
        val zeros = 0.U((16-params.intSrcWidth).W)
        Cat(zeros, topei, zeros, topei)
      }
      toCSR.topeis(0) := wrap(topeis_forEachIntFiles(0)) // m
      toCSR.topeis(1) := wrap(topeis_forEachIntFiles(1)) // s
      toCSR.topeis(2) := wrap(ParallelMux(
        UIntToOH(fromCSR.vgein, params.geilen).asBools,
        topeis_forEachIntFiles.drop(2)
      )) // vs
    }
    toCSR.illegal := RegNext(fromCSR.addr.valid) & Seq(
      illegals_forEachIntFiles.reduce(_|_),
      fromCSR.vgein >= params.geilen.asUInt,
      illegal_priv,
    ).reduce(_|_)
  }
}
