SIM ?= verilator
TOPLEVEL_LANG ?= verilog
VERILOG_SOURCES += $(addprefix $(realpath ../gen)/, $(shell cat ../gen/filelist.f))
TOPLEVEL = TLIMSICWrapper
MODULE = simple_ut
EXTRA_ARGS += --trace --trace-fst --trace-structs
include $(shell cocotb-config --makefiles)/Makefile.sim
