target("OpenAIA.scala")
  add_rules("verilua")
  add_toolchains("@verilator")

  add_files("gen/*.sv")
  add_files("gen/*.v")
  add_values("verilator.flags", "--trace", "--no-trace-top")

  set_values("cfg.deps", "./test/?.lua")
  set_values("cfg.top", "TLIMSICWrapper")
  set_values("cfg.lua_main", "test/main.lua")
  set_values("cfg.tb_gen_flags", "-ne")
