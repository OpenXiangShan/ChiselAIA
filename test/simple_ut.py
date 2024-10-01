import cocotb
from cocotb.clock import Clock
from cocotb.triggers import RisingEdge


@cocotb.test()
async def mseteipnum_test(dut):
  cocotb.start_soon(Clock(dut.clock, 1, units="ns").start())
  dut.reset.value = 1
  for _ in range(10):
    await RisingEdge(dut.clock)
  dut.reset.value = 0
  dut.m_0_d_ready.value = 1
  dut.sg_0_d_ready.value = 1


