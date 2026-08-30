"""
Combination Test C10: Host CapsLock LED Sync + Dynamic Keymap Inversion.
Verifies host LED output reports (CapsLock state) updating the host decoder
and validating letter case behavior under active CapsLock.
"""
import unittest
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.hid_constants import LED_CAPS_LOCK


class TestCombination10LedCapsLock(unittest.TestCase):
    """Tier 3: Combination 10 - Host CapsLock LED Sync + Dynamic Keymap Inversion"""

    def setUp(self):
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_capslock_off_default_case(self):
        """TC01: With CapsLock OFF, lowercase letters are received as lowercase."""
        self.host.set_caps_lock(False)
        self.dispatcher.dispatch_burst("text")
        self.assertEqual(self.host.host_text, "text")

    def test_tc02_capslock_on_inverts_letter_case(self):
        """TC02: With CapsLock ON, unshifted letters are received as uppercase on host."""
        self.host.set_caps_lock(True)
        self.dispatcher.dispatch_burst("text")
        self.assertEqual(self.host.host_text, "TEXT")

    def test_tc03_capslock_toggled_mid_stream(self):
        """TC03: Toggling CapsLock mid-stream correctly switches letter case output."""
        self.host.set_caps_lock(False)
        self.dispatcher.dispatch_burst("teil1_")
        self.host.set_caps_lock(True)
        self.dispatcher.dispatch_burst("teil2")
        self.assertEqual(self.host.host_text, "teil1_TEIL2")


if __name__ == "__main__":
    unittest.main()
