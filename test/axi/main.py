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
from cocotb.triggers import RisingEdge, FallingEdge, Edge
from common import *

async def axi4_aw_send(dut, addr, prot, size):
  await FallingEdge(dut.clock)
  while not dut.domain_0_aw_ready:
    await FallingEdge(dut.clock)
  dut.domain_0_aw_valid.value = 1
  dut.domain_0_aw_bits_addr.value = addr
  dut.domain_0_aw_bits_prot.value = prot
  # TODO: why need this? how to use axi4lite?
  dut.domain_0_aw_bits_size.value = size
  await FallingEdge(dut.clock)
  dut.domain_0_aw_valid.value = 0
async def axi4_w_send(dut, data, strb):
  await FallingEdge(dut.clock)
  while not dut.domain_0_w_ready:
    await FallingEdge(dut.clock)
  dut.domain_0_w_valid.value = 1
  dut.domain_0_w_bits_data.value = data
  dut.domain_0_w_bits_strb.value = strb
  await FallingEdge(dut.clock)
  dut.domain_0_w_valid.value = 0
async def axi4_b_receive(dut):
  await RisingEdge(dut.domain_0_b_valid)
async def axi4_ar_send(dut, addr, prot, size):
  await FallingEdge(dut.clock)
  while not dut.domain_0_ar_ready:
    await FallingEdge(dut.clock)
  dut.domain_0_ar_valid.value = 1
  dut.domain_0_ar_bits_addr.value = addr
  dut.domain_0_ar_bits_prot.value = prot
  # TODO: why need this? how to use axi4lite?
  dut.domain_0_ar_bits_size.value = size
  await FallingEdge(dut.clock)
  dut.domain_0_ar_valid.value = 0
async def axi4_r_receive(dut):
  await RisingEdge(dut.domain_0_r_valid)
  return dut.domain_0_r_bits_data.value

async def axi4_write(dut, addr, data, strb, size):
  cocotb.start_soon(axi4_aw_send(dut, addr, 0, size))
  cocotb.start_soon(axi4_w_send(dut, data, strb))
  await axi4_b_receive(dut)
async def axi4_write32(dut, addr, data):
  await axi4_write(dut, addr,
    data if addr%8==0 else data<<32,
    0x0f if addr%8==0 else 0xf0,
    2,
  )

async def axi4_toimsic_b_send(dut, id):
  await FallingEdge(dut.clock)
  dut.toimsic_0_b_valid.value = 1
  dut.toimsic_0_b_bits_id.value = id
  await FallingEdge(dut.clock)
  dut.toimsic_0_b_valid.value = 0

async def axi4_read(dut, addr, size):
  cocotb.start_soon(axi4_ar_send(dut, addr, 0, size))
  task = cocotb.start_soon(axi4_r_receive(dut))
  return await task
async def axi4_read32(dut, addr):
  return await axi4_read(dut, addr, 2)

@cocotb.test()
async def axi_simple_test(dut):
  # Start the clock
  cocotb.start_soon(Clock(dut.clock, 1, units="ns").start())
  # Apply reset
  dut.reset.value = 1
  for _ in range(10):
    await RisingEdge(dut.clock)
  dut.reset.value = 0
  # Initialize ready signals
  dut.domain_0_b_ready.value  = 1
  dut.domain_0_r_ready.value  = 1
  dut.toimsic_0_aw_ready.value = 1
  dut.toimsic_0_w_ready.value = 1
  dut.intfile_0_b_ready.value  = 1
  dut.intfile_0_r_ready.value  = 1
  await RisingEdge(dut.clock)

  # Init APLIC
  await axi4_write32(dut, aplic_m_base_addr+offset_domaincfg, 0x80000104)
  ## init sourcecfg1~sourcecfg63, target1~target63
  for i in range(0,62):
    await axi4_write32(dut, aplic_m_base_addr+offset_sourcecfg+i*4, sourcecfg_sm_edge1)
    await axi4_write32(dut, aplic_m_base_addr+offset_targets+i*4, i+1)
  ## enable ie bit1~63
  await axi4_write32(dut, aplic_m_base_addr+offset_seties+0*4, 0xfffffffe)
  await axi4_write32(dut, aplic_m_base_addr+offset_seties+1*4, 0xffffffff)

  async def int(i):
    intSrcs_x = getattr(dut, f"intSrcs_{i}")
    await FallingEdge(dut.clock)
    intSrcs_x.value = 0
    await FallingEdge(dut.clock)
    intSrcs_x.value = 1

    async def receive_aw_id(dut):
      await RisingEdge(dut.toimsic_0_aw_valid)
      return dut.toimsic_0_aw_bits_id.value
    async def receive_w(dut):
      await RisingEdge(dut.toimsic_0_w_valid)
      assert dut.toimsic_0_w_bits_data == i
    # aw arrives not later than w
    task_receive_aw_id = cocotb.start_soon(receive_aw_id(dut))
    await receive_w(dut)
    await axi4_toimsic_b_send(dut, await task_receive_aw_id)

  # Init IMSIC0
  await init_imsic(dut, 0)

  await int(19)
  await int(2)

  # TODO: remove
  async def axi4_aw_send_intfile(dut, addr, prot, size):
    await FallingEdge(dut.clock)
    while not dut.intfile_0_aw_ready:
      await FallingEdge(dut.clock)
    dut.intfile_0_aw_valid.value = 1
    dut.intfile_0_aw_bits_addr.value = addr
    dut.intfile_0_aw_bits_prot.value = prot
    # TODO: why need this? how to use axi4lite?
    dut.intfile_0_aw_bits_size.value = size
    await FallingEdge(dut.clock)
    dut.intfile_0_aw_valid.value = 0
  async def axi4_w_send_intfile(dut, data, strb):
    await FallingEdge(dut.clock)
    while not dut.intfile_0_w_ready:
      await FallingEdge(dut.clock)
    dut.intfile_0_w_valid.value = 1
    dut.intfile_0_w_bits_data.value = data
    dut.intfile_0_w_bits_strb.value = strb
    await FallingEdge(dut.clock)
    dut.intfile_0_w_valid.value = 0
  async def axi4_b_receive_intfile(dut):
    await RisingEdge(dut.intfile_0_b_valid)
  async def axi4_ar_send_intfile(dut, addr, prot, size):
    await FallingEdge(dut.clock)
    while not dut.intfile_0_ar_ready:
      await FallingEdge(dut.clock)
    dut.intfile_0_ar_valid.value = 1
    dut.intfile_0_ar_bits_addr.value = addr
    dut.intfile_0_ar_bits_prot.value = prot
    # TODO: why need this? how to use axi4lite?
    dut.intfile_0_ar_bits_size.value = size
    await FallingEdge(dut.clock)
    dut.intfile_0_ar_valid.value = 0
  async def axi4_r_receive_intfile(dut):
    await RisingEdge(dut.intfile_0_r_valid)
    return dut.intfile_0_r_bits_data.value
  
  async def axi4_write_intfile(dut, addr, data, strb, size):
    cocotb.start_soon(axi4_aw_send_intfile(dut, addr, 0, size))
    cocotb.start_soon(axi4_w_send_intfile(dut, data, strb))
    await axi4_b_receive_intfile(dut)
  async def axi4_write32_intfile(dut, addr, data):
    await axi4_write_intfile(dut, addr,
      data if addr%8==0 else data<<32,
      0x0f if addr%8==0 else 0xf0,
      2,
    )

  async def m_int_intfile(dut, intnum, imsicID=1):
    """Issue an interrupt to the M-mode interrupt file."""
    await axi4_write32_intfile(dut, imsic_m_base_addr+0x1000*imsicID, intnum)
    if dut.toCSR0_topeis_0.value != wrap_topei(intnum):
      await Edge(dut.toCSR0_topeis_0)
      assert dut.toCSR0_topeis_0.value == wrap_topei(intnum)

  await m_int_intfile(dut, 19, 0)
  await m_int_intfile(dut, 2, 0)
