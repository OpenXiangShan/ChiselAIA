# IMSICs

<!-- vim-markdown-toc GFM -->

* [Individual IMSIC Functionality](#individual-imsic-functionality)
* [Multiple IMSICs Arrangement](#multiple-imsics-arrangement)
  * [IMSIC Address Fields](#imsic-address-fields)
  * [IMSIC Memory Regions](#imsic-memory-regions)

<!-- vim-markdown-toc -->

In a typical RISC-V system, each hart is paired with its dedicated IMSIC.
The IMSIC performs three main functions:

* Receives MSIs through memory-mapped registers,
* Generates interrupts for its associated hart,
* Manages CSRs under hart control.

In symmetric multiprocessing systems, multiple harts-IMSIC pairs can be organized into groups,
with each group containing an equal number of pairs.

This document covers:

* The functionality of an individual IMSIC,
* The logical arrangement of multiple IMSICs within a system.

## Individual IMSIC Functionality

The IMSIC is tightly coupled with its hart,
directly using wire connection rather than bus/network for information transfer.
Key signals include:

* `pendings`: Pending interrupt status for each interrupt file (introduced as below).
* `{m,s,vs}topei`: Top external interrupt ID for each privilege level.
* `{m.s,vs}iselect`: CSR indirect access address for each privilege level.
* `{m,s,vs}ireg`: Read and write data for indirect CSR access for each privilege level.
* `vgein`: Virtualized supervisor level selector.

![](./images/imsic_py.svg)

One IMSIC manages all privilege levels in its hart,
including: one machine level, one supervisor level, and multiple virtualized supervisor levels.
As the behaviors of each level are identical in general, the AIA specification modularizes these functionalities of each level into independent and reusable components, called interrupt files.
Each interrupt file  exchanges privilege-agnostic information with IMSIC:

* `pending`: Interrupt pending status for this interrupt file.
* `topei`: Top external interrupt ID for this interrupt file.
* `iselect`: CSR indirect access address for this interrupt file.
* `ireg`: Read and write data for indirect CSR access for this interrupt file.

In addition, each interrupt file includes a 4KB memory page for receiving messages from bus/network.
The memory page including only one 4B memory-mapped register:

* `seteipnum`: Located at offset of 0x0, receiving incoming interrupt IDs.

## Multiple IMSICs Arrangement

In a large system, hart-IMSIC pairs can be divided into groups.
The below figure shows a symmetric 4-hart-IMSIC system.
These 4 pairs are divided into 2 **groups**, and each group contains 2 **members** (hart-IMSIC pairs).

![](./images/imsics_arrangement_py.svg)

### IMSIC Address Fields

To support physical memory protection (PMP), interrupt files of the same privilege level are located in a same memory region:

* Machine-level memory region:
  * One machine-level interrupt file per hart
* Supervisor-level memory region:
  * One supervisor-level interrupt file per hart,
  * Multiple virtualized supervisor-level interrupt files per hart.

Thus, each hart has only one page in machine-level memory region and multiple pages in supervisor-level memory region,
indexed by a **guest ID** (0 for supervisor-level, 1,2,3,... for virtualized supervisor level).
When determining the memory page address for a given IMSIC, four fields are needed:

* Privilege Level: Machine level or supervisor level.
* Group ID: The group to which this IMSIC belongs.
* Member ID: The member to which this IMSIC belongs.
* Guest ID: Supervisor level or one of the virtualized supervisor levels.

![](./images/imsic_addr.svg)

The formal expression for a machine-level interrupt file address:

$$
\begin{align}
mIntFileAddr =
& mBaseAddr \\\\
& + groupID \times 2^{mGroupStrideWidth} \\\\
& + memberID \times 2^{mMemberStrideWidth} \\\\
& + guestID \times 4K
\end{align}
$$

The formal expression for a virtualized supervisor-level interrupt file address:

$$
\begin{align}
vsIntFileAddr =
& vsBaseAddr \\\\
& + groupID \times 2^{vsGroupStrideWidth} \\\\
& + memberID \times 2^{vsMemberStrideWidth} \\\\
& + guestID \times 4K
\end{align}
$$

As required by the AIA specification, the `vsGroupStrideWidth` is the same as the `mGroupStrideWidth`.
For more details, please refer to the AIA specification[^imsic_memory_region].

### IMSIC Memory Regions

The memory regions for machine and supervisor levels are shown as below.

![](./images/imsic_addr_space.svg)

Here is a concrete example.
Assuming the base addresses for machine-level and supervisor-level memory regions are `0x6100_0000` and `0x8290_0000`, respectively,
the addresses for each interrupt file are:

* Machine-level interrupt files:
  * IMSIC00: `[0x61000000, 0x61000fff]`
  * IMSIC01: `[0x61001000, 0x61001fff]`
  * IMSIC10: `[0x61008000, 0x61008fff]`
  * IMSIC11: `[0x61009000, 0x61009fff]`
* Supervisor-level interrupt files:
  * IMSIC00: `[0x82900000, 0x82903fff]`
  * IMSIC01: `[0x82904000, 0x82907fff]`
  * IMSIC10: `[0x82908000, 0x8290bfff]`
  * IMSIC11: `[0x8290c000, 0x8290ffff]`

[^imsic_memory_region]: The RISC-V Advanced Interrupt Architecture: 3.6. Arrangement of the memory regions of multiple interrupt files
