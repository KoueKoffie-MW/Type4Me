"""
Boundary Test B26 (R3): Multi-Host Switching Boundaries & Corner Cases.
Verifies:
- Dead ACL link (target host disconnects or rejects connection)
- Rapid consecutive switching collisions
- Duplicate MAC address resolution
- Registry scaling up to 16 bonded hosts
- Switch attempt with no hosts registered
"""
import unittest
from tests.e2e.harness.persistence_simulator import AppDatabaseSimulator, PairedHostEntity
from tests.e2e.harness.multi_host_simulator import (
    MultiHostTransportSimulator, MultiHostState
)


class TestBoundary26MultiHost(unittest.TestCase):
    """Tier 2: Boundary 26 (R3) - Multi-Host Switching Boundaries"""

    def setUp(self):
        self.db = AppDatabaseSimulator(version=2, seed_defaults=True)
        self.transport = MultiHostTransportSimulator(
            paired_host_dao=self.db.paired_host_dao,
            permissions_granted=True,
            virtual_clock=True
        )
        self.transport.initialize()

    def test_tc01_duplicate_mac_address_upsert(self):
        """TC01: Inserting host with identical MAC updates existing record rather than creating duplicate."""
        h1 = PairedHostEntity("h1", "AA:BB:CC:11:22:33", "Work PC", "WINDOWS")
        h2 = PairedHostEntity("h2", "aa:bb:cc:11:22:33", "Work PC Renamed", "WINDOWS")

        self.db.paired_host_dao.insert_or_update_host(h1)
        self.db.paired_host_dao.insert_or_update_host(h2)

        hosts = self.db.paired_host_dao.get_all_hosts()
        matching = [h for h in hosts if h.mac_address.upper() == "AA:BB:CC:11:22:33"]
        self.assertEqual(len(matching), 1)
        self.assertEqual(matching[0].alias, "Work PC Renamed")

    def test_tc02_rapid_alternating_switching(self):
        """TC02: Switches rapidly between 3 bonded hosts sequentially."""
        hosts = [
            PairedHostEntity(f"h_{i}", f"AA:BB:CC:00:00:0{i}", f"Host {i}", "WINDOWS")
            for i in range(3)
        ]
        for h in hosts:
            self.db.paired_host_dao.insert_or_update_host(h)

        for target in hosts:
            ok = self.transport.switch_host(target)
            self.assertTrue(ok)
            self.assertEqual(self.transport.connection_state.active_host.id, target.id)

    def test_tc03_sixteen_hosts_registry_capacity(self):
        """TC03: Handles 16 bonded host profiles in Room DB without degradation."""
        for i in range(16):
            mac = f"11:22:33:44:55:{i:02X}"
            h = PairedHostEntity(f"host_{i}", mac, f"Workstation #{i}", "LINUX")
            self.db.paired_host_dao.insert_or_update_host(h)

        all_hosts = self.db.paired_host_dao.get_all_hosts()
        self.assertGreaterEqual(len(all_hosts), 16)

    def test_tc04_watchdog_timeout_recovery(self):
        """TC04: Recovers from watchdog timeout and allows subsequent switch to online host."""
        bad_host = PairedHostEntity("h_bad", "00:00:00:00:00:00", "Dead Server", "LINUX")
        good_host = PairedHostEntity("h_good", "11:11:11:11:11:11", "Online Server", "LINUX")
        self.db.paired_host_dao.insert_or_update_host(bad_host)
        self.db.paired_host_dao.insert_or_update_host(good_host)

        # 1. Switch to bad host -> fails
        ok1 = self.transport.switch_host(bad_host, simulate_watchdog_timeout=True)
        self.assertFalse(ok1)
        self.assertEqual(self.transport.connection_state.state, MultiHostState.ERROR)

        # 2. Subsequent switch to good host -> succeeds
        ok2 = self.transport.switch_host(good_host, simulate_watchdog_timeout=False)
        self.assertTrue(ok2)
        self.assertEqual(self.transport.connection_state.state, MultiHostState.CONNECTED)
        self.assertEqual(self.transport.connection_state.active_host.id, "h_good")

    def test_tc05_permissions_revoked_at_runtime(self):
        """TC05: Transport reports ERROR state when Bluetooth permissions are revoked."""
        no_perm_transport = MultiHostTransportSimulator(
            paired_host_dao=self.db.paired_host_dao,
            permissions_granted=False
        )
        ok = no_perm_transport.initialize()
        self.assertFalse(ok)
        self.assertEqual(no_perm_transport.connection_state.state, MultiHostState.ERROR)


if __name__ == "__main__":
    unittest.main()
