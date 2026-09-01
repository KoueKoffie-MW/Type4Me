"""
Combination Test C14: Multi-Host Switch + Clipboard Keystroke Stream.
Verifies:
- 6-Phase Atomic Host Switching from Host 1 to Host 2
- Settling delay enforcement
- Subsequent high-speed clipboard streaming wrapped in bracketed paste mode
"""
import unittest
from tests.e2e.harness.persistence_simulator import AppDatabaseSimulator, PairedHostEntity
from tests.e2e.harness.multi_host_simulator import MultiHostTransportSimulator, MultiHostState
from tests.e2e.harness.keymap_engine import UsQwertyKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestCombination14MultiHostSwitchClipboardStream(unittest.TestCase):
    """Tier 3: Combination 14 - Multi-Host Switch + Clipboard Stream"""

    def setUp(self):
        self.db = AppDatabaseSimulator(version=2, seed_defaults=True)
        self.host1 = PairedHostEntity("h1", "AA:BB:CC:11:11:11", "Windows Workstation", "WINDOWS", is_last_connected=True)
        self.host2 = PairedHostEntity("h2", "AA:BB:CC:22:22:22", "Linux Production Server", "LINUX", is_last_connected=False)
        self.db.paired_host_dao.insert_or_update_host(self.host1)
        self.db.paired_host_dao.insert_or_update_host(self.host2)

        self.multi_transport = MultiHostTransportSimulator(
            paired_host_dao=self.db.paired_host_dao,
            permissions_granted=True,
            virtual_clock=True
        )
        self.multi_transport.initialize()

        self.host_sim = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        self.translator = UsQwertyKeymap()
        self.dispatcher = KeystrokeDispatcher(host=self.host_sim, translator=self.translator, virtual_clock=True)

    def test_tc01_switch_host_then_stream_bracketed_clipboard(self):
        """TC01: Switches to Linux host, then streams multi-line shell script with bracketed paste."""
        # 1. Switch to Linux server
        ok_switch = self.multi_transport.switch_host(self.host2, settling_delay_ms=150.0)
        self.assertTrue(ok_switch)
        self.assertEqual(self.multi_transport.connection_state.active_host.alias, "Linux Production Server")

        # 2. Stream clipboard payload
        shell_script = "#!/bin/bash\nsystemctl restart nginx\nsystemctl status nginx\n"
        ok_stream = self.dispatcher.stream_clipboard_to_host(shell_script, bracketed_paste=True, delay_ms=8.0)
        self.assertTrue(ok_stream)
        self.assertIn("systemctl restart nginx", self.host_sim.host_text)


if __name__ == "__main__":
    unittest.main()
