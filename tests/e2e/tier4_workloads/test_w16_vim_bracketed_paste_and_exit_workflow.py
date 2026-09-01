"""
Workload Test W16: Vim Bracketed Paste Mode & Navigation Workflow.
Verifies:
- Opening Vim editor
- Entering Insert mode ('i')
- Streaming multi-line YAML clipboard with bracketed paste mode (\\x1b[200~ ... \\x1b[201~)
- Exiting Insert mode with Escape hotkey
- Saving and quitting with ':wq'
"""
import unittest
from tests.e2e.harness.keymap_engine import UsQwertyKeymap, KeyLayout, HOTKEY_MAP
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestWorkload16VimBracketedPasteWorkflow(unittest.TestCase):
    """Tier 4: Workload 16 - Vim Bracketed Paste Mode & Exit"""

    def setUp(self):
        self.host = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        self.translator = UsQwertyKeymap()
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_vim_bracketed_paste_and_save_quit(self):
        """TC01: Opens vim, enters insert mode, streams YAML with bracketed paste, Esc, and :wq."""
        # 1. Open Vim
        self.dispatcher.dispatch_burst("vim deploy.yaml\n")

        # 2. Enter Insert Mode
        self.dispatcher.dispatch_burst("i")

        # 3. Stream multi-line YAML snippet with bracketed paste mode
        yaml_config = "apiVersion: v1\nkind: Service\nmetadata:\n  name: type4me-service\n"
        ok = self.dispatcher.stream_clipboard_to_host(yaml_config, bracketed_paste=True, delay_ms=8.0)
        self.assertTrue(ok)

        # 4. Exit Insert Mode via Escape hotkey
        esc = HOTKEY_MAP["ESC"]
        self.dispatcher.send_single_keystroke(esc.modifier_mask, esc.usage_id)

        # 5. Save & Quit
        self.dispatcher.dispatch_burst(":wq\n")

        self.assertIn("vim deploy.yaml\n", self.host.host_text)
        self.assertIn("apiVersion: v1\nkind: Service\nmetadata:\n  name: type4me-service\n", self.host.host_text)
        self.assertIn(":wq\n", self.host.host_text)


if __name__ == "__main__":
    unittest.main()
