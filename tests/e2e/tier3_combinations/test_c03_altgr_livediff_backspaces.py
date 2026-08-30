"""
Combination Test C03: AltGr Symbols + Live Diff Backspaces.
Verifies real-time streaming edits of AltGr-heavy strings (email addresses, JSON, paths)
with dynamic backspacing and AltGr modifier state management.
"""
import unittest
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestCombination03AltGrLiveDiffBackspaces(unittest.TestCase):
    """Tier 3: Combination 3 - AltGr Symbols + Live Diff Backspaces"""

    def setUp(self):
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_live_email_domain_correction(self):
        """TC01: Live revision of email address containing AltGr @ sign: dev@gmail.com -> dev@company.de."""
        s1 = "Kontakt: info@oldcorp.com"
        s2 = "Kontakt: info@newcompany.de"
        self.dispatcher.dispatch_live_diff(s1)
        self.assertEqual(self.host.host_text, s1)
        self.dispatcher.dispatch_live_diff(s2)
        self.assertEqual(self.host.host_text, s2)

    def test_tc02_live_json_structure_editing(self):
        """TC02: Live editing of JSON brackets { } and [ ] with backspaces."""
        json_a = '{"port": 8080}'
        json_b = '{"ports": [8080, 8081]}'
        self.dispatcher.dispatch_live_diff(json_a)
        self.assertEqual(self.host.host_text, json_a)
        self.dispatcher.dispatch_live_diff(json_b)
        self.assertEqual(self.host.host_text, json_b)

    def test_tc03_live_currency_and_measurement_edits(self):
        """TC03: Live revision of price and unit containing € and µm²."""
        m1 = "Fläche: 100 µm² | Preis: 50 €"
        m2 = "Fläche: 250 µm² | Preis: 120 €"
        self.dispatcher.dispatch_live_diff(m1)
        self.dispatcher.dispatch_live_diff(m2)
        self.assertEqual(self.host.host_text, m2)


if __name__ == "__main__":
    unittest.main()
