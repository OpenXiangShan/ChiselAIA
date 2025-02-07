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

module Queue2_UInt4(
  input        clock,
  input        reset,
  output       io_enq_ready,
  input        io_enq_valid,
  input  [3:0] io_enq_bits,
  output       io_deq_valid,
  output [3:0] io_deq_bits
);

  wire [3:0] _ram_ext_R0_data;
  reg        wrap;
  reg        maybe_full;
  wire       empty = ~wrap & ~maybe_full;
  wire       full = ~wrap & maybe_full;
  wire       do_enq = ~full & io_enq_valid;
  always @(posedge clock) begin
    if (reset) begin
      wrap <= 1'h0;
      maybe_full <= 1'h0;
    end
    else if (do_enq) begin
      wrap <= 1'(wrap - 1'h1);
      maybe_full <= do_enq;
    end
  end // always @(posedge)
  ram_2x4 ram_ext (
    .R0_addr (1'h0),
    .R0_en   (1'h1),
    .R0_clk  (clock),
    .R0_data (_ram_ext_R0_data),
    .W0_addr (wrap),
    .W0_en   (do_enq),
    .W0_clk  (clock),
    .W0_data (io_enq_bits)
  );
  assign io_enq_ready = ~full;
  assign io_deq_valid = io_enq_valid | ~empty;
  assign io_deq_bits = empty ? io_enq_bits : _ram_ext_R0_data;
endmodule

