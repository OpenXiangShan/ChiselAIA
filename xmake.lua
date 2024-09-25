target("OpenAIA.scala")
  add_rules("verilua")
  add_toolchains("@verilator")

  add_files("*.sv")
  add_files("rocket-chip/src/main/resources/vsrc/plusarg_reader.v")
  add_values("verilator.flags", "--trace", "--no-trace-top")

  set_values("cfg.deps", "./test/?.lua")
  set_values("cfg.top", "IMSIC")
  set_values("cfg.lua_main", "test/main.lua")
  set_values("cfg.tb_gen_flags", "-ne")
