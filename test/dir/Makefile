SIM ?= verilator
TOPLEVEL_LANG ?= verilog
VERILOG_SOURCES += $(addprefix $(realpath ../../gen)/, $(shell cat ../../gen/filelist.f))
TOPLEVEL = OpenAIA
MODULE = main
EXTRA_ARGS += --trace --trace-fst --trace-structs
SIM_BUILD = ../sim_build
include $(shell cocotb-config --makefiles)/Makefile.sim
