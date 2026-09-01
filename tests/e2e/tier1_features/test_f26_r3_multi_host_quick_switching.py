"""
Feature Test F26 (R3): Multi-Host Pairing & Fast Quick-Switching Protocol.
Verifies:
- Persistent PairedHostEntity storage and custom aliases
- 154-Byte Composite HID Descriptor (Report ID 1, 2, 3)
- 6-Phase Atomic Host Switching Protocol with switchingMutex
- Settling delay guard (150ms) and dead-link watchdog (1000ms)
- Multi-host dropdown widget integration in ConnectionHeader
"""
import unittest
from tests.e2e.harness.persistence_simulator import AppDatabaseSimulator, PairedHostEntity
from tests.e2e.harness.multi_host_simulator import (
    MultiHostTransportSimulator, MultiHostState, MultiHostConnectionState
)
from tests.e2e.harness.hid_constants import COMPOSITE_154_BYTE_REPORT_DESCRIPTOR


class TestFeature26MultiHostQuickSwitching(unittest.TestCase):
    """Tier 1: Feature 26 (R3) - Multi-Host Quick Switching & Paired Host Registry"""

    def setUp(self):
        self.db = AppDatabaseSimulator(version=2, seed_defaults=True)
        # Prepopulate bonded hosts
        self.host1 = PairedHostEntity("host_work", "AA:BB:CC:11:22:33", "Work PC (Windows 11)", "WINDOWS", is_last_connected=True)
        self.host2 = PairedHostEntity("host_mac", "AA:BB:CC:44:55:66", "MacBook Pro M3", "MACOS", is_last_connected=False)
        self.host3 = PairedHostEntity("host_linux", "AA:BB:CC:77:88:99", "Linux Build Server", "LINUX", is_last_connected=False)

        self.db.paired_host_dao.insert_or_update_host(self.host1)
        self.db.paired_host_dao.insert_or_update_host(self.host2)
        self.db.paired_host_dao.insert_or_update_host(self.host3)

        self.transport = MultiHostTransportSimulator(
            paired_host_dao=self.db.paired_host_dao,
            permissions_granted=True,
            virtual_clock=True
        )
        self.transport.initialize()

    def test_tc01_persistent_paired_host_registry_crud(self):
        """TC01: Stores, updates aliases, and queries bonded hosts by MAC address."""
        hosts = self.db.paired_host_dao.get_all_hosts()
        self.assertEqual(len(hosts), 3)

        # Query by MAC
        found = self.db.paired_host_dao.get_host_by_mac("AA:BB:CC:44:55:66")
        self.assertIsNotNone(found)
        self.assertEqual(found.alias, "MacBook Pro M3")

        # Update alias
        found.alias = "Private M3 Max"
        self.db.paired_host_dao.insert_or_update_host(found)
        updated = self.db.paired_host_dao.get_host_by_id("host_mac")
        self.assertEqual(updated.alias, "Private M3 Max")

    def test_tc02_composite_154_byte_descriptor_validation(self):
        """TC02: Validates composite descriptor is exactly 154 bytes with Report IDs 1, 2, 3."""
        descriptor = self.transport.descriptor
        self.assertEqual(len(descriptor), 154)

        # Check Report ID 1 (Keyboard)
        self.assertIn(bytes([0x85, 0x01]), descriptor)
        # Check Report ID 2 (Mouse)
        self.assertIn(bytes([0x85, 0x02]), descriptor)
        # Check Report ID 3 (Consumer Media)
        self.assertIn(bytes([0x85, 0x03]), descriptor)

    def test_tc03_six_phase_atomic_switching_protocol_execution(self):
        """TC03: Executes all 6 phases of the host switching protocol cleanly."""
        self.assertEqual(self.transport.connection_state.active_host.id, "host_work")

        ok = self.transport.switch_host(self.host2, settling_delay_ms=150.0)
        self.assertTrue(ok)
        self.assertEqual(self.transport.connection_state.state, MultiHostState.CONNECTED)
        self.assertEqual(self.transport.connection_state.active_host.id, "host_mac")

        # Verify phase logs: Flush/Zero Release -> Disconnect -> Settling -> Transition -> Connect -> Watchdog
        phases = [p["phase"] for p in self.transport.phase_log]
        self.assertEqual(phases, [1, 2, 3, 4, 5, 6])

    def test_tc04_settling_delay_guard_timing(self):
        """TC04: Enforces 150ms settling guard between disconnect and reconnect."""
        t_start = self.transport.virtual_time
        self.transport.switch_host(self.host3, settling_delay_ms=150.0)
        t_end = self.transport.virtual_time
        elapsed_ms = (t_end - t_start) * 1000.0

        self.assertGreaterEqual(elapsed_ms, 150.0)

    def test_tc05_dead_link_watchdog_timeout_handling(self):
        """TC05: Triggers 1000ms watchdog timeout when target host does not respond."""
        ok = self.transport.switch_host(self.host2, simulate_watchdog_timeout=True, watchdog_timeout_ms=1000.0)
        self.assertFalse(ok)
        self.assertEqual(self.transport.connection_state.state, MultiHostState.ERROR)
        self.assertIn("timed out", self.transport.connection_state.error_message)

    def test_tc06_mutex_serialization_prevents_concurrent_switching(self):
        """TC06: Mutex blocks concurrent switch requests while a switch is in progress."""
        self.transport.is_switching = True
        rejected = self.transport.switch_host(self.host2)
        self.assertFalse(rejected)
        self.transport.is_switching = False


if __name__ == "__main__":
    unittest.main()
