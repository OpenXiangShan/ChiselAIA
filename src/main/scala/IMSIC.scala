package aia

import chisel3._
import chisel3.IO
import chisel3.util._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.amba.axi4.AXI4Xbar
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.prci.ClockSinkDomain
import freechips.rocketchip.regmapper._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util._
import org.chipsalliance.cde.config.Parameters
import utility._

object RegMapDV {
  def Unwritable = null
  def apply(addr: Int, reg: UInt, wfn: UInt => UInt = (x => x)) = (addr, (reg, wfn))
  def generate(
      default: UInt,
      mapping: Map[Int, (UInt, UInt => UInt)],
      raddr:   UInt,
      rvld:    Bool,
      rdata:   UInt,
      rvalid:  Bool,
      waddr:   UInt,
      wen:     Bool,
      wdata:   UInt,
      wmask:   UInt,
      illegal_priv: Bool,
      illegal_op:   Bool
  ): Unit = {
    val chiselMapping = mapping.map { case (a, (r, w)) => (a.U, r, w) }
    when(rvld) {
      rdata := LookupTreeDefault(
        raddr,
        Cat(default),
        chiselMapping.map { case (a, r, _) => (a, r) }
      )
      rvalid := true.B
    }.otherwise {
      rdata  := 0.U((rdata.getWidth).W)
      rvalid := illegal_priv | illegal_op
    }

    chiselMapping.foreach { case (a, r, w) =>
      if (w != null) {
        when(wen && waddr === a && !illegal_priv && !illegal_op) {
          r := w(MaskData(r, wdata, wmask))
        }
      }
    }
  }
  def generate(
      default: UInt,
      mapping: Map[Int, (UInt, UInt => UInt)],
      addr:    UInt,
      rvld:    Bool,
      rdata:   UInt,
      rvalid:  Bool,
      wen:     Bool,
      wdata:   UInt,
      wmask:   UInt,
      illegal_priv: Bool,
      illegal_wdata_op:   Bool
  ): Unit = generate(default, mapping, addr, rvld, rdata, rvalid, addr, wen, wdata, wmask, illegal_priv, illegal_wdata_op)
}

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
  val H = Value(2.U)
  val M = Value(3.U)
}
class MSITransBundle(params: IMSICParams) extends Bundle {
  val vld_req = Input(Bool()) // request from axireg
  val data = Input(UInt(params.MSI_INFO_WIDTH.W))
  val vld_ack = Output(Bool())  // ack for axireg from imsic. which indicates imsic can work actively.
}
class ForCVMBundle extends Bundle {
  val cmode = Input(Bool()) // add port: cpu mode is tee or ree
  val notice_pending =
    Output(Bool()) // add port: interrupt pending of ree when cmode is tee,else interrupt pending of tee.
}
class AddrBundle(params: IMSICParams) extends  Bundle {
  val valid = Bool()                      // 表示 addr 是否有效
  val bits  = new Bundle {
    val addr = UInt(params.iselectWidth.W) // 存储实际地址值
    val virt  = Bool()
    val priv  = PrivType()
  }
}
class CSRToIMSICBundle(params: IMSICParams) extends Bundle {
  val addr  = new AddrBundle(params)
  val vgein = UInt(params.vgeinWidth.W)
  val wdata = ValidIO(new Bundle {
    val op   = OpType()
    val data = UInt(params.xlen.W)
  })
  val claims = Vec(params.privNum, Bool())
}
class IMSICToCSRBundle(params: IMSICParams) extends Bundle {
  val rdata    = ValidIO(UInt(params.xlen.W))
  val illegal  = Bool()
  val pendings = UInt(params.intFilesNum.W)
  val topeis   = Vec(params.privNum, UInt(32.W))
}
case class IMSICParams(
    // MC IMSIC中断源数量的对数，默认值8表示IMSIC支持最多256（2^8）个中断源

    // MC （Logarithm of number of interrupt sources to IMSIC.
    // MC The default 8 means IMSIC support at most 256 (2^8) interrupt sources）:
    // MC{visible}
    imsicIntSrcWidth: Int = 8,
    // MC 👉 本IMSIC的机器态中断文件的地址（Address of machine-level interrupt files for this IMSIC）：
    mAddr: Long = 0x00000L,
    // MC 👉 本IMSIC的监管态和客户态中断文件的地址（Addr for supervisor-level and guest-level interrupt files for this IMSIC）:
    sgAddr: Long = 0x10000L,
    // MC 👉 客户中断文件的数量（Number of guest interrupt files）:
    geilen: Int = 7,
    // MC vgein信号的位宽（The width of the vgein signal）:
    vgeinWidth: Int = 6,
    // MC iselect信号的位宽(The width of iselect signal):
    iselectWidth:           Int = 12,
    EnableImsicAsyncBridge: Boolean = false,
    HasTEEIMSIC:            Boolean = false
    // MC{hide}
) {
  lazy val xlen: Int = 64 // currently only support xlen = 64
  lazy val xlenWidth = log2Ceil(xlen)
  require(
    imsicIntSrcWidth <= 11 && imsicIntSrcWidth >= 6,
    f"imsicIntSrcWidth=${imsicIntSrcWidth}, must not greater than log2(2048)=11, as there are at most 2048 eip/eie bits" +
      "must not be less than log2(64)=6, as there must be at least 64 eip/eie bits"
  )
  lazy val privNum:     Int = 3          // number of privilege modes: machine, supervisor, virtualized supervisor
  lazy val intFilesNum: Int = 2 + geilen // number of interrupt files, m, s, vs0, vs1, ...

  lazy val eixNum: Int = pow2(imsicIntSrcWidth).toInt / xlen // number of eip/eie registers
  lazy val intFileMemWidth: Int = 12 // interrupt file memory region width: 12-bit width => 4KB size
  require(vgeinWidth >= log2Ceil(geilen))
  require(
    iselectWidth >= 8,
    f"iselectWidth=${iselectWidth} needs to be able to cover addr [0x70, 0xFF], that is from CSR eidelivery to CSR eie63"
  )
  lazy val INTP_FILE_WIDTH = log2Ceil(intFilesNum)
  lazy val MSI_INFO_WIDTH  = imsicIntSrcWidth + INTP_FILE_WIDTH
}

class IMSIC(
    params:    IMSICParams,
    beatBytes: Int = 4
)(implicit p: Parameters) extends Module {
  println(f"IMSICParams.geilen:            ${params.geilen}%d")

  class IMSICGateWay extends Module {
    // === io port define ===
    val msiio = IO(new MSITransBundle(params))
    val msi_data_o  = IO(Output(UInt(params.imsicIntSrcWidth.W)))
    val msi_valid_o = IO(Output(UInt(params.intFilesNum.W)))

    // === main body ===
    val msi_in = Wire(UInt(params.MSI_INFO_WIDTH.W))
    msi_in := msiio.data
    val msi_vld_req_cpu = WireInit(false.B)
    when(params.EnableImsicAsyncBridge.B) {
      msi_vld_req_cpu := AsyncResetSynchronizerShiftReg(msiio.vld_req, 3, 0)
    }.otherwise {
      msi_vld_req_cpu := msiio.vld_req
    }
    val msi_vld_ack_cpu = RegInit(false.B)
    when(msi_vld_req_cpu)(
      msi_vld_ack_cpu := true.B
    ).otherwise(
      msi_vld_ack_cpu := false.B
    )
    // generate the msi_vld_ack,to handle with the input msi request.
    msiio.vld_ack := msi_vld_ack_cpu
    val msi_vld_ris_cpu = msi_vld_req_cpu & (~msi_vld_ack_cpu) // rising of msi_vld_req
    val msi_data_catch  = RegInit(0.U(params.imsicIntSrcWidth.W))
    val msi_intf_valids = RegInit(0.U(params.intFilesNum.W))
    msi_data_o  := msi_data_catch(params.imsicIntSrcWidth - 1, 0)
    msi_valid_o := msi_intf_valids // multi-bis switch vector
    when(msi_vld_ris_cpu) {
      msi_data_catch := msi_in(params.imsicIntSrcWidth - 1, 0)
      msi_intf_valids := 1.U << msi_in(params.MSI_INFO_WIDTH - 1,params.imsicIntSrcWidth)
    }.otherwise {
      msi_intf_valids := 0.U
    }
  }
  class IntFile extends Module {
    override def desiredName = "IntFile"
    val fromCSR = IO(Input(new Bundle {
      val seteipnum = ValidIO(UInt(params.imsicIntSrcWidth.W))
      val addr      = ValidIO(UInt(params.iselectWidth.W))
      val vgein     = UInt(params.vgeinWidth.W)
      val wdata = ValidIO(new Bundle {
        val op   = OpType()
        val data = UInt(params.xlen.W)
      })
      val claim = Bool()
    }))
    val toCSR = IO(Output(new Bundle {
      val rdata   = ValidIO(UInt(params.xlen.W))
      val illegal = Bool()
      val pending = Bool()
      val topei   = UInt(params.imsicIntSrcWidth.W)
    }))
    val illegal_io = IO(new Bundle {
      val illegal_priv = Input(Bool())
    })
    val illegal_priv = illegal_io.illegal_priv
  
    /// indirect CSRs
    val eidelivery  = RegInit(0.U(params.xlen.W))
    val eithreshold = RegInit(0.U(params.xlen.W))
    val eips        = RegInit(VecInit.fill(params.eixNum)(0.U(params.xlen.W)))
    val eies        = RegInit(VecInit.fill(params.eixNum)(0.U(params.xlen.W)))

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
      def bit0ReadOnlyZero(x: UInt): UInt = x & ~1.U(x.getWidth.W)
      def fixEIDelivery(x: UInt): UInt = x & 1.U
      RegMapDV.generate(
        0.U,
        Map(
          RegMapDV(0x70, eidelivery, fixEIDelivery),
          RegMapDV(0x72, eithreshold),
          RegMapDV(0x80, eips(0), bit0ReadOnlyZero),
          RegMapDV(0xc0, eies(0), bit0ReadOnlyZero)
        ) ++ eips.drop(1).zipWithIndex.map { case (eip: UInt, i: Int) =>
          RegMapDV(0x82 + i * 2, eip)
        } ++ eies.drop(1).zipWithIndex.map { case (eie: UInt, i: Int) =>
          RegMapDV(0xc2 + i * 2, eie)
        },
        /*raddr*/  fromCSR.addr.bits,
        /*rvld */  fromCSR.addr.valid,
        /*rdata*/  toCSR.rdata.bits,
        /*rvalid*/ toCSR.rdata.valid,
        /*waddr*/  fromCSR.addr.bits,
        /*wen  */  fromCSR.wdata.valid,
        /*wdata*/  wdata,
        /*wmask*/  wmask,
        /*priv*/   illegal_priv,
        /*op*/     illegal_wdata_op
      )
      val illegal_csr = WireDefault(false.B)
      when(fromCSR.addr.bits >= 0x80.U && fromCSR.addr.bits <= 0xFF.U &&
        fromCSR.addr.bits(0) === 1.U) {
          illegal_csr := true.B
      }
      toCSR.illegal := illegal_csr
    }
    locally {
      val index  = fromCSR.seteipnum.bits(params.imsicIntSrcWidth - 1, params.xlenWidth)
      val offset = fromCSR.seteipnum.bits(params.xlenWidth - 1, 0)
      when(fromCSR.seteipnum.valid) {
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
        val tmp_xtopei = Mux(xeidelivery(params.xlen - 1, 1) === 0.U, Mux(xeidelivery(0), xtopei, 0.U) , 0.U)
        // {
        //   all interrupts are enabled, when eithreshold == 1;
        //   interrupts, when i < eithreshold, are enabled;
        // } <=> interrupts, when i <= (eithreshold -1), are enabled
        Mux(tmp_xtopei <= (xeithreshold - 1.U), tmp_xtopei, 0.U)
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
      val index  = toCSR.topei(params.imsicIntSrcWidth - 1, params.xlenWidth)
      val offset = toCSR.topei(params.xlenWidth - 1, 0)
      // clear the pending bit indexed by xtopei in xeip
      eips(index) := eips(index) & ~UIntToOH(offset)
    }
  }
  val toCSR   = IO(Output(new IMSICToCSRBundle(params)))
  val fromCSR = IO(Input(new CSRToIMSICBundle(params)))
  val msiio   = IO(new MSITransBundle(params))
  val illegal_priv = WireInit(false.B)

  private val intFilesSelOH_r = WireDefault(0.U(params.intFilesNum.W))
  private val intFilesSelOH_w = WireDefault(0.U(params.intFilesNum.W))
  locally {
    when (fromCSR.addr.valid)
    {
      when(fromCSR.addr.bits.virt === false.B )
      {
        when(((fromCSR.addr.bits.priv.asUInt === 3.U) || (fromCSR.addr.bits.priv.asUInt === 1.U)) && fromCSR.vgein === 0.U){
          illegal_priv := false.B
        }.otherwise{
          illegal_priv := true.B
        }
      }.otherwise{
        when(fromCSR.addr.bits.priv.asUInt === 1.U && (fromCSR.vgein >= 1.U) && (fromCSR.vgein < (params.geilen + 1).U((params.vgeinWidth+1).W)))
        {
          illegal_priv := false.B
        }.otherwise{
          illegal_priv := true.B
        }
      }
    }
    when (fromCSR.addr.valid && !illegal_priv) // read
    {
      val pv = Cat(fromCSR.addr.bits.priv.asUInt, fromCSR.addr.bits.virt)
      when(pv === Cat(PrivType.M.asUInt, false.B)){intFilesSelOH_r := UIntToOH(0.U)}
        .elsewhen(pv === Cat(PrivType.S.asUInt, false.B)){intFilesSelOH_r := UIntToOH(1.U)}
        .elsewhen(pv === Cat(PrivType.S.asUInt, true.B)){intFilesSelOH_r := UIntToOH(1.U((fromCSR.vgein.getWidth+1).W)
         + fromCSR.vgein.pad(params.vgeinWidth+1))
      }
    }
    when (fromCSR.addr.valid && fromCSR.wdata.valid && !(fromCSR.wdata.bits.op.asUInt === 0.U) && !illegal_priv) // write
    {
        val pv = Cat(fromCSR.addr.bits.priv.asUInt, fromCSR.addr.bits.virt)
        when(pv === Cat(PrivType.M.asUInt, false.B)){intFilesSelOH_w := UIntToOH(0.U)}
        .elsewhen(pv === Cat(PrivType.S.asUInt, false.B)){intFilesSelOH_w := UIntToOH(1.U)}
        .elsewhen(pv === Cat(PrivType.S.asUInt, true.B)){intFilesSelOH_w := UIntToOH(1.U((fromCSR.vgein.getWidth+1).W)
         + fromCSR.vgein.pad(params.vgeinWidth+1))
        }
    }
  }

  private val topeis_forEachIntFiles   = Wire(Vec(params.intFilesNum, UInt(params.imsicIntSrcWidth.W)))
  private val illegals_forEachIntFiles = Wire(Vec(params.intFilesNum, Bool()))
  // instance and connect IMSICGateWay.
  val imsicGateWay = Module(new IMSICGateWay)
  imsicGateWay.msiio <> msiio
  val pendings = Wire(Vec(params.intFilesNum,Bool()))
  val vec_rdata = Wire(Vec(params.intFilesNum, ValidIO(UInt(params.xlen.W))))
  Seq(1, 1 + params.geilen).zipWithIndex.map {
    case (intFilesNum: Int, i: Int) => {
      // j: index for S intFile: S, G1, G2, ...
      val maps = (0 until intFilesNum).map { j =>
        val flati = i + j
        val pi    = if (flati > 2) 2 else flati // index for privileges: M, S, VS.

        def sel_addr(old: AddrBundle): AddrBundle = {
          val new_ = Wire(new AddrBundle(params))
          new_.valid := old.valid & intFilesSelOH_r(flati)
          new_.bits.addr := old.bits.addr
          new_.bits.virt := old.bits.virt
          new_.bits.priv := old.bits.priv
          new_
        }
        def sel_wdata[T <: Data](old: Valid[T]): Valid[T] = {
          val new_ = Wire(Valid(chiselTypeOf(old.bits)))
          new_.bits  := old.bits
          new_.valid := old.valid & intFilesSelOH_w(flati)
          new_
        }

        val intFile = Module(new IntFile)
        // Preventing overflow
        when (flati.U((params.vgeinWidth + 1).W) === fromCSR.vgein.pad(params.vgeinWidth + 1)+1.U) {
          intFile.fromCSR.vgein := fromCSR.vgein
        } .otherwise {
          intFile.fromCSR.vgein := 0.U
        }
        val intfile_rdata_d = RegNext(intFile.toCSR.rdata)
        intFile.fromCSR.seteipnum.bits  := imsicGateWay.msi_data_o
        intFile.fromCSR.seteipnum.valid := imsicGateWay.msi_valid_o(flati)
        intFile.fromCSR.addr.valid      := sel_addr(fromCSR.addr).valid
        intFile.fromCSR.addr.bits       := sel_addr(fromCSR.addr).bits.addr
        intFile.fromCSR.wdata           := sel_wdata(fromCSR.wdata)
        intFile.fromCSR.claim           := fromCSR.claims(pi)
        intFile.illegal_io.illegal_priv := illegal_priv
        vec_rdata(flati)                := intfile_rdata_d
        pendings(flati)                 := intFile.toCSR.pending
        topeis_forEachIntFiles(flati)   := intFile.toCSR.topei
        illegals_forEachIntFiles(flati) := intFile.toCSR.illegal
      }
    }
  }
  toCSR.rdata.valid   := vec_rdata.map(_.valid).reduce(_|_)
  toCSR.rdata.bits    := vec_rdata.map(_.bits).reduce(_|_)
  toCSR.pendings := (pendings.zipWithIndex.map{case (p,i) => p << i.U}).reduce(_ | _) //vector -> multi-bit
  locally {
    // Format of *topei:
    // * bits 26:16 Interrupt identity
    // * bits 10:0 Interrupt priority (same as identity)
    // * All other bit positions are zeros.
    // For detailed explainations of these memory region arguments,
    // please refer to the manual *The RISC-V Advanced Interrupt Architeture*: 3.9. Top external interrupt CSRs
    def wrap(topei: UInt): UInt = {
      val zeros = 0.U((16 - params.imsicIntSrcWidth).W)
      Cat(zeros, topei, zeros, topei)
    }
    toCSR.topeis(0) := wrap(topeis_forEachIntFiles(0)) // m
    toCSR.topeis(1) := wrap(topeis_forEachIntFiles(1)) // s
    toCSR.topeis(2) := wrap(ParallelMux(
      UIntToOH(fromCSR.vgein - 1.U, params.geilen).asBools,
      topeis_forEachIntFiles.drop(2)
    )) // vs
  }  
  val toCSR_illegal_d = RegNext((fromCSR.addr.valid | fromCSR.wdata.valid) & Seq(
    illegals_forEachIntFiles.reduce(_ | _),
    (fromCSR.wdata.valid && fromCSR.wdata.bits.op.asUInt === 0.U),
    illegal_priv
  ).reduce(_ | _))
  toCSR.illegal := toCSR_illegal_d
}

//define IMSIC_WRAP: instance one imsic when HasCVMExtention is supported, else instance two imsic modules.
class IMSIC_WRAP(
    params:    IMSICParams,
    beatBytes: Int = 4
)(implicit p: Parameters) extends Module {
  // define the ports
  val toCSR   = IO(Output(new IMSICToCSRBundle(params)))
  val fromCSR = IO(Input(new CSRToIMSICBundle(params)))
  val msiio = IO(new MSITransBundle(params))
  // define additional ports when HasCVMExtention is supported.
  val sec = if (params.HasTEEIMSIC) Some(IO(new ForCVMBundle()))
  else None // include cmode input port,and o_notice_pending output port.
  val teemsiio = if (params.HasTEEIMSIC) Some(IO(new MSITransBundle(params))) else None
  // instance module,and body logic
  private val imsic = Module(new IMSIC(params, beatBytes))
  imsic.fromCSR := fromCSR
  toCSR         := imsic.toCSR
  imsic.msiio <> msiio
  // define additional logic for sec extention
  // .foreach logic only happens when sec is not none.
  sec.foreach { secIO =>
    // get the sec.mode, connect sec.o_notice_pending to top.
    val cmode          = Wire(Bool())
    val notice_pending = Wire(Bool())
    cmode                := secIO.cmode
    secIO.notice_pending := notice_pending

    // instance tee imsic module.
    val teeimsic = Module(new IMSIC(params, beatBytes))
    teemsiio.foreach(teemsiio => teeimsic.msiio <> teemsiio)
    toCSR.rdata   := Mux(cmode, teeimsic.toCSR.rdata, imsic.toCSR.rdata) // toCSR needs to the selected depending cmode.
    toCSR.illegal := Mux(cmode, teeimsic.toCSR.illegal, imsic.toCSR.illegal)
    val s_pendings = Mux(cmode, teeimsic.toCSR.pendings(params.intFilesNum-1,1), imsic.toCSR.pendings(params.intFilesNum-1,1))
    val m_pendings = imsic.toCSR.pendings(0) // machine mode only from imsic.
    toCSR.pendings := Cat(s_pendings,m_pendings)
    //  toCSR.pendings := VecInit((0 until params.intFilesNum).map(i => pendings(i))) // uint->vector
    
    toCSR.topeis    := Mux(cmode, teeimsic.toCSR.topeis, imsic.toCSR.topeis)
    toCSR.topeis(0) := imsic.toCSR.topeis(0) // machine mode only from imsic.
    // to get the o_notice_pending, excluding the machine interrupt
//    val s_orpend_ree = imsic.toCSR.pendings.slice(1, params.intFilesNum) // extract the | of vector(1,N-1)
//    val s_orpend_tee = teeimsic.toCSR.pendings.slice(1, params.intFilesNum)
//    notice_pending := Mux(cmode, s_orpend_ree.reduce(_ | _), s_orpend_tee.reduce(_ | _))
    val s_orpend_ree = imsic.toCSR.pendings(params.intFilesNum-1,1) // extract the | of vector(1,N-1)
    val s_orpend_tee = teeimsic.toCSR.pendings(params.intFilesNum-1,1) //bit(params.intFilesNum-1:1)
    notice_pending   := Mux(cmode, s_orpend_ree.orR, s_orpend_tee.orR)
    teeimsic.fromCSR := fromCSR
    teeimsic.fromCSR.addr.valid := cmode & fromCSR.addr.valid // cmode=1,controls tee csr access to interrupt file indirectly
    teeimsic.fromCSR.wdata.valid := cmode & fromCSR.wdata.valid
    teeimsic.fromCSR.claims(0)   := false.B // machine interrupts are inactive for tee imsic.
    for (i <- 1 until params.privNum) {
      teeimsic.fromCSR.claims(i) := cmode & fromCSR.claims(i)
    }

    imsic.fromCSR.addr.valid := (cmode === false.B) & fromCSR.addr.valid // cmode=1,controls tee csr access to interrupt file indirectly
    imsic.fromCSR.wdata.valid := (cmode === false.B) & fromCSR.wdata.valid
    imsic.fromCSR.claims(0)   := fromCSR.claims(0) // machine interrupts are inactive for tee imsic.
    for (i <- 1 until params.privNum) {
      imsic.fromCSR.claims(i) := (cmode === false.B) & fromCSR.claims(i)
    }
  }
}

//generate TLIMSIC top module:including TLRegIMSIC_WRAP and IMSIC_WRAP
class TLIMSIC(
    params:    IMSICParams,
    beatBytes: Int = 4
//  asyncQueueParams: AsyncQueueParams
)(implicit p: Parameters) extends LazyModule with HasIMSICParameters {
  val axireg      = LazyModule(new TLRegIMSIC_WRAP(IMSICParams(HasTEEIMSIC = GHasTEEIMSIC), beatBytes))
  lazy val module = new Imp

  class Imp extends LazyModuleImp(this) {
    val toCSR         = IO(Output(new IMSICToCSRBundle(params)))
    val fromCSR       = IO(Input(new CSRToIMSICBundle(params)))
    private val imsic = Module(new IMSIC_WRAP(IMSICParams(HasTEEIMSIC = GHasTEEIMSIC), beatBytes))
    toCSR := imsic.toCSR
    imsic.fromCSR := fromCSR
    axireg.module.msiio <> imsic.msiio // msi_req/msi_ack interconnect
    // define additional ports for cvm extention
    val io_sec = if (GHasTEEIMSIC) Some(IO(new ForCVMBundle()))
    else None // include cmode input port,and o_notice_pending output port.
    /* code on when imsic has two clock domains.*/
    // --- define soc_clock for imsic bus logic ***//
    val soc_clock = IO(Input(Clock()))
    val soc_reset = IO(Input(Reset()))
    axireg.module.clock := soc_clock
    axireg.module.reset := soc_reset
    imsic.clock         := clock
    imsic.reset         := reset
    axireg.module.msiio <> imsic.msiio // msi_req/msi_ack interconnect
    // code will be compiled only when io_sec is not None.
    io_sec.foreach(iosec => imsic.sec.foreach(imsicsec => imsicsec <> iosec))
    // code will be compiled only when tee_axireg is not None.
    axireg.module.teemsiio.foreach(tee_msi_trans => imsic.teemsiio.foreach(teemsiio => tee_msi_trans <> teemsiio))
  }
}

class AXI4IMSIC(
    params:    IMSICParams,
    beatBytes: Int = 4
)(implicit p: Parameters) extends LazyModule with HasIMSICParameters {
  val axireg      = LazyModule(new AXIRegIMSIC_WRAP(IMSICParams(HasTEEIMSIC = GHasTEEIMSIC), beatBytes))
  lazy val module = new Imp
  class Imp extends LazyModuleImp(this) {
    val toCSR         = IO(Output(new IMSICToCSRBundle(params)))
    val fromCSR       = IO(Input(new CSRToIMSICBundle(params)))
    private val imsic = Module(new IMSIC_WRAP(IMSICParams(HasTEEIMSIC = GHasTEEIMSIC), beatBytes))
    toCSR := imsic.toCSR
    imsic.fromCSR := fromCSR
    axireg.module.msiio <> imsic.msiio // msi_req/msi_ack interconnect
    // define additional ports for cvm extention
    val io_sec = if (GHasTEEIMSIC) Some(IO(new ForCVMBundle()))
    else None // include cmode input port,and o_notice_pending output port.
    /* code on when imsic has two clock domains.*/
    // --- define soc_clock for imsic bus logic ***//
    val soc_clock = IO(Input(Clock()))
    val soc_reset = IO(Input(Reset()))
    axireg.module.clock := soc_clock
    axireg.module.reset := soc_reset
    imsic.clock         := clock
    imsic.reset         := reset
    // code will be compiled only when io_sec is not None.
    io_sec.foreach(iosec => imsic.sec.foreach(imsicsec => imsicsec <> iosec))
    // code will be compiled only when tee_axireg is not None.
    axireg.module.teemsiio.foreach(tee_msi_trans => imsic.teemsiio.foreach(teemsiio => tee_msi_trans <> teemsiio))
  }
}


// code below is for SEC IMSIC spec
//generate TLRegIMSIC_WRAP for IMSIC, when HasCVMExtention is supported, IMSIC is instantiated by two times,else only one
class TLRegIMSIC_WRAP(
    params:    IMSICParams,
    beatBytes: Int = 4
)(implicit p: Parameters) extends LazyModule {
  val axireg = LazyModule(new TLRegIMSIC(params, beatBytes)(Parameters.empty))
  val tee_axireg =
    if (params.HasTEEIMSIC) Some(LazyModule(new TLRegIMSIC(params, beatBytes)(Parameters.empty))) else None
  lazy val module = new TLRegIMSICImp(this)

  class TLRegIMSICImp(outer: LazyModule) extends LazyModuleImp(outer) {
    val msiio = IO(Flipped(new MSITransBundle(params)))
    msiio <> axireg.module.msiio
    val teemsiio = if (params.HasTEEIMSIC) Some(IO(Flipped(new MSITransBundle(params))))
      else None // backpressure signal for axi4bus, from imsic working on cpu clock

    // code below will be compiled only when teeio is not none.
    teemsiio.foreach(teemsiio => tee_axireg.foreach(tee_axireg => teemsiio <> tee_axireg.module.msiio))
  }
}

//generate AXIRegIMSIC_WRAP for IMSIC, when HasCVMExtention is supported, IMSIC is instantiated by two times,else only one
class AXIRegIMSIC_WRAP(
    params:    IMSICParams,
    beatBytes: Int = 4
)(implicit p: Parameters) extends LazyModule {
  val axireg = LazyModule(new AXIRegIMSIC(params, beatBytes)(Parameters.empty))
  //  val tee_axireg = if (params.HasTEEIMSIC) Some(LazyModule(new AXIRegIMSIC(IMSICParams(teemode = true), beatBytes)(Parameters.empty))) else None
  val tee_axireg =
    if (params.HasTEEIMSIC) Some(LazyModule(new AXIRegIMSIC(params, beatBytes)(Parameters.empty))) else None
  lazy val module = new AXIRegIMSICImp(this)

  class AXIRegIMSICImp(outer: LazyModule) extends LazyModuleImp(outer) {
    val msiio = IO(Flipped(new MSITransBundle(params))) // backpressure signal for axi4bus, from imsic working on cpu clock
    msiio <> axireg.module.msiio
    val teemsiio = if (params.HasTEEIMSIC) Some(IO(Flipped(new MSITransBundle(params))))
    else None // backpressure signal for axi4bus, from imsic working on cpu clock
    // code below will be compiled only when teeio is not none.
    teemsiio.foreach(teemsiio => tee_axireg.foreach(tee_axireg => teemsiio <> tee_axireg.module.msiio))
  }
}

class TLRegIMSIC(
    params:      IMSICParams,
    beatBytes:   Int = 4,
    seperateBus: Boolean = false
)(implicit p: Parameters) extends LazyModule {
  val fromMem = Seq.fill(if (seperateBus) 2 else 1)(TLXbar())
  // val fromMem = LazyModule(new TLXbar).node
  private val intfileFromMems = Seq(
    AddressSet(params.mAddr, pow2(params.intFileMemWidth) - 1),
    AddressSet(params.sgAddr, pow2(params.intFileMemWidth) * pow2(log2Ceil(1 + params.geilen)) - 1)
  ).zipWithIndex.map { case (addrset, i) =>
    val intfileFromMem = TLRegMapperNode(
      address = Seq(addrset),
      beatBytes = beatBytes
    )
    intfileFromMem := (if (seperateBus) fromMem(i) else fromMem.head)
    intfileFromMem
  }

  lazy val module = new TLRegIMSICImp(this)
  class TLRegIMSICImp(outer: LazyModule) extends LazyModuleImp(outer) {
    val msiio = IO(Flipped(new MSITransBundle(params)))  // backpressure signal for axi4bus, from imsic working on cpu clock
    private val reggen = Module(new RegGen(params, beatBytes))
    // ---- instance sync fifo ----//
    // --- fifo wdata: {vector_valid,setipnum}, fifo wren: |vector_valid---//
    val FifoDataWidth = params.MSI_INFO_WIDTH
    val fifo_wdata    = Wire(Valid(UInt(FifoDataWidth.W)))

    // depth:8, data width: FifoDataWidth
    private val fifo_sync = Module(new Queue(UInt(FifoDataWidth.W), 8))
    // define about fifo write
    fifo_wdata.bits        := reggen.io.seteipnum
    fifo_wdata.valid       := reggen.io.valid
    fifo_sync.io.enq.valid := fifo_wdata.valid
    fifo_sync.io.enq.bits  := fifo_wdata.bits
    // fifo rd,controlled by msi_vld_ack from imsic working on csr clock.
    // msi_vld_ack_soc: sync result with soc clock
    val msi_vld_ack_soc = WireInit(false.B)
    val msi_vld_ack_cpu = msiio.vld_ack
    val msi_vld_req     = RegInit(false.B)
    when(params.EnableImsicAsyncBridge.B) {
      msi_vld_ack_soc := AsyncResetSynchronizerShiftReg(msi_vld_ack_cpu, 3, 0)
    }.otherwise {
      msi_vld_ack_soc := msi_vld_ack_cpu
    }
    fifo_sync.io.deq.ready := ~msi_vld_req
    // generate the msi_vld_req: high if ~empty,low when msi_vld_ack_soc
    msiio.vld_req := msi_vld_req
    val msi_vld_ack_soc_1f  = RegNext(msi_vld_ack_soc)
    val msi_vld_ack_soc_ris = msi_vld_ack_soc & (~msi_vld_ack_soc_1f)
    //    val fifo_empty = ~fifo_sync.io.deq.valid
    // msi_vld_req : high when fifo empty is false, low when ack is high. and io.deq.valid := ~empty
    when(fifo_sync.io.deq.valid === true.B) {
      msi_vld_req := true.B
    }.elsewhen(msi_vld_ack_soc_ris) {
      msi_vld_req := false.B
    }.otherwise {
      msi_vld_req := msi_vld_req
    }

    // get the msi interrupt ID info
    val msi_id_data = RegInit(0.U(params.MSI_INFO_WIDTH.W))
    val rdata_vld   = fifo_sync.io.deq.fire // assign to fifo rdata
    when(rdata_vld) { // fire: io.deq.valid & io.deq.ready
      msi_id_data := fifo_sync.io.deq.bits(params.MSI_INFO_WIDTH - 1, 0)
    }.otherwise {
      msi_id_data := msi_id_data
    }
    // port connect: io.valid is interrupt file index info.
    msiio.data := msi_id_data
    val backpress = fifo_sync.io.enq.ready
    (intfileFromMems zip reggen.regmapIOs).map {
      case (intfileFromMem, regmapIO) => intfileFromMem.regmap(regmapIO._1, regmapIO._2, backpress)
    }
  }
}


//generate axi42reg for IMSIC
class AXIRegIMSIC(
    params:      IMSICParams,
    beatBytes:   Int = 4,
    seperateBus: Boolean = false
)(implicit p: Parameters) extends LazyModule {
  val fromMem = Seq.fill(if (seperateBus) 2 else 1)(AXI4Xbar())
  val axi4tolite = Seq.fill(if (seperateBus) 2 else 1)(LazyModule(new AXI4ToLite()(Parameters.empty)))
  fromMem zip axi4tolite.map(_.node) foreach (x => x._1 := x._2)
  private val intfileFromMems = Seq(
    AddressSet(params.mAddr, pow2(params.intFileMemWidth) - 1),
    AddressSet(params.sgAddr, pow2(params.intFileMemWidth) * pow2(log2Ceil(1 + params.geilen)) - 1)
  ).zipWithIndex.map { case (addrset, i) =>
    val intfileFromMem = AXI4RegMapperNode(
      address = addrset,
      beatBytes = beatBytes
    )
    intfileFromMem := (if (seperateBus) fromMem(i) else fromMem.head)
    intfileFromMem
  }
  
  lazy val module = new AXIRegIMSICImp(this)
  class AXIRegIMSICImp(outer: LazyModule) extends LazyModuleImp(outer) {
    val msiio          = IO(Flipped(new MSITransBundle(params))) // backpressure signal for axi4bus, from imsic working on cpu clock
    private val reggen = Module(new RegGen(params, beatBytes))
    // ---- instance sync fifo ----//
    // --- fifo wdata: {vector_valid,setipnum}, fifo wren: |vector_valid---//
    val FifoDataWidth = params.MSI_INFO_WIDTH
    val fifo_wdata    = Wire(Valid(UInt(FifoDataWidth.W)))

    // depth:8, data width: FifoDataWidth
    private val fifo_sync = Module(new Queue(UInt(FifoDataWidth.W), 8))
    // define about fifo write
    fifo_wdata.bits        := reggen.io.seteipnum
    fifo_wdata.valid       := reggen.io.valid
    fifo_sync.io.enq.valid := fifo_wdata.valid
    fifo_sync.io.enq.bits  := fifo_wdata.bits
    // fifo rd,controlled by msi_vld_ack from imsic working on csr clock.
    // msi_vld_ack_soc: sync result with soc clock
    val msi_vld_ack_soc = WireInit(false.B)
    val msi_vld_ack_cpu = msiio.vld_ack
    val msi_vld_req     = RegInit(false.B)
    when(params.EnableImsicAsyncBridge.B) {
      msi_vld_ack_soc := AsyncResetSynchronizerShiftReg(msi_vld_ack_cpu, 3, 0)
    }.otherwise {
      msi_vld_ack_soc := msi_vld_ack_cpu
    }
    fifo_sync.io.deq.ready := ~msi_vld_req
    // generate the msi_vld_req: high if ~empty,low when msi_vld_ack_soc
    msiio.vld_req := msi_vld_req
    val msi_vld_ack_soc_1f  = RegNext(msi_vld_ack_soc)
    val msi_vld_ack_soc_ris = msi_vld_ack_soc & (~msi_vld_ack_soc_1f)
    // val fifo_empty = ~fifo_sync.io.deq.valid
    // msi_vld_req : high when fifo empty is false, low when ack is high. and io.deq.valid := ~empty
    when(fifo_sync.io.deq.valid === true.B) {
      msi_vld_req := true.B
    }.elsewhen(msi_vld_ack_soc_ris) {
      msi_vld_req := false.B
    }.otherwise {
      msi_vld_req := msi_vld_req
    }

    // get the msi interrupt ID info
    val msi_id_data = RegInit(0.U(params.MSI_INFO_WIDTH.W))
    val rdata_vld   = fifo_sync.io.deq.fire // assign to fifo rdata
    when(rdata_vld) { // fire: io.deq.valid & io.deq.ready
      msi_id_data := fifo_sync.io.deq.bits(params.MSI_INFO_WIDTH - 1, 0)
    }.otherwise {
      msi_id_data := msi_id_data
    }
    // port connect: io.valid is interrupt file index info.
    msiio.data := msi_id_data
    val backpress = fifo_sync.io.enq.ready
    (intfileFromMems zip reggen.regmapIOs).map {
      case (intfileFromMem, regmapIO) => intfileFromMem.regmap(regmapIO._1, regmapIO._2, backpress)
    }
  }
}

//integrated for async clock domain,kmh,zhaohong
class RegGen(
    params:    IMSICParams,
    beatBytes: Int = 4
) extends Module {
  val regmapIOs = Seq(
    params.intFileMemWidth,
    params.intFileMemWidth + log2Ceil(1 + params.geilen)
  ).map { width =>
    val regmapParams = RegMapperParams(width - log2Up(beatBytes), beatBytes)
    (IO(Flipped(Decoupled(new RegMapperInput(regmapParams)))), IO(Decoupled(new RegMapperOutput(regmapParams))))
  }
  // define the output reg: seteipnum is the MSI id,vld[],valid flag for interrupt file domains: m,s,vs1~vsgeilen
  val io = IO(Output(new Bundle {
    val seteipnum = UInt(params.MSI_INFO_WIDTH.W)
    val valid     = Bool()
  }))
  val valids       = WireInit(VecInit(Seq.fill(params.intFilesNum)(false.B)))
  val seteipnums   = WireInit(VecInit(Seq.fill(params.intFilesNum)(0.U(params.imsicIntSrcWidth.W))))
  val outseteipnum = RegInit(0.U(params.MSI_INFO_WIDTH.W))
  val outvalids    = RegInit(VecInit(Seq.fill(params.intFilesNum)(false.B)))

  (regmapIOs zip Seq(1, 1 + params.geilen)).zipWithIndex.map { // seq[0]: m interrupt file, seq[1]: s&vs interrupt file
    case ((regmapIO: (DecoupledIO[RegMapperInput], DecoupledIO[RegMapperOutput]), intFilesNum: Int), i: Int) =>
      {
        // j: index is 0 for m file for seq[0],index is 0~params.geilen for S intFile for seq[1]: S, G1, G2, ...
        val maps = (0 until intFilesNum).map { j =>
          val flati = i + j // seq[0]:0+0=0;seq[1]:(0~geilen)+1
          val seteipnum = WireInit(0.U.asTypeOf(Valid(UInt(params.imsicIntSrcWidth.W)))); /*for debug*/
          dontTouch(seteipnum)
          valids(flati)     := seteipnum.valid
          seteipnums(flati) := seteipnum.bits
          j * pow2(params.intFileMemWidth).toInt -> Seq(RegField(
            32,
            0.U,
            RegWriteFn { (valid, data) =>
              when(valid) { seteipnum.bits := data(params.imsicIntSrcWidth - 1, 0); seteipnum.valid := true.B }; true.B
            }
          ))
        }
        regmapIO._2 <> RegMapper(beatBytes, 1, true, regmapIO._1, maps: _*)
      }
      for (i <- 0 until params.intFilesNum) {
        when(valids(i)) {
          outseteipnum := Cat(i.U, seteipnums(i))
        }
      }
      outvalids    := valids
      io.seteipnum := outseteipnum
      io.valid     := outvalids.reduce(_ | _)
  }
}
