GEN_DIR := $(if $(wildcard ../../gen/filelist.f),$(realpath ../../gen),$(realpath ../../../gen))
TOPLEVEL = TLAIA
VERILOG_SOURCES += $(addprefix $(GEN_DIR)/, $(shell cat $(GEN_DIR)/filelist.f))
SIM_BUILD = ../sim_build_tl_fifo_full
include ../Makefile.common