// Generated by CIRCT firtool-1.62.0
// Standard header to adapt well known macros for prints and assertions.

// Users can define 'ASSERT_VERBOSE_COND' to add an extra gate to assert error printing.
`ifndef ASSERT_VERBOSE_COND_
  `ifdef ASSERT_VERBOSE_COND
    `define ASSERT_VERBOSE_COND_ (`ASSERT_VERBOSE_COND)
  `else  // ASSERT_VERBOSE_COND
    `define ASSERT_VERBOSE_COND_ 1
  `endif // ASSERT_VERBOSE_COND
`endif // not def ASSERT_VERBOSE_COND_

// Users can define 'STOP_COND' to add an extra gate to stop conditions.
`ifndef STOP_COND_
  `ifdef STOP_COND
    `define STOP_COND_ (`STOP_COND)
  `else  // STOP_COND
    `define STOP_COND_ 1
  `endif // STOP_COND
`endif // not def STOP_COND_

module TLIMSIC(
  input         clock,
  input         reset,
  output        auto_axireg_axireg_xbar_in_a_ready,
  input         auto_axireg_axireg_xbar_in_a_valid,
  input  [2:0]  auto_axireg_axireg_xbar_in_a_bits_opcode,
  input  [2:0]  auto_axireg_axireg_xbar_in_a_bits_param,
  input  [1:0]  auto_axireg_axireg_xbar_in_a_bits_size,
  input  [5:0]  auto_axireg_axireg_xbar_in_a_bits_source,
  input  [16:0] auto_axireg_axireg_xbar_in_a_bits_address,
  input  [7:0]  auto_axireg_axireg_xbar_in_a_bits_mask,
  input  [63:0] auto_axireg_axireg_xbar_in_a_bits_data,
  input         auto_axireg_axireg_xbar_in_a_bits_corrupt,
  input         auto_axireg_axireg_xbar_in_d_ready,
  output        auto_axireg_axireg_xbar_in_d_valid,
  output [2:0]  auto_axireg_axireg_xbar_in_d_bits_opcode,
  output [1:0]  auto_axireg_axireg_xbar_in_d_bits_size,
  output [5:0]  auto_axireg_axireg_xbar_in_d_bits_source,
  output        toCSR_rdata_valid,
  output [63:0] toCSR_rdata_bits,
  output        toCSR_illegal,
  output [5:0]  toCSR_pendings,
  output [31:0] toCSR_topeis_0,
  output [31:0] toCSR_topeis_1,
  output [31:0] toCSR_topeis_2,
  input         fromCSR_addr_valid,
  input  [11:0] fromCSR_addr_bits,
  input         fromCSR_virt,
  input  [1:0]  fromCSR_priv,
  input  [5:0]  fromCSR_vgein,
  input         fromCSR_wdata_valid,
  input  [1:0]  fromCSR_wdata_bits_op,
  input  [63:0] fromCSR_wdata_bits_data,
  input         fromCSR_claims_0,
  input         fromCSR_claims_1,
  input         fromCSR_claims_2,
  input         soc_clock,
  input         soc_reset
);

  wire        _imsic_msiio_msi_vld_ack;
  wire [10:0] _axireg_io_seteipnum;
  wire        _axireg_io_valid;
  wire        _axireg_msiio_msi_vld_req;
  TLRegIMSIC_WRAP axireg (
    .clock                              (soc_clock),
    .reset                              (soc_reset),
    .auto_axireg_xbar_in_a_ready        (auto_axireg_axireg_xbar_in_a_ready),
    .auto_axireg_xbar_in_a_valid        (auto_axireg_axireg_xbar_in_a_valid),
    .auto_axireg_xbar_in_a_bits_opcode  (auto_axireg_axireg_xbar_in_a_bits_opcode),
    .auto_axireg_xbar_in_a_bits_param   (auto_axireg_axireg_xbar_in_a_bits_param),
    .auto_axireg_xbar_in_a_bits_size    (auto_axireg_axireg_xbar_in_a_bits_size),
    .auto_axireg_xbar_in_a_bits_source  (auto_axireg_axireg_xbar_in_a_bits_source),
    .auto_axireg_xbar_in_a_bits_address (auto_axireg_axireg_xbar_in_a_bits_address),
    .auto_axireg_xbar_in_a_bits_mask    (auto_axireg_axireg_xbar_in_a_bits_mask),
    .auto_axireg_xbar_in_a_bits_data    (auto_axireg_axireg_xbar_in_a_bits_data),
    .auto_axireg_xbar_in_a_bits_corrupt (auto_axireg_axireg_xbar_in_a_bits_corrupt),
    .auto_axireg_xbar_in_d_ready        (auto_axireg_axireg_xbar_in_d_ready),
    .auto_axireg_xbar_in_d_valid        (auto_axireg_axireg_xbar_in_d_valid),
    .auto_axireg_xbar_in_d_bits_opcode  (auto_axireg_axireg_xbar_in_d_bits_opcode),
    .auto_axireg_xbar_in_d_bits_size    (auto_axireg_axireg_xbar_in_d_bits_size),
    .auto_axireg_xbar_in_d_bits_source  (auto_axireg_axireg_xbar_in_d_bits_source),
    .io_seteipnum                       (_axireg_io_seteipnum),
    .io_valid                           (_axireg_io_valid),
    .msiio_msi_vld_req                  (_axireg_msiio_msi_vld_req),
    .msiio_msi_vld_ack                  (_imsic_msiio_msi_vld_ack)
  );
  IMSIC_WRAP imsic (
    .clock                   (clock),
    .reset                   (reset),
    .toCSR_rdata_valid       (toCSR_rdata_valid),
    .toCSR_rdata_bits        (toCSR_rdata_bits),
    .toCSR_illegal           (toCSR_illegal),
    .toCSR_pendings          (toCSR_pendings),
    .toCSR_topeis_0          (toCSR_topeis_0),
    .toCSR_topeis_1          (toCSR_topeis_1),
    .toCSR_topeis_2          (toCSR_topeis_2),
    .fromCSR_addr_valid      (fromCSR_addr_valid),
    .fromCSR_addr_bits       (fromCSR_addr_bits),
    .fromCSR_virt            (fromCSR_virt),
    .fromCSR_priv            (fromCSR_priv),
    .fromCSR_vgein           (fromCSR_vgein),
    .fromCSR_wdata_valid     (fromCSR_wdata_valid),
    .fromCSR_wdata_bits_op   (fromCSR_wdata_bits_op),
    .fromCSR_wdata_bits_data (fromCSR_wdata_bits_data),
    .fromCSR_claims_0        (fromCSR_claims_0),
    .fromCSR_claims_1        (fromCSR_claims_1),
    .fromCSR_claims_2        (fromCSR_claims_2),
    .io_seteipnum            (_axireg_io_seteipnum),
    .io_valid                (_axireg_io_valid),
    .msiio_msi_vld_req       (_axireg_msiio_msi_vld_req),
    .msiio_msi_vld_ack       (_imsic_msiio_msi_vld_ack)
  );
endmodule

