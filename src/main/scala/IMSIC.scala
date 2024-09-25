package aia

import chisel3._
import chisel3.util._
import freechips.rocketchip.diplomacy._
import org.chipsalliance.cde.config.Parameters

// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage

case class IntFileParams(
  membersNum      : Int    = 2          , // h_max: members number with in a group

  mBaseAddr       : BigInt = 0x61000000, // A: base addr for machine-level interrupt files
  mStrideBits     : Int    = 12        , // C: stride between each machine-level interrupt files

  sgBaseAddr      : BigInt = 0x82900000, // B: base addr for supervisor- and guest-level interrupt files
  sgStrideBits    : Int    = 15        , // D: stride between each supervisor- and guest-level interrupt files
  geilen          : Int    = 4         , // number of guest interrupt files

  groupsNum       : Int    = 1         , // g_max: groups number
  groupStrideBits : Int    = 16        , // E: stride between each interrupt file groups
) {
  def pow2(n: Int) = BigInt(1) << n

  val k: Int = log2Ceil(membersNum)
  println("IntFileParams.k: " + k)
  require((mBaseAddr & (pow2(k + mStrideBits) -1)) == 0) // A should be aligned to a 2^(k+C)
  require(mStrideBits >= 12)

  require(sgStrideBits >= log2Ceil(geilen+1)+12)

  require(groupStrideBits >= k + math.max(mStrideBits, sgStrideBits))

  val j: Int = log2Ceil(groupsNum + 1)
  println("IntFileParams.j: " + j)
  require((sgBaseAddr & (pow2(k + sgStrideBits) - 1)) == 0) // B should be aligned to a 2^(k+D)

  require((
    ((pow2(j)-1) * pow2(groupStrideBits))
    & mBaseAddr
  ) == 0)
  require((
    ((pow2(j)-1) * pow2(groupStrideBits))
    & sgBaseAddr
  ) == 0)

  println("IntFileParams.membersNum:      " + membersNum     )
  println("IntFileParams.mBaseAddr:       " + mBaseAddr      ) // TODO: hex
  println("IntFileParams.mStrideBits:     " + mStrideBits    )
  println("IntFileParams.sgBaseAddr:      " + sgBaseAddr     ) // TODO: hex
  println("IntFileParams.sgStrideBits:    " + sgStrideBits   )
  println("IntFileParams.geilen:          " + geilen         )
  println("IntFileParams.groupsNum:       " + groupsNum      )
  println("IntFileParams.groupStrideBits: " + groupStrideBits)
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
