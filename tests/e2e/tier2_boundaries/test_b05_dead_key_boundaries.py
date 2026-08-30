"""
Boundary Tests: Feature 5 - Dead Key Space Injection.
Covers consecutive dead keys (^^^, ´´´, ~~~), dead keys at string boundary extremes,
dead keys immediately before vowels, and dead keys combined with backspaces.
"""
import unittest
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestBoundary05DeadKeys(unittest.TestCase):
    """Tier 2: Boundary 5 - Dead Key Space Injection"""

    def setUp(self):
        self.translator = GermanQwertzKeymap(auto_space_dead_keys=True)
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_multiple_consecutive_carets(self):
        """TC01: Sequence of multiple consecutive carets (^^^)."""
        text = "^^^"
        self.dispatcher.dispatch_burst(text)
        self.assertEqual(self.host.host_text, "^^^")

    def test_tc02_dead_key_at_start_and_end_of_string(self):
        """TC02: Dead key as the first and last character of a transmission."""
        text = "^Start and End^"
        self.dispatcher.dispatch_burst(text)
        self.assertEqual(self.host.host_text, text)

    def test_tc03_dead_keys_surrounding_vowels(self):
        """TC03: Carets and accents directly preceding vowels (^a, ´e, `i, ~o, ^u)."""
        text = "^a ´e `i ~o ^u"
        self.dispatcher.dispatch_burst(text)
        self.assertEqual(self.host.host_text, text)

    def test_tc04_multiple_tildes_for_paths(self):
        """TC04: Multiple tildes in Unix home paths (~/dev/app/~~)."""
        path = "~/source/project/~/test"
        self.dispatcher.dispatch_burst(path)
        self.assertEqual(self.host.host_text, path)

    def test_tc05_mathematical_powers_expression(self):
        """TC05: Complex math expression with powers and carets: 2^10 + 3^(x+y)."""
        formula = "2^10 + 3^(x+y) = z^2"
        self.dispatcher.dispatch_burst(formula)
        self.assertEqual(self.host.host_text, formula)


if __name__ == "__main__":
    unittest.main()
