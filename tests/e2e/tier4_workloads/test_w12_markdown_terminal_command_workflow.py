"""
Workload Scenario W12: Complex Markdown Table & Bash Terminal Command Dictation.
Simulates dictating a complete Markdown table with pipes (|), dashes (-), and formatting,
followed by a complex Bash shell script on US QWERTY and German QWERTZ layouts.
"""
import unittest
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, UsQwertyKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestWorkload12MarkdownTerminalCommand(unittest.TestCase):
    """Tier 4: Workload Scenario 12 - Markdown Table & Bash Command Dictation"""

    def test_tc01_markdown_table_on_german_layout(self):
        """TC01: Dictates Markdown table with pipes (|) and headers on German layout."""
        translator = GermanQwertzKeymap()
        host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        dispatcher = KeystrokeDispatcher(host=host, translator=translator)

        md_table = (
            "| Feature | Status | Pacing |\n"
            "| :--- | :---: | :---: |\n"
            "| Bluetooth HID | OK | 8ms |\n"
            "| German DIN 2137 | OK | 8ms |"
        )
        dispatcher.dispatch_burst(md_table)
        self.assertEqual(host.host_text, md_table)

    def test_tc02_complex_bash_script_on_us_layout(self):
        """TC02: Dictates complex Bash script with variables ($), quotes, and redirects on US layout."""
        translator = UsQwertyKeymap()
        host = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        dispatcher = KeystrokeDispatcher(host=host, translator=translator)

        bash_script = (
            "#!/usr/bin/env bash\n"
            "set -euo pipefail\n"
            "TARGET_DIR=\"/opt/transcriptor\"\n"
            "mkdir -p \"$TARGET_DIR\"\n"
            "echo \"Starting build...\" > \"$TARGET_DIR/build.log\"\n"
        )
        dispatcher.dispatch_burst(bash_script)
        self.assertEqual(host.host_text, bash_script)


if __name__ == "__main__":
    unittest.main()
