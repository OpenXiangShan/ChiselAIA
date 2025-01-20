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

module TLXbar_1(
  input         clock,
  input         reset,
  output        auto_in_1_a_ready,
  input         auto_in_1_a_valid,
  input  [1:0]  auto_in_1_a_bits_size,
  input  [4:0]  auto_in_1_a_bits_source,
  input  [31:0] auto_in_1_a_bits_address,
  input  [7:0]  auto_in_1_a_bits_mask,
  input  [63:0] auto_in_1_a_bits_data,
  input         auto_in_1_d_ready,
  output        auto_in_1_d_valid,
  output [2:0]  auto_in_1_d_bits_opcode,
  output [1:0]  auto_in_1_d_bits_size,
  output [4:0]  auto_in_1_d_bits_source,
  output        auto_in_0_a_ready,
  input         auto_in_0_a_valid,
  input  [2:0]  auto_in_0_a_bits_opcode,
  input  [2:0]  auto_in_0_a_bits_param,
  input  [1:0]  auto_in_0_a_bits_size,
  input  [3:0]  auto_in_0_a_bits_source,
  input  [31:0] auto_in_0_a_bits_address,
  input  [7:0]  auto_in_0_a_bits_mask,
  input  [63:0] auto_in_0_a_bits_data,
  input         auto_in_0_a_bits_corrupt,
  input         auto_in_0_d_ready,
  output        auto_in_0_d_valid,
  output [2:0]  auto_in_0_d_bits_opcode,
  output [1:0]  auto_in_0_d_bits_size,
  output [3:0]  auto_in_0_d_bits_source,
  input         auto_out_3_a_ready,
  output        auto_out_3_a_valid,
  output [2:0]  auto_out_3_a_bits_opcode,
  output [2:0]  auto_out_3_a_bits_param,
  output [1:0]  auto_out_3_a_bits_size,
  output [5:0]  auto_out_3_a_bits_source,
  output [31:0] auto_out_3_a_bits_address,
  output [7:0]  auto_out_3_a_bits_mask,
  output [63:0] auto_out_3_a_bits_data,
  output        auto_out_3_a_bits_corrupt,
  output        auto_out_3_d_ready,
  input         auto_out_3_d_valid,
  input  [2:0]  auto_out_3_d_bits_opcode,
  input  [1:0]  auto_out_3_d_bits_size,
  input  [5:0]  auto_out_3_d_bits_source,
  input         auto_out_2_a_ready,
  output        auto_out_2_a_valid,
  output [2:0]  auto_out_2_a_bits_opcode,
  output [2:0]  auto_out_2_a_bits_param,
  output [1:0]  auto_out_2_a_bits_size,
  output [5:0]  auto_out_2_a_bits_source,
  output [31:0] auto_out_2_a_bits_address,
  output [7:0]  auto_out_2_a_bits_mask,
  output [63:0] auto_out_2_a_bits_data,
  output        auto_out_2_a_bits_corrupt,
  output        auto_out_2_d_ready,
  input         auto_out_2_d_valid,
  input  [2:0]  auto_out_2_d_bits_opcode,
  input  [1:0]  auto_out_2_d_bits_size,
  input  [5:0]  auto_out_2_d_bits_source,
  input         auto_out_1_a_ready,
  output        auto_out_1_a_valid,
  output [2:0]  auto_out_1_a_bits_opcode,
  output [2:0]  auto_out_1_a_bits_param,
  output [1:0]  auto_out_1_a_bits_size,
  output [5:0]  auto_out_1_a_bits_source,
  output [31:0] auto_out_1_a_bits_address,
  output [7:0]  auto_out_1_a_bits_mask,
  output [63:0] auto_out_1_a_bits_data,
  output        auto_out_1_a_bits_corrupt,
  output        auto_out_1_d_ready,
  input         auto_out_1_d_valid,
  input  [2:0]  auto_out_1_d_bits_opcode,
  input  [1:0]  auto_out_1_d_bits_size,
  input  [5:0]  auto_out_1_d_bits_source,
  input         auto_out_0_a_ready,
  output        auto_out_0_a_valid,
  output [2:0]  auto_out_0_a_bits_opcode,
  output [2:0]  auto_out_0_a_bits_param,
  output [1:0]  auto_out_0_a_bits_size,
  output [5:0]  auto_out_0_a_bits_source,
  output [31:0] auto_out_0_a_bits_address,
  output [7:0]  auto_out_0_a_bits_mask,
  output [63:0] auto_out_0_a_bits_data,
  output        auto_out_0_a_bits_corrupt,
  output        auto_out_0_d_ready,
  input         auto_out_0_d_valid,
  input  [2:0]  auto_out_0_d_bits_opcode,
  input  [1:0]  auto_out_0_d_bits_size,
  input  [5:0]  auto_out_0_d_bits_source
);

  wire        allowed_5_3;
  wire        allowed_5_2;
  wire        allowed_5_1;
  wire        allowed_5_0;
  wire        allowed_4_3;
  wire        allowed_4_2;
  wire        allowed_4_1;
  wire        allowed_4_0;
  wire        allowed_3_1;
  wire        allowed_3_0;
  wire        allowed_2_1;
  wire        allowed_2_0;
  wire        allowed_1_1;
  wire        allowed_1_0;
  wire        allowed_1;
  wire        allowed_0;
  wire [5:0]  in_0_a_bits_source = {2'h2, auto_in_0_a_bits_source};
  wire [5:0]  in_1_a_bits_source = {1'h0, auto_in_1_a_bits_source};
  wire        requestAIO_0_0 =
    {auto_in_0_a_bits_address[31],
     auto_in_0_a_bits_address[16:15],
     auto_in_0_a_bits_address[12]} == 4'h0
    | {~(auto_in_0_a_bits_address[31]), auto_in_0_a_bits_address[16:15]} == 3'h0;
  wire [16:0] _requestAIO_T_16 = auto_in_0_a_bits_address[31:15] ^ 17'h10001;
  wire        requestAIO_0_1 =
    {auto_in_0_a_bits_address[31],
     auto_in_0_a_bits_address[16:15],
     ~(auto_in_0_a_bits_address[12])} == 4'h0
    | {_requestAIO_T_16[16], _requestAIO_T_16[1:0]} == 3'h0;
  wire [16:0] _requestAIO_T_27 = auto_in_0_a_bits_address[31:15] ^ 17'h10002;
  wire        requestAIO_0_2 =
    {auto_in_0_a_bits_address[31],
     auto_in_0_a_bits_address[16:15] ^ 2'h2,
     auto_in_0_a_bits_address[12]} == 4'h0
    | {_requestAIO_T_27[16], _requestAIO_T_27[1:0]} == 3'h0;
  wire [4:0]  _GEN = auto_in_0_a_bits_address[16:12] ^ 5'h11;
  wire [16:0] _requestAIO_T_38 = auto_in_0_a_bits_address[31:15] ^ 17'h10003;
  wire        requestAIO_0_3 =
    {auto_in_0_a_bits_address[31], _GEN[4:3], _GEN[0]} == 4'h0
    | {_requestAIO_T_38[16], _requestAIO_T_38[1:0]} == 3'h0;
  wire        requestAIO_1_0 =
    {auto_in_1_a_bits_address[31],
     auto_in_1_a_bits_address[16:15],
     auto_in_1_a_bits_address[12]} == 4'h0
    | {~(auto_in_1_a_bits_address[31]), auto_in_1_a_bits_address[16:15]} == 3'h0;
  wire [16:0] _requestAIO_T_60 = auto_in_1_a_bits_address[31:15] ^ 17'h10001;
  wire        requestAIO_1_1 =
    {auto_in_1_a_bits_address[31],
     auto_in_1_a_bits_address[16:15],
     ~(auto_in_1_a_bits_address[12])} == 4'h0
    | {_requestAIO_T_60[16], _requestAIO_T_60[1:0]} == 3'h0;
  wire [16:0] _requestAIO_T_71 = auto_in_1_a_bits_address[31:15] ^ 17'h10002;
  wire        requestAIO_1_2 =
    {auto_in_1_a_bits_address[31],
     auto_in_1_a_bits_address[16:15] ^ 2'h2,
     auto_in_1_a_bits_address[12]} == 4'h0
    | {_requestAIO_T_71[16], _requestAIO_T_71[1:0]} == 3'h0;
  wire [4:0]  _GEN_0 = auto_in_1_a_bits_address[16:12] ^ 5'h11;
  wire [16:0] _requestAIO_T_82 = auto_in_1_a_bits_address[31:15] ^ 17'h10003;
  wire        requestAIO_1_3 =
    {auto_in_1_a_bits_address[31], _GEN_0[4:3], _GEN_0[0]} == 4'h0
    | {_requestAIO_T_82[16], _requestAIO_T_82[1:0]} == 3'h0;
  wire        requestDOI_0_0 = auto_out_0_d_bits_source[5:4] == 2'h2;
  wire        requestDOI_1_0 = auto_out_1_d_bits_source[5:4] == 2'h2;
  wire        requestDOI_2_0 = auto_out_2_d_bits_source[5:4] == 2'h2;
  wire        requestDOI_3_0 = auto_out_3_d_bits_source[5:4] == 2'h2;
  wire        portsAOI_filtered_0_valid = auto_in_0_a_valid & requestAIO_0_0;
  wire        portsAOI_filtered_1_valid = auto_in_0_a_valid & requestAIO_0_1;
  wire        portsAOI_filtered_2_valid = auto_in_0_a_valid & requestAIO_0_2;
  wire        portsAOI_filtered_3_valid = auto_in_0_a_valid & requestAIO_0_3;
  wire        _portsAOI_in_0_a_ready_T_6 =
    requestAIO_0_0 & auto_out_0_a_ready & allowed_0 | requestAIO_0_1 & auto_out_1_a_ready
    & allowed_1_0 | requestAIO_0_2 & auto_out_2_a_ready & allowed_2_0 | requestAIO_0_3
    & auto_out_3_a_ready & allowed_3_0;
  wire        portsAOI_filtered_1_0_valid = auto_in_1_a_valid & requestAIO_1_0;
  wire        portsAOI_filtered_1_1_valid = auto_in_1_a_valid & requestAIO_1_1;
  wire        portsAOI_filtered_1_2_valid = auto_in_1_a_valid & requestAIO_1_2;
  wire        portsAOI_filtered_1_3_valid = auto_in_1_a_valid & requestAIO_1_3;
  wire        _portsAOI_in_1_a_ready_T_6 =
    requestAIO_1_0 & auto_out_0_a_ready & allowed_1 | requestAIO_1_1 & auto_out_1_a_ready
    & allowed_1_1 | requestAIO_1_2 & auto_out_2_a_ready & allowed_2_1 | requestAIO_1_3
    & auto_out_3_a_ready & allowed_3_1;
  wire        portsDIO_filtered_0_valid = auto_out_0_d_valid & requestDOI_0_0;
  wire        portsDIO_filtered_1_valid =
    auto_out_0_d_valid & ~(auto_out_0_d_bits_source[5]);
  wire        portsDIO_filtered_1_0_valid = auto_out_1_d_valid & requestDOI_1_0;
  wire        portsDIO_filtered_1_1_valid =
    auto_out_1_d_valid & ~(auto_out_1_d_bits_source[5]);
  wire        portsDIO_filtered_2_0_valid = auto_out_2_d_valid & requestDOI_2_0;
  wire        portsDIO_filtered_2_1_valid =
    auto_out_2_d_valid & ~(auto_out_2_d_bits_source[5]);
  wire        portsDIO_filtered_3_0_valid = auto_out_3_d_valid & requestDOI_3_0;
  wire        portsDIO_filtered_3_1_valid =
    auto_out_3_d_valid & ~(auto_out_3_d_bits_source[5]);
  reg         beatsLeft;
  wire [1:0]  readys_valid = {portsAOI_filtered_1_0_valid, portsAOI_filtered_0_valid};
  reg  [1:0]  readys_mask;
  wire [1:0]  _readys_filter_T_1 = readys_valid & ~readys_mask;
  wire [1:0]  readys_readys =
    ~({readys_mask[1], _readys_filter_T_1[1] | readys_mask[0]}
      & ({_readys_filter_T_1[0], portsAOI_filtered_1_0_valid} | _readys_filter_T_1));
  wire        winner_0 = readys_readys[0] & portsAOI_filtered_0_valid;
  wire        winner_1 = readys_readys[1] & portsAOI_filtered_1_0_valid;
  wire        _out_0_a_valid_T = portsAOI_filtered_0_valid | portsAOI_filtered_1_0_valid;
  reg         state_0;
  reg         state_1;
  wire        muxState_0 = beatsLeft ? state_0 : winner_0;
  wire        muxState_1 = beatsLeft ? state_1 : winner_1;
  assign allowed_0 = beatsLeft ? state_0 : readys_readys[0];
  assign allowed_1 = beatsLeft ? state_1 : readys_readys[1];
  wire        out_0_a_valid =
    beatsLeft
      ? state_0 & portsAOI_filtered_0_valid | state_1 & portsAOI_filtered_1_0_valid
      : _out_0_a_valid_T;
  reg         beatsLeft_1;
  wire [1:0]  readys_valid_1 = {portsAOI_filtered_1_1_valid, portsAOI_filtered_1_valid};
  reg  [1:0]  readys_mask_1;
  wire [1:0]  _readys_filter_T_3 = readys_valid_1 & ~readys_mask_1;
  wire [1:0]  readys_readys_1 =
    ~({readys_mask_1[1], _readys_filter_T_3[1] | readys_mask_1[0]}
      & ({_readys_filter_T_3[0], portsAOI_filtered_1_1_valid} | _readys_filter_T_3));
  wire        winner_1_0 = readys_readys_1[0] & portsAOI_filtered_1_valid;
  wire        winner_1_1 = readys_readys_1[1] & portsAOI_filtered_1_1_valid;
  wire        _out_1_a_valid_T = portsAOI_filtered_1_valid | portsAOI_filtered_1_1_valid;
  reg         state_1_0;
  reg         state_1_1;
  wire        muxState_1_0 = beatsLeft_1 ? state_1_0 : winner_1_0;
  wire        muxState_1_1 = beatsLeft_1 ? state_1_1 : winner_1_1;
  assign allowed_1_0 = beatsLeft_1 ? state_1_0 : readys_readys_1[0];
  assign allowed_1_1 = beatsLeft_1 ? state_1_1 : readys_readys_1[1];
  wire        out_1_a_valid =
    beatsLeft_1
      ? state_1_0 & portsAOI_filtered_1_valid | state_1_1 & portsAOI_filtered_1_1_valid
      : _out_1_a_valid_T;
  reg         beatsLeft_2;
  wire [1:0]  readys_valid_2 = {portsAOI_filtered_1_2_valid, portsAOI_filtered_2_valid};
  reg  [1:0]  readys_mask_2;
  wire [1:0]  _readys_filter_T_5 = readys_valid_2 & ~readys_mask_2;
  wire [1:0]  readys_readys_2 =
    ~({readys_mask_2[1], _readys_filter_T_5[1] | readys_mask_2[0]}
      & ({_readys_filter_T_5[0], portsAOI_filtered_1_2_valid} | _readys_filter_T_5));
  wire        winner_2_0 = readys_readys_2[0] & portsAOI_filtered_2_valid;
  wire        winner_2_1 = readys_readys_2[1] & portsAOI_filtered_1_2_valid;
  wire        _out_2_a_valid_T = portsAOI_filtered_2_valid | portsAOI_filtered_1_2_valid;
  reg         state_2_0;
  reg         state_2_1;
  wire        muxState_2_0 = beatsLeft_2 ? state_2_0 : winner_2_0;
  wire        muxState_2_1 = beatsLeft_2 ? state_2_1 : winner_2_1;
  assign allowed_2_0 = beatsLeft_2 ? state_2_0 : readys_readys_2[0];
  assign allowed_2_1 = beatsLeft_2 ? state_2_1 : readys_readys_2[1];
  wire        out_2_a_valid =
    beatsLeft_2
      ? state_2_0 & portsAOI_filtered_2_valid | state_2_1 & portsAOI_filtered_1_2_valid
      : _out_2_a_valid_T;
  reg         beatsLeft_3;
  wire [1:0]  readys_valid_3 = {portsAOI_filtered_1_3_valid, portsAOI_filtered_3_valid};
  reg  [1:0]  readys_mask_3;
  wire [1:0]  _readys_filter_T_7 = readys_valid_3 & ~readys_mask_3;
  wire [1:0]  readys_readys_3 =
    ~({readys_mask_3[1], _readys_filter_T_7[1] | readys_mask_3[0]}
      & ({_readys_filter_T_7[0], portsAOI_filtered_1_3_valid} | _readys_filter_T_7));
  wire        winner_3_0 = readys_readys_3[0] & portsAOI_filtered_3_valid;
  wire        winner_3_1 = readys_readys_3[1] & portsAOI_filtered_1_3_valid;
  wire        _out_3_a_valid_T = portsAOI_filtered_3_valid | portsAOI_filtered_1_3_valid;
  reg         state_3_0;
  reg         state_3_1;
  wire        muxState_3_0 = beatsLeft_3 ? state_3_0 : winner_3_0;
  wire        muxState_3_1 = beatsLeft_3 ? state_3_1 : winner_3_1;
  assign allowed_3_0 = beatsLeft_3 ? state_3_0 : readys_readys_3[0];
  assign allowed_3_1 = beatsLeft_3 ? state_3_1 : readys_readys_3[1];
  wire        out_3_a_valid =
    beatsLeft_3
      ? state_3_0 & portsAOI_filtered_3_valid | state_3_1 & portsAOI_filtered_1_3_valid
      : _out_3_a_valid_T;
  reg         beatsLeft_4;
  wire [3:0]  readys_valid_4 =
    {portsDIO_filtered_3_0_valid,
     portsDIO_filtered_2_0_valid,
     portsDIO_filtered_1_0_valid,
     portsDIO_filtered_0_valid};
  reg  [3:0]  readys_mask_4;
  wire [3:0]  _readys_filter_T_9 = readys_valid_4 & ~readys_mask_4;
  wire [5:0]  _GEN_1 =
    {_readys_filter_T_9[2:0],
     portsDIO_filtered_3_0_valid,
     portsDIO_filtered_2_0_valid,
     portsDIO_filtered_1_0_valid}
    | {_readys_filter_T_9, portsDIO_filtered_3_0_valid, portsDIO_filtered_2_0_valid};
  wire [4:0]  _GEN_2 = _GEN_1[4:0] | {_readys_filter_T_9[3], _GEN_1[5:2]};
  wire [3:0]  readys_readys_4 =
    ~({readys_mask_4[3],
       _readys_filter_T_9[3] | readys_mask_4[2],
       _GEN_1[5] | readys_mask_4[1],
       _GEN_2[4] | readys_mask_4[0]} & _GEN_2[3:0]);
  wire        winner_4_0 = readys_readys_4[0] & portsDIO_filtered_0_valid;
  wire        winner_4_1 = readys_readys_4[1] & portsDIO_filtered_1_0_valid;
  wire        winner_4_2 = readys_readys_4[2] & portsDIO_filtered_2_0_valid;
  wire        winner_4_3 = readys_readys_4[3] & portsDIO_filtered_3_0_valid;
  wire        _in_0_d_valid_T = portsDIO_filtered_0_valid | portsDIO_filtered_1_0_valid;
  reg         state_4_0;
  reg         state_4_1;
  reg         state_4_2;
  reg         state_4_3;
  wire        muxState_4_0 = beatsLeft_4 ? state_4_0 : winner_4_0;
  wire        muxState_4_1 = beatsLeft_4 ? state_4_1 : winner_4_1;
  wire        muxState_4_2 = beatsLeft_4 ? state_4_2 : winner_4_2;
  wire        muxState_4_3 = beatsLeft_4 ? state_4_3 : winner_4_3;
  assign allowed_4_0 = beatsLeft_4 ? state_4_0 : readys_readys_4[0];
  assign allowed_4_1 = beatsLeft_4 ? state_4_1 : readys_readys_4[1];
  assign allowed_4_2 = beatsLeft_4 ? state_4_2 : readys_readys_4[2];
  assign allowed_4_3 = beatsLeft_4 ? state_4_3 : readys_readys_4[3];
  wire        in_0_d_valid =
    beatsLeft_4
      ? state_4_0 & portsDIO_filtered_0_valid | state_4_1 & portsDIO_filtered_1_0_valid
        | state_4_2 & portsDIO_filtered_2_0_valid | state_4_3
        & portsDIO_filtered_3_0_valid
      : _in_0_d_valid_T | portsDIO_filtered_2_0_valid | portsDIO_filtered_3_0_valid;
  wire [3:0]  _in_0_d_bits_T_34 =
    (muxState_4_0 ? auto_out_0_d_bits_source[3:0] : 4'h0)
    | (muxState_4_1 ? auto_out_1_d_bits_source[3:0] : 4'h0)
    | (muxState_4_2 ? auto_out_2_d_bits_source[3:0] : 4'h0)
    | (muxState_4_3 ? auto_out_3_d_bits_source[3:0] : 4'h0);
  wire [1:0]  _in_0_d_bits_T_41 =
    (muxState_4_0 ? auto_out_0_d_bits_size : 2'h0)
    | (muxState_4_1 ? auto_out_1_d_bits_size : 2'h0)
    | (muxState_4_2 ? auto_out_2_d_bits_size : 2'h0)
    | (muxState_4_3 ? auto_out_3_d_bits_size : 2'h0);
  wire [2:0]  _in_0_d_bits_T_55 =
    (muxState_4_0 ? auto_out_0_d_bits_opcode : 3'h0)
    | (muxState_4_1 ? auto_out_1_d_bits_opcode : 3'h0)
    | (muxState_4_2 ? auto_out_2_d_bits_opcode : 3'h0)
    | (muxState_4_3 ? auto_out_3_d_bits_opcode : 3'h0);
  reg         beatsLeft_5;
  wire [3:0]  readys_valid_5 =
    {portsDIO_filtered_3_1_valid,
     portsDIO_filtered_2_1_valid,
     portsDIO_filtered_1_1_valid,
     portsDIO_filtered_1_valid};
  reg  [3:0]  readys_mask_5;
  wire [3:0]  _readys_filter_T_11 = readys_valid_5 & ~readys_mask_5;
  wire [5:0]  _GEN_3 =
    {_readys_filter_T_11[2:0],
     portsDIO_filtered_3_1_valid,
     portsDIO_filtered_2_1_valid,
     portsDIO_filtered_1_1_valid}
    | {_readys_filter_T_11, portsDIO_filtered_3_1_valid, portsDIO_filtered_2_1_valid};
  wire [4:0]  _GEN_4 = _GEN_3[4:0] | {_readys_filter_T_11[3], _GEN_3[5:2]};
  wire [3:0]  readys_readys_5 =
    ~({readys_mask_5[3],
       _readys_filter_T_11[3] | readys_mask_5[2],
       _GEN_3[5] | readys_mask_5[1],
       _GEN_4[4] | readys_mask_5[0]} & _GEN_4[3:0]);
  wire        winner_5_0 = readys_readys_5[0] & portsDIO_filtered_1_valid;
  wire        winner_5_1 = readys_readys_5[1] & portsDIO_filtered_1_1_valid;
  wire        winner_5_2 = readys_readys_5[2] & portsDIO_filtered_2_1_valid;
  wire        winner_5_3 = readys_readys_5[3] & portsDIO_filtered_3_1_valid;
  wire        _in_1_d_valid_T = portsDIO_filtered_1_valid | portsDIO_filtered_1_1_valid;
  `ifndef SYNTHESIS
    wire prefixOR_2 = winner_4_0 | winner_4_1;
    wire prefixOR_2_1 = winner_5_0 | winner_5_1;
    always @(posedge clock) begin
      if (~reset & ~(~winner_0 | ~winner_1)) begin
        if (`ASSERT_VERBOSE_COND_)
          $error("Assertion failed\n    at Arbiter.scala:77 assert((prefixOR zip winner) map { case (p,w) => !p || !w } reduce {_ && _})\n");
        if (`STOP_COND_)
          $fatal;
      end
      if (~reset & ~(~_out_0_a_valid_T | winner_0 | winner_1)) begin
        if (`ASSERT_VERBOSE_COND_)
          $error("Assertion failed\n    at Arbiter.scala:79 assert (!valids.reduce(_||_) || winner.reduce(_||_))\n");
        if (`STOP_COND_)
          $fatal;
      end
      if (~reset & ~(~winner_1_0 | ~winner_1_1)) begin
        if (`ASSERT_VERBOSE_COND_)
          $error("Assertion failed\n    at Arbiter.scala:77 assert((prefixOR zip winner) map { case (p,w) => !p || !w } reduce {_ && _})\n");
        if (`STOP_COND_)
          $fatal;
      end
      if (~reset & ~(~_out_1_a_valid_T | winner_1_0 | winner_1_1)) begin
        if (`ASSERT_VERBOSE_COND_)
          $error("Assertion failed\n    at Arbiter.scala:79 assert (!valids.reduce(_||_) || winner.reduce(_||_))\n");
        if (`STOP_COND_)
          $fatal;
      end
      if (~reset & ~(~winner_2_0 | ~winner_2_1)) begin
        if (`ASSERT_VERBOSE_COND_)
          $error("Assertion failed\n    at Arbiter.scala:77 assert((prefixOR zip winner) map { case (p,w) => !p || !w } reduce {_ && _})\n");
        if (`STOP_COND_)
          $fatal;
      end
      if (~reset & ~(~_out_2_a_valid_T | winner_2_0 | winner_2_1)) begin
        if (`ASSERT_VERBOSE_COND_)
          $error("Assertion failed\n    at Arbiter.scala:79 assert (!valids.reduce(_||_) || winner.reduce(_||_))\n");
        if (`STOP_COND_)
          $fatal;
      end
      if (~reset & ~(~winner_3_0 | ~winner_3_1)) begin
        if (`ASSERT_VERBOSE_COND_)
          $error("Assertion failed\n    at Arbiter.scala:77 assert((prefixOR zip winner) map { case (p,w) => !p || !w } reduce {_ && _})\n");
        if (`STOP_COND_)
          $fatal;
      end
      if (~reset & ~(~_out_3_a_valid_T | winner_3_0 | winner_3_1)) begin
        if (`ASSERT_VERBOSE_COND_)
          $error("Assertion failed\n    at Arbiter.scala:79 assert (!valids.reduce(_||_) || winner.reduce(_||_))\n");
        if (`STOP_COND_)
          $fatal;
      end
      if (~reset
          & ~((~winner_4_0 | ~winner_4_1) & (~prefixOR_2 | ~winner_4_2)
              & (~(prefixOR_2 | winner_4_2) | ~winner_4_3))) begin
        if (`ASSERT_VERBOSE_COND_)
          $error("Assertion failed\n    at Arbiter.scala:77 assert((prefixOR zip winner) map { case (p,w) => !p || !w } reduce {_ && _})\n");
        if (`STOP_COND_)
          $fatal;
      end
      if (~reset
          & ~(~(_in_0_d_valid_T | portsDIO_filtered_2_0_valid
                | portsDIO_filtered_3_0_valid) | winner_4_0 | winner_4_1 | winner_4_2
              | winner_4_3)) begin
        if (`ASSERT_VERBOSE_COND_)
          $error("Assertion failed\n    at Arbiter.scala:79 assert (!valids.reduce(_||_) || winner.reduce(_||_))\n");
        if (`STOP_COND_)
          $fatal;
      end
      if (~reset
          & ~((~winner_5_0 | ~winner_5_1) & (~prefixOR_2_1 | ~winner_5_2)
              & (~(prefixOR_2_1 | winner_5_2) | ~winner_5_3))) begin
        if (`ASSERT_VERBOSE_COND_)
          $error("Assertion failed\n    at Arbiter.scala:77 assert((prefixOR zip winner) map { case (p,w) => !p || !w } reduce {_ && _})\n");
        if (`STOP_COND_)
          $fatal;
      end
      if (~reset
          & ~(~(_in_1_d_valid_T | portsDIO_filtered_2_1_valid
                | portsDIO_filtered_3_1_valid) | winner_5_0 | winner_5_1 | winner_5_2
              | winner_5_3)) begin
        if (`ASSERT_VERBOSE_COND_)
          $error("Assertion failed\n    at Arbiter.scala:79 assert (!valids.reduce(_||_) || winner.reduce(_||_))\n");
        if (`STOP_COND_)
          $fatal;
      end
    end // always @(posedge)
  `endif // not def SYNTHESIS
  reg         state_5_0;
  reg         state_5_1;
  reg         state_5_2;
  reg         state_5_3;
  wire        muxState_5_0 = beatsLeft_5 ? state_5_0 : winner_5_0;
  wire        muxState_5_1 = beatsLeft_5 ? state_5_1 : winner_5_1;
  wire        muxState_5_2 = beatsLeft_5 ? state_5_2 : winner_5_2;
  wire        muxState_5_3 = beatsLeft_5 ? state_5_3 : winner_5_3;
  assign allowed_5_0 = beatsLeft_5 ? state_5_0 : readys_readys_5[0];
  assign allowed_5_1 = beatsLeft_5 ? state_5_1 : readys_readys_5[1];
  assign allowed_5_2 = beatsLeft_5 ? state_5_2 : readys_readys_5[2];
  assign allowed_5_3 = beatsLeft_5 ? state_5_3 : readys_readys_5[3];
  wire        in_1_d_valid =
    beatsLeft_5
      ? state_5_0 & portsDIO_filtered_1_valid | state_5_1 & portsDIO_filtered_1_1_valid
        | state_5_2 & portsDIO_filtered_2_1_valid | state_5_3
        & portsDIO_filtered_3_1_valid
      : _in_1_d_valid_T | portsDIO_filtered_2_1_valid | portsDIO_filtered_3_1_valid;
  wire [4:0]  _in_1_d_bits_T_34 =
    (muxState_5_0 ? auto_out_0_d_bits_source[4:0] : 5'h0)
    | (muxState_5_1 ? auto_out_1_d_bits_source[4:0] : 5'h0)
    | (muxState_5_2 ? auto_out_2_d_bits_source[4:0] : 5'h0)
    | (muxState_5_3 ? auto_out_3_d_bits_source[4:0] : 5'h0);
  wire [1:0]  _in_1_d_bits_T_41 =
    (muxState_5_0 ? auto_out_0_d_bits_size : 2'h0)
    | (muxState_5_1 ? auto_out_1_d_bits_size : 2'h0)
    | (muxState_5_2 ? auto_out_2_d_bits_size : 2'h0)
    | (muxState_5_3 ? auto_out_3_d_bits_size : 2'h0);
  wire [2:0]  _in_1_d_bits_T_55 =
    (muxState_5_0 ? auto_out_0_d_bits_opcode : 3'h0)
    | (muxState_5_1 ? auto_out_1_d_bits_opcode : 3'h0)
    | (muxState_5_2 ? auto_out_2_d_bits_opcode : 3'h0)
    | (muxState_5_3 ? auto_out_3_d_bits_opcode : 3'h0);
  wire [1:0]  _readys_mask_T = readys_readys & readys_valid;
  wire [1:0]  _readys_mask_T_5 = readys_readys_1 & readys_valid_1;
  wire [1:0]  _readys_mask_T_10 = readys_readys_2 & readys_valid_2;
  wire [1:0]  _readys_mask_T_15 = readys_readys_3 & readys_valid_3;
  wire [3:0]  _readys_mask_T_20 = readys_readys_4 & readys_valid_4;
  wire [3:0]  _readys_mask_T_23 = _readys_mask_T_20 | {_readys_mask_T_20[2:0], 1'h0};
  wire [3:0]  _readys_mask_T_28 = readys_readys_5 & readys_valid_5;
  wire [3:0]  _readys_mask_T_31 = _readys_mask_T_28 | {_readys_mask_T_28[2:0], 1'h0};
  wire        latch = ~beatsLeft & auto_out_0_a_ready;
  wire        latch_1 = ~beatsLeft_1 & auto_out_1_a_ready;
  wire        latch_2 = ~beatsLeft_2 & auto_out_2_a_ready;
  wire        latch_3 = ~beatsLeft_3 & auto_out_3_a_ready;
  wire        latch_4 = ~beatsLeft_4 & auto_in_0_d_ready;
  wire        latch_5 = ~beatsLeft_5 & auto_in_1_d_ready;
  always @(posedge clock) begin
    if (reset) begin
      beatsLeft <= 1'h0;
      readys_mask <= 2'h3;
      state_0 <= 1'h0;
      state_1 <= 1'h0;
      beatsLeft_1 <= 1'h0;
      readys_mask_1 <= 2'h3;
      state_1_0 <= 1'h0;
      state_1_1 <= 1'h0;
      beatsLeft_2 <= 1'h0;
      readys_mask_2 <= 2'h3;
      state_2_0 <= 1'h0;
      state_2_1 <= 1'h0;
      beatsLeft_3 <= 1'h0;
      readys_mask_3 <= 2'h3;
      state_3_0 <= 1'h0;
      state_3_1 <= 1'h0;
      beatsLeft_4 <= 1'h0;
      readys_mask_4 <= 4'hF;
      state_4_0 <= 1'h0;
      state_4_1 <= 1'h0;
      state_4_2 <= 1'h0;
      state_4_3 <= 1'h0;
      beatsLeft_5 <= 1'h0;
      readys_mask_5 <= 4'hF;
      state_5_0 <= 1'h0;
      state_5_1 <= 1'h0;
      state_5_2 <= 1'h0;
      state_5_3 <= 1'h0;
    end
    else begin
      beatsLeft <= ~latch & 1'(beatsLeft - (auto_out_0_a_ready & out_0_a_valid));
      if (latch & (|readys_valid))
        readys_mask <= _readys_mask_T | {_readys_mask_T[0], 1'h0};
      if (beatsLeft) begin
      end
      else begin
        state_0 <= winner_0;
        state_1 <= winner_1;
      end
      beatsLeft_1 <= ~latch_1 & 1'(beatsLeft_1 - (auto_out_1_a_ready & out_1_a_valid));
      if (latch_1 & (|readys_valid_1))
        readys_mask_1 <= _readys_mask_T_5 | {_readys_mask_T_5[0], 1'h0};
      if (beatsLeft_1) begin
      end
      else begin
        state_1_0 <= winner_1_0;
        state_1_1 <= winner_1_1;
      end
      beatsLeft_2 <= ~latch_2 & 1'(beatsLeft_2 - (auto_out_2_a_ready & out_2_a_valid));
      if (latch_2 & (|readys_valid_2))
        readys_mask_2 <= _readys_mask_T_10 | {_readys_mask_T_10[0], 1'h0};
      if (beatsLeft_2) begin
      end
      else begin
        state_2_0 <= winner_2_0;
        state_2_1 <= winner_2_1;
      end
      beatsLeft_3 <= ~latch_3 & 1'(beatsLeft_3 - (auto_out_3_a_ready & out_3_a_valid));
      if (latch_3 & (|readys_valid_3))
        readys_mask_3 <= _readys_mask_T_15 | {_readys_mask_T_15[0], 1'h0};
      if (beatsLeft_3) begin
      end
      else begin
        state_3_0 <= winner_3_0;
        state_3_1 <= winner_3_1;
      end
      beatsLeft_4 <= ~latch_4 & 1'(beatsLeft_4 - (auto_in_0_d_ready & in_0_d_valid));
      if (latch_4 & (|readys_valid_4))
        readys_mask_4 <= _readys_mask_T_23 | {_readys_mask_T_23[1:0], 2'h0};
      if (beatsLeft_4) begin
      end
      else begin
        state_4_0 <= winner_4_0;
        state_4_1 <= winner_4_1;
        state_4_2 <= winner_4_2;
        state_4_3 <= winner_4_3;
      end
      beatsLeft_5 <= ~latch_5 & 1'(beatsLeft_5 - (auto_in_1_d_ready & in_1_d_valid));
      if (latch_5 & (|readys_valid_5))
        readys_mask_5 <= _readys_mask_T_31 | {_readys_mask_T_31[1:0], 2'h0};
      if (beatsLeft_5) begin
      end
      else begin
        state_5_0 <= winner_5_0;
        state_5_1 <= winner_5_1;
        state_5_2 <= winner_5_2;
        state_5_3 <= winner_5_3;
      end
    end
  end // always @(posedge)
  TLMonitor_1 monitor (
    .clock                (clock),
    .reset                (reset),
    .io_in_a_ready        (_portsAOI_in_0_a_ready_T_6),
    .io_in_a_valid        (auto_in_0_a_valid),
    .io_in_a_bits_opcode  (auto_in_0_a_bits_opcode),
    .io_in_a_bits_param   (auto_in_0_a_bits_param),
    .io_in_a_bits_size    (auto_in_0_a_bits_size),
    .io_in_a_bits_source  (auto_in_0_a_bits_source),
    .io_in_a_bits_address (auto_in_0_a_bits_address),
    .io_in_a_bits_mask    (auto_in_0_a_bits_mask),
    .io_in_a_bits_corrupt (auto_in_0_a_bits_corrupt),
    .io_in_d_ready        (auto_in_0_d_ready),
    .io_in_d_valid        (in_0_d_valid),
    .io_in_d_bits_opcode  (_in_0_d_bits_T_55),
    .io_in_d_bits_size    (_in_0_d_bits_T_41),
    .io_in_d_bits_source  (_in_0_d_bits_T_34)
  );
  TLMonitor_2 monitor_1 (
    .clock                (clock),
    .reset                (reset),
    .io_in_a_ready        (_portsAOI_in_1_a_ready_T_6),
    .io_in_a_valid        (auto_in_1_a_valid),
    .io_in_a_bits_size    (auto_in_1_a_bits_size),
    .io_in_a_bits_source  (auto_in_1_a_bits_source),
    .io_in_a_bits_address (auto_in_1_a_bits_address),
    .io_in_a_bits_mask    (auto_in_1_a_bits_mask),
    .io_in_d_ready        (auto_in_1_d_ready),
    .io_in_d_valid        (in_1_d_valid),
    .io_in_d_bits_opcode  (_in_1_d_bits_T_55),
    .io_in_d_bits_size    (_in_1_d_bits_T_41),
    .io_in_d_bits_source  (_in_1_d_bits_T_34)
  );
  assign auto_in_1_a_ready = _portsAOI_in_1_a_ready_T_6;
  assign auto_in_1_d_valid = in_1_d_valid;
  assign auto_in_1_d_bits_opcode = _in_1_d_bits_T_55;
  assign auto_in_1_d_bits_size = _in_1_d_bits_T_41;
  assign auto_in_1_d_bits_source = _in_1_d_bits_T_34;
  assign auto_in_0_a_ready = _portsAOI_in_0_a_ready_T_6;
  assign auto_in_0_d_valid = in_0_d_valid;
  assign auto_in_0_d_bits_opcode = _in_0_d_bits_T_55;
  assign auto_in_0_d_bits_size = _in_0_d_bits_T_41;
  assign auto_in_0_d_bits_source = _in_0_d_bits_T_34;
  assign auto_out_3_a_valid = out_3_a_valid;
  assign auto_out_3_a_bits_opcode = muxState_3_0 ? auto_in_0_a_bits_opcode : 3'h0;
  assign auto_out_3_a_bits_param = muxState_3_0 ? auto_in_0_a_bits_param : 3'h0;
  assign auto_out_3_a_bits_size =
    (muxState_3_0 ? auto_in_0_a_bits_size : 2'h0)
    | (muxState_3_1 ? auto_in_1_a_bits_size : 2'h0);
  assign auto_out_3_a_bits_source =
    (muxState_3_0 ? in_0_a_bits_source : 6'h0)
    | (muxState_3_1 ? in_1_a_bits_source : 6'h0);
  assign auto_out_3_a_bits_address =
    (muxState_3_0 ? auto_in_0_a_bits_address : 32'h0)
    | (muxState_3_1 ? auto_in_1_a_bits_address : 32'h0);
  assign auto_out_3_a_bits_mask =
    (muxState_3_0 ? auto_in_0_a_bits_mask : 8'h0)
    | (muxState_3_1 ? auto_in_1_a_bits_mask : 8'h0);
  assign auto_out_3_a_bits_data =
    (muxState_3_0 ? auto_in_0_a_bits_data : 64'h0)
    | (muxState_3_1 ? auto_in_1_a_bits_data : 64'h0);
  assign auto_out_3_a_bits_corrupt = muxState_3_0 & auto_in_0_a_bits_corrupt;
  assign auto_out_3_d_ready =
    requestDOI_3_0 & auto_in_0_d_ready & allowed_4_3 | ~(auto_out_3_d_bits_source[5])
    & auto_in_1_d_ready & allowed_5_3;
  assign auto_out_2_a_valid = out_2_a_valid;
  assign auto_out_2_a_bits_opcode = muxState_2_0 ? auto_in_0_a_bits_opcode : 3'h0;
  assign auto_out_2_a_bits_param = muxState_2_0 ? auto_in_0_a_bits_param : 3'h0;
  assign auto_out_2_a_bits_size =
    (muxState_2_0 ? auto_in_0_a_bits_size : 2'h0)
    | (muxState_2_1 ? auto_in_1_a_bits_size : 2'h0);
  assign auto_out_2_a_bits_source =
    (muxState_2_0 ? in_0_a_bits_source : 6'h0)
    | (muxState_2_1 ? in_1_a_bits_source : 6'h0);
  assign auto_out_2_a_bits_address =
    (muxState_2_0 ? auto_in_0_a_bits_address : 32'h0)
    | (muxState_2_1 ? auto_in_1_a_bits_address : 32'h0);
  assign auto_out_2_a_bits_mask =
    (muxState_2_0 ? auto_in_0_a_bits_mask : 8'h0)
    | (muxState_2_1 ? auto_in_1_a_bits_mask : 8'h0);
  assign auto_out_2_a_bits_data =
    (muxState_2_0 ? auto_in_0_a_bits_data : 64'h0)
    | (muxState_2_1 ? auto_in_1_a_bits_data : 64'h0);
  assign auto_out_2_a_bits_corrupt = muxState_2_0 & auto_in_0_a_bits_corrupt;
  assign auto_out_2_d_ready =
    requestDOI_2_0 & auto_in_0_d_ready & allowed_4_2 | ~(auto_out_2_d_bits_source[5])
    & auto_in_1_d_ready & allowed_5_2;
  assign auto_out_1_a_valid = out_1_a_valid;
  assign auto_out_1_a_bits_opcode = muxState_1_0 ? auto_in_0_a_bits_opcode : 3'h0;
  assign auto_out_1_a_bits_param = muxState_1_0 ? auto_in_0_a_bits_param : 3'h0;
  assign auto_out_1_a_bits_size =
    (muxState_1_0 ? auto_in_0_a_bits_size : 2'h0)
    | (muxState_1_1 ? auto_in_1_a_bits_size : 2'h0);
  assign auto_out_1_a_bits_source =
    (muxState_1_0 ? in_0_a_bits_source : 6'h0)
    | (muxState_1_1 ? in_1_a_bits_source : 6'h0);
  assign auto_out_1_a_bits_address =
    (muxState_1_0 ? auto_in_0_a_bits_address : 32'h0)
    | (muxState_1_1 ? auto_in_1_a_bits_address : 32'h0);
  assign auto_out_1_a_bits_mask =
    (muxState_1_0 ? auto_in_0_a_bits_mask : 8'h0)
    | (muxState_1_1 ? auto_in_1_a_bits_mask : 8'h0);
  assign auto_out_1_a_bits_data =
    (muxState_1_0 ? auto_in_0_a_bits_data : 64'h0)
    | (muxState_1_1 ? auto_in_1_a_bits_data : 64'h0);
  assign auto_out_1_a_bits_corrupt = muxState_1_0 & auto_in_0_a_bits_corrupt;
  assign auto_out_1_d_ready =
    requestDOI_1_0 & auto_in_0_d_ready & allowed_4_1 | ~(auto_out_1_d_bits_source[5])
    & auto_in_1_d_ready & allowed_5_1;
  assign auto_out_0_a_valid = out_0_a_valid;
  assign auto_out_0_a_bits_opcode = muxState_0 ? auto_in_0_a_bits_opcode : 3'h0;
  assign auto_out_0_a_bits_param = muxState_0 ? auto_in_0_a_bits_param : 3'h0;
  assign auto_out_0_a_bits_size =
    (muxState_0 ? auto_in_0_a_bits_size : 2'h0)
    | (muxState_1 ? auto_in_1_a_bits_size : 2'h0);
  assign auto_out_0_a_bits_source =
    (muxState_0 ? in_0_a_bits_source : 6'h0) | (muxState_1 ? in_1_a_bits_source : 6'h0);
  assign auto_out_0_a_bits_address =
    (muxState_0 ? auto_in_0_a_bits_address : 32'h0)
    | (muxState_1 ? auto_in_1_a_bits_address : 32'h0);
  assign auto_out_0_a_bits_mask =
    (muxState_0 ? auto_in_0_a_bits_mask : 8'h0)
    | (muxState_1 ? auto_in_1_a_bits_mask : 8'h0);
  assign auto_out_0_a_bits_data =
    (muxState_0 ? auto_in_0_a_bits_data : 64'h0)
    | (muxState_1 ? auto_in_1_a_bits_data : 64'h0);
  assign auto_out_0_a_bits_corrupt = muxState_0 & auto_in_0_a_bits_corrupt;
  assign auto_out_0_d_ready =
    requestDOI_0_0 & auto_in_0_d_ready & allowed_4_0 | ~(auto_out_0_d_bits_source[5])
    & auto_in_1_d_ready & allowed_5_0;
endmodule
