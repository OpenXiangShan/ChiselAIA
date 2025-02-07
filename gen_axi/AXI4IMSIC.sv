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

module AXI4IMSIC(
  input         clock,
  input         reset,
  input         auto_axireg_axireg_axi4xbar_in_aw_valid,
  input  [5:0]  auto_axireg_axireg_axi4xbar_in_aw_bits_id,
  input  [16:0] auto_axireg_axireg_axi4xbar_in_aw_bits_addr,
  input  [7:0]  auto_axireg_axireg_axi4xbar_in_aw_bits_len,
  input  [2:0]  auto_axireg_axireg_axi4xbar_in_aw_bits_size,
  input  [1:0]  auto_axireg_axireg_axi4xbar_in_aw_bits_burst,
  input         auto_axireg_axireg_axi4xbar_in_aw_bits_lock,
  input  [3:0]  auto_axireg_axireg_axi4xbar_in_aw_bits_cache,
  input  [2:0]  auto_axireg_axireg_axi4xbar_in_aw_bits_prot,
  input  [3:0]  auto_axireg_axireg_axi4xbar_in_aw_bits_qos,
  input         auto_axireg_axireg_axi4xbar_in_w_valid,
  input  [31:0] auto_axireg_axireg_axi4xbar_in_w_bits_data,
  input  [3:0]  auto_axireg_axireg_axi4xbar_in_w_bits_strb,
  input         auto_axireg_axireg_axi4xbar_in_w_bits_last,
  input         auto_axireg_axireg_axi4xbar_in_b_ready,
  output        auto_axireg_axireg_axi4xbar_in_b_valid,
  output [5:0]  auto_axireg_axireg_axi4xbar_in_b_bits_id,
  output [1:0]  auto_axireg_axireg_axi4xbar_in_b_bits_resp,
  output        auto_axireg_axireg_axi4xbar_in_ar_ready,
  input         auto_axireg_axireg_axi4xbar_in_ar_valid,
  input  [5:0]  auto_axireg_axireg_axi4xbar_in_ar_bits_id,
  input  [16:0] auto_axireg_axireg_axi4xbar_in_ar_bits_addr,
  input  [7:0]  auto_axireg_axireg_axi4xbar_in_ar_bits_len,
  input  [2:0]  auto_axireg_axireg_axi4xbar_in_ar_bits_size,
  input  [1:0]  auto_axireg_axireg_axi4xbar_in_ar_bits_burst,
  input         auto_axireg_axireg_axi4xbar_in_ar_bits_lock,
  input  [3:0]  auto_axireg_axireg_axi4xbar_in_ar_bits_cache,
  input  [2:0]  auto_axireg_axireg_axi4xbar_in_ar_bits_prot,
  input  [3:0]  auto_axireg_axireg_axi4xbar_in_ar_bits_qos,
  input         auto_axireg_axireg_axi4xbar_in_r_ready,
  output        auto_axireg_axireg_axi4xbar_in_r_valid,
  output [5:0]  auto_axireg_axireg_axi4xbar_in_r_bits_id,
  output [31:0] auto_axireg_axireg_axi4xbar_in_r_bits_data,
  output [1:0]  auto_axireg_axireg_axi4xbar_in_r_bits_resp,
  output        auto_axireg_axireg_axi4xbar_in_r_bits_last,
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
  wire        _axireg_msiio_msi_vld_req;
  AXIRegIMSIC_WRAP axireg (
    .clock                                 (soc_clock),
    .reset                                 (soc_reset),
    .auto_axireg_axi4xbar_in_aw_valid      (auto_axireg_axireg_axi4xbar_in_aw_valid),
    .auto_axireg_axi4xbar_in_aw_bits_id    (auto_axireg_axireg_axi4xbar_in_aw_bits_id),
    .auto_axireg_axi4xbar_in_aw_bits_addr  (auto_axireg_axireg_axi4xbar_in_aw_bits_addr),
    .auto_axireg_axi4xbar_in_aw_bits_len   (auto_axireg_axireg_axi4xbar_in_aw_bits_len),
    .auto_axireg_axi4xbar_in_aw_bits_size  (auto_axireg_axireg_axi4xbar_in_aw_bits_size),
    .auto_axireg_axi4xbar_in_aw_bits_burst (auto_axireg_axireg_axi4xbar_in_aw_bits_burst),
    .auto_axireg_axi4xbar_in_aw_bits_lock  (auto_axireg_axireg_axi4xbar_in_aw_bits_lock),
    .auto_axireg_axi4xbar_in_aw_bits_cache (auto_axireg_axireg_axi4xbar_in_aw_bits_cache),
    .auto_axireg_axi4xbar_in_aw_bits_prot  (auto_axireg_axireg_axi4xbar_in_aw_bits_prot),
    .auto_axireg_axi4xbar_in_aw_bits_qos   (auto_axireg_axireg_axi4xbar_in_aw_bits_qos),
    .auto_axireg_axi4xbar_in_w_valid       (auto_axireg_axireg_axi4xbar_in_w_valid),
    .auto_axireg_axi4xbar_in_w_bits_data   (auto_axireg_axireg_axi4xbar_in_w_bits_data),
    .auto_axireg_axi4xbar_in_w_bits_strb   (auto_axireg_axireg_axi4xbar_in_w_bits_strb),
    .auto_axireg_axi4xbar_in_w_bits_last   (auto_axireg_axireg_axi4xbar_in_w_bits_last),
    .auto_axireg_axi4xbar_in_b_ready       (auto_axireg_axireg_axi4xbar_in_b_ready),
    .auto_axireg_axi4xbar_in_b_valid       (auto_axireg_axireg_axi4xbar_in_b_valid),
    .auto_axireg_axi4xbar_in_b_bits_id     (auto_axireg_axireg_axi4xbar_in_b_bits_id),
    .auto_axireg_axi4xbar_in_b_bits_resp   (auto_axireg_axireg_axi4xbar_in_b_bits_resp),
    .auto_axireg_axi4xbar_in_ar_ready      (auto_axireg_axireg_axi4xbar_in_ar_ready),
    .auto_axireg_axi4xbar_in_ar_valid      (auto_axireg_axireg_axi4xbar_in_ar_valid),
    .auto_axireg_axi4xbar_in_ar_bits_id    (auto_axireg_axireg_axi4xbar_in_ar_bits_id),
    .auto_axireg_axi4xbar_in_ar_bits_addr  (auto_axireg_axireg_axi4xbar_in_ar_bits_addr),
    .auto_axireg_axi4xbar_in_ar_bits_len   (auto_axireg_axireg_axi4xbar_in_ar_bits_len),
    .auto_axireg_axi4xbar_in_ar_bits_size  (auto_axireg_axireg_axi4xbar_in_ar_bits_size),
    .auto_axireg_axi4xbar_in_ar_bits_burst (auto_axireg_axireg_axi4xbar_in_ar_bits_burst),
    .auto_axireg_axi4xbar_in_ar_bits_lock  (auto_axireg_axireg_axi4xbar_in_ar_bits_lock),
    .auto_axireg_axi4xbar_in_ar_bits_cache (auto_axireg_axireg_axi4xbar_in_ar_bits_cache),
    .auto_axireg_axi4xbar_in_ar_bits_prot  (auto_axireg_axireg_axi4xbar_in_ar_bits_prot),
    .auto_axireg_axi4xbar_in_ar_bits_qos   (auto_axireg_axireg_axi4xbar_in_ar_bits_qos),
    .auto_axireg_axi4xbar_in_r_ready       (auto_axireg_axireg_axi4xbar_in_r_ready),
    .auto_axireg_axi4xbar_in_r_valid       (auto_axireg_axireg_axi4xbar_in_r_valid),
    .auto_axireg_axi4xbar_in_r_bits_id     (auto_axireg_axireg_axi4xbar_in_r_bits_id),
    .auto_axireg_axi4xbar_in_r_bits_data   (auto_axireg_axireg_axi4xbar_in_r_bits_data),
    .auto_axireg_axi4xbar_in_r_bits_resp   (auto_axireg_axireg_axi4xbar_in_r_bits_resp),
    .auto_axireg_axi4xbar_in_r_bits_last   (auto_axireg_axireg_axi4xbar_in_r_bits_last),
    .io_seteipnum                          (_axireg_io_seteipnum),
    .msiio_msi_vld_req                     (_axireg_msiio_msi_vld_req),
    .msiio_msi_vld_ack                     (_imsic_msiio_msi_vld_ack)
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
    .msiio_msi_vld_req       (_axireg_msiio_msi_vld_req),
    .msiio_msi_vld_ack       (_imsic_msiio_msi_vld_ack)
  );
endmodule

