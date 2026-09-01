"""
Multi-Host Bluetooth HID Transport & 6-Phase Atomic Switching Protocol Simulator.
Accurately implements:
- Persistent PairedHostEntity registry
- 154-byte composite descriptor registration
- 6-Phase Atomic Host Switching Protocol:
  * Phase 1: Flush & Zero Release Report [0,0,0,0,0,0,0,0] (Report ID 1)
  * Phase 2: Disconnect active host L2CAP channels
  * Phase 3: 150ms settling delay guard
  * Phase 4: State transition to SWITCHING(target)
  * Phase 5: Connect target host L2CAP channels
  * Phase 6: Dead-link watchdog (1000ms timeout)
- Mutex serialization to prevent concurrent switching race conditions
"""
from dataclasses import dataclass
from enum import Enum
from typing import List, Optional, Callable, Dict, Any
import time

from .hid_constants import COMPOSITE_154_BYTE_REPORT_DESCRIPTOR, SDP_SUBCLASS_COMBO
from .persistence_simulator import PairedHostEntity, PairedHostDaoSimulator
from .keymap_engine import HidReport


class MultiHostState(Enum):
    DISCONNECTED = "DISCONNECTED"
    CONNECTED = "CONNECTED"
    SWITCHING = "SWITCHING"
    ERROR = "ERROR"


@dataclass
class MultiHostConnectionState:
    state: MultiHostState
    active_host: Optional[PairedHostEntity] = None
    target_host: Optional[PairedHostEntity] = None
    phase: int = 0
    error_message: Optional[str] = None


class MultiHostTransportSimulator:
    """
    Simulates BluetoothHidDevice transport with multi-host management and 6-phase switching.
    """

    def __init__(
        self,
        paired_host_dao: PairedHostDaoSimulator,
        permissions_granted: bool = True,
        virtual_clock: bool = True
    ):
        self.paired_host_dao = paired_host_dao
        self.permissions_granted = permissions_granted
        self.virtual_clock = virtual_clock
        self.virtual_time = 0.0

        self.is_registered = False
        self.descriptor = COMPOSITE_154_BYTE_REPORT_DESCRIPTOR
        self.is_switching = False  # Mutex flag
        self.connection_state = MultiHostConnectionState(state=MultiHostState.DISCONNECTED)
        self._listeners: List[Callable[[MultiHostConnectionState], None]] = []
        self.phase_log: List[Dict[str, Any]] = []

    def _now(self) -> float:
        if self.virtual_clock:
            return self.virtual_time
        return time.time()

    def _advance_time(self, ms: float):
        if self.virtual_clock:
            self.virtual_time += (ms / 1000.0)
        else:
            time.sleep(ms / 1000.0)

    def add_state_listener(self, listener: Callable[[MultiHostConnectionState], None]):
        self._listeners.append(listener)
        listener(self.connection_state)

    def _emit_state(self, **kwargs):
        data = {
            "state": self.connection_state.state,
            "active_host": self.connection_state.active_host,
            "target_host": self.connection_state.target_host,
            "phase": self.connection_state.phase,
            "error_message": self.connection_state.error_message
        }
        data.update(kwargs)
        self.connection_state = MultiHostConnectionState(**data)
        for l in self._listeners:
            l(self.connection_state)

    def initialize(self) -> bool:
        if not self.permissions_granted:
            self._emit_state(state=MultiHostState.ERROR, error_message="Bluetooth permissions denied")
            return False

        self.is_registered = True
        # Check if there is a last connected host
        last_host = self.paired_host_dao.get_last_connected_host()
        if last_host:
            self._emit_state(state=MultiHostState.CONNECTED, active_host=last_host)
        else:
            self._emit_state(state=MultiHostState.DISCONNECTED)
        return True

    def switch_host(
        self,
        target_host: PairedHostEntity,
        simulate_watchdog_timeout: bool = False,
        settling_delay_ms: float = 150.0,
        watchdog_timeout_ms: float = 1000.0
    ) -> bool:
        """
        Executes the 6-Phase Atomic Host Switching Protocol:
        1. Mutex Lock & Flush/Zero Release
        2. Disconnect active host L2CAP
        3. Settling delay guard (150ms)
        4. State update: SWITCHING(target)
        5. Reconnect to target host L2CAP
        6. Watchdog verification
        """
        if self.is_switching:
            # Mutex acquisition failed -> concurrent switch rejected
            return False

        self.is_switching = True
        current_host = self.connection_state.active_host

        try:
            # --- Phase 1: Flush & Zero-Modifier Release Report ---
            self.phase_log.append({
                "phase": 1,
                "name": "FLUSH_ZERO_RELEASE",
                "host": current_host.alias if current_host else None,
                "timestamp": self._now()
            })
            self._emit_state(
                state=MultiHostState.SWITCHING,
                active_host=current_host,
                target_host=target_host,
                phase=1
            )

            # --- Phase 2: Disconnect active host L2CAP channels ---
            self.phase_log.append({
                "phase": 2,
                "name": "DISCONNECT_ACTIVE",
                "host": current_host.alias if current_host else None,
                "timestamp": self._now()
            })
            self._emit_state(phase=2)

            # --- Phase 3: Settling Delay Guard (150ms) ---
            self.phase_log.append({
                "phase": 3,
                "name": "SETTLING_GUARD",
                "duration_ms": settling_delay_ms,
                "timestamp": self._now()
            })
            self._advance_time(settling_delay_ms)
            self._emit_state(phase=3)

            # --- Phase 4: State Transition to SWITCHING(target) ---
            self.phase_log.append({
                "phase": 4,
                "name": "TRANSITION_TARGET",
                "target": target_host.alias,
                "timestamp": self._now()
            })
            self._emit_state(phase=4)

            # --- Phase 5: Connect target host L2CAP ---
            self.phase_log.append({
                "phase": 5,
                "name": "CONNECT_TARGET_L2CAP",
                "target": target_host.alias,
                "timestamp": self._now()
            })
            self._emit_state(phase=5)

            # --- Phase 6: Dead-Link Watchdog Verification ---
            if simulate_watchdog_timeout:
                self._advance_time(watchdog_timeout_ms)
                self.phase_log.append({
                    "phase": 6,
                    "name": "WATCHDOG_TIMEOUT",
                    "target": target_host.alias,
                    "timestamp": self._now()
                })
                self._emit_state(
                    state=MultiHostState.ERROR,
                    active_host=None,
                    target_host=target_host,
                    phase=6,
                    error_message=f"Connection to {target_host.alias} timed out after {watchdog_timeout_ms}ms"
                )
                return False

            # Successful connection
            self.paired_host_dao.set_last_connected(target_host.id)
            self.phase_log.append({
                "phase": 6,
                "name": "WATCHDOG_PASS_CONNECTED",
                "active": target_host.alias,
                "timestamp": self._now()
            })
            self._emit_state(
                state=MultiHostState.CONNECTED,
                active_host=target_host,
                target_host=None,
                phase=6,
                error_message=None
            )
            return True

        finally:
            self.is_switching = False
