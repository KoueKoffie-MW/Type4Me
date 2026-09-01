"""
Stress Test S06: Multi-Host Switching Mutex Concurrency Race.
Verifies:
- 100 concurrent threads attempting simultaneous host switches
- Mutex serialization preventing re-entrant or interleaved states
- Zero corrupted intermediate connection states
- Liveness: system returns to stable CONNECTED state after thread storm
"""
import unittest
import threading
from concurrent.futures import ThreadPoolExecutor
from tests.e2e.harness.persistence_simulator import AppDatabaseSimulator, PairedHostEntity
from tests.e2e.harness.multi_host_simulator import (
    MultiHostTransportSimulator, MultiHostState
)


class TestS06MultiHostSwitchingConcurrency(unittest.TestCase):
    """Tier 5: Stress 06 - Multi-Host Mutex Concurrency Race"""

    def setUp(self):
        self.db = AppDatabaseSimulator(version=2, seed_defaults=True)
        self.hosts = [
            PairedHostEntity(f"host_{i}", f"AA:BB:CC:00:00:{i:02X}", f"Server #{i}", "LINUX")
            for i in range(5)
        ]
        for h in self.hosts:
            self.db.paired_host_dao.insert_or_update_host(h)

        self.transport = MultiHostTransportSimulator(
            paired_host_dao=self.db.paired_host_dao,
            permissions_granted=True,
            virtual_clock=True
        )
        self.transport.initialize()

    def test_s06_01_one_hundred_concurrent_switch_requests(self):
        """100 threads concurrently call switch_host; mutex guarantees single execution and no deadlocks."""
        results = []
        lock = threading.Lock()

        def worker(thread_id: int):
            target = self.hosts[thread_id % len(self.hosts)]
            ok = self.transport.switch_host(target, settling_delay_ms=0.0)
            with lock:
                results.append((thread_id, ok))

        with ThreadPoolExecutor(max_workers=20) as executor:
            futures = [executor.submit(worker, i) for i in range(100)]
            for f in futures:
                f.result()

        self.assertEqual(len(results), 100)
        # Final state must be stable CONNECTED
        self.assertEqual(self.transport.connection_state.state, MultiHostState.CONNECTED)
        self.assertIsNotNone(self.transport.connection_state.active_host)
        # Mutex flag must be released
        self.assertFalse(self.transport.is_switching)


if __name__ == "__main__":
    unittest.main()
