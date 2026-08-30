"""
Combination Test C04: Dead Keys + Live Diff Editing.
Verifies live streaming edits of mathematical powers and accented expressions
involving dead keys (^, ´, `, ~) and backspace deletions on German layout.
"""
import unittest
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestCombination04DeadKeyLiveDiff(unittest.TestCase):
    """Tier 3: Combination 4 - Dead Keys + Live Diff Editing"""

    def setUp(self):
        self.translator = GermanQwertzKeymap(auto_space_dead_keys=True)
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_live_editing_power_exponent(self):
        """TC01: Live revision of math formula: 'f(x) = x^2' to 'f(x) = x^3'."""
        f1 = "f(x) = x^2"
        f2 = "f(x) = x^3"
        self.dispatcher.dispatch_live_diff(f1)
        self.assertEqual(self.host.host_text, f1)
        self.dispatcher.dispatch_live_diff(f2)
        self.assertEqual(self.host.host_text, f2)

    def test_tc02_live_editing_tilde_approximation(self):
        """TC02: Live correction of approximation formula with tildes."""
        t1 = "pi ~ 3.14"
        t2 = "pi ~ 3.14159"
        self.dispatcher.dispatch_live_diff(t1)
        self.assertEqual(self.host.host_text, t1)
        self.dispatcher.dispatch_live_diff(t2)
        self.assertEqual(self.host.host_text, t2)

    def test_tc03_erasing_dead_key_formula_to_empty(self):
        """TC03: Erasing formula with dead keys completely to empty string."""
        expr = "x^2 + y^2 = r^2"
        self.dispatcher.dispatch_live_diff(expr)
        self.assertEqual(self.host.host_text, expr)
        self.dispatcher.dispatch_live_diff("")
        self.assertEqual(self.host.host_text, "")


if __name__ == "__main__":
    unittest.main()
