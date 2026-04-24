import cocotb
from cocotb.clock import Clock
from cocotb.result import SimTimeoutError
from cocotb.triggers import RisingEdge, FallingEdge, with_timeout, Timer, Event
from cocotb.utils import get_sim_time


def now_ns():
    return int(get_sim_time("ns"))


def first_signal(dut, *names):
    for name in names:
        if hasattr(dut, name):
            return getattr(dut, name)
    raise AttributeError(f"None of the signals exist: {names}")


def hierarchical_signal(root, path):
    current = root
    for name in path.split("."):
        if not hasattr(current, name):
            return None
        current = getattr(current, name)
    return current


def first_hierarchical_signal(root, *paths):
    for path in paths:
        signal = hierarchical_signal(root, path)
        if signal is not None:
            return signal
    return None


def signal_value(signal):
    return None if signal is None else int(signal.value)


class Ports:
    def __init__(self, dut):
        self.soc_clock = first_signal(dut, "soc_clock")
        self.cpu_clock = first_signal(dut, "clock")
        self.reset = first_signal(dut, "reset")
        self.soc_reset = first_signal(dut, "soc_reset")

        self.aw_ready = first_signal(dut, "auto_axireg_axi4xbar_in_aw_ready", "toaia_0_aw_ready")
        self.aw_valid = first_signal(dut, "auto_axireg_axi4xbar_in_aw_valid", "toaia_0_aw_valid")
        self.aw_id = first_signal(dut, "auto_axireg_axi4xbar_in_aw_bits_id", "toaia_0_aw_bits_id")
        self.aw_addr = first_signal(dut, "auto_axireg_axi4xbar_in_aw_bits_addr", "toaia_0_aw_bits_addr")
        self.aw_len = first_signal(dut, "auto_axireg_axi4xbar_in_aw_bits_len", "toaia_0_aw_bits_len")
        self.aw_size = first_signal(dut, "auto_axireg_axi4xbar_in_aw_bits_size", "toaia_0_aw_bits_size")
        self.aw_burst = first_signal(dut, "auto_axireg_axi4xbar_in_aw_bits_burst", "toaia_0_aw_bits_burst")
        self.aw_lock = first_signal(dut, "auto_axireg_axi4xbar_in_aw_bits_lock", "toaia_0_aw_bits_lock")
        self.aw_cache = first_signal(dut, "auto_axireg_axi4xbar_in_aw_bits_cache", "toaia_0_aw_bits_cache")
        self.aw_prot = first_signal(dut, "auto_axireg_axi4xbar_in_aw_bits_prot", "toaia_0_aw_bits_prot")
        self.aw_qos = first_signal(dut, "auto_axireg_axi4xbar_in_aw_bits_qos", "toaia_0_aw_bits_qos")

        self.w_ready = first_signal(dut, "auto_axireg_axi4xbar_in_w_ready", "toaia_0_w_ready")
        self.w_valid = first_signal(dut, "auto_axireg_axi4xbar_in_w_valid", "toaia_0_w_valid")
        self.w_data = first_signal(dut, "auto_axireg_axi4xbar_in_w_bits_data", "toaia_0_w_bits_data")
        self.w_strb = first_signal(dut, "auto_axireg_axi4xbar_in_w_bits_strb", "toaia_0_w_bits_strb")
        self.w_last = first_signal(dut, "auto_axireg_axi4xbar_in_w_bits_last", "toaia_0_w_bits_last")

        self.b_ready = first_signal(dut, "auto_axireg_axi4xbar_in_b_ready", "toaia_0_b_ready")
        self.b_valid = first_signal(dut, "auto_axireg_axi4xbar_in_b_valid", "toaia_0_b_valid")
        self.b_resp = first_signal(dut, "auto_axireg_axi4xbar_in_b_bits_resp", "toaia_0_b_bits_resp")

        self.ar_valid = first_signal(dut, "auto_axireg_axi4xbar_in_ar_valid", "toaia_0_ar_valid")
        self.ar_id = first_signal(dut, "auto_axireg_axi4xbar_in_ar_bits_id", "toaia_0_ar_bits_id")
        self.ar_addr = first_signal(dut, "auto_axireg_axi4xbar_in_ar_bits_addr", "toaia_0_ar_bits_addr")
        self.ar_len = first_signal(dut, "auto_axireg_axi4xbar_in_ar_bits_len", "toaia_0_ar_bits_len")
        self.ar_size = first_signal(dut, "auto_axireg_axi4xbar_in_ar_bits_size", "toaia_0_ar_bits_size")
        self.ar_burst = first_signal(dut, "auto_axireg_axi4xbar_in_ar_bits_burst", "toaia_0_ar_bits_burst")
        self.ar_lock = first_signal(dut, "auto_axireg_axi4xbar_in_ar_bits_lock", "toaia_0_ar_bits_lock")
        self.ar_cache = first_signal(dut, "auto_axireg_axi4xbar_in_ar_bits_cache", "toaia_0_ar_bits_cache")
        self.ar_prot = first_signal(dut, "auto_axireg_axi4xbar_in_ar_bits_prot", "toaia_0_ar_bits_prot")
        self.ar_qos = first_signal(dut, "auto_axireg_axi4xbar_in_ar_bits_qos", "toaia_0_ar_bits_qos")
        self.r_ready = first_signal(dut, "auto_axireg_axi4xbar_in_r_ready", "toaia_0_r_ready")

        self.csr_addr_valid = first_signal(dut, "fromCSR_addr_valid", "fromCSR0_addr_valid")
        self.csr_addr_bits_addr = first_signal(dut, "fromCSR_addr_bits_addr", "fromCSR0_addr_bits_addr")
        self.csr_addr_bits_virt = first_signal(dut, "fromCSR_addr_bits_virt", "fromCSR0_addr_bits_virt")
        self.csr_addr_bits_priv = first_signal(dut, "fromCSR_addr_bits_priv", "fromCSR0_addr_bits_priv")
        self.csr_vgein = first_signal(dut, "fromCSR_vgein", "fromCSR0_vgein")
        self.csr_wdata_valid = first_signal(dut, "fromCSR_wdata_valid", "fromCSR0_wdata_valid")
        self.csr_wdata_bits_op = first_signal(dut, "fromCSR_wdata_bits_op", "fromCSR0_wdata_bits_op")
        self.csr_wdata_bits_data = first_signal(dut, "fromCSR_wdata_bits_data", "fromCSR0_wdata_bits_data")
        self.csr_claims_0 = first_signal(dut, "fromCSR_claims_0", "fromCSR0_claims_0")
        self.csr_claims_1 = first_signal(dut, "fromCSR_claims_1", "fromCSR0_claims_1")
        self.csr_claims_2 = first_signal(dut, "fromCSR_claims_2", "fromCSR0_claims_2")

        self.to_csr_rdata_valid = first_signal(dut, "toCSR_rdata_valid", "toCSR0_rdata_valid")
        self.to_csr_rdata_bits = first_signal(dut, "toCSR_rdata_bits", "toCSR0_rdata_bits")


class InternalSignals:
    def __init__(self, dut):
        self.xbar_aw_valid = hierarchical_signal(dut, "axireg.axireg.axi4xbar.auto_out_0_aw_valid")
        self.xbar_w_valid = hierarchical_signal(dut, "axireg.axireg.axi4xbar.auto_out_0_w_valid")
        self.xbar_b_ready = hierarchical_signal(dut, "axireg.axireg.axi4xbar.auto_out_0_b_ready")
        self.reggen_in_ready = hierarchical_signal(dut, "axireg.axireg.reggen.regmapIOs_0_1_ready")
        self.write_data = hierarchical_signal(dut, "axireg.axireg.intfileFromMemIn_w_bits_data")
        self.reggen_irq_valid = hierarchical_signal(dut, "axireg.axireg.reggen.io_valid")
        self.reggen_irq_bits = hierarchical_signal(dut, "axireg.axireg.reggen.io_seteipnum")
        self.fifo_enq_ready = hierarchical_signal(dut, "axireg.axireg.fifo_sync.io_enq_ready")
        self.fifo_deq_valid = hierarchical_signal(dut, "axireg.axireg.fifo_sync.io_deq_valid")
        self.fifo_deq_bits = hierarchical_signal(dut, "axireg.axireg.fifo_sync.io_deq_bits")
        self.handshake_state = hierarchical_signal(dut, "axireg.axireg.handshakeState")
        self.stage_valid = first_hierarchical_signal(
            dut,
            "axireg.axireg.stageValid",
            "axireg.axireg.stageQueue.io_deq_valid",
        )
        self.stage_bits = first_hierarchical_signal(
            dut,
            "axireg.axireg.stageBits",
            "axireg.axireg.stageQueue.io_deq_bits",
        )
        self.stage_ready = first_hierarchical_signal(
            dut,
            "axireg.axireg.stageReady",
            "axireg.axireg.stageQueue.io_enq_ready",
        )
        self.reggen_back_full = hierarchical_signal(dut, "axireg.axireg.reggen.regmapIOs_0_2_back_q.full")
        self.axilite_state = hierarchical_signal(dut, "axireg.axireg.axi4tolite.state")
        self.axilite_awready = hierarchical_signal(dut, "axireg.axireg.axi4tolite.awready")
        self.axilite_wready = hierarchical_signal(dut, "axireg.axireg.axi4tolite.wready")
        self.axilite_aw_last = hierarchical_signal(dut, "axireg.axireg.axi4tolite.aw_last")
        self.axilite_w_last = hierarchical_signal(dut, "axireg.axireg.axi4tolite.w_last")
        self.axilite_bvalid = hierarchical_signal(dut, "axireg.axireg.axi4tolite.auto_in_b_valid")
        self.axilite_out_bvalid = hierarchical_signal(dut, "axireg.axireg.axi4tolite.auto_out_b_valid")


async def monitor_irq_pipeline(dut, ports, internal):
    last_snapshot = None
    while True:
        await RisingEdge(ports.soc_clock)
        timestamp = now_ns()

        regmap_accept = (
            internal.xbar_aw_valid is not None
            and internal.xbar_w_valid is not None
            and internal.reggen_in_ready is not None
            and internal.fifo_enq_ready is not None
            and internal.write_data is not None
            and signal_value(internal.xbar_aw_valid)
            and signal_value(internal.xbar_w_valid)
            and signal_value(internal.reggen_in_ready)
            and signal_value(internal.fifo_enq_ready)
        )
        if regmap_accept:
            dut._log.info(
                f"pipe regmap_accept irq={signal_value(internal.write_data)} at {timestamp} ns"
            )

        fifo_enq_fire = (
            internal.reggen_irq_valid is not None
            and internal.fifo_enq_ready is not None
            and signal_value(internal.reggen_irq_valid)
            and signal_value(internal.fifo_enq_ready)
        )
        if fifo_enq_fire:
            dut._log.info(
                f"pipe fifo_enq irq={signal_value(internal.reggen_irq_bits)} at {timestamp} ns"
            )

        fifo_deq_fire = (
            internal.fifo_deq_valid is not None
            and internal.handshake_state is not None
            and signal_value(internal.fifo_deq_valid)
            and signal_value(internal.handshake_state) == 0
        )
        if fifo_deq_fire:
            dut._log.info(
                f"pipe fifo_deq irq={signal_value(internal.fifo_deq_bits)} at {timestamp} ns"
            )

        snapshot = (
            signal_value(internal.axilite_state),
            signal_value(internal.axilite_awready),
            signal_value(internal.axilite_wready),
            signal_value(internal.axilite_aw_last),
            signal_value(internal.axilite_w_last),
            signal_value(internal.axilite_out_bvalid),
            signal_value(internal.axilite_bvalid),
            signal_value(internal.reggen_back_full),
            signal_value(internal.reggen_irq_valid),
            signal_value(internal.reggen_irq_bits),
            signal_value(internal.stage_valid),
            signal_value(internal.stage_bits),
            signal_value(internal.fifo_enq_ready),
            signal_value(internal.fifo_deq_valid),
            signal_value(internal.handshake_state),
        )
        if snapshot != last_snapshot:
            dut._log.info(
                "pipe state "
                f"state={snapshot[0]} awr={snapshot[1]} wr={snapshot[2]} "
                f"aw_last={snapshot[3]} w_last={snapshot[4]} out_bv={snapshot[5]} in_bv={snapshot[6]} "
                f"back_full={snapshot[7]} reggen_v={snapshot[8]} reggen_bits={snapshot[9]} "
                f"stage_v={snapshot[10]} stage_bits={snapshot[11]} fifo_enq_r={snapshot[12]} "
                f"fifo_deq_v={snapshot[13]} hs={snapshot[14]} at {timestamp} ns"
            )
            last_snapshot = snapshot


async def wait_for_b_valid(ports):
    while not int(ports.b_valid.value):
        await RisingEdge(ports.soc_clock)
    return now_ns()


async def axi4_b_receive(ports):
    return await with_timeout(wait_for_b_valid(ports), 20000, "ns")


async def axi4_write32(dut, ports, addr, data):
    await RisingEdge(ports.soc_clock)
    issue_ns = now_ns()
    ports.aw_valid.value = 1
    ports.aw_id.value = 0
    ports.aw_addr.value = addr
    ports.aw_len.value = 0
    ports.aw_size.value = 2
    ports.aw_burst.value = 1
    ports.aw_lock.value = 0
    ports.aw_cache.value = 0
    ports.aw_prot.value = 0
    ports.aw_qos.value = 0
    ports.w_valid.value = 1
    ports.w_last.value = 1
    ports.w_data.value = data
    ports.w_strb.value = 0xF

    dut._log.info(f"irq {data}: issue at {issue_ns} ns addr=0x{addr:x}")

    aw_fire_ns = None
    w_fire_ns = None

    while aw_fire_ns is None or w_fire_ns is None:
        await RisingEdge(ports.soc_clock)
        timestamp = now_ns()
        aw_fire = aw_fire_ns is None and int(ports.aw_valid.value) and int(ports.aw_ready.value)
        w_fire = w_fire_ns is None and int(ports.w_valid.value) and int(ports.w_ready.value)

        if aw_fire:
            aw_fire_ns = timestamp
            dut._log.info(f"irq {data}: AW fire at {aw_fire_ns} ns")
        if w_fire:
            w_fire_ns = timestamp
            dut._log.info(f"irq {data}: W fire at {w_fire_ns} ns")

        if aw_fire:
            ports.aw_valid.value = 0
        if w_fire:
            ports.w_valid.value = 0
            ports.w_last.value = 0

    try:
        b_valid_ns = await axi4_b_receive(ports)
    except SimTimeoutError:
        debug_signals = {
            "aw_valid": signal_value(ports.aw_valid),
            "aw_ready": signal_value(ports.aw_ready),
            "w_valid": signal_value(ports.w_valid),
            "w_ready": signal_value(ports.w_ready),
            "b_valid": signal_value(ports.b_valid),
            "b_ready": signal_value(ports.b_ready),
            "xbar_aw_valid": signal_value(hierarchical_signal(dut, "axireg.axireg.axi4xbar.auto_out_0_aw_valid")),
            "xbar_w_valid": signal_value(hierarchical_signal(dut, "axireg.axireg.axi4xbar.auto_out_0_w_valid")),
            "reggen_in_ready": signal_value(hierarchical_signal(dut, "axireg.axireg.reggen.regmapIOs_0_1_ready")),
            "reggen_io_valid": signal_value(hierarchical_signal(dut, "axireg.axireg.reggen.io_valid")),
            "reggen_io_seteipnum": signal_value(hierarchical_signal(dut, "axireg.axireg.reggen.io_seteipnum")),
            "stage_valid": signal_value(hierarchical_signal(dut, "axireg.axireg.stageValid")),
            "stage_bits": signal_value(hierarchical_signal(dut, "axireg.axireg.stageBits")),
            "fifo_enq_ready": signal_value(hierarchical_signal(dut, "axireg.axireg.fifo_sync.io_enq_ready")),
            "fifo_deq_valid": signal_value(hierarchical_signal(dut, "axireg.axireg.fifo_sync.io_deq_valid")),
            "fifo_deq_bits": signal_value(hierarchical_signal(dut, "axireg.axireg.fifo_sync.io_deq_bits")),
            "handshakeState": signal_value(hierarchical_signal(dut, "axireg.axireg.handshakeState")),
            "msi_vld_req": signal_value(hierarchical_signal(dut, "axireg.axireg.msi_vld_req")),
            "ack_soc": signal_value(hierarchical_signal(dut, "axireg.axireg._msi_vld_ack_soc_chain_io_q")),
            "axilite_state": signal_value(hierarchical_signal(dut, "axireg.axireg.axi4tolite.state")),
            "axilite_aw_last": signal_value(hierarchical_signal(dut, "axireg.axireg.axi4tolite.aw_last")),
            "axilite_w_last": signal_value(hierarchical_signal(dut, "axireg.axireg.axi4tolite.w_last")),
            "axilite_out_bvalid": signal_value(hierarchical_signal(dut, "axireg.axireg.axi4tolite.auto_out_b_valid")),
        }
        dut._log.error(
            f"irq {data}: timeout waiting B, "
            + ", ".join(f"{name}={value}" for name, value in debug_signals.items())
        )
        raise
    dut._log.info(
        f"irq {data}: B valid at {b_valid_ns} ns resp=0x{int(ports.b_resp.value):x}"
    )

    return {
        "irq": data,
        "issue_ns": issue_ns,
        "aw_fire_ns": aw_fire_ns,
        "w_fire_ns": w_fire_ns,
        "b_valid_ns": b_valid_ns,
    }


async def axi4_write_burst_first_beat(dut, ports, addr, beats, aw_id=0):
    if len(beats) < 2:
        raise ValueError("Burst helper expects at least two beats so aw_len > 0")

    await RisingEdge(ports.soc_clock)
    issue_ns = now_ns()
    ports.aw_valid.value = 1
    ports.aw_id.value = aw_id
    ports.aw_addr.value = addr
    ports.aw_len.value = len(beats) - 1
    ports.aw_size.value = 2
    ports.aw_burst.value = 1
    ports.aw_lock.value = 0
    ports.aw_cache.value = 0
    ports.aw_prot.value = 0
    ports.aw_qos.value = 0
    ports.w_valid.value = 1
    ports.w_data.value = beats[0]
    ports.w_strb.value = 0xF
    ports.w_last.value = int(len(beats) == 1)

    dut._log.info(
        f"burst first_beat=0x{beats[0]:x} len={len(beats) - 1} issue at {issue_ns} ns addr=0x{addr:x}"
    )

    aw_fire_ns = None
    w_fire_ns = []
    beat_index = 0

    while beat_index < len(beats):
        await RisingEdge(ports.soc_clock)
        timestamp = now_ns()

        if aw_fire_ns is None and int(ports.aw_valid.value) and int(ports.aw_ready.value):
            aw_fire_ns = timestamp
            dut._log.info(f"burst first_beat=0x{beats[0]:x}: AW fire at {aw_fire_ns} ns")
            ports.aw_valid.value = 0

        if int(ports.w_valid.value) and int(ports.w_ready.value):
            current_beat = beat_index
            w_fire_ns.append(timestamp)
            dut._log.info(
                f"burst first_beat=0x{beats[0]:x}: W fire beat={current_beat} data=0x{beats[current_beat]:x} at {timestamp} ns"
            )
            beat_index += 1
            if beat_index == len(beats):
                ports.w_valid.value = 0
                ports.w_last.value = 0
            else:
                ports.w_data.value = beats[beat_index]
                ports.w_last.value = int(beat_index == len(beats) - 1)

    try:
        b_valid_ns = await axi4_b_receive(ports)
    except SimTimeoutError:
        debug_signals = {
            "aw_valid": signal_value(ports.aw_valid),
            "aw_ready": signal_value(ports.aw_ready),
            "w_valid": signal_value(ports.w_valid),
            "w_ready": signal_value(ports.w_ready),
            "b_valid": signal_value(ports.b_valid),
            "stage_valid": signal_value(hierarchical_signal(dut, "axireg.axireg.stageValid")),
            "stage_bits": signal_value(hierarchical_signal(dut, "axireg.axireg.stageBits")),
            "fifo_enq_ready": signal_value(hierarchical_signal(dut, "axireg.axireg.fifo_sync.io_enq_ready")),
            "reggen_io_valid": signal_value(hierarchical_signal(dut, "axireg.axireg.reggen.io_valid")),
            "reggen_io_seteipnum": signal_value(hierarchical_signal(dut, "axireg.axireg.reggen.io_seteipnum")),
            "handshakeState": signal_value(hierarchical_signal(dut, "axireg.axireg.handshakeState")),
        }
        dut._log.error(
            f"burst first_beat=0x{beats[0]:x}: timeout waiting B, "
            + ", ".join(f"{name}={value}" for name, value in debug_signals.items())
        )
        raise

    dut._log.info(
        f"burst first_beat=0x{beats[0]:x}: B valid at {b_valid_ns} ns resp=0x{int(ports.b_resp.value):x}"
    )

    return {
        "first_beat": beats[0],
        "issue_ns": issue_ns,
        "aw_fire_ns": aw_fire_ns,
        "w_fire_ns": w_fire_ns,
        "b_valid_ns": b_valid_ns,
    }


async def axi4_issue_bursts_without_waiting_b(dut, ports, addr, bursts, aw_id=0):
    if not bursts:
        return []

    issued = []
    pending = []
    next_index = 0
    current = None
    beat_index = 0

    def load_burst(index, issue_ns):
        nonlocal current, beat_index
        beats = bursts[index]
        if len(beats) < 2:
            raise ValueError("Outstanding burst helper expects aw_len > 0")

        beat_index = 0
        current = {
            "first_beat": beats[0],
            "beats": beats,
            "issue_ns": issue_ns,
            "aw_fire_ns": None,
            "w_fire_ns": [],
            "b_valid_ns": None,
        }
        issued.append(current)

        ports.aw_valid.value = 1
        ports.aw_id.value = aw_id
        ports.aw_addr.value = addr
        ports.aw_len.value = len(beats) - 1
        ports.aw_size.value = 2
        ports.aw_burst.value = 1
        ports.aw_lock.value = 0
        ports.aw_cache.value = 0
        ports.aw_prot.value = 0
        ports.aw_qos.value = 0
        ports.w_valid.value = 1
        ports.w_data.value = beats[0]
        ports.w_strb.value = 0xF
        ports.w_last.value = int(len(beats) == 1)

        dut._log.info(
            f"outstanding burst first_beat=0x{beats[0]:x} len={len(beats) - 1} issue at {issue_ns} ns addr=0x{addr:x}"
        )

    await RisingEdge(ports.soc_clock)
    load_burst(next_index, now_ns())
    next_index += 1

    while current is not None or next_index < len(bursts) or pending or any(entry["b_valid_ns"] is None for entry in issued):
        await RisingEdge(ports.soc_clock)
        timestamp = now_ns()

        if current is not None:
            beats = current["beats"]
            if current["aw_fire_ns"] is None and int(ports.aw_valid.value) and int(ports.aw_ready.value):
                current["aw_fire_ns"] = timestamp
                ports.aw_valid.value = 0
                dut._log.info(
                    f"outstanding burst first_beat=0x{beats[0]:x}: AW fire at {timestamp} ns"
                )

            if int(ports.w_valid.value) and int(ports.w_ready.value):
                current["w_fire_ns"].append(timestamp)
                dut._log.info(
                    f"outstanding burst first_beat=0x{beats[0]:x}: W fire beat={beat_index} data=0x{beats[beat_index]:x} at {timestamp} ns"
                )
                beat_index += 1
                if beat_index == len(beats):
                    ports.w_valid.value = 0
                    ports.w_last.value = 0
                    pending.append(current)
                    current = None
                else:
                    ports.w_data.value = beats[beat_index]
                    ports.w_last.value = int(beat_index == len(beats) - 1)

        if int(ports.b_valid.value):
            if not pending:
                raise AssertionError("Observed B valid with no pending outstanding burst")
            completed = pending.pop(0)
            completed["b_valid_ns"] = timestamp
            dut._log.info(
                f"outstanding burst first_beat=0x{completed['first_beat']:x}: B valid at {timestamp} ns resp=0x{int(ports.b_resp.value):x}"
            )

        if current is None and next_index < len(bursts):
            load_burst(next_index, timestamp)
            next_index += 1

    return issued


async def select_m_intfile(ports):
    await FallingEdge(ports.cpu_clock)
    ports.csr_addr_bits_priv.value = 3
    ports.csr_addr_bits_virt.value = 0
    ports.csr_vgein.value = 0


async def select_s_intfile(ports):
    await FallingEdge(ports.cpu_clock)
    ports.csr_addr_bits_priv.value = 1
    ports.csr_addr_bits_virt.value = 0
    ports.csr_vgein.value = 0


async def write_csr(ports, addr, data):
    await FallingEdge(ports.cpu_clock)
    ports.csr_addr_valid.value = 1
    ports.csr_addr_bits_addr.value = addr
    ports.csr_wdata_valid.value = 1
    ports.csr_wdata_bits_op.value = 1
    ports.csr_wdata_bits_data.value = data
    await FallingEdge(ports.cpu_clock)
    ports.csr_addr_valid.value = 0
    ports.csr_wdata_valid.value = 0


async def read_csr(ports, addr):
    await FallingEdge(ports.cpu_clock)
    ports.csr_addr_valid.value = 1
    ports.csr_addr_bits_addr.value = addr
    await FallingEdge(ports.cpu_clock)
    ports.csr_addr_valid.value = 0
    for _ in range(16):
        await RisingEdge(ports.cpu_clock)
        if ports.to_csr_rdata_valid.value:
            return int(ports.to_csr_rdata_bits.value)
    raise AssertionError(f"Timeout waiting CSR read addr=0x{addr:x}")


async def init_imsic(ports):
    await select_m_intfile(ports)
    await write_csr(ports, 0x70, 1)
    await write_csr(ports, 0xC0, 0xFFFFFFFE)


async def init_s_intfile(ports):
    await select_s_intfile(ports)
    await write_csr(ports, 0x70, 1)
    await write_csr(ports, 0xC0, 0xFFFFFFFE)
    await select_m_intfile(ports)


def drive_idle_inputs(ports):
    ports.b_ready.value = 1
    ports.r_ready.value = 1
    ports.aw_valid.value = 0
    ports.aw_id.value = 0
    ports.aw_addr.value = 0
    ports.aw_len.value = 0
    ports.aw_size.value = 0
    ports.aw_burst.value = 0
    ports.aw_lock.value = 0
    ports.aw_cache.value = 0
    ports.aw_prot.value = 0
    ports.aw_qos.value = 0
    ports.w_valid.value = 0
    ports.w_data.value = 0
    ports.w_strb.value = 0
    ports.w_last.value = 0
    ports.ar_valid.value = 0
    ports.ar_id.value = 0
    ports.ar_addr.value = 0
    ports.ar_len.value = 0
    ports.ar_size.value = 0
    ports.ar_burst.value = 0
    ports.ar_lock.value = 0
    ports.ar_cache.value = 0
    ports.ar_prot.value = 0
    ports.ar_qos.value = 0
    ports.csr_addr_valid.value = 0
    ports.csr_wdata_valid.value = 0
    ports.csr_claims_0.value = 0
    ports.csr_claims_1.value = 0
    ports.csr_claims_2.value = 0


async def setup_testbench(dut):
    ports = Ports(dut)
    internal = InternalSignals(dut)

    cocotb.start_soon(Clock(ports.soc_clock, 1, units="ns").start())
    cocotb.start_soon(Clock(ports.cpu_clock, 200, units="ns").start())
    cocotb.start_soon(monitor_irq_pipeline(dut, ports, internal))

    ports.reset.value = 1
    ports.soc_reset.value = 1
    drive_idle_inputs(ports)

    for _ in range(10):
        await RisingEdge(ports.cpu_clock)
    ports.reset.value = 0
    ports.soc_reset.value = 0

    await init_imsic(ports)
    return ports, internal


async def find_stalled_write(dut, ports, last_irq, addr=0x0, first_irq=1):
    for irq in range(first_irq, last_irq + 1):
        write_task = cocotb.start_soon(axi4_write32(dut, ports, addr, irq))
        await Timer(20, units="ns")
        request_backpressured = int(ports.aw_valid.value) or int(ports.w_valid.value)
        if not request_backpressured:
            await write_task
            continue

        dut._log.info(f"Observed FIFO backpressure when issuing irq {irq}")
        return irq, write_task

    raise AssertionError("No FIFO-full backpressure observed while issuing interrupts")


async def find_stalled_burst_first_beat(dut, ports, last_irq, tail_beats, addr=0x0, first_irq=1):
    for irq in range(first_irq, last_irq + 1):
        beats = [irq] + tail_beats
        write_task = cocotb.start_soon(axi4_write_burst_first_beat(dut, ports, addr, beats))
        await Timer(20, units="ns")
        request_backpressured = int(ports.aw_valid.value) or int(ports.w_valid.value)
        if not request_backpressured:
            result = await write_task
            assert len(result["w_fire_ns"]) == len(beats), (
                f"Burst 0x{irq:x} did not send all beats: fired {len(result['w_fire_ns'])}, expected {len(beats)}"
            )
            continue

        dut._log.info(f"Observed FIFO backpressure when issuing len=4 burst first beat {irq}")
        return irq, write_task

    raise AssertionError("No FIFO-full backpressure observed while issuing len=4 bursts")


async def wait_for_stage_backpressure(ports, internal):
    while True:
        await RisingEdge(ports.soc_clock)
        if (
            int(ports.aw_valid.value)
            and int(ports.w_valid.value)
            and signal_value(internal.stage_valid) == 1
            and signal_value(internal.fifo_enq_ready) == 0
        ):
            return now_ns()


async def watch_reggen_leak_while_blocked(ports, internal, stop_event):
    observed_backpressure = False
    while not stop_event.is_set():
        await RisingEdge(ports.soc_clock)
        fifo_blocked = signal_value(internal.fifo_enq_ready) == 0
        stage_blocked = signal_value(internal.stage_ready) == 0
        if fifo_blocked:
            observed_backpressure = True
        if fifo_blocked and stage_blocked:
            assert signal_value(internal.reggen_irq_valid) == 0, (
                "reggen.io.valid pulsed while fifo_sync was blocked and stageReady was low"
            )
    return observed_backpressure


async def watch_redbox_response_progress(ports, internal, target_irq, stop_event):
    observed_window = False
    observed_target = False
    while not stop_event.is_set():
        await RisingEdge(ports.soc_clock)
        fifo_blocked = signal_value(internal.fifo_enq_ready) == 0
        stage_blocked = signal_value(internal.stage_ready) == 0
        stage_recovered = signal_value(internal.stage_ready) == 1
        redbox_window = (
            fifo_blocked
            and signal_value(internal.xbar_b_ready) == 1
            and stage_blocked
        )
        if redbox_window:
            observed_window = True
        if (
            observed_window
            and stage_recovered
            and signal_value(internal.xbar_b_ready) == 0
            and signal_value(internal.reggen_irq_valid) == 1
            and signal_value(internal.reggen_irq_bits) == target_irq
        ):
            observed_target = True
    return observed_window, observed_target


async def watch_reggen_irq_pulses(ports, internal, watched_irqs, stop_event):
    counts = {irq: 0 for irq in watched_irqs}
    while not stop_event.is_set():
        await RisingEdge(ports.soc_clock)
        if signal_value(internal.reggen_irq_valid) != 1:
            continue
        irq = signal_value(internal.reggen_irq_bits)
        if irq in counts:
            counts[irq] += 1
    return counts


@cocotb.test()
async def fifo_full_keeps_all_msi(dut):
    ports, _ = await setup_testbench(dut)

    last_irq = 20
    stalled_irq, stalled_task = await find_stalled_write(dut, ports, last_irq)
    try:
        await with_timeout(stalled_task, 50000, "ns")
    except SimTimeoutError:
        debug_signals = {
            "aw_valid": signal_value(ports.aw_valid),
            "aw_ready": signal_value(ports.aw_ready),
            "w_valid": signal_value(ports.w_valid),
            "w_ready": signal_value(ports.w_ready),
            "b_valid": signal_value(ports.b_valid),
            "b_ready": signal_value(ports.b_ready),
            "top_req": signal_value(hierarchical_signal(dut, "_axireg_msiio_vld_req")),
            "top_ack": signal_value(hierarchical_signal(dut, "_imsic_msiio_vld_ack")),
            "handshakeState": signal_value(hierarchical_signal(dut, "axireg.axireg.handshakeState")),
            "msi_vld_req": signal_value(hierarchical_signal(dut, "axireg.axireg.msi_vld_req")),
            "ack_soc": signal_value(hierarchical_signal(dut, "axireg.axireg._msi_vld_ack_soc_chain_io_q")),
            "fifo_enq_ready": signal_value(hierarchical_signal(dut, "axireg.axireg._fifo_sync_io_enq_ready")),
            "fifo_deq_valid": signal_value(hierarchical_signal(dut, "axireg.axireg._fifo_sync_io_deq_valid")),
        }
        dut._log.error("Timed out waiting for stalled AXI write to complete: " + ", ".join(f"{name}={value}" for name, value in debug_signals.items()))
        raise

    for irq in range(stalled_irq + 1, last_irq + 1):
        await axi4_write32(dut, ports, 0x0, irq)

    for _ in range(120):
        await RisingEdge(ports.cpu_clock)

    eip0 = await read_csr(ports, 0x80)
    expected = sum(1 << irq for irq in range(1, last_irq + 1))
    assert (eip0 & expected) == expected, f"Missing delivered MSI bits: eip0=0x{eip0:x}, expected mask=0x{expected:x}"

    dut._log.info(f"Observed eip0=0x{eip0:08x}, expected mask=0x{expected:08x}")


@cocotb.test()
async def backpressure_blocks_aw_w_when_stage_occupied(dut):
    ports, internal = await setup_testbench(dut)

    stalled_irq, stalled_task = await find_stalled_write(dut, ports, 20)
    backpressure_ns = await with_timeout(wait_for_stage_backpressure(ports, internal), 50000, "ns")
    dut._log.info(f"Observed stage-backed AXI backpressure at {backpressure_ns} ns for irq {stalled_irq}")

    for cycle in range(8):
        assert int(ports.aw_valid.value) == 1, f"AW valid dropped while request was backpressured at sample {cycle}"
        assert int(ports.w_valid.value) == 1, f"W valid dropped while request was backpressured at sample {cycle}"
        assert int(ports.aw_ready.value) == 0, f"AW ready unexpectedly reasserted during stage/FIFO backpressure at sample {cycle}"
        assert int(ports.w_ready.value) == 0, f"W ready unexpectedly reasserted during stage/FIFO backpressure at sample {cycle}"
        assert signal_value(internal.stage_valid) == 1, f"stageValid unexpectedly deasserted during backpressure at sample {cycle}"
        assert signal_value(internal.fifo_enq_ready) == 0, f"fifo_sync enq.ready unexpectedly high during backpressure at sample {cycle}"
        await RisingEdge(ports.soc_clock)

    await with_timeout(stalled_task, 50000, "ns")

    for _ in range(120):
        await RisingEdge(ports.cpu_clock)

    eip0 = await read_csr(ports, 0x80)
    expected = sum(1 << irq for irq in range(1, stalled_irq + 1))
    assert (eip0 & expected) == expected, f"Backpressured write sequence lost MSI bits: eip0=0x{eip0:x}, expected mask=0x{expected:x}"


@cocotb.test()
async def burst_len4_first_beat_only_survives_fifo_backpressure(dut):
    ports, internal = await setup_testbench(dut)

    stop_watch = Event()
    leak_watch = cocotb.start_soon(watch_reggen_leak_while_blocked(ports, internal, stop_watch))

    first_beats = list(range(1, 21))
    tail_beats = [33, 34, 35, 36]
    for irq in first_beats:
        beats = [irq] + tail_beats
        result = await axi4_write_burst_first_beat(dut, ports, 0x0, beats)
        assert len(result["w_fire_ns"]) == len(beats), (
            f"Burst 0x{irq:x} did not send all beats: fired {len(result['w_fire_ns'])}, expected {len(beats)}"
        )

    stop_watch.set()
    observed_backpressure = await leak_watch
    assert observed_backpressure, "The len=4 burst stress never reached stage/fifo backpressure"

    for _ in range(200):
        await RisingEdge(ports.cpu_clock)

    eip0 = await read_csr(ports, 0x80)
    expected = sum(1 << irq for irq in first_beats)
    assert (eip0 & expected) == expected, (
        "Burst len=4 first-beat-only MSI sequence lost interrupt bits: "
        f"eip0=0x{eip0:x}, expected mask=0x{expected:x}"
    )

    unexpected_tail_mask = sum(1 << irq for irq in tail_beats)
    assert (eip0 & unexpected_tail_mask) == 0, (
        "Unexpected non-first-beat MSI bits observed: "
        f"eip0=0x{eip0:x}, tail_mask=0x{unexpected_tail_mask:x}"
    )

    dut._log.info(
        f"Observed len=4 burst completion with first-beat-only mask 0x{expected:08x} and eip0=0x{eip0:08x}"
    )


@cocotb.test()
async def outstanding_burst_keeps_progress_across_redbox_window(dut):
    ports, internal = await setup_testbench(dut)

    target_irq = 0xCF
    next_irq = 0xCE
    stop_watch = Event()
    redbox_watch = cocotb.start_soon(
        watch_redbox_response_progress(ports, internal, target_irq, stop_watch)
    )

    bursts = [[irq, 0x21, 0x22, 0x23, 0x24] for irq in range(0xD9, 0xCD, -1)]
    issued = await with_timeout(
        axi4_issue_bursts_without_waiting_b(dut, ports, 0x0, bursts),
        200000,
        "ns",
    )

    stop_watch.set()
    observed_window, observed_target = await redbox_watch
    results = {entry["first_beat"]: entry for entry in issued}

    assert observed_window, (
        "Never observed the red-box window where fifo_sync.io.enq.ready=0, "
        "axi4xbar.auto_out_0_b_ready=1, and stageReady=0"
    )
    assert observed_target, (
        f"Did not observe reggen.io.valid for 0x{target_irq:x} after the missed b_ready pulse was replayed"
    )
    assert results[target_irq]["b_valid_ns"] is not None, (
        f"Outstanding burst 0x{target_irq:x} never completed its B response"
    )
    assert results[next_irq]["aw_fire_ns"] is not None, (
        f"Next outstanding burst 0x{next_irq:x} never handshook AW"
    )
    assert len(results[next_irq]["w_fire_ns"]) == len(results[next_irq]["beats"]), (
        f"Next outstanding burst 0x{next_irq:x} did not send all W beats"
    )
    assert results[next_irq]["b_valid_ns"] is not None, (
        f"Next outstanding burst 0x{next_irq:x} never completed its B response"
    )

    dut._log.info(
        "Observed red-box recovery with target 0x%02x at B=%dns and next 0x%02x AW=%dns B=%dns"
        % (
            target_irq,
            results[target_irq]["b_valid_ns"],
            next_irq,
            results[next_irq]["aw_fire_ns"],
            results[next_irq]["b_valid_ns"],
        )
    )


@cocotb.test()
async def sticky_write_response_replays_target_once_only(dut):
    ports, internal = await setup_testbench(dut)

    target_irq = 0xCF
    next_irq = 0xCE
    stop_watch = Event()
    redbox_watch = cocotb.start_soon(
        watch_redbox_response_progress(ports, internal, target_irq, stop_watch)
    )
    pulse_watch = cocotb.start_soon(
        watch_reggen_irq_pulses(ports, internal, {target_irq}, stop_watch)
    )

    bursts = [[irq, 0x21, 0x22, 0x23, 0x24] for irq in range(0xD9, 0xCD, -1)]
    issued = await with_timeout(
        axi4_issue_bursts_without_waiting_b(dut, ports, 0x0, bursts),
        200000,
        "ns",
    )

    for _ in range(32):
        await RisingEdge(ports.soc_clock)

    stop_watch.set()
    observed_window, observed_target = await redbox_watch
    pulse_counts = await pulse_watch
    results = {entry["first_beat"]: entry for entry in issued}

    assert observed_window, (
        "Never observed the red-box window where fifo_sync.io.enq.ready=0, "
        "axi4xbar.auto_out_0_b_ready=1, and stageReady=0"
    )
    assert observed_target, (
        f"Did not observe reggen.io.valid for 0x{target_irq:x} after the missed b_ready pulse was replayed"
    )
    assert pulse_counts[target_irq] == 1, (
        f"Expected exactly one reggen.io.valid pulse for 0x{target_irq:x}, saw {pulse_counts[target_irq]}"
    )
    assert results[target_irq]["b_valid_ns"] is not None, (
        f"Outstanding burst 0x{target_irq:x} never completed its B response"
    )
    assert results[next_irq]["b_valid_ns"] is not None, (
        f"Outstanding burst 0x{next_irq:x} never completed its B response"
    )

    dut._log.info(
        "Observed sticky write-response replay once only: target 0x%02x pulses=%d next 0x%02x B=%dns"
        % (
            target_irq,
            pulse_counts[target_irq],
            next_irq,
            results[next_irq]["b_valid_ns"],
        )
    )


@cocotb.test()
async def m_full_then_switch_to_s_mode_stays_correct(dut):
    ports, _ = await setup_testbench(dut)

    s_addr = 0x10000

    await init_s_intfile(ports)

    await axi4_write32(dut, ports, 0x0, 1)
    m_stalled_irq, m_stalled_task = await find_stalled_write(dut, ports, 20, addr=0x0, first_irq=2)
    await with_timeout(m_stalled_task, 50000, "ns")
    for irq in range(m_stalled_irq + 1, 20 + 1):
        await axi4_write32(dut, ports, 0x0, irq)

    for _ in range(160):
        await RisingEdge(ports.cpu_clock)

    await select_m_intfile(ports)
    m_eip0 = await read_csr(ports, 0x80)
    expected_m = sum(1 << irq for irq in range(1, 20 + 1))
    assert (m_eip0 & expected_m) == expected_m, (
        f"M-mode writes lost pending bits after fifo full/not-full mix: eip0=0x{m_eip0:x}, expected mask=0x{expected_m:x}"
    )

    await select_s_intfile(ports)
    s_eip0_before = await read_csr(ports, 0x80)
    assert s_eip0_before == 0, (
        f"S-mode pending bits were polluted by earlier M-mode traffic: eip0=0x{s_eip0_before:x}"
    )

    await axi4_write32(dut, ports, s_addr, 21)
    s_stalled_irq, s_stalled_task = await find_stalled_write(dut, ports, 31, addr=s_addr, first_irq=22)
    await with_timeout(s_stalled_task, 50000, "ns")
    for irq in range(s_stalled_irq + 1, 31 + 1):
        await axi4_write32(dut, ports, s_addr, irq)

    for _ in range(160):
        await RisingEdge(ports.cpu_clock)

    await select_s_intfile(ports)
    s_eip0_after = await read_csr(ports, 0x80)
    expected_s = sum(1 << irq for irq in range(21, 31 + 1))
    assert (s_eip0_after & expected_s) == expected_s, (
        f"S-mode writes lost pending bits after mode switch and fifo full/not-full mix: eip0=0x{s_eip0_after:x}, expected mask=0x{expected_s:x}"
    )

    await select_m_intfile(ports)
    m_eip0_after = await read_csr(ports, 0x80)
    assert (m_eip0_after & expected_m) == expected_m, (
        f"Switching to S-mode disturbed M-mode pending bits: eip0=0x{m_eip0_after:x}, expected mask=0x{expected_m:x}"
    )

    dut._log.info(
        "Observed correct M/S split across fifo empty/full cases: M mask=0x%08x S mask=0x%08x"
        % (expected_m, expected_s)
    )


@cocotb.test()
async def m_to_s_switch_len4_first_beat_only_stays_correct(dut):
    ports, _ = await setup_testbench(dut)

    s_addr = 0x10000
    tail_beats = [33, 34, 35, 36]
    tail_mask = sum(1 << irq for irq in tail_beats)

    await init_s_intfile(ports)

    first_m = await axi4_write_burst_first_beat(dut, ports, 0x0, [1] + tail_beats)
    assert len(first_m["w_fire_ns"]) == 5, (
        f"Initial M-mode len=4 burst did not send all beats: fired {len(first_m['w_fire_ns'])}, expected 5"
    )

    m_stalled_irq, m_stalled_task = await find_stalled_burst_first_beat(
        dut,
        ports,
        20,
        tail_beats,
        addr=0x0,
        first_irq=2,
    )
    m_stalled_result = await with_timeout(m_stalled_task, 50000, "ns")
    assert len(m_stalled_result["w_fire_ns"]) == 5, (
        f"Backpressured M-mode len=4 burst 0x{m_stalled_irq:x} did not send all beats: fired {len(m_stalled_result['w_fire_ns'])}, expected 5"
    )
    for irq in range(m_stalled_irq + 1, 20 + 1):
        result = await axi4_write_burst_first_beat(dut, ports, 0x0, [irq] + tail_beats)
        assert len(result["w_fire_ns"]) == 5, (
            f"M-mode len=4 burst 0x{irq:x} did not send all beats: fired {len(result['w_fire_ns'])}, expected 5"
        )

    for _ in range(220):
        await RisingEdge(ports.cpu_clock)

    await select_m_intfile(ports)
    m_eip0 = await read_csr(ports, 0x80)
    expected_m = sum(1 << irq for irq in range(1, 20 + 1))
    assert (m_eip0 & expected_m) == expected_m, (
        "M-mode len=4 bursts lost first-beat pending bits after fifo full/not-full mix: "
        f"eip0=0x{m_eip0:x}, expected mask=0x{expected_m:x}"
    )
    assert (m_eip0 & tail_mask) == 0, (
        f"Unexpected M-mode non-first-beat pending bits observed: eip0=0x{m_eip0:x}, tail_mask=0x{tail_mask:x}"
    )

    await select_s_intfile(ports)
    s_eip0_before = await read_csr(ports, 0x80)
    assert s_eip0_before == 0, (
        f"S-mode pending bits were polluted by earlier M-mode len=4 traffic: eip0=0x{s_eip0_before:x}"
    )

    first_s = await axi4_write_burst_first_beat(dut, ports, s_addr, [21] + tail_beats)
    assert len(first_s["w_fire_ns"]) == 5, (
        f"Initial S-mode len=4 burst did not send all beats: fired {len(first_s['w_fire_ns'])}, expected 5"
    )

    s_stalled_irq, s_stalled_task = await find_stalled_burst_first_beat(
        dut,
        ports,
        31,
        tail_beats,
        addr=s_addr,
        first_irq=22,
    )
    s_stalled_result = await with_timeout(s_stalled_task, 50000, "ns")
    assert len(s_stalled_result["w_fire_ns"]) == 5, (
        f"Backpressured S-mode len=4 burst 0x{s_stalled_irq:x} did not send all beats: fired {len(s_stalled_result['w_fire_ns'])}, expected 5"
    )
    for irq in range(s_stalled_irq + 1, 31 + 1):
        result = await axi4_write_burst_first_beat(dut, ports, s_addr, [irq] + tail_beats)
        assert len(result["w_fire_ns"]) == 5, (
            f"S-mode len=4 burst 0x{irq:x} did not send all beats: fired {len(result['w_fire_ns'])}, expected 5"
        )

    for _ in range(220):
        await RisingEdge(ports.cpu_clock)

    await select_s_intfile(ports)
    s_eip0_after = await read_csr(ports, 0x80)
    expected_s = sum(1 << irq for irq in range(21, 31 + 1))
    assert (s_eip0_after & expected_s) == expected_s, (
        "S-mode len=4 bursts lost first-beat pending bits after mode switch and fifo full/not-full mix: "
        f"eip0=0x{s_eip0_after:x}, expected mask=0x{expected_s:x}"
    )
    assert (s_eip0_after & tail_mask) == 0, (
        f"Unexpected S-mode non-first-beat pending bits observed: eip0=0x{s_eip0_after:x}, tail_mask=0x{tail_mask:x}"
    )

    await select_m_intfile(ports)
    m_eip0_after = await read_csr(ports, 0x80)
    assert (m_eip0_after & expected_m) == expected_m, (
        "Switching to S-mode len=4 bursts disturbed M-mode first-beat pending bits: "
        f"eip0=0x{m_eip0_after:x}, expected mask=0x{expected_m:x}"
    )
    assert (m_eip0_after & tail_mask) == 0, (
        f"Unexpected M-mode tail bits appeared after S-mode len=4 bursts: eip0=0x{m_eip0_after:x}, tail_mask=0x{tail_mask:x}"
    )

    dut._log.info(
        "Observed correct M/S split for len=4 bursts across fifo empty/full cases: M mask=0x%08x S mask=0x%08x"
        % (expected_m, expected_s)
    )