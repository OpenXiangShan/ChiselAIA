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

# domaincfg.r has the fixed high field at bit31, DM=1 at bit2, and IE=0 here.
domaincfg_read_high = 0x80000000
domaincfg_dm_msi = 0x4
domaincfg_msi_mode_ie_disabled = domaincfg_read_high | domaincfg_dm_msi

@cocotb.test()
async def aplic_write_read_test(dut):
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

  async def write_read_check_2(dut, addr, idata, odata):
    await a_put_full32  (dut, addr, idata)
    gdata = await a_get32(dut, addr)
    assert gdata==odata
  async def write_read_check_1(dut, addr, data):
    await write_read_check_2(dut, addr, data, data)

  # TODO: utilize random number
  await write_read_check_2(dut, aplic_m_base_addr+offset_domaincfg, 0xfedcab98, 0x80000104)
  # WARL offset_sourcecfg
  await write_read_check_2(dut, aplic_m_base_addr+offset_sourcecfg+3*4, 0x2, 0x0)
  await write_read_check_1(dut, aplic_m_base_addr+offset_sourcecfg+3*4, 0x1)
  await write_read_check_2(dut, aplic_m_base_addr+offset_sourcecfg+3*4, 0x407, 0x400) # machine-level dont support child index
  ## enable offset_sourcecfg1 ~ offset_sourcecfg63
  for i in range(0,63):
    await write_read_check_1(dut, aplic_m_base_addr+offset_sourcecfg+i*4, sourcecfg_sm_edge1)
  await write_read_check_2(dut, aplic_m_base_addr+offset_setips+0*4, 0xf, 0xe) # bit0 is readonly zero
  await write_read_check_2(dut, aplic_m_base_addr+offset_setips+1*4, 0xf, 0xf) # bit0 is readonly zero
  await write_read_check_2(dut, aplic_m_base_addr+offset_seties+0*4, 0xf, 0xe) # bit0 is readonly zero
  await write_read_check_2(dut, aplic_m_base_addr+offset_seties+1*4, 0xf, 0xf) # bit0 is readonly zero
  # On a write to a setie/setip register, for each bit that is one in the 32-bit value written, if that bit position
  # corresponds to an active interrupt source, the interrupt-enable bit for that source is set to one.
  await write_read_check_2(dut, aplic_m_base_addr+offset_seties+1*4, 0x0, 0xf)
  await write_read_check_2(dut, aplic_m_base_addr+offset_seties+1*4, 0x10, 0x1f)
  # If the source mode is Level1 or Level0 and the interrupt domain is configured in MSI delivery mode
  # (domaincfg.DM = 1):
  # • The pending bit is set to one by a low-to-high transition in the rectified input value. The pending
  # bit may also be set by a relevant write to a setip or setipnum register when the rectified input
  # value is high, but not when the rectified input value is low.
  setips_0 = await a_get32(dut, aplic_m_base_addr+offset_setips+0*4)
  int_num = 17
  await write_read_check_1(dut, aplic_m_base_addr+offset_sourcecfg+(int_num-1)*4, sourcecfg_sm_level1)
  await write_read_check_2(dut, aplic_m_base_addr+offset_setips+0*4, 1<<int_num, setips_0)
  await a_put_full32(dut, aplic_m_base_addr+offset_setipnum, int_num)
  assert (await a_get32(dut, aplic_m_base_addr+offset_setips+0*4)) == setips_0
  # TODO: move to aplic_set_clr_test
  await write_read_check_1(dut, aplic_m_base_addr+offset_targets+0*4, (offset_targets+0*4)%8) # GuestIndex is not worked in machine-level domain
  await write_read_check_1(dut, aplic_m_base_addr+offset_targets+3*4, (offset_targets+3*4)%8) # GuestIndex is not worked in machine-level domain
  # TODO: inactive target readonly zeros
  await write_read_check_2(dut, aplic_m_base_addr+offset_targets+64*4, offset_targets+64*4, 0)
  # readonly zeros
  await write_read_check_2(dut, aplic_m_base_addr+offset_readonly0+1*4, 0xdeadbeef, 0)
  await write_read_check_2(dut, aplic_m_base_addr+offset_mmsiaddrcfgh, 0xdeadbeef, 0x80000000)
  await write_read_check_2(dut, aplic_m_base_addr+offset_smsiaddrcfgh, 0xdeadbeef, 0)

@cocotb.test()
async def aplic_set_clr_test(dut):
  # Start the clock
  cocotb.start_soon(Clock(dut.clock, 1, units="ns").start())

  # setienum 0, which should be ignored
  ie0 = await a_get32(dut, aplic_m_base_addr+offset_seties)
  await a_put_full32(dut, aplic_m_base_addr+offset_setienum, 0)
  ie0_ignore0 = await a_get32(dut, aplic_m_base_addr+offset_seties)
  assert ie0==ie0_ignore0

  # setienum ie0
  await a_put_full32(dut, aplic_m_base_addr+offset_setienum, 27)
  ie0_set = await a_get32(dut, aplic_m_base_addr+offset_seties)
  assert ie0|(1<<27)==ie0_set
  # in_clrie0 clear all ie0
  await a_put_full32(dut, aplic_m_base_addr+offset_clries+0*4, 0xffffffff)
  ie0_clear_all = await a_get32(dut, aplic_m_base_addr+offset_seties)
  assert ie0_clear_all==0

  # setienum ie1
  ie1 = await a_get32(dut, aplic_m_base_addr+offset_seties+1*4)
  setienum_1 = 63
  await a_put_full32(dut, aplic_m_base_addr+offset_setienum, setienum_1)
  ie1_set1 = await a_get32(dut, aplic_m_base_addr+offset_seties+1*4)
  assert ie1|(1<<(setienum_1-32))==ie1_set1
  # clrienum ie1
  await a_put_full32(dut, aplic_m_base_addr+offset_clrienum, setienum_1)
  ie1_clr1 = await a_get32(dut, aplic_m_base_addr+offset_seties+1*4)
  assert ie1==ie1_clr1

  # setipnum_le ip1
  ip1 = await a_get32(dut, aplic_m_base_addr+offset_setips+1*4)
  setipnum_1 = 54
  await a_put_full32(dut, aplic_m_base_addr+offset_setipnum_le, setipnum_1)
  ip1_set1 = await a_get32(dut, aplic_m_base_addr+offset_setips+1*4)
  assert ip1|(1<<(setipnum_1-32))==ip1_set1
  # setipnum_be readonly zeros
  assert 0==await a_get32(dut, aplic_m_base_addr+offset_setipnum_be)

@cocotb.test()
async def aplic_triggered_int_test(dut):
  # Start the clock
  cocotb.start_soon(Clock(dut.clock, 1, units="ns").start())

  # int sources
  async def expect_intSrcsTriggered_2(dut, value):
    for _ in range(10):
      await RisingEdge(dut.clock)
      if dut.aplic.aplic.domains_0.intSrcsTriggered_2 == value:
        break
    else:
      assert False, f"Timeout waiting for dut.aplic.intSrcsTriggered_2"

  ## edge1
  await a_put_full32(dut, aplic_m_base_addr+offset_sourcecfg+1*4, sourcecfg_sm_edge1)
  await FallingEdge(dut.clock)
  dut.intSrcs_2.value = 0
  assert dut.aplic.aplic.domains_0.intSrcsTriggered_2 == 0
  await FallingEdge(dut.clock)
  dut.intSrcs_2.value = 1
  await expect_intSrcsTriggered_2(dut, 1)
  ## edge0
  await a_put_full32(dut, aplic_m_base_addr+offset_sourcecfg+1*4, sourcecfg_sm_edge0)
  await expect_intSrcsTriggered_2(dut, 0)
  await FallingEdge(dut.clock)
  dut.intSrcs_2.value = 0
  await expect_intSrcsTriggered_2(dut, 1)
  ## level1
  await a_put_full32(dut, aplic_m_base_addr+offset_sourcecfg+1*4, sourcecfg_sm_level1)
  await expect_intSrcsTriggered_2(dut, 0)
  await FallingEdge(dut.clock)
  dut.intSrcs_2.value = 1
  await expect_intSrcsTriggered_2(dut, 1)
  ## level0
  await a_put_full32(dut, aplic_m_base_addr+offset_sourcecfg+1*4, sourcecfg_sm_level0)
  await expect_intSrcsTriggered_2(dut, 0)
  await FallingEdge(dut.clock)
  dut.intSrcs_2.value = 0
  await expect_intSrcsTriggered_2(dut, 1)

@cocotb.test()
async def aplic_in_clrips_test(dut):
  # Start the clock
  cocotb.start_soon(Clock(dut.clock, 1, units="ns").start())

  await a_put_full32(dut, aplic_m_base_addr+offset_seties, 0)
  rect_before = await a_get32(dut, aplic_m_base_addr+offset_in_clrips+0*4)
  await a_put_full32(dut, aplic_m_base_addr+offset_sourcecfg+3*4, sourcecfg_sm_edge1)
  await a_put_full32(dut, aplic_m_base_addr+offset_sourcecfg+4*4, sourcecfg_sm_edge0)
  await a_put_full32(dut, aplic_m_base_addr+offset_sourcecfg+5*4, sourcecfg_sm_level1)
  await a_put_full32(dut, aplic_m_base_addr+offset_sourcecfg+6*4, sourcecfg_sm_level0)
  await FallingEdge(dut.clock)
  dut.intSrcs_4.value = 0
  dut.intSrcs_5.value = 1
  dut.intSrcs_6.value = 0
  dut.intSrcs_7.value = 1
  await FallingEdge(dut.clock)
  dut.intSrcs_4.value = 1
  dut.intSrcs_5.value = 0
  dut.intSrcs_6.value = 1
  dut.intSrcs_7.value = 0
  await FallingEdge(dut.clock)
  rect_after = await a_get32(dut, aplic_m_base_addr+offset_in_clrips+0*4)
  assert rect_after == 0xf0 | rect_before
  # clean
  await FallingEdge(dut.clock)
  dut.intSrcs_4.value = 0
  dut.intSrcs_5.value = 0
  dut.intSrcs_6.value = 0
  dut.intSrcs_7.value = 0
  await a_put_full32(dut, aplic_m_base_addr+offset_sourcecfg+3*4, 0)
  await a_put_full32(dut, aplic_m_base_addr+offset_sourcecfg+4*4, 0)
  await a_put_full32(dut, aplic_m_base_addr+offset_sourcecfg+5*4, 0)
  await a_put_full32(dut, aplic_m_base_addr+offset_sourcecfg+6*4, 0)
  await FallingEdge(dut.clock)

@cocotb.test()
async def aplic_level_msi_pending_clear_on_low_test(dut):
  # Start the clock
  cocotb.start_soon(Clock(dut.clock, 1, units="ns").start())

  dut.reset.value = 1
  dut.toaia_0_d_ready.value = 1
  dut.toaia_0_a_valid.value = 0
  for i in range(64):
    getattr(dut, f"intSrcs_{i}").value = 0
  for _ in range(10):
    await RisingEdge(dut.clock)
  dut.reset.value = 0
  await RisingEdge(dut.clock)

  await a_put_full32(dut, aplic_m_base_addr+offset_domaincfg, domaincfg_msi_mode_ie_disabled)
  # Keep domain IE and targets unconfigured here. The test intentionally
  # observes only pending set/clear-on-low through setips; MSI forwarding is
  # exercised separately by aplic_msi_test.

  async def check_level_source(int_num, source_mode, asserted_value, deasserted_value):
    intSrc = getattr(dut, f"intSrcs_{int_num}")
    ip_addr = aplic_m_base_addr + offset_setips + (int_num // 32) * 4
    ip_mask = 1 << (int_num % 32)

    intSrc.value = deasserted_value
    # APLIC source numbers are 1-based; sourcecfg register slots are 0-based.
    await a_put_full32(dut, aplic_m_base_addr+offset_sourcecfg+(int_num-1)*4, source_mode)
    for _ in range(6):
      await FallingEdge(dut.clock)
    assert (await a_get32(dut, ip_addr) & ip_mask) == 0

    intSrc.value = asserted_value
    for _ in range(6):
      await FallingEdge(dut.clock)
    assert (await a_get32(dut, ip_addr) & ip_mask) != 0

    intSrc.value = deasserted_value
    for _ in range(6):
      await FallingEdge(dut.clock)
    assert (await a_get32(dut, ip_addr) & ip_mask) == 0

    await a_put_full32(dut, aplic_m_base_addr+offset_sourcecfg+(int_num-1)*4, 0)
    intSrc.value = deasserted_value
    for _ in range(2):
      await FallingEdge(dut.clock)

  await check_level_source(19, sourcecfg_sm_level1, 1, 0)
  # Level0 is low-active: raw intSrc=0 is asserted, raw intSrc=1 is deasserted.
  await check_level_source(20, sourcecfg_sm_level0, 0, 1)

@cocotb.test()
async def aplic_msi_test(dut):
  # Start the clock
  cocotb.start_soon(Clock(dut.clock, 1, units="ns").start())

  async def expect_int_num(dut, num, addr):
    for _ in range(0,10):
      await RisingEdge(dut.clock)
      if dut.aplic.auto_toIMSIC_out_a_bits_data == num:
        assert dut.aplic.auto_toIMSIC_out_a_bits_address == addr
        break
    else:
      assert False, f"Timeout waiting for dut.aplic.auto_toIMSIC_out_a_bits_data"

  # # setipnum 0, which should be ignored
  # ip0 = await a_get32(dut, base_addr+offset_setips)
  # await a_put_full32(dut, base_addr+offset_setipnum, 0)
  # ip0_ignore0 = await a_get32(dut, base_addr+offset_setips)
  # assert ip0==ip0_ignore0

  # setipnum ip0
  int_num = 27
  eiid = 0xCA
  guest_id = 2
  await a_put_full32(dut, aplic_m_base_addr+offset_targets+(int_num-1)*4, (guest_id<<12)|eiid)
  await a_put_full32(dut, aplic_m_base_addr+offset_seties+0*4, 0xffffffff)
  await a_put_full32(dut, aplic_m_base_addr+offset_setipnum, int_num)
  await expect_int_num(dut, eiid, imsic_m_base_addr) # guest_id is not worked in machine-level domain

  eiid = 0x77
  hart_index = 1
  await a_put_full32(dut, aplic_m_base_addr+offset_genmsi, hart_index<<18|eiid)
  await expect_int_num(dut, eiid, imsic_m_base_addr+0x1000*hart_index)
  # setipnum ip1
  int_num = 63
  eiid = 0xEF
  await a_put_full32(dut, aplic_m_base_addr+offset_targets+(int_num-1)*4, eiid)
  await a_put_full32(dut, aplic_m_base_addr+offset_seties+1*4, 1<<(int_num-32))
  await a_put_full32(dut, aplic_m_base_addr+offset_setipnum, int_num)
  await expect_int_num(dut, eiid, imsic_m_base_addr)

  # intSrc
  await FallingEdge(dut.clock)
  dut.intSrcs_63.value = 1
  await FallingEdge(dut.clock)
  dut.intSrcs_63.value = 0
  await expect_int_num(dut, eiid, imsic_m_base_addr)

  # delegation
  int_num = 43
  eiid = 0xAB
  guest_id = 3
  await a_put_full32(dut, aplic_m_base_addr+offset_sourcecfg+(int_num-1)*4, 1<<10)
  await a_put_full32(dut, aplic_m_base_addr+offset_targets+(int_num-1)*4, eiid)
  await a_put_full32(dut, aplic_m_base_addr+offset_seties+1*4, 1<<(int_num-32))
  await a_put_full32(dut, aplic_m_base_addr+offset_setipnum, int_num)
  await a_put_full32(dut, aplic_sg_base_addr+offset_domaincfg, 0x80000104)
  await a_put_full32(dut, aplic_sg_base_addr+offset_sourcecfg+(int_num-1)*4, sourcecfg_sm_edge1)
  await a_put_full32(dut, aplic_sg_base_addr+offset_targets+(int_num-1)*4, (guest_id<<12)|eiid)
  await a_put_full32(dut, aplic_sg_base_addr+offset_seties+1*4, 1<<(int_num-32))
  await FallingEdge(dut.clock)
  dut.intSrcs_43.value = 1
  await FallingEdge(dut.clock)
  dut.intSrcs_43.value = 0
  await expect_int_num(dut, eiid, imsic_sg_base_addr+0x1000*guest_id)
