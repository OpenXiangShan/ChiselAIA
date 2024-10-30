# APLIC

<!-- vim-markdown-toc GFM -->

* [Domain](#domain)
  * [Internal Registers](#internal-registers)

<!-- vim-markdown-toc -->

In message-based interrupt mode, the APLIC converts traditional wired interrupts into MSIs.
For efficiency, a single APLIC instance should serve all harts, unless devices are physically separated (e.g. on different chiplets).

## Domain

The APLIC implements a hierarchical domain structure to manage different privilege levels:

* The root domain (machine level) directly receives all wired interrupts,
* Child domains receive delegated interrupts from their parent domains,
* A supervisor-level domain can handle both supervisor-level and virtualized supervisor-level interrupts.

For large symmetric multiprocessing systems, a two-domain configuration typically suffices:

* One machine-level domain,
* One supervisor-level domain.

![](./images/aplic.svg)

### Internal Registers

APLIC maintains interrupt status in internal registers, including two critical registers:

* `ip[intSrcNum bits]`: Interrupt pending status registers,
* `ie[intSrcNum bits]`: Interrupt enable control registers.

These registers are controlled through memory-mapped interfaces.
For detailed register specifications, refer to the AIA specification[^aplic_mem_regs].

**Race Conditions**

The `ip` registers can be modified by multiple sources, creating potential race conditions.
The AIA specification does not specify the APLIC behaviors under this race condition.
OpenAIA implements a priority-based resolution mechanism.
Priority levels (highest to lowest):

*  APLIC internal operations: Clearing `ip` after sending an MSI,
*  Wired device operations: Setting `ip` via `intSrc`,
*  Hart operations: Setting/Clearing `ip` via memory mapped registers.

Higher priority operations override the lower priority ones.
However, best practices recommend:

* Avoid race conditions through programming,
* Detaching the wired device before modifying corresponding `ip` through memory-mapped registers.

[^aplic_mem_regs]: The RISC-V Advanced Interrupt Architecture: 4.5. Memory-mapped control region for an interrupt domain
