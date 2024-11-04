########################################################################################
# Copyright (c) 2024 Beijing Institute of Open Source Chip (BOSC)
#
# ChiselAIA is licensed under Mulan PSL v2.
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
from common import *

@cocotb.test()
async def integration_simple_test(dut):
  # Start the clock
  cocotb.start_soon(Clock(dut.clock, 1, units="ns").start())
  # Apply reset
  dut.reset.value = 1
  for _ in range(10):
    await RisingEdge(dut.clock)
  dut.reset.value = 0
  # Initialize ready signals
  dut.toaia_0_d_ready.value = 1
  await RisingEdge(dut.clock)

  # Init APLIC
  await a_put_full32(dut, aplic_m_base_addr+offset_domaincfg, 0x80000104)
  ## init sourcecfg1~sourcecfg63, target1~target63
  for i in range(0,62):
    await a_put_full32(dut, aplic_m_base_addr+offset_sourcecfg+i*4, sourcecfg_sm_edge1)
    await a_put_full32(dut, aplic_m_base_addr+offset_targets+i*4, i+1)
  ## enable ie bit1~63
  await a_put_full32(dut, aplic_m_base_addr+offset_seties+0*4, 0xfffffffe)
  await a_put_full32(dut, aplic_m_base_addr+offset_seties+1*4, 0xffffffff)

  # Init IMSIC0
  await init_imsic(dut, 0)

  async def int(i):
    intSrcs_x = getattr(dut, f"intSrcs_{i}")
    await FallingEdge(dut.clock)
    intSrcs_x.value = 0
    await FallingEdge(dut.clock)
    intSrcs_x.value = 1
    for _ in range(10):
      await FallingEdge(dut.clock)
      if dut.toCSR0_topeis_0 == wrap_topei(i):
        break
    else:
      assert False, f"Timeout waiting for toCSR0_topeis_0 == wrap_topei({i})"

  await int(19)
  await int(2)
