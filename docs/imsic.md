# IMSICs

In a typical RISC-V system, each hart is paired with its dedicated IMSIC. The IMSIC performs three main functions:

* Receives MSIs through memory-mapped registers,
* Generates interrupts for its associated hart,
* Manages CSRs under hart control.

In symmetric multiprocessing systems, multiple harts and their corresponding IMSICs can be organized into groups. Each group contains an equal number of hart-IMSIC pairs.

The following document will cover:

* The IO functionality of an individual IMSIC,
* The logical arrangement of multiple IMSICs within a system.

## The IO Functionality of an Individual IMSIC

![](./images/imsic_py.svg)

## Logic Arrangement of Multiple IMSICs

![](./images/imsics_arrangement_py.svg)
