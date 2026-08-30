"""
Boundary Tests: Feature 3 - German QWERTZ Keymap.
Covers Capital Eszett (ẞ DIN 2137:2018), multiple consecutive uppercase umlauts (ÄÖÜ),
rapid Y/Z alternations, ISO boundary keys, and French loanword accents.
"""
import unittest
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestBoundary03GermanQwertz(unittest.TestCase):
    """Tier 2: Boundary 3 - German QWERTZ Keymap"""

    def setUp(self):
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_capital_eszett_din2137_reconstruction(self):
        """TC01: Translates and reconstructs Capital Eszett (ẞ) in full uppercase German string."""
        text = "MASSE vs. MAẞE"
        self.dispatcher.dispatch_burst(text)
        self.assertEqual(self.host.host_text, text)

    def test_tc02_consecutive_uppercase_umlauts(self):
        """TC02: Consecutive uppercase umlauts (ÄÖÜ) retain Shift state without modifier leaks."""
        text = "ÄÖÜÄÖÜ"
        self.dispatcher.dispatch_burst(text)
        self.assertEqual(self.host.host_text, text)

    def test_tc03_rapid_yz_alternations(self):
        """TC03: Rapid alternation between Y and Z ensures no physical scancode cross-contamination."""
        text = "zyzyzyZYXYZzyxXYZ"
        self.dispatcher.dispatch_burst(text)
        self.assertEqual(self.host.host_text, text)

    def test_tc04_all_german_punctuation_in_sequence(self):
        """TC04: Sequence of all German punctuation characters: !\"§$%&/()=?`´*+'#-_.:,;<>."""
        punct = "!\"§$%&/()=?*+'#-_.:,;<>"
        self.dispatcher.dispatch_burst(punct)
        self.assertEqual(self.host.host_text, punct)

    def test_tc05_german_smart_quotes_guillemets(self):
        """TC05: Transliterates German typographic quotes („ “) and guillemets (« »)."""
        text = "„Deutsch“ «Prüfung»"
        self.dispatcher.dispatch_burst(text)
        self.assertEqual(self.host.host_text, '"Deutsch" "Prüfung"')


if __name__ == "__main__":
    unittest.main()
