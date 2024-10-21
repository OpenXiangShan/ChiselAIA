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

import chisel3.util._

object pow2 {
  def apply(n: Int): Long = 1L << n
}

case class IMSICParams(
  // # IMSICParams Arguments
  xlen            : Int  = 64          ,
  intSrcWidth     : Int  = 8          ,// log2(number of interrupt sources)
  // ## Arguments for interrupt file's memory region
  // For detailed explainations of these memory region arguments,
  // please refer to the manual *The RISC-V Advanced Interrupt Architeture*: 3.6. Arrangement of the memory regions of multiple interrupt files
  membersNum      : Int  = 2           ,// h_max: members number with in a group
  mBaseAddr       : Long = 0x61000000L ,// A: base addr for machine-level interrupt files
  sgBaseAddr      : Long = 0x82900000L ,// B: base addr for supervisor- and guest-level interrupt files
  geilen          : Int  = 4           ,// number of guest interrupt files
  groupsNum       : Int  = 1           ,// g_max: groups number
  // ## Arguments for CSRs
  vgeinWidth      : Int  = 6           ,
  // ### Arguments for indirect accessed CSRs, aka, CSRs accessed by *iselect and *ireg
  iselectWidth    : Int  = 12          ,
) {
  // # IMSICParams Arguments
  require(xlen == 64, "currently only support xlen = 64")
  val xlenWidth = log2Ceil(xlen)
  require(intSrcWidth <= 11, f"intSrcWidth=${intSrcWidth}, must not greater than log2(2048)=11, as there are at most 2048 eip/eie bits")
  val privNum     : Int  = 3            // number of privilege modes: machine, supervisor, virtualized supervisor
  val intFilesNum : Int  = 2 + geilen   // number of interrupt files, m, s, vs0, vs1, ...
  val eixNum      : Int  = pow2(intSrcWidth).toInt / xlen // number of eip/eie registers

  // ## Arguments for interrupt file's memory region
  val intFileMemWidth : Int  = 12        // interrupt file memory region width: 12-bit width => 4KB size
  val membersWidth    : Int = log2Ceil(membersNum) // k
  // require(mStrideWidth >= intFileMemWidth)
  val mStrideWidth    : Int  = intFileMemWidth // C: stride between each machine-level interrupt files
  require((mBaseAddr & (pow2(membersWidth + mStrideWidth) -1)) == 0, "mBaseAddr should be aligned to a 2^(k+C)")
  // require(sgStrideWidth >= log2Ceil(geilen+1) + intFileMemWidth)
  val sgStrideWidth   : Int = log2Ceil(geilen+1) + intFileMemWidth // D: stride between each supervisor- and guest-level interrupt files
  // require(groupStrideWidth >= k + math.max(mStrideWidth, sgStrideWidth))
  val groupStrideWidth: Int = membersWidth + math.max(mStrideWidth, sgStrideWidth) // E: stride between each interrupt file groups
  val groupsWidth     : Int = log2Ceil(groupsNum) // j
  require((sgBaseAddr & (pow2(membersWidth + sgStrideWidth) - 1)) == 0, "sgBaseAddr should be aligned to a 2^(k+D)")
  require(( ((pow2(groupsWidth)-1) * pow2(groupStrideWidth)) & mBaseAddr ) == 0)
  require(( ((pow2(groupsWidth)-1) * pow2(groupStrideWidth)) & sgBaseAddr) == 0)

  println(f"IMSICParams.membersWidth:      ${membersWidth    }%d")
  println(f"IMSICParams.groupsWidth:       ${groupsWidth     }%d")
  println(f"IMSICParams.membersNum:        ${membersNum      }%d")
  println(f"IMSICParams.mBaseAddr:       0x${mBaseAddr       }%x")
  println(f"IMSICParams.mStrideWidth:      ${mStrideWidth    }%d")
  println(f"IMSICParams.sgBaseAddr:      0x${sgBaseAddr      }%x")
  println(f"IMSICParams.sgStrideWidth:     ${sgStrideWidth   }%d")
  println(f"IMSICParams.geilen:            ${geilen          }%d")
  println(f"IMSICParams.groupsNum:         ${groupsNum       }%d")
  println(f"IMSICParams.groupStrideWidth:  ${groupStrideWidth}%d")

  // ## Arguments for CSRs
  require(vgeinWidth >= log2Ceil(geilen))
  // ### Arguments for indirect accessed CSRs, aka, CSRs accessed by *iselect and *ireg
  require(iselectWidth >=8, f"iselectWidth=${iselectWidth} needs to be able to cover addr [0x70, 0xFF], that is from CSR eidelivery to CSR eie63")

  def hartIndex_to_gh(hartIndex: Int): (Int, Int) = {
    val g = (hartIndex>>membersWidth) & (pow2(groupsWidth)-1)
    val h = hartIndex & (pow2(membersWidth)-1)
    (g.toInt, h.toInt)
  }
  def gh_to_hartIndex(g: Int, h: Int): Int = {
    (g<<membersWidth) | h
  }
}

case class APLICParams(
  intSrcWidth: Int = 7, // Noted: APLIC's int source num is LESS THAN IMSIC's
  baseAddr: Long = 0x19960000L,
) {
  require(intSrcWidth <= 10, f"intSrcWidth=${intSrcWidth}, must not greater than log2(1024)=10, as there are at most 1023 sourcecfgs")
  val intSrcNum: Int = pow2(intSrcWidth).toInt
  val ixNum: Int = pow2(intSrcWidth).toInt / 32
  val domainMemWidth : Int  = 14 // interrupt file memory region width: 14-bit width => 16KB size
}
