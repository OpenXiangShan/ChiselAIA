import sys
from pathlib import Path

import cocotb
from cocotb.clock import Clock
from cocotb.triggers import RisingEdge, FallingEdge

sys.path.append(str(Path(__file__).resolve().parent.parent))

from common import imsic_m_base_addr, op_access_ack, op_put_full


def _maybe_get(handle, name):
  try:
    return handle[name]
  except Exception:
    pass
  try:
    return getattr(handle, name)
  except Exception:
    pass
  try:
    return handle._id(name, extended=False)
  except Exception:
    return None


def resolve_path(root, path):
  handle = root
  for name in path.split('.'):
    handle = _maybe_get(handle, name)
    if handle is None:
      return None
  return handle


def require_path(root, paths):
  for path in paths:
    handle = resolve_path(root, path)
    if handle is not None:
      return handle, path
  raise AssertionError(f"Missing internal signal, tried: {paths}")


def optional_path(root, paths):
  for path in paths:
    handle = resolve_path(root, path)
    if handle is not None:
      return handle, path
  return None, None


def build_probe_set(dut, prefix):
  reggen_valid, reggen_valid_path = optional_path(
    dut,
    [f"{prefix}.axireg.axireg._reggen_io_valid"],
  )
  fifo_enq_ready, fifo_enq_ready_path = optional_path(
    dut,
    [f"{prefix}.axireg.axireg._fifo_sync_io_enq_ready"],
  )
  fifo_enq_valid, fifo_enq_valid_path = optional_path(
    dut,
    [f"{prefix}.axireg.axireg.fifo_sync.io_enq_valid"],
  )
  fifo_full, fifo_full_path = optional_path(
    dut,
    [f"{prefix}.axireg.axireg.fifo_sync.full"],
  )
  if None in (reggen_valid, fifo_enq_ready, fifo_enq_valid, fifo_full):
    return None
  return {
    "prefix": prefix,
    "reggen_valid": reggen_valid,
    "reggen_valid_path": reggen_valid_path,
    "fifo_enq_ready": fifo_enq_ready,
    "fifo_enq_ready_path": fifo_enq_ready_path,
    "fifo_enq_valid": fifo_enq_valid,
    "fifo_enq_valid_path": fifo_enq_valid_path,
    "fifo_full": fifo_full,
    "fifo_full_path": fifo_full_path,
  }


def set_tl_put32(dut, addr, data, source_id):
  dut.toaia_0_a_valid.value = 1
  dut.toaia_0_a_bits_opcode.value = op_put_full
  dut.toaia_0_a_bits_address.value = addr
  dut.toaia_0_a_bits_mask.value = 0x0f
  dut.toaia_0_a_bits_size.value = 2
  dut.toaia_0_a_bits_data.value = data
  source = _maybe_get(dut, "toaia_0_a_bits_source")
  assert source is not None, "TileLink source signal is required for outstanding-write testing"
  source.value = source_id
  param = _maybe_get(dut, "toaia_0_a_bits_param")
  if param is not None:
    param.value = 0
  corrupt = _maybe_get(dut, "toaia_0_a_bits_corrupt")
  if corrupt is not None:
    corrupt.value = 0


@cocotb.test()
async def tl_fifo_full_backpressure_test(dut):
  cocotb.start_soon(Clock(dut.clock, 1, units="ns").start())

  dut.reset.value = 1
  dut.toaia_0_a_valid.value = 0
  dut.toaia_0_d_ready.value = 1
  for _ in range(10):
    await RisingEdge(dut.clock)
  dut.reset.value = 0
  await RisingEdge(dut.clock)

  probe_sets = [
    probe for probe in (
      build_probe_set(dut, "imsic"),
      build_probe_set(dut, "imsic_1"),
      build_probe_set(dut, "imsic_2"),
      build_probe_set(dut, "imsic_3"),
    ) if probe is not None
  ]
  active_probe = None
  if probe_sets:
    dut._log.info("prepared internal fifo probes for %s", ", ".join(probe["prefix"] for probe in probe_sets))
  else:
    dut._log.info("internal fifo probes unavailable via VPI, using external a.ready stall/recovery checks")

  target_addr = imsic_m_base_addr + 0x1000
  total_requests = 16
  max_cycles = 300
  source_pool = list(range(16))

  current_request = 1
  current_source = source_pool[0]
  accepted = 0
  responses = 0
  backpressure_seen = False
  recovery_seen = False
  stall_request = None
  stalled_cycles = 0
  first_stall_after_accepts = None
  first_internal_backpressure_accepts = None
  response_bubble = False

  set_tl_put32(dut, target_addr, current_request, current_source)

  for _ in range(max_cycles):
    await FallingEdge(dut.clock)

    if active_probe is None:
      for probe in probe_sets:
        if int(probe["reggen_valid"].value) or int(probe["fifo_enq_valid"].value):
          active_probe = probe
          dut._log.info(
            "selected active probe %s via %s / %s",
            probe["prefix"],
            probe["reggen_valid_path"],
            probe["fifo_enq_valid_path"],
          )
          break

    internal_backpressure = (
      active_probe is not None and
      int(active_probe["fifo_full"].value) and
      int(active_probe["fifo_enq_valid"].value) and
      not int(active_probe["fifo_enq_ready"].value)
    )
    if internal_backpressure:
      if first_internal_backpressure_accepts is None:
        first_internal_backpressure_accepts = accepted
      assert int(dut.toaia_0_a_ready.value) == 0, (
        "TileLink a.ready must deassert when fifo is full and the skid stage is occupied"
      )
      backpressure_seen = True
      if stall_request is None and current_request is not None and int(dut.toaia_0_a_valid.value):
        stall_request = current_request

    if current_request is not None and int(dut.toaia_0_a_valid.value) and int(dut.toaia_0_a_ready.value):
      accepted += 1
      if stall_request == current_request:
        recovery_seen = True
        stall_request = None
      if accepted < total_requests:
        current_request = accepted + 1
        current_source = source_pool[accepted % len(source_pool)]
      else:
        current_request = None
        current_source = None
    elif current_request is not None and int(dut.toaia_0_a_valid.value) and not int(dut.toaia_0_a_ready.value):
      stalled_cycles += 1
      if first_stall_after_accepts is None:
        first_stall_after_accepts = accepted
      if accepted >= 9:
        backpressure_seen = True
        if stall_request is None:
          stall_request = current_request

    d_handshake = int(dut.toaia_0_d_valid.value) and int(dut.toaia_0_d_ready.value)
    if d_handshake:
      assert int(dut.toaia_0_d_bits_opcode.value) == op_access_ack
      responses += 1
      response_bubble = True

    if current_request is None:
      dut.toaia_0_a_valid.value = 0
      if backpressure_seen and recovery_seen and responses >= accepted:
        break
    elif response_bubble:
      dut.toaia_0_a_valid.value = 0
      response_bubble = False
    else:
      set_tl_put32(dut, target_addr, current_request, current_source)

  assert backpressure_seen, "Did not observe fifo-full backpressure on TileLink a.ready"
  assert stalled_cycles > 0, "Did not observe any stalled TileLink write while fifo was saturated"
  if active_probe is not None:
    assert first_internal_backpressure_accepts is not None, (
      "Expected to observe internal fifo backpressure on the selected IMSIC probe"
    )
  assert recovery_seen, "The stalled TileLink write did not recover after backpressure released"
  assert accepted == total_requests, f"Expected {total_requests} accepted writes, got {accepted}"
  assert responses >= accepted, f"Expected at least {accepted} write responses, got {responses}"