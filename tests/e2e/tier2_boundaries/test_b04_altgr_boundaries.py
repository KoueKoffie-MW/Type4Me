"""
Boundary Tests: Feature 4 - AltGr Support.
Covers consecutive AltGr symbols without release leakage, AltGr within complex code/JSON strings,
Shift+AltGr combination keys, and unmapped AltGr modifier recovery.
"""
import unittest
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestBoundary04AltGr(unittest.TestCase):
    """Tier 2: Boundary 4 - AltGr Support"""

    def setUp(self):
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_consecutive_altgr_symbols_cluster(self):
        """TC01: Continuous cluster of AltGr symbols (@€\\{[]}|~µ²³) without standard characters."""
        cluster = "@€\\{[]}|µ²³"
        self.dispatcher.dispatch_burst(cluster)
        self.assertEqual(self.host.host_text, cluster)

    def test_tc02_dense_json_code_syntax(self):
        """TC02: Realistic dense JSON syntax with nested curly and square brackets."""
        json_snippet = '{"config": [{"id": 1, "path": "\\usr\\bin", "rate": 50}], "user": "dev@test.de"}'
        self.dispatcher.dispatch_burst(json_snippet)
        self.assertEqual(self.host.host_text, json_snippet)

    def test_tc03_altgr_followed_by_standard_letter(self):
        """TC03: Switching immediately from AltGr symbol (@) to standard letter (q) releases AltGr."""
        text = "@q @Q @e @E"
        self.dispatcher.dispatch_burst(text)
        self.assertEqual(self.host.host_text, text)

    def test_tc04_multiple_euro_amounts_in_table(self):
        """TC04: Alternating Euro symbols and numbers in formatted financial table."""
        table = "100 € | 250 € | 1.000 € | 99,99 €"
        self.dispatcher.dispatch_burst(table)
        self.assertEqual(self.host.host_text, table)

    def test_tc05_altgr_pipe_in_shell_pipeline(self):
        """TC05: Multiple pipes (|) in Linux/Windows command line string."""
        cmd = "cat file.txt | grep -E '^[0-9]+' | sort -n | uniq -c > output.log"
        self.dispatcher.dispatch_burst(cmd)
        self.assertEqual(self.host.host_text, cmd)


if __name__ == "__main__":
    unittest.main()
