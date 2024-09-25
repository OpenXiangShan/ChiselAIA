package aia

import chisel3._
import chisel3.util._
import freechips.rocketchip.diplomacy._
import org.chipsalliance.cde.config.Parameters

// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage

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
  def pow2(n: Int) = 1L << n

  val k: Int = log2Ceil(membersNum)
  println(f"IntFileParams.k: ${k}%d")
  require((mBaseAddr & (pow2(k + mStrideBits) -1)) == 0) // A should be aligned to a 2^(k+C)
  require(mStrideBits >= 12)

  require(sgStrideBits >= log2Ceil(geilen+1)+12)

  require(groupStrideBits >= k + math.max(mStrideBits, sgStrideBits))

  val j: Int = log2Ceil(groupsNum + 1)
  println(f"IntFileParams.j: ${j}%d")
  require((sgBaseAddr & (pow2(k + sgStrideBits) - 1)) == 0) // B should be aligned to a 2^(k+D)

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

class TLIMSIC(
  intFileParams: IntFileParams,
)(implicit p: Parameters) extends LazyModule {
  // TODO
  // val intFileNode: TLRegisterNode = TLRegisterNode(
  // )

  lazy val module = new LazyModuleImp(this) {
  }
}

/**
 * Generate Verilog sources
 */
object TLIMSIC extends App {
  ChiselStage.emitSystemVerilogFile(
    (LazyModule(
      new TLIMSIC(
        IntFileParams(),
      )(Parameters.empty)
    )).module,
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}
