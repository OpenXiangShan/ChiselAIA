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

import chisel3.util._

object pow2 {
  def apply(n: Int): Long = 1L << n
}

//MC 本节概述了APLIC和IMSIC的可配置参数。
//MC 虽然提供了默认值，但我们强烈建议根据具体的集成需求，自定义带有👉标记的参数。
//MC 其他参数要么是派生的，要么是硬编码的（详情参见`Params.scala`）。
//MC
//MC This section outlines the configurable parameters for APLIC and IMSIC.
//MC While defaul values are provided,
//MC we strongly recommend customizing parameters marked with 👉 to suit your specific integration needs.
//MC Other parameters are either derived or hard-coded, (see `Params.scala` for details).
//MC
//MC 命名约定：
//MC * `Num`后缀：某实体的数量，
//MC * `Width`后缀：某实体的位宽（通常是`log2(实体数量)`），
//MC * `Addr`后缀：某实体的地址。
//MC
//MC Naming conventions:
//MC
//MC * `Num` suffix: Number of the items.
//MC * `Width` suffix: Bit width of an item (typically `log2(number of the item)`).
//MC * `Addr` suffix: Address of an item.
//MC
//MC ### Class `IMSICParams`
case class IMSICParams(
  //MC
  //MC log2(IMSIC中断源的数量)
  //MC 默认值8表示IMSIC支持最多256（2^8）个中断源：
  //MC
  //MC log2(number of interrupt sources to IMSIC).
  //MC The default 8 means IMSIC support at most 256 (2^8) interrupt sources:
  //MC{visible}
  intSrcWidth     : Int  = 8          ,
  //MC
  //MC #### 中断文件的参数（Parameters for interrupt file）
  //MC
  //MC **注意**：中括号内的变量与AIA规范中的一致（第3.6节：用于多个中断文件的内存区域排列）。
  //MC
  //MC **Note**: The variables in bracket align with the AIA specification (Section 3.6: Memory Region Arrangement for Multiple Interrupt Files).
  //MC
  //MC 👉 每个组的成员数量（Number of members per group）[\\(h_{max}\\)]：
  membersNum      : Int  = 2           ,
  //MC 👉 机器态中断文件的基地址（Base address of machine-level interrupt files）[\\(A\\)]：
  mBaseAddr       : Long = 0x61000000L ,
  //MC 👉 监管态和客户态中断文件的基地址（Base addr for supervisor-level and guest-level interrupt files ）[\\(B\\)]:
  sgBaseAddr      : Long = 0x82900000L ,
  //MC 👉 客户中断文件的数量（Number of guest interrupt files）:
  geilen          : Int  = 4           ,
  //MC 👉 组的数量（Number of groups ）[\\(g_{max}\\)]:
  groupsNum       : Int  = 1           ,
  //MC
  //MC #### 控制状态寄存器的参数（Parameters for CSRs）
  //MC
  //MC vgein信号的位宽（The width of the vgein signal）:
  vgeinWidth      : Int  = 6           ,
  //MC iselect信号的位宽(The width of iselect signal):
  iselectWidth    : Int  = 12          ,
  //MC{hide}
) {
  val xlen        : Int  = 64 // currently only support xlen = 64
  val xlenWidth = log2Ceil(xlen)
  require(intSrcWidth <= 11, f"intSrcWidth=${intSrcWidth}, must not greater than log2(2048)=11, as there are at most 2048 eip/eie bits")
  val privNum     : Int  = 3            // number of privilege modes: machine, supervisor, virtualized supervisor
  val intFilesNum : Int  = 2 + geilen   // number of interrupt files, m, s, vs0, vs1, ...
  val eixNum      : Int  = pow2(intSrcWidth).toInt / xlen // number of eip/eie registers

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

  require(vgeinWidth >= log2Ceil(geilen))
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

//MC ### Class `APLICParams`
case class APLICParams(
  //MC log2(APLIC接收的中断源数量)。
  //MC 默认值7表示APLIC支持最多128（2^7）个中断源。
  //MC **注意**：APLIC的`intSrcWidth`必须小于IMSIC的`intSrcWidth`，
  //MC 因为APLIC的中断源将被转换为MSI，
  //MC 而APLIC转换成的MSI是IMSIC中断源的子集。
  //MC
  //MC log2(number of interrupt sources to APLIC):
  //MC The default 7 means APLIC support at most 128 (2^7) interrupt sources.
  //MC **Note**: APLIC's `intSrcWidth` must be **less than** IMSIC's `intSrcWidth`,
  //MC as APLIC interrupt sources are converted to MSIs,
  //MC which are a subset of IMSIC's interrupt sources.
  //MC{visible}
  intSrcWidth: Int = 7,
  //MC 👉 APLIC域的基地址（Base address of APLIC domains）:
  baseAddr: Long = 0x19960000L,
  //MC{hide}
) {
  require(intSrcWidth <= 10, f"intSrcWidth=${intSrcWidth}, must not greater than log2(1024)=10, as there are at most 1023 sourcecfgs")
  val intSrcNum: Int = pow2(intSrcWidth).toInt
  val ixNum: Int = pow2(intSrcWidth).toInt / 32
  val domainMemWidth : Int  = 14 // interrupt file memory region width: 14-bit width => 16KB size
}
