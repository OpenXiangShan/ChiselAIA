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

# Directed test for AXI IDs that do not fit in 16 bits.
#
# AXIRegIMSIC_WRAP used to join an AXI4SlaveNode to an
# AXI4MasterNode(IdRange(0, 65536)) with a raw `<>`. Every request ID above bit
# 15 was dropped on the way to the interrupt files and the R/B ID was
# zero-extended on the way back, so an upstream xbar routed the response to the
# wrong master. XiangShan's imsic_bus_top is such a case: its CPU port ID is
# 0x10040.

import cocotb
from cocotb.clock import Clock
from cocotb.triggers import RisingEdge, FallingEdge

M_BASE      = 0x61000000
EIDELIVERY  = 0x70
TEST_ID     = 0x10040   # = 0x40 + 0x10000, the CPU port ID in XiangShan's imsic_bus_top


async def reset(dut):
    cocotb.start_soon(Clock(dut.clock, 1, units="ns").start())
    dut.reset.value = 1
    for _ in range(10):
        await RisingEdge(dut.clock)
    dut.reset.value = 0
    dut.master_0_r_ready.value = 1
    dut.master_0_b_ready.value = 1
    await RisingEdge(dut.clock)


@cocotb.test()
async def axi_read_id_survives_wrap(dut):
    await reset(dut)

    await FallingEdge(dut.clock)
    dut.master_0_ar_valid.value = 1
    dut.master_0_ar_bits_id.value = TEST_ID
    dut.master_0_ar_bits_addr.value = M_BASE + EIDELIVERY
    dut.master_0_ar_bits_len.value = 0
    dut.master_0_ar_bits_size.value = 2
    dut.master_0_ar_bits_burst.value = 1
    while not dut.master_0_ar_ready.value:
        await FallingEdge(dut.clock)
    await FallingEdge(dut.clock)
    dut.master_0_ar_valid.value = 0

    rid = None
    for _ in range(100):
        await RisingEdge(dut.clock)
        if dut.master_0_r_valid.value == 1:
            rid = int(dut.master_0_r_bits_id.value)
            break
    assert rid is not None, "no R response"
    cocotb.log.info("read 0x%x: R id = 0x%x (request id 0x%x)",
                    M_BASE + EIDELIVERY, rid, TEST_ID)
    assert rid == TEST_ID, \
        f"R id 0x{rid:x} != request id 0x{TEST_ID:x}"


@cocotb.test()
async def axi_write_id_survives_wrap(dut):
    await reset(dut)

    # AXI4RegMapperNode accepts AW and W in the same cycle, as noted in test/axi.
    await FallingEdge(dut.clock)
    dut.master_0_aw_valid.value = 1
    dut.master_0_aw_bits_id.value = TEST_ID
    dut.master_0_aw_bits_addr.value = M_BASE + EIDELIVERY
    dut.master_0_aw_bits_len.value = 0
    dut.master_0_aw_bits_size.value = 2
    dut.master_0_aw_bits_burst.value = 1
    dut.master_0_w_valid.value = 1
    dut.master_0_w_bits_data.value = 1
    dut.master_0_w_bits_strb.value = 0xf
    dut.master_0_w_bits_last.value = 1
    while not (dut.master_0_aw_ready.value and dut.master_0_w_ready.value):
        await FallingEdge(dut.clock)
    await FallingEdge(dut.clock)
    dut.master_0_aw_valid.value = 0
    dut.master_0_w_valid.value = 0
    dut.master_0_w_bits_last.value = 0

    bid = None
    for _ in range(100):
        await RisingEdge(dut.clock)
        if dut.master_0_b_valid.value == 1:
            bid = int(dut.master_0_b_bits_id.value)
            break
    assert bid is not None, "no B response"
    cocotb.log.info("write 0x%x: B id = 0x%x (request id 0x%x)",
                    M_BASE + EIDELIVERY, bid, TEST_ID)
    assert bid == TEST_ID, \
        f"B id 0x{bid:x} != request id 0x{TEST_ID:x}"
