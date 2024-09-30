local tl = require("TileLink")
local bit = require("bit")
local clock = dut.clock:chdl()
local gen_a = function (mode)
  return ([[
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
    prefix = mode .. "_0_a_",
    is_decoupled = true,
    name = "tilelink_channel_a"
  }
end
local m_a = gen_a("m")
local sg_a = gen_a("sg")
local a_get = function (a, addr, mask, size)
  clock:negedge()
  a.valid:set(1)
  a.bits.opcode:set(tl.TLMessageA.Get)
  a.bits.address:set(addr)
  a.bits.mask:set(mask)
  a.bits.size:set(size)
  clock:negedge()
  a.valid:set(0)
end
local a_put_full = function (a, addr, mask, size, data)
  clock:negedge()
  a.valid:set(1)
  a.bits.opcode:set(tl.TLMessageA.PutFullData)
  a.bits.address:set(addr)
  a.bits.mask:set(mask)
  a.bits.size:set(size)
  a.bits.data:set(data, true)
  clock:negedge()
  a.valid:set(0)
end

local gen_d = function (mode)
  return ([[
    | ready
    | valid
    | opcode
    | size
    | source
    | data
  ]]):bundle{
    hier = cfg.top,
    prefix = mode .. "_0_d_",
    is_decoupled = true,
    name = "tilelink_channel_d"
  }
end
local m_d = gen_d("m")
local sg_d = gen_d("sg")

local mBaseAddr = 0x61001000
local sgBaseAddr = 0x82908000
local csr_addr_eidelivery = 0x70
local csr_addr_eithreshold = 0x72
local csr_addr_eip0 = 0x80
local csr_addr_eip2 = 0x82
local csr_addr_eie0 = 0xC0
local m_int = function(intnum)
  a_put_full(m_a, mBaseAddr, 0xf, 2, intnum)
  dut.clock:posedge_until(10, function ()
    return dut.u_TLIMSICWrapper.imsic.seteipnum:get() == intnum
  end)
  clock:posedge()
end
local s_int = function(intnum)
  a_put_full(sg_a, sgBaseAddr, 0xf, 2, intnum)
  dut.clock:posedge_until(10, function ()
    return dut.u_TLIMSICWrapper.imsic.seteipnum_1:get() == intnum
  end)
  clock:posedge()
end
local v_int_vgein2 = function(intnum)
  a_put_full(sg_a, sgBaseAddr + 0x1000*(1+2), 0xf, 2, intnum)
  dut.clock:posedge_until(10, function ()
    return dut.u_TLIMSICWrapper.imsic.seteipnum_4:get() == intnum
  end)
  clock:posedge()
end
local claim = function()
    dut.clock:negedge(1)
    dut.fromCSR_claims_0:set(1)
    dut.clock:negedge(1)
    dut.fromCSR_claims_0:set(0)
end
local op_illegal = 0
local op_csrrw = 1
local op_csrrs = 2
local op_csrrc = 3
local write_csr_op = function(miselect, data, op)
  dut.clock:negedge(1)
  dut.fromCSR_addr_valid:set(1)
  dut.fromCSR_addr_bits:set(miselect)
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
  dut.fromCSR_addr_bits:set(miselect)
  dut.clock:negedge(1)
  dut.fromCSR_addr_valid:set(0)
end
local select_m_intfile = function()
  dut.clock:negedge(1)
  dut.fromCSR_priv:set(3)
  dut.fromCSR_virt:set(0)
end
local select_s_intfile = function()
  dut.clock:negedge(1)
  dut.fromCSR_priv:set(1)
  dut.fromCSR_virt:set(0)
end
local select_vs_intfile = function(vgein)
  dut.clock:negedge(1)
  dut.fromCSR_priv:set(1)
  dut.fromCSR_vgein:set(vgein)
  dut.fromCSR_virt:set(1)
end
local wrap_topei = function(in_)
  local extract = bit.band(0x7ff, in_)
  local out = bit.bor(extract, bit.lshift(extract, 16))
  return out
end

verilua "appendTasks" {
  main_task = function ()
    sim.dump_wave()

    dut.reset = 1
    dut.clock:posedge(10)
    dut.reset = 0
    m_d.ready:set(1)
    sg_d.ready:set(1)

    dut.clock:posedge(10)

    dut.toCSR_pendings_0:expect(0)
    select_m_intfile()

    do
      dut.cycles:dump()
      print("mseteipnum began")
      m_int(1996)
      dut.toCSR_topeis_0:expect(wrap_topei(1996))
      dut.toCSR_pendings_0:expect(1)
      print("mseteipnum passed")
    end

    do
      dut.cycles:dump()
      print("mclaim began")
      claim()
      dut.toCSR_topeis_0:expect(wrap_topei(0))
      print("mclaim passed")
    end

    do
      dut.cycles:dump()
      print("2_mseteipnum_1_mclaim began")
      m_int(12)
      dut.toCSR_topeis_0:expect(wrap_topei(12))
      m_int(8)
      dut.toCSR_topeis_0:expect(wrap_topei(8))
      claim()
      dut.toCSR_topeis_0:expect(wrap_topei(12))
      print("2_mseteipnum_1_mclaim passed")
    end

    do
      dut.cycles:dump()
      print("write_csr:op began")
      write_csr_op(csr_addr_eidelivery, 0xc0, op_csrrs)
      dut.u_TLIMSICWrapper.imsic.intFile.eidelivery:expect(0xc1)
      write_csr_op(csr_addr_eidelivery, 0xc0, op_csrrc)
      dut.u_TLIMSICWrapper.imsic.intFile.eidelivery:expect(0x1)
      print("write_csr:op passed")
    end

    do
      dut.cycles:dump()
      print("write_csr:eidelivery began")
      write_csr(csr_addr_eidelivery, 0)
      dut.toCSR_pendings_0:expect(0)
      write_csr(csr_addr_eidelivery, 1)
      print("write_csr:eidelivery passed")
    end

    do
      dut.cycles:dump()
      print("write_csr:meithreshold began")
      local mtopei = dut.toCSR_topeis_0:get()
      write_csr(csr_addr_eithreshold, bit.band(0x7ff,mtopei))
      assert(dut.toCSR_topeis_0:get() ~= wrap_topei(mtopei))
      write_csr(csr_addr_eithreshold, mtopei+1)
      dut.toCSR_topeis_0:expect(mtopei)
      write_csr(csr_addr_eithreshold, 0)
      print("write_csr:meithreshold end")
    end

    do
      dut.cycles:dump()
      print("write_csr:eip began")
      write_csr(csr_addr_eip0, 0xc)
      dut.toCSR_topeis_0:expect(wrap_topei(2))
      print("write_csr:eip end")
    end

    do
      dut.cycles:dump()
      print("write_csr:eie began")
      local mtopei = dut.toCSR_topeis_0:get()
      local mask = bit.lshift(1, bit.band(mtopei, 0x7ff))
      write_csr_op(csr_addr_eie0, mask, op_csrrc)
      assert(dut.toCSR_topeis_0:get() ~= mtopei)
      write_csr_op(csr_addr_eie0, mask, op_csrrs)
      dut.toCSR_topeis_0:expect(mtopei)
      print("write_csr:eie passed")
    end

    do
      dut.cycles:dump()
      print("read_csr:eie began")
      read_csr(csr_addr_eie0)
      dut.toCSR_rdata_bits:expect(dut.u_TLIMSICWrapper.imsic.intFile.eies_0:get())
      print("read_csr:eie passed")
    end

    do
      dut.cycles:dump()
      dut.toCSR_topeis_1:expect(wrap_topei(0))
      select_s_intfile()
      print("simple_supervisor_level began")
      s_int(1234)
      dut.toCSR_topeis_1:expect(wrap_topei(1234))
      dut.toCSR_pendings_1:expect(1)
      select_m_intfile()
      print("simple_supervisor_level end")
    end

    do
      dut.cycles:dump()
      select_vs_intfile(2)
      dut.toCSR_topeis_2:expect(wrap_topei(0))
      print("simple_virtualized_supervisor_level:vgein2 began")
      v_int_vgein2(137)
      dut.toCSR_topeis_2:expect(wrap_topei(137))
      dut.toCSR_pendings_4:expect(1)
      select_m_intfile()
      dut.toCSR_topeis_2:expect(wrap_topei(137))
      dut.toCSR_pendings_4:expect(1)
      print("simple_virtualized_supervisor_level:vgein2 end")
    end

    do
      dut.cycles:dump()
      print("illegal:iselect began")
      write_csr_op(0x71, 0xc0, op_csrrs)
      dut.toCSR_illegal:expect(1)
      print("illegal:iselect passed")
    end

    do
      dut.cycles:dump()
      print("illegal:vgein began")
      dut.clock:negedge(2)
      dut.toCSR_illegal:expect(0)
      select_vs_intfile(4)
      write_csr(csr_addr_eidelivery, 1)
      dut.toCSR_illegal:expect(1)
      select_m_intfile()
      print("illegal:vgein end")
    end

    do
      dut.cycles:dump()
      print("illegal:iselect:wdata_op began")
      dut.clock:negedge(2)
      dut.toCSR_illegal:expect(0)
      write_csr_op(csr_addr_eidelivery, 0xc0, op_illegal)
      dut.toCSR_illegal:expect(1)
      print("illegal:iselect:wdata_op passed")
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
      if m_a:fire() then
        ("[" .. _cycles .. "] [monitor] " .. m_a:dump_str()):print()
      end

      if m_d:fire() then
        ("[" .. _cycles .. "] [monitor] " .. m_d:dump_str()):print()
      end

      clock:posedge()
    end
  end
}
