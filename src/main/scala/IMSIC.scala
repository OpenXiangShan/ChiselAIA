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

// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage

object pow2 {
  def apply(n: Int): Long = 1L << n
}

case class IntFileParams(
  membersNum      : Int  = 2           ,// h_max: members number with in a group

  mBaseAddr       : Long = 0x61000000L ,// A: base addr for machine-level interrupt files
  mStrideBits     : Int  = 12          ,// C: stride between each machine-level interrupt files

  sgBaseAddr      : Long = 0x82900000L ,// B: base addr for supervisor- and guest-level interrupt files
  sgStrideBits    : Int  = 15          ,// D: stride between each supervisor- and guest-level interrupt files
  geilen          : Int  = 4           ,// number of guest interrupt files

  groupsNum       : Int  = 1           ,// g_max: groups number
  groupStrideBits : Int  = 16          ,// E: stride between each interrupt file groups
) {
  val intFileWidth: Int  = 12           // interrupt file width: 12-bit width => 4KB size

  val k: Int = log2Ceil(membersNum)
  println(f"IntFileParams.k: ${k}%d")
  require((mBaseAddr & (pow2(k + mStrideBits) -1)) == 0, "mBaseAddr should be aligned to a 2^(k+C)")
  require(mStrideBits >= 12)

  require(sgStrideBits >= log2Ceil(geilen+1)+12)

  require(groupStrideBits >= k + math.max(mStrideBits, sgStrideBits))

  val j: Int = log2Ceil(groupsNum + 1)
  println(f"IntFileParams.j: ${j}%d")
  require((sgBaseAddr & (pow2(k + sgStrideBits) - 1)) == 0, "sgBaseAddr should be aligned to a 2^(k+D)")

  require((
    ((pow2(j)-1) * pow2(groupStrideBits))
    & mBaseAddr
  ) == 0)
  require((
    ((pow2(j)-1) * pow2(groupStrideBits))
    & sgBaseAddr
  ) == 0)

  println(f"IntFileParams.membersNum:        ${membersNum     }%d")
  println(f"IntFileParams.mBaseAddr:       0x${mBaseAddr      }%x")
  println(f"IntFileParams.mStrideBits:       ${mStrideBits    }%d")
  println(f"IntFileParams.sgBaseAddr:      0x${sgBaseAddr     }%x")
  println(f"IntFileParams.sgStrideBits:      ${sgStrideBits   }%d")
  println(f"IntFileParams.geilen:            ${geilen         }%d")
  println(f"IntFileParams.groupsNum:         ${groupsNum      }%d")
  println(f"IntFileParams.groupStrideBits:   ${groupStrideBits}%d")
}

// TODO: implement all signals in the belowing two bundles
// Based on Xiangshan NewCSR
object OpType extends ChiselEnum {
  val ILLEGAL = Value(0.U)
  val CSRRW   = Value(1.U)
  val CSRRS   = Value(2.U)
  val CSRRC   = Value(3.U)
}
class CSRToIMSICBundle extends Bundle {
  private final val AddrWidth = 12

  val addr = ValidIO(new Bundle {
    val addr = UInt(AddrWidth.W)
    // val virt = Bool()
    // val priv = UInt(2.W) // U, S, M
  })

  // val VGEINWidth = 6
  // val vgein = UInt(VGEINWidth.W)

  val wdata = ValidIO(new Bundle {
    val op = OpType()
    val data = UInt(64.W)
  })

  val mClaim = Bool()
  // val sClaim = Bool()
  // val vsClaim = Bool()
}
class IMSICToCSRBundle extends Bundle {
  // private val NumVSIRFiles = 63
  val rdata = ValidIO(new Bundle {
    val data = UInt(64.W)
    // TODO:
    // val illegal = Bool()
  })
  val meipB    = Bool()
  // val seipB    = Bool()
  // val vseipB   = UInt(NumVSIRFiles.W)
  // 11 bits: 32*64 = 2048 interrupt sources
  val mtopei  = UInt(11.W)
  // val stopei  = UInt(11.W)
  // val vstopei = UInt(11.W)
}


class IntFile extends Module {
  // TODO: unify this parameterization with CSRToIMSICBundle's
  private final val AddrWidth = 12

  val fromCSR = IO(Input(new Bundle {
    val seteipnum = ValidIO(new Bundle {
      val value = UInt(32.W)
    })
    val addr = ValidIO(new Bundle {
      val addr = UInt(AddrWidth.W)
    })
    val wdata = ValidIO(new Bundle {
      val op = OpType()
      val data = UInt(64.W)
    })
    val claim = Bool()
  }))

  val toCSR = IO(Output(new Bundle {
    val rdata = ValidIO(new Bundle {
      val data = UInt(64.W)
      // TODO:
      // val illegal = Bool()
    })
    val eipB = Bool()
    // 11 bits: 32*64 = 2048 interrupt sources
    val topei  = UInt(11.W)
  }))

  // TODO: LazyModule?
    // TODO: Add a struct for these CSRs in a interrupt file
    /// indirect CSRs
    val eidelivery = RegInit(1.U(64.W)) // TODO: default: disable it
    val eithreshold = RegInit(0.U(64.W))
    // TODO: eips(0)(0) is read-only false.B
    val eips = RegInit(VecInit.fill(32){0.U(64.W)})
    // TODO: eies(0)(0) is read-only false.B
    val eies = RegInit(VecInit.fill(32){Fill(64, 1.U)}) // TODO: default: disable all
    dontTouch(eips)

    locally { // scope for xiselect CSR reg map
      val wdata = WireDefault(0.U(64.W))
      val wmask = WireDefault(0.U(64.W))
      when(fromCSR.wdata.valid) {
        switch(fromCSR.wdata.bits.op) {
          is(OpType.ILLEGAL) {
            // TODO
          }
          is(OpType.CSRRW) {
            wdata := fromCSR.wdata.bits.data
            wmask := Fill(64, 1.U)
          }
          is(OpType.CSRRS) {
            wdata := Fill(64, 1.U)
            wmask := fromCSR.wdata.bits.data
          }
          is(OpType.CSRRC) {
            wdata := 0.U
            wmask := fromCSR.wdata.bits.data
          }
        }
      }
      RegMap.generate(
        Map(
          RegMap(0x70, eidelivery),
          RegMap(0x72, eithreshold),
        ) ++ eips.zipWithIndex.map { case (eip: UInt, i: Int) =>
          RegMap(0x80+i*2, eip)
        }.toMap ++ eies.zipWithIndex.map { case (eie: UInt, i: Int) =>
          RegMap(0xC0+i*2, eie)
        },
        /*raddr*/ fromCSR.addr.bits.addr,
        /*rdata*/ toCSR.rdata.bits.data,
        /*waddr*/ fromCSR.addr.bits.addr,
        /*wen  */ fromCSR.wdata.valid,
        /*wdata*/ wdata,
        /*wmask*/ wmask,
      )
      toCSR.rdata.valid := RegNext(fromCSR.addr.valid)
    } // end of scope for xiselect CSR reg map
    // TODO: End of the CSRs for a interrupt file

    // TODO: parameterization
    // TODO: locally: shorter name fromCSR.seteipnum.bits.value
    when (
      fromCSR.seteipnum.valid
      & eies(fromCSR.seteipnum.bits.value(10,6))(fromCSR.seteipnum.bits.value(5,0))
    ) {
      // set eips bit
      eips(fromCSR.seteipnum.bits.value(10,6)) := eips(fromCSR.seteipnum.bits.value(10,6)) | (1.U << (fromCSR.seteipnum.bits.value(5,0)))
    }

    locally { // scope for xtopei
      // The ":+ true.B" trick explain:
      //  Append true.B to handle the cornor case, where all bits in eip and eie are disabled.
      //  If do not append true.B, then we need to check whether the eip & eie are empty,
      //  otherwise, the returned topei will become the max index, that is 2048-1
      // TODO: require the support max interrupt sources number must be 2^N
      //              [0,                2^N-1] :+ 2^N
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
    }
    toCSR.eipB := toCSR.topei =/= 0.U

    when(fromCSR.claim) {
      // clear the pending bit indexed by xtopei in xeip
      eips(toCSR.topei(10,6)) := eips(toCSR.topei(10,6)) & ~(1.U << toCSR.topei(5,0))
    }
}

class TLIMSIC(
  intFileParams: IntFileParams,
  groupID: Int = 0, // g
  memberID: Int = 1, // h
  beatBytes: Int = 8,
)(implicit p: Parameters) extends LazyModule {
  require(groupID < intFileParams.groupsNum,    f"groupID ${groupID} should less than groupsNum ${intFileParams.groupsNum}")
  require(memberID < intFileParams.membersNum,  f"memberID ${memberID} should less than membersNum ${intFileParams.membersNum}")
  println(f"groupID:  0x${groupID }%x")
  println(f"memberID: 0x${memberID}%x")

  val device: SimpleDevice = new SimpleDevice(
    "interrupt-controller",
    Seq(f"riscv,imsic.${groupID}%d.${memberID}%d")
  ) {}


  // addr for the machine-level interrupt file: g*2^E + A + h*2^C
  val mAddr = AddressSet(
    groupID * pow2(intFileParams.groupStrideBits) + intFileParams.mBaseAddr + memberID * pow2(intFileParams.mStrideBits),
    pow2(intFileParams.intFileWidth) - 1
  )
  // addr for the supervisor-level and guest-level interrupt files: g*2^E + B + h*2^D
  val sgAddr = AddressSet(
    groupID * pow2(intFileParams.groupStrideBits) + intFileParams.sgBaseAddr + memberID * pow2(intFileParams.sgStrideBits),
    pow2(intFileParams.intFileWidth) * (1+intFileParams.geilen) - 1
  )
  println(f"mAddr:  [0x${mAddr.base }%x, 0x${mAddr.max }%x]")
  println(f"sgAddr: [0x${sgAddr.base}%x, 0x${sgAddr.max}%x]")

  val mTLNode: TLRegisterNode = TLRegisterNode(
    address = Seq(mAddr),
    device = device,
    beatBytes = beatBytes,
    undefZero = true,
    concurrency = 1
  )

  lazy val module = new Imp
  class Imp extends LazyModuleImp(this) {
    val toCSR = IO(Output(new IMSICToCSRBundle))
    val fromCSR = IO(Input(new CSRToIMSICBundle))


    // TODO: directly access TL protocol, instead of use the regmap
    val mseteipnum = RegInit(0.U(32.W))
    val mseteipnumRF = RegField(32, mseteipnum)
    mTLNode.regmap(
      0 -> Seq(mseteipnumRF)
    )
    when (mseteipnum =/= 0.U) {
      mseteipnum := 0.U
    }
    val mIntFile = Module(new IntFile)
    mIntFile.fromCSR.seteipnum.valid      := mseteipnum =/= 0.U
    mIntFile.fromCSR.seteipnum.bits.value := mseteipnum
    mIntFile.fromCSR.addr.valid           := fromCSR.addr.valid
    mIntFile.fromCSR.addr.bits.addr       := fromCSR.addr.bits.addr
    mIntFile.fromCSR.wdata                := fromCSR.wdata
    mIntFile.fromCSR.claim                := fromCSR.mClaim
    toCSR.rdata  := mIntFile.toCSR.rdata
    toCSR.meipB  := mIntFile.toCSR.eipB
    toCSR.mtopei := mIntFile.toCSR.topei
  }
}

class TLIMSICWrapper()(implicit p: Parameters) extends LazyModule {
  val tl = TLClientNode(
    Seq(TLMasterPortParameters.v1(
      Seq(TLMasterParameters.v1("tl", IdRange(0, 16)))
  )))

  val imsic = LazyModule(new TLIMSIC(IntFileParams())(Parameters.empty))
  imsic.mTLNode := tl

  lazy val module = new LazyModuleImp(this) {
    tl.makeIOs()
    val toCSR = IO(Output(new IMSICToCSRBundle))
    val fromCSR = IO(Input(new CSRToIMSICBundle))
    toCSR   <> imsic.module.toCSR
    fromCSR <> imsic.module.fromCSR

    dontTouch(imsic.module.toCSR)
    dontTouch(imsic.module.fromCSR)
  }
}

/**
 * Generate Verilog sources
 */
object TLIMSIC extends App {
  val top = DisableMonitors(p => LazyModule(
    new TLIMSICWrapper()(Parameters.empty))
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
