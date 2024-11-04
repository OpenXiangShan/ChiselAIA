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

from cocotb.triggers import RisingEdge, FallingEdge

op_put_full = 0
op_get = 4
op_access_ack = 0
op_access_ack_data = 1
async def a_op(dut, addr, data, op, mask, size) -> None:
  await FallingEdge(dut.clock)
  while not dut.toaia_0_a_ready:
    await FallingEdge(dut.clock)
  dut.toaia_0_a_valid.value = 1
  dut.toaia_0_a_bits_opcode.value = op
  dut.toaia_0_a_bits_address.value = addr
  dut.toaia_0_a_bits_mask.value = mask
  dut.toaia_0_a_bits_size.value = size
  dut.toaia_0_a_bits_data.value = data
  await FallingEdge(dut.clock)
  dut.toaia_0_a_valid.value = 0
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
    if dut.toaia_0_d_bits_opcode == op_access_ack and dut.toaia_0_d_valid == 1:
      break
  else:
    assert False, f"Timeout waiting for op_access_ack"
async def a_get32(dut, addr) -> int:
  await a_op32(dut, addr, 0, op_get)
  for _ in range(10):
    await RisingEdge(dut.clock)
    if dut.toaia_0_d_bits_opcode == op_access_ack_data and dut.toaia_0_d_valid == 1:
      break
  else:
    assert False, f"Timeout waiting for op_access_ack_data"
  odata = int(dut.toaia_0_d_bits_data)
  res = odata if addr%8==0 else odata>>32
  return res & 0xffffffff

base_addr           = 0x19960000
m_base_addr         = base_addr
sg_base_addr        = base_addr + 0x4000
imsic_m_base_addr   = 0x61000000
imsic_sg_base_addr  = 0x82900000
offset_domaincfg    = 0
offset_sourcecfg    = 0x0004
offset_readonly0    = 0x1000
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
sourcecfg_sm_inactive = 0
sourcecfg_sm_detached = 1
sourcecfg_sm_edge1    = 4
sourcecfg_sm_edge0    = 5
sourcecfg_sm_level1   = 6
sourcecfg_sm_level0   = 7
