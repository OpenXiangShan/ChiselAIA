########################################################################################
# Copyright (c) 2024 Beijing Institute of Open Source Chip (BOSC)
#
# OpenAIA.scala is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
#          http://license.coscl.org.cn/MulanPSL2
#
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
# EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
# MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
#
# See the Mulan PSL v2 for more details.
########################################################################################

import cocotb
from cocotb.clock import Clock
from cocotb.triggers import RisingEdge, FallingEdge

op_put_full = 0
op_get = 4
op_access_ack = 0
op_access_ack_data = 1
async def a_op(dut, addr, data, op, mask, size) -> None:
  await FallingEdge(dut.clock)
  while not dut.domain_0_a_ready:
    await FallingEdge(dut.clock)
  dut.domain_0_a_valid.value = 1
  dut.domain_0_a_bits_opcode.value = op
  dut.domain_0_a_bits_address.value = addr
  dut.domain_0_a_bits_mask.value = mask
  dut.domain_0_a_bits_size.value = size
  dut.domain_0_a_bits_data.value = data
  await FallingEdge(dut.clock)
  dut.domain_0_a_valid.value = 0
async def a_op32(dut, addr, data, op) -> None:
  await a_op(
    dut, addr,
    data if addr%8==0 else data<<32,
    op,
    0x0f if addr%8==0 else 0xf0,
    2,
  )
async def a_put_full32(dut, addr, data) -> None:
  await a_op32(dut, addr, data, op_put_full)
  for _ in range(10):
    await RisingEdge(dut.clock)
    if dut.domain_0_d_bits_opcode == op_access_ack and dut.domain_0_d_valid == 1:
      break
  else:
    assert False, f"Timeout waiting for op_access_ack"
async def a_get32(dut, addr) -> int:
  await a_op32(dut, addr, 0, op_get)
  for _ in range(10):
    await RisingEdge(dut.clock)
    if dut.domain_0_d_bits_opcode == op_access_ack_data and dut.domain_0_d_valid == 1:
      break
  else:
    assert False, f"Timeout waiting for op_access_ack_data"
  odata = int(dut.domain_0_d_bits_data)
  res = odata if addr%8==0 else odata>>32
  return res & 0xffffffff

base_addr           = 0x19960000
offset_domaincfg    = 0
offset_sourcecfg    = 0x0004
offset_mmsiaddrcfg  = 0x1BC0
offset_mmsiaddrcfgh = 0x1BC4
offset_smsiaddrcfg  = 0x1BC8
offset_smsiaddrcfgh = 0x1BCC
offset_setips       = 0x1C00
offset_setipnum     = 0x1CDC
offset_in_clrips    = 0x1D00
offset_clripnum     = 0x1DDC
offset_seties       = 0x1E00
offset_setienum     = 0x1EDC
offset_clries       = 0x1F00
offset_clrienum     = 0x1FDC
offset_setipnum_le  = 0x2000
offset_setipnum_be  = 0x2004
offset_genmsi       = 0x3000
offset_targets      = 0x3004

@cocotb.test()
async def aplic_simple_write_read_test(dut):
  # Start the clock
  cocotb.start_soon(Clock(dut.clock, 1, units="ns").start())
  # Apply reset
  dut.reset.value = 1
  for _ in range(10):
    await RisingEdge(dut.clock)
  dut.reset.value = 0
  # Initialize ready signals
  dut.domain_0_d_ready.value = 1
  await RisingEdge(dut.clock)

  async def write_read_check_2(dut, addr, idata, odata):
    await a_put_full32  (dut, addr, idata)
    gdata = await a_get32(dut, addr)
    assert gdata==odata
  async def write_read_check_1(dut, addr, data):
    await write_read_check_2(dut, addr, data, data)

  # TODO: utilize random number
  await write_read_check_2(dut, base_addr+offset_domaincfg, 0xfedcab98, 0x80000104)
  # WARL offset_sourcecfg
  await write_read_check_2(dut, base_addr+offset_sourcecfg+3*4, 0x2, 0x0)
  await write_read_check_1(dut, base_addr+offset_sourcecfg+3*4, 0x1)
  await write_read_check_1(dut, base_addr+offset_sourcecfg+3*4, 0x407)
  # Lock offset_mmsiaddrcfg
  await write_read_check_1(dut, base_addr+offset_mmsiaddrcfg, offset_mmsiaddrcfg)
  await write_read_check_1(dut, base_addr+offset_mmsiaddrcfgh, 1<<31 | offset_mmsiaddrcfgh)
  # WARL locked offset_mmsiaddrcfg
  await write_read_check_2(dut, base_addr+offset_mmsiaddrcfg, 0xdead, offset_mmsiaddrcfg)
  await write_read_check_2(dut, base_addr+offset_mmsiaddrcfgh, 0xdeadbeef, 1<<31 | offset_mmsiaddrcfgh)
  # WARL locked offset_smsiaddrcfg
  await write_read_check_2(dut, base_addr+offset_smsiaddrcfg, 0xabcd, 0)
  await write_read_check_2(dut, base_addr+offset_smsiaddrcfgh, 0x1234, 0)
  # Unlock offset_mmsiaddrcfgh
  await write_read_check_2(dut, base_addr+offset_mmsiaddrcfgh, 0, offset_mmsiaddrcfgh)
  # WARL unlocked offset_smsiaddrcfg
  await write_read_check_2(dut, base_addr+offset_smsiaddrcfgh, 0xffffffff, 0x700fff)
  await write_read_check_2(dut, base_addr+offset_setips+0*4, 0xf, 0xe) # bit0 is readonly zero
  await write_read_check_2(dut, base_addr+offset_setips+1*4, 0xf, 0xf) # bit0 is readonly zero
  await write_read_check_1(dut, base_addr+offset_setipnum, offset_setipnum)
  for i in [0,3]: # TODO: random
    await write_read_check_1(dut, base_addr+offset_in_clrips+i*4, offset_in_clrips+i*4)
  await write_read_check_1(dut, base_addr+offset_clripnum, offset_clripnum)
  for i in [0,3]: # TODO: random
    await write_read_check_1(dut, base_addr+offset_seties+i*4, offset_seties+i*4)
  await write_read_check_1(dut, base_addr+offset_setienum, offset_setienum)
  for i in [0,3]: # TODO: random
    await write_read_check_1(dut, base_addr+offset_clries+i*4, offset_clries+i*4)
  await write_read_check_1(dut, base_addr+offset_clrienum, offset_clrienum)
  await write_read_check_1(dut, base_addr+offset_setipnum_le, offset_setipnum_le)
  await write_read_check_1(dut, base_addr+offset_setipnum_be, offset_setipnum_be)
  await write_read_check_1(dut, base_addr+offset_genmsi, offset_genmsi)
  for i in [0,3]: # TODO: random
    await write_read_check_1(dut, base_addr+offset_targets+i*4, offset_targets+i*4)
