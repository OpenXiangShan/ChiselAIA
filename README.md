# ChiselAIA

<!-- vim-markdown-toc GFM -->

* [简介（Introduction）](#简介introduction)
* [使用方法（Usage）](#使用方法usage)
* [相关工作（Related Works）](#相关工作related-works)

<!-- vim-markdown-toc -->

ChiselAIA是RISC-V高级中断架构(Advanced Interrupt Architecture, AIA)的开源Chisel实现。
现有的开源AIA实现主要是用Verilog编写的[相关工作](#相关工作related-works)。
ChiselAIA旨在将Chisel敏捷开发的方法应用于AIA的实现。

`ChiselAIA` is an open-sourced Chisel implementation of the RISC-V Advanced Interrupt Architecture (AIA).
Existing open-sourced AIA implementations are primarily written in Verilog (see [Related Works](#相关工作related-works)).
`ChiselAIA` aims to leverage Chisel's agile development methodology for AIA implementation.

## 简介（Introduction）

该实现包括:

* Incoming Message-Signaled Interrupt Controller (IMSIC): src/main/scala/IMSIC.scala
* Advanced Platform-Level Interrupt Controller (APLIC): src/main/scala/APLIC.scala
  * 支持消息信号中断(message-signaled interrupt, MSI)传递模式(domaincfg.DM=1)，
    该模式下，APLIC将线中断转换为MSI并发送给IMSIC
  * 暂不支持直接传递模式(domaincfg.DM=0)
* IMSIC和APLIC的单元测试: test/*/main.py

更多信息，请参阅[文档](https://openxiangshan-nanhu.github.io/ChiselAIA/)。

This implementation includes:

* Incoming Message-Signaled Interrupt Controller (IMSIC): `src/main/scala/IMSIC.scala`
* Advanced Platform-Level Interrupt Controller (APLIC): `src/main/scala/APLIC.scala`
  * Supports message-signaled interrupt (MSI) delivery mode (domaincfg.DM=1),
    where the APLIC converts wired interrupts to MSI and sends them to IMSIC
  * Direct delivery mode is currently not supported (domaincfg.DM=0)
* The unit tests for IMSIC and APLIC: `test/*/main.py`

For more detailed information, please refer to the [documentation](https://openxiangshan-nanhu.github.io/ChiselAIA/).

## 使用方法（Usage）

依赖均由`nix`管理。
如果你还没有安装`nix`，可以参考`nix`[官方文档进行安装](https://nixos.org/download/)。

Dependencies are managed by `nix`.
If you haven't installed `nix`, you can refer to [nix official installation](https://nixos.org/download/).

```bash
# 进入nix shell（推荐使用direnv自动进入nix shell）：
# Enter the nix shell (direnv is recommended for auto entering the nix shell):
nix-shell

# 生成Verilog并运行单元测试：
# Generate Verilog and run unit tests:
make -j

# 显示帮助信息：
# Display help information:
h
```

## 相关工作（Related Works）

* [OpenXiangShan/OpenAIA](https://github.com/OpenXiangShan/OpenAIA)
  * 采用Verilog（Implemented in Verilog）
  * 支持IMSIC（IMSIC supported）
  * 不支持APLIC（APLIC not supported）
* [zero-day-labs/riscv-aia](https://github.com/zero-day-labs/riscv-aia)
  * 采用Verilog（Implemented in Verilog）
  * 支持IMSIC（IMSIC supported）
    * 支持多种IMSIC微架构：原生、岛式和嵌入式（Multiple IMSIC microarchitectures available: vanilla, island and embedded）
  * 支持APLIC（APLIC supported）
