import cocotb
from cocotb.clock import Clock
from cocotb.triggers import RisingEdge, FallingEdge

mBaseAddr = 0x61001000
sgBaseAddr = 0x82908000
csr_addr_eidelivery = 0x70
csr_addr_eithreshold = 0x72
csr_addr_eip0 = 0x80
csr_addr_eip2 = 0x82
csr_addr_eie0 = 0xC0

op_illegal = 0
op_csrrw = 1
op_csrrs = 2
op_csrrc = 3
async def write_csr_op(dut, miselect, data, op):
  await FallingEdge(dut.clock)
  dut.fromCSR_addr_valid.value = 1
  dut.fromCSR_addr_bits.value = miselect
  dut.fromCSR_wdata_valid.value = 1
  dut.fromCSR_wdata_bits_op.value = op
  dut.fromCSR_wdata_bits_data.value = data
  await FallingEdge(dut.clock)
  dut.fromCSR_addr_valid.value = 0
  dut.fromCSR_wdata_valid.value = 0
async def write_csr(dut, miselect, data):
  await write_csr_op(dut, miselect, data, op_csrrw)
async def select_m_intfile(dut):
  await FallingEdge(dut.clock)
  dut.fromCSR_priv.value = 3
  dut.fromCSR_virt.value = 0
async def select_s_intfile(dut):
  await FallingEdge(dut.clock)
  dut.fromCSR_priv.value = 1
  dut.fromCSR_virt.value = 0
async def select_vs_intfile(dut, vgein):
  await FallingEdge(dut.clock)
  dut.fromCSR_priv.value = 1
  dut.fromCSR_vgein.value = vgein
  dut.fromCSR_virt.value = 1
async def init_imsic(dut):
  await select_m_intfile(dut)
  await write_csr(dut, csr_addr_eidelivery, 1)
  for e in range(0,32):
    await write_csr(dut, csr_addr_eie0 + 2*e, -1)
  await select_s_intfile(dut)
  await write_csr(dut, csr_addr_eidelivery, 1)
  for e in range(0,32):
    await write_csr(dut, csr_addr_eie0 + 2*e, -1)
  for i in range(0,4):
    await select_vs_intfile(dut, i)
    await write_csr(dut, csr_addr_eidelivery, 1)
    for e in range(0,32):
      await write_csr(dut, csr_addr_eie0 + 2*e, -1)

@cocotb.test()
async def mseteipnum_test(dut):
  cocotb.start_soon(Clock(dut.clock, 1, units="ns").start())
  dut.reset.value = 1
  for _ in range(10):
    await RisingEdge(dut.clock)
  dut.reset.value = 0
  dut.m_0_d_ready.value = 1
  dut.sg_0_d_ready.value = 1
  for _ in range(10):
    await RisingEdge(dut.clock)

  await init_imsic(dut)


