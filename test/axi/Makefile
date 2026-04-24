TOPLEVEL = AXI4AIA
GEN_DIR ?= ../../../gen_axi
FILELIST ?= $(GEN_DIR)/filelist.f
VERILOG_SOURCES = $(addprefix $(realpath $(GEN_DIR))/, $(shell cat $(FILELIST)))
SIM_BUILD = ../sim_build_axi
include ../Makefile.common
