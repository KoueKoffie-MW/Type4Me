"""
Combination Test C11: Rapid Keymap and Mode Switching.
Verifies dynamic switching between US QWERTY and German QWERTZ layouts,
and toggling between Burst and Live Diff modes during an active session.
"""
import unittest
from tests.e2e.harness.keymap_engine import UsQwertyKeymap, GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestCombination11RapidKeymapModeSwitching(unittest.TestCase):
    """Tier 3: Combination 11 - Rapid Keymap and Mode Switching"""

    def setUp(self):
        self.us_map = UsQwertyKeymap()
        self.de_map = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.de_map)

    def test_tc01_switch_layout_german_to_us_mid_session(self):
        """TC01: Switches layout from German QWERTZ to US QWERTY and verifies host reception."""
        # 1. Type on German
        self.dispatcher.dispatch_burst("Zahl: 100")
        self.assertEqual(self.host.host_text, "Zahl: 100")

        # 2. Switch host and dispatcher to US QWERTY
        self.host.layout = KeyLayout.US_QWERTY
        self.dispatcher.set_translator(self.us_map)

        # 3. Type on US QWERTY
        self.dispatcher.dispatch_burst(" | Price: $100")
        self.assertEqual(self.host.host_text, "Zahl: 100 | Price: $100")

    def test_tc02_alternate_burst_and_live_diff_modes(self):
        """TC02: Alternating between Burst and Live Diff modes maintains exact state."""
        # Burst 1
        self.dispatcher.dispatch_burst("Satz 1. ")
        self.assertEqual(self.host.host_text, "Satz 1. ")

        # Live Diff update
        self.dispatcher.dispatch_live_diff("Satz 1. Satz 2")
        self.assertEqual(self.host.host_text, "Satz 1. Satz 2")

        # Burst 2
        self.dispatcher.dispatch_burst(". Satz 3 fertig.")
        self.assertEqual(self.host.host_text, "Satz 1. Satz 2. Satz 3 fertig.")

    def test_tc03_rapid_layout_churn_without_corruption(self):
        """TC03: 10 rapid switches between US and DE layouts typing test strings."""
        for i in range(10):
            if i % 2 == 0:
                self.host.reset(KeyLayout.GERMAN_QWERTZ)
                self.dispatcher.reset_state()
                self.dispatcher.set_translator(self.de_map)
                self.dispatcher.dispatch_burst("Überprüfung")
                self.assertEqual(self.host.host_text, "Überprüfung")
            else:
                self.host.reset(KeyLayout.US_QWERTY)
                self.dispatcher.reset_state()
                self.dispatcher.set_translator(self.us_map)
                self.dispatcher.dispatch_burst("Verification")
                self.assertEqual(self.host.host_text, "Verification")


if __name__ == "__main__":
    unittest.main()
