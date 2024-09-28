local tl = require("TileLink")
local bit = require("bit")
local clock = dut.clock:chdl()
local a = ([[
  | ready
  | valid
  | opcode
  | param
  | size
  | source
  | address
  | mask
  | data
  | corrupt
]]):bundle{
  hier = cfg.top,
  prefix = "localanon_0_a_",
  is_decoupled = true,
  name = "tilelink_channel_a"
}
a.get = function (this, addr, mask, size)
  clock:negedge()
  this.valid:set(1)
  this.bits.opcode:set(tl.TLMessageA.Get)
  this.bits.address:set(addr)
  this.bits.mask:set(mask)
  this.bits.size:set(size)
  clock:negedge()
  this.valid:set(0)
end
a.put_full = function (this, addr, mask, size, data)
  clock:negedge()
  this.valid:set(1)
  this.bits.opcode:set(tl.TLMessageA.PutFullData)
  this.bits.address:set(addr)
  this.bits.mask:set(mask)
  this.bits.size:set(size)
  this.bits.data:set(data, true)
  clock:negedge()
  this.valid:set(0)
end

local d = ([[
  | ready
  | valid
  | opcode
  | size
  | source
  | data
]]):bundle{
  hier = cfg.top,
  prefix = "localanon_0_d_",
  is_decoupled = true,
  name = "tilelink_channel_d"
}

local mBaseAddr = 0x61001000
local csr_addr_eidelivery = 0x70
local csr_addr_eithreshold = 0x72
local csr_addr_eip0 = 0x80
local csr_addr_eip2 = 0x82
local csr_addr_eie0 = 0xC0
local inject_interrupt = function(intnum)
  a:put_full(mBaseAddr, 0xf, 2, intnum)
  dut.clock:posedge_until(10, function ()
    return dut.u_TLIMSICWrapper.imsic.mseteipnum:get() == intnum
  end)
  clock:posedge()
end
local claim = function()
    dut.clock:negedge(1)
    dut.fromCSR_mClaim:set(1)
    dut.clock:negedge(1)
    dut.fromCSR_mClaim:set(0)
end
local op_csrrw = 1
local op_csrrs = 2
local op_csrrc = 3
local write_csr_op = function(miselect, data, op)
  dut.clock:negedge(1)
  dut.fromCSR_addr_valid:set(1)
  dut.fromCSR_addr_bits_addr:set(miselect)
  dut.fromCSR_wdata_valid:set(1)
  dut.fromCSR_wdata_bits_op:set(op)
  dut.fromCSR_wdata_bits_data:chdl():set(data, true)
  dut.clock:negedge(1)
  dut.fromCSR_addr_valid:set(0)
  dut.fromCSR_wdata_valid:set(0)
end
local write_csr = function(miselect, data)
  write_csr_op(miselect, data, op_csrrw)
end
local read_csr = function(miselect)
  dut.clock:negedge(1)
  dut.fromCSR_addr_valid:set(1)
  dut.fromCSR_addr_bits_addr:set(miselect)
  dut.clock:negedge(1)
  dut.fromCSR_addr_valid:set(0)
end

verilua "appendTasks" {
  main_task = function ()
    sim.dump_wave()

    dut.reset = 1
    dut.clock:posedge(10)
    dut.reset = 0
    d.ready:set(1)

    dut.clock:posedge(10)

    dut.toCSR_meipB:expect(0)

    do
      dut.cycles:dump()
      print("mseteipnum began")
      inject_interrupt(1996)
      dut.toCSR_mtopei:expect(1996)
      dut.toCSR_meipB:expect(1)
      print("mseteipnum passed")
    end

    do
      dut.cycles:dump()
      print("mclaim began")
      claim()
      dut.toCSR_mtopei:expect(0)
      print("mclaim passed")
    end

    do
      dut.cycles:dump()
      print("2_mseteipnum_1_mclaim began")
      inject_interrupt(12)
      dut.toCSR_mtopei:expect(12)
      inject_interrupt(8)
      dut.toCSR_mtopei:expect(8)
      claim()
      dut.toCSR_mtopei:expect(12)
      print("2_mseteipnum_1_mclaim passed")
    end

    do
      dut.cycles:dump()
      print("write_csr:op began")
      write_csr_op(csr_addr_eidelivery, 0xc0, op_csrrs)
      dut.u_TLIMSICWrapper.imsic.mIntFile.eidelivery:expect(0xc1)
      write_csr_op(csr_addr_eidelivery, 0xc0, op_csrrc)
      dut.u_TLIMSICWrapper.imsic.mIntFile.eidelivery:expect(0x1)
      print("write_csr:op passed")
    end

    do
      dut.cycles:dump()
      print("write_csr:eidelivery began")
      write_csr(csr_addr_eidelivery, 0)
      dut.toCSR_meipB:expect(0)
      write_csr(csr_addr_eidelivery, 1)
      print("write_csr:eidelivery passed")
    end

    do
      dut.cycles:dump()
      print("write_csr:meithreshold began")
      local mtopei = dut.toCSR_mtopei:get()
      write_csr(csr_addr_eithreshold, mtopei)
      assert(dut.toCSR_mtopei:get() < mtopei)
      write_csr(csr_addr_eithreshold, mtopei+1)
      dut.toCSR_mtopei:expect(mtopei)
      write_csr(csr_addr_eithreshold, 0)
      print("write_csr:meithreshold end")
    end

    do
      dut.cycles:dump()
      print("write_csr:eip began")
      write_csr(csr_addr_eip0, 0xc)
      dut.toCSR_mtopei:expect(2)
      print("write_csr:eip end")
    end

    do
      dut.cycles:dump()
      print("write_csr:eie began")
      local mtopei = dut.toCSR_mtopei:get()
      local mask = bit.lshift(1, mtopei)
      write_csr_op(csr_addr_eie0, mask, op_csrrc)
      assert(dut.toCSR_mtopei:get() ~= mtopei)
      write_csr_op(csr_addr_eie0, mask, op_csrrs)
      dut.toCSR_mtopei:expect(mtopei)
      print("write_csr:eie passed")
    end

    do
      dut.cycles:dump()
      print("read_csr:eie began")
      read_csr(csr_addr_eie0)
      dut.toCSR_rdata_bits_data:expect(dut.u_TLIMSICWrapper.imsic.mIntFile.eies_0:get())
      print("read_csr:eie passed")
    end

    dut.cycles:dump()
    dut.clock:posedge(1000)
    dut.cycles:dump()

    sim.finish()
    print()
    print("Verilua passed!")
  end,

  monitor_task = function()
    local clock = dut.clock:chdl()
    local cycles = dut.cycles:chdl()
    while true do
      local _cycles = cycles:get()
      if a:fire() then
        ("[" .. _cycles .. "] [monitor] " .. a:dump_str()):print()
      end

      if d:fire() then
        ("[" .. _cycles .. "] [monitor] " .. d:dump_str()):print()
      end

      clock:posedge()
    end
  end
}
