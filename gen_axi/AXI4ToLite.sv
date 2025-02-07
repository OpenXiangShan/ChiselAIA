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

module AXI4ToLite(
  input         clock,
  input         reset,
  input         auto_in_aw_valid,
  input  [5:0]  auto_in_aw_bits_id,
  input  [11:0] auto_in_aw_bits_addr,
  input  [7:0]  auto_in_aw_bits_len,
  input  [2:0]  auto_in_aw_bits_size,
  input  [1:0]  auto_in_aw_bits_burst,
  input         auto_in_aw_bits_lock,
  input  [3:0]  auto_in_aw_bits_cache,
  input  [2:0]  auto_in_aw_bits_prot,
  input  [3:0]  auto_in_aw_bits_qos,
  input         auto_in_w_valid,
  input  [31:0] auto_in_w_bits_data,
  input  [3:0]  auto_in_w_bits_strb,
  input         auto_in_w_bits_last,
  input         auto_in_b_ready,
  output        auto_in_b_valid,
  output [5:0]  auto_in_b_bits_id,
  output [1:0]  auto_in_b_bits_resp,
  output        auto_in_ar_ready,
  input         auto_in_ar_valid,
  input  [5:0]  auto_in_ar_bits_id,
  input  [11:0] auto_in_ar_bits_addr,
  input  [7:0]  auto_in_ar_bits_len,
  input  [2:0]  auto_in_ar_bits_size,
  input  [1:0]  auto_in_ar_bits_burst,
  input         auto_in_ar_bits_lock,
  input  [3:0]  auto_in_ar_bits_cache,
  input  [2:0]  auto_in_ar_bits_prot,
  input  [3:0]  auto_in_ar_bits_qos,
  input         auto_in_r_ready,
  output        auto_in_r_valid,
  output [5:0]  auto_in_r_bits_id,
  output [31:0] auto_in_r_bits_data,
  output [1:0]  auto_in_r_bits_resp,
  output        auto_in_r_bits_last,
  input         auto_out_aw_ready,
  output [5:0]  auto_out_aw_bits_id,
  output [11:0] auto_out_aw_bits_addr,
  output [7:0]  auto_out_aw_bits_len,
  output [2:0]  auto_out_aw_bits_size,
  output [1:0]  auto_out_aw_bits_burst,
  output        auto_out_aw_bits_lock,
  output [3:0]  auto_out_aw_bits_cache,
  output [2:0]  auto_out_aw_bits_prot,
  output [3:0]  auto_out_aw_bits_qos,
  output [31:0] auto_out_w_bits_data,
  output [3:0]  auto_out_w_bits_strb,
  output        auto_out_w_bits_last,
  output        auto_out_b_ready,
  input  [5:0]  auto_out_b_bits_id,
  input  [1:0]  auto_out_b_bits_resp,
  input         auto_out_ar_ready,
  output        auto_out_ar_valid,
  output [5:0]  auto_out_ar_bits_id,
  output [11:0] auto_out_ar_bits_addr,
  output [7:0]  auto_out_ar_bits_len,
  output [2:0]  auto_out_ar_bits_size,
  output [1:0]  auto_out_ar_bits_burst,
  output        auto_out_ar_bits_lock,
  output [3:0]  auto_out_ar_bits_cache,
  output [2:0]  auto_out_ar_bits_prot,
  output [3:0]  auto_out_ar_bits_qos,
  output        auto_out_r_ready,
  input         auto_out_r_valid,
  input  [5:0]  auto_out_r_bits_id,
  input  [31:0] auto_out_r_bits_data,
  input  [1:0]  auto_out_r_bits_resp,
  input         auto_out_r_bits_last
);

  wire [5:0]  nodeIn_aw_bits_id = auto_in_aw_bits_id;
  wire [11:0] nodeIn_aw_bits_addr = auto_in_aw_bits_addr;
  wire [7:0]  nodeIn_aw_bits_len = auto_in_aw_bits_len;
  wire [2:0]  nodeIn_aw_bits_size = auto_in_aw_bits_size;
  wire [1:0]  nodeIn_aw_bits_burst = auto_in_aw_bits_burst;
  wire        nodeIn_aw_bits_lock = auto_in_aw_bits_lock;
  wire [3:0]  nodeIn_aw_bits_cache = auto_in_aw_bits_cache;
  wire [2:0]  nodeIn_aw_bits_prot = auto_in_aw_bits_prot;
  wire [3:0]  nodeIn_aw_bits_qos = auto_in_aw_bits_qos;
  wire [31:0] nodeIn_w_bits_data = auto_in_w_bits_data;
  wire [3:0]  nodeIn_w_bits_strb = auto_in_w_bits_strb;
  wire        nodeIn_w_bits_last = auto_in_w_bits_last;
  wire [5:0]  nodeIn_ar_bits_id = auto_in_ar_bits_id;
  wire [11:0] nodeIn_ar_bits_addr = auto_in_ar_bits_addr;
  wire [7:0]  nodeIn_ar_bits_len = auto_in_ar_bits_len;
  wire [2:0]  nodeIn_ar_bits_size = auto_in_ar_bits_size;
  wire [1:0]  nodeIn_ar_bits_burst = auto_in_ar_bits_burst;
  wire        nodeIn_ar_bits_lock = auto_in_ar_bits_lock;
  wire [3:0]  nodeIn_ar_bits_cache = auto_in_ar_bits_cache;
  wire [2:0]  nodeIn_ar_bits_prot = auto_in_ar_bits_prot;
  wire [3:0]  nodeIn_ar_bits_qos = auto_in_ar_bits_qos;
  wire [5:0]  nodeOut_b_bits_id = auto_out_b_bits_id;
  wire [1:0]  nodeOut_b_bits_resp = auto_out_b_bits_resp;
  wire [5:0]  nodeOut_r_bits_id = auto_out_r_bits_id;
  wire [31:0] nodeOut_r_bits_data = auto_out_r_bits_data;
  wire [1:0]  nodeOut_r_bits_resp = auto_out_r_bits_resp;
  wire        nodeOut_r_bits_last = auto_out_r_bits_last;
  wire [5:0]  nodeOut_aw_bits_id = nodeIn_aw_bits_id;
  wire [7:0]  nodeOut_aw_bits_len = nodeIn_aw_bits_len;
  wire [2:0]  nodeOut_aw_bits_size = nodeIn_aw_bits_size;
  wire [1:0]  nodeOut_aw_bits_burst = nodeIn_aw_bits_burst;
  wire        nodeOut_aw_bits_lock = nodeIn_aw_bits_lock;
  wire [3:0]  nodeOut_aw_bits_cache = nodeIn_aw_bits_cache;
  wire [2:0]  nodeOut_aw_bits_prot = nodeIn_aw_bits_prot;
  wire [3:0]  nodeOut_aw_bits_qos = nodeIn_aw_bits_qos;
  wire [3:0]  nodeOut_w_bits_strb = nodeIn_w_bits_strb;
  wire        nodeOut_w_bits_last = nodeIn_w_bits_last;
  wire [5:0]  nodeOut_ar_bits_id = nodeIn_ar_bits_id;
  wire [11:0] nodeOut_ar_bits_addr = nodeIn_ar_bits_addr;
  wire [7:0]  nodeOut_ar_bits_len = nodeIn_ar_bits_len;
  wire [2:0]  nodeOut_ar_bits_size = nodeIn_ar_bits_size;
  wire [1:0]  nodeOut_ar_bits_burst = nodeIn_ar_bits_burst;
  wire        nodeOut_ar_bits_lock = nodeIn_ar_bits_lock;
  wire [3:0]  nodeOut_ar_bits_cache = nodeIn_ar_bits_cache;
  wire [2:0]  nodeOut_ar_bits_prot = nodeIn_ar_bits_prot;
  wire [3:0]  nodeOut_ar_bits_qos = nodeIn_ar_bits_qos;
  wire [5:0]  nodeIn_r_bits_id = nodeOut_r_bits_id;
  wire [31:0] nodeIn_r_bits_data = nodeOut_r_bits_data;
  wire [1:0]  nodeIn_r_bits_resp = nodeOut_r_bits_resp;
  reg  [1:0]  state;
  reg  [11:0] aw_l_addr;
  wire [11:0] nodeOut_aw_bits_addr = aw_l_addr;
  reg  [7:0]  aw_l_len;
  reg  [31:0] w_l_data;
  wire [31:0] nodeOut_w_bits_data = w_l_data;
  reg  [5:0]  b_l_id;
  wire [5:0]  nodeIn_b_bits_id = b_l_id;
  reg  [1:0]  b_l_resp;
  wire [1:0]  nodeIn_b_bits_resp = b_l_resp;
  reg  [7:0]  ar_l_len;
  reg  [7:0]  awcnt;
  wire        nodeIn_r_bits_last = (&state) & awcnt == ar_l_len;
  wire [1:0]  _GEN = auto_in_aw_valid & auto_in_w_valid ? 2'h1 : {2{auto_in_ar_valid}};
  wire        _nodeOut_w_valid_T = state == 2'h1;
  always @(posedge clock) begin
    if (reset) begin
      state <= 2'h0;
      awcnt <= 8'h0;
    end
    else begin
      if (|state)
        state <= 2'h0;
      else
        state <= _GEN;
      if (_nodeOut_w_valid_T) begin
        if (awcnt >= aw_l_len
            | ~(~(_nodeOut_w_valid_T
                  & ~((|aw_l_addr) | (|(aw_l_addr[1:0])) | (|(aw_l_addr[11:2])))
                  & awcnt == 8'h0) | auto_out_aw_ready)) begin
        end
        else
          awcnt <= 8'(awcnt + 8'h1);
      end
      else if (&state) begin
        if (auto_out_ar_ready)
          awcnt <= 8'(awcnt + 8'h1);
      end
      else
        awcnt <= 8'h0;
    end
    if (~(|state) & ((|state) ? 2'h0 : _GEN) == 2'h1) begin
      aw_l_addr <= nodeIn_aw_bits_addr;
      aw_l_len <= nodeIn_aw_bits_len;
      w_l_data <= nodeIn_w_bits_data;
      b_l_id <= nodeIn_b_bits_id;
      b_l_resp <= nodeIn_b_bits_resp;
      ar_l_len <= nodeIn_ar_bits_len;
    end
  end // always @(posedge)
  assign auto_in_b_valid = state == 2'h2;
  assign auto_in_b_bits_id = nodeIn_b_bits_id;
  assign auto_in_b_bits_resp = nodeIn_b_bits_resp;
  assign auto_in_ar_ready = auto_out_ar_ready;
  assign auto_in_r_valid = auto_out_r_valid;
  assign auto_in_r_bits_id = nodeIn_r_bits_id;
  assign auto_in_r_bits_data = nodeIn_r_bits_data;
  assign auto_in_r_bits_resp = nodeIn_r_bits_resp;
  assign auto_in_r_bits_last = nodeIn_r_bits_last;
  assign auto_out_aw_bits_id = nodeOut_aw_bits_id;
  assign auto_out_aw_bits_addr = nodeOut_aw_bits_addr;
  assign auto_out_aw_bits_len = nodeOut_aw_bits_len;
  assign auto_out_aw_bits_size = nodeOut_aw_bits_size;
  assign auto_out_aw_bits_burst = nodeOut_aw_bits_burst;
  assign auto_out_aw_bits_lock = nodeOut_aw_bits_lock;
  assign auto_out_aw_bits_cache = nodeOut_aw_bits_cache;
  assign auto_out_aw_bits_prot = nodeOut_aw_bits_prot;
  assign auto_out_aw_bits_qos = nodeOut_aw_bits_qos;
  assign auto_out_w_bits_data = nodeOut_w_bits_data;
  assign auto_out_w_bits_strb = nodeOut_w_bits_strb;
  assign auto_out_w_bits_last = nodeOut_w_bits_last;
  assign auto_out_b_ready = auto_in_b_ready;
  assign auto_out_ar_valid = auto_in_ar_valid;
  assign auto_out_ar_bits_id = nodeOut_ar_bits_id;
  assign auto_out_ar_bits_addr = nodeOut_ar_bits_addr;
  assign auto_out_ar_bits_len = nodeOut_ar_bits_len;
  assign auto_out_ar_bits_size = nodeOut_ar_bits_size;
  assign auto_out_ar_bits_burst = nodeOut_ar_bits_burst;
  assign auto_out_ar_bits_lock = nodeOut_ar_bits_lock;
  assign auto_out_ar_bits_cache = nodeOut_ar_bits_cache;
  assign auto_out_ar_bits_prot = nodeOut_ar_bits_prot;
  assign auto_out_ar_bits_qos = nodeOut_ar_bits_qos;
  assign auto_out_r_ready = auto_in_r_ready;
endmodule

