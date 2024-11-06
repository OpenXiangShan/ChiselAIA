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
import freechips.rocketchip.amba._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.util._

object pow2 { def apply(n: Int): Long = 1L << n }

object SeqAddressSet_Subtract_SeqAddressSet {
  def apply(op1s: Seq[AddressSet], op2s: Seq[AddressSet]): Seq[AddressSet] = {
    if (op2s.size == 0) { op1s }
    else { SeqAddressSet_Subtract_SeqAddressSet(
      op1s.map(_.subtract(op2s.head)).flatten,
      op2s.drop(1)
)}}}

class AXI4Map(fn: AddressSet => BigInt)(implicit p: Parameters) extends LazyModule
{
  val node = AXI4AdapterNode(
    masterFn = { mp => mp },
    slaveFn = { sp =>
      sp.copy(slaves = sp.slaves.map(s =>
        s.copy(address = s.address.map(a =>
          AddressSet(fn(a), a.mask)))))})

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    (node.in zip node.out) foreach { case ((in, edgeIn), (out, edgeOut)) =>
      out <> in
      val convert = edgeIn.slave.slaves.flatMap(_.address) zip edgeOut.slave.slaves.flatMap(_.address)
      def forward(x: UInt) =
        convert.map { case (i, o) => Mux(i.contains(x), o.base.U | (x & o.mask.U), 0.U) }.reduce(_ | _)
      def backward(x: UInt) =
        convert.map { case (i, o) => Mux(o.contains(x), i.base.U | (x & i.mask.U), 0.U) }.reduce(_ | _)

      out.aw.bits.addr := forward(in.aw.bits.addr)
      out.ar.bits.addr := forward(in.ar.bits.addr)
    }
  }
}

object AXI4Map
{
  def apply(fn: AddressSet => BigInt)(implicit p: Parameters): AXI4Node =
  {
    val map = LazyModule(new AXI4Map(fn))
    map.node
  }
}
