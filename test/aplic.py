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

async def a_put_full32(dut, addr, data):
  await FallingEdge(dut.clock)
  while not dut.domain_0_a_ready:
    await RisingEdge(dut.clock)

  dut.domain_0_a_valid.value = 1
  dut.domain_0_a_bits_opcode.value = 0
  dut.domain_0_a_bits_address.value = addr
  dut.domain_0_a_bits_mask.value = 0xf
  dut.domain_0_a_bits_size.value = 2
  dut.domain_0_a_bits_data.value = data
  await FallingEdge(dut.clock)
  dut.domain_0_a_valid.value = 0

offset_domaincfg = 0
async def write_mapped_reg(dut, base, offset, data):
  await a_put_full32(dut, base + offset, data)
  for _ in range(10):
    await RisingEdge(dut.clock)
    if dut.aplic.domaincfg == data:
      break
  else:
    assert False, f"Timeout waiting for domaincfg == 0x${data}%x"
  await RisingEdge(dut.clock)

@cocotb.test()
async def aplic_test(dut):
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

  await write_mapped_reg(dut, 0x19960000, offset_domaincfg, 0x137)
