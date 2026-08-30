"""
Workload Scenario W05: Multilingual Code-Switching Dictation.
Simulates a bilingual German/English technical dictation mixing German prose
with English software terms, Git commands, and file paths.
"""
import unittest
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestWorkload05MultilingualCodeSwitching(unittest.TestCase):
    """Tier 4: Workload Scenario 5 - Multilingual Code-Switching Dictation"""

    def setUp(self):
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_bilingual_git_workflow_instruction(self):
        """TC01: Dictates bilingual instructions containing Git commands and German umlauts."""
        text = (
            "Bitte führen Sie 'git checkout -b feature/hid-engine' aus, "
            "überprüfen Sie die Unit-Tests und pushen Sie den Branch zu origin."
        )
        self.dispatcher.dispatch_burst(text)
        self.assertEqual(self.host.host_text, text)
        self.assertIn("feature/hid-engine", self.host.host_text)
        self.assertIn("überprüfen Sie die Unit-Tests", self.host.host_text)

    def test_tc02_mixed_terminal_path_and_german_instructions(self):
        """TC02: Dictates command pipeline with file path on German QWERTZ."""
        cmd = "Führen Sie './gradlew test --stacktrace' im Verzeichnis C:\\Projects\\Transcriptor aus."
        self.dispatcher.dispatch_burst(cmd)
        self.assertEqual(self.host.host_text, cmd)


if __name__ == "__main__":
    unittest.main()
