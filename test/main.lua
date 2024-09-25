local tl = require("TileLink")
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
verilua "appendTasks" {
  main_task = function ()
    sim.dump_wave()

    dut.reset = 1
    dut.clock:posedge(10)
    dut.reset = 0

    dut.clock:posedge(10)

    a:get(mBaseAddr, 0xf, 2)

    dut.clock:posedge(1000)
    dut.cycles:dump()

    sim.finish()
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
