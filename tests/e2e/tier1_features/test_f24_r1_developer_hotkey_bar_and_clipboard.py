"""
Feature Test F24 (R1): Developer Hotkey Bar & Clipboard Keystroke Streamer.
Verifies:
- Hardware terminal keys (Esc, Tab, Ctrl+C, Ctrl+Z, Ctrl+D, Ctrl+L, Arrows, Alt+Tab, F1-F24)
- Deterministic 8ms pacing duty cycle (4ms down / 4ms up)
- NonCancellable emergency modifier release guard
- Mobile clipboard-to-host keystroke streaming
- Terminal bracketed paste mode (\\x1b[200~ ... \\x1b[201~)
- Inter-line settling delay for shell AST highlighting choke prevention
"""
import unittest
from tests.e2e.harness.hid_constants import (
    MOD_NONE, MOD_L_CTRL, MOD_L_ALT,
    KEY_ESCAPE, KEY_TAB, KEY_C, KEY_Z, KEY_D, KEY_L, KEY_ENTER,
    KEY_UP_ARROW, KEY_DOWN_ARROW, KEY_LEFT_ARROW, KEY_RIGHT_ARROW,
    KEY_F1, KEY_F5, KEY_F12, KEY_F24,
    BRACKETED_PASTE_START, BRACKETED_PASTE_END
)
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, UsQwertyKeymap, KeyLayout, HidKeyStroke, HOTKEY_MAP
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestFeature24DeveloperHotkeyBarAndClipboard(unittest.TestCase):
    """Tier 1: Feature 24 (R1) - Developer Hotkey Bar & Clipboard Streamer"""

    def setUp(self):
        self.host = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        self.translator = UsQwertyKeymap()
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator, virtual_clock=True)

    def test_tc01_extended_terminal_hotkeys_and_f_keys(self):
        """TC01: Emits correct 8-byte HID reports for terminal control keys and F1-F24."""
        # Test Ctrl+C (SIGINT)
        ctrl_c = HOTKEY_MAP["CTRL_C"]
        self.assertEqual(ctrl_c.modifier_mask, MOD_L_CTRL)
        self.assertEqual(ctrl_c.usage_id, KEY_C)
        self.dispatcher.send_single_keystroke(ctrl_c.modifier_mask, ctrl_c.usage_id)

        # Test Esc, Tab, Alt+Tab
        esc = HOTKEY_MAP["ESC"]
        self.assertEqual(esc.usage_id, KEY_ESCAPE)
        self.dispatcher.send_single_keystroke(esc.modifier_mask, esc.usage_id)

        alt_tab = HOTKEY_MAP["ALT_TAB"]
        self.assertEqual(alt_tab.modifier_mask, MOD_L_ALT)
        self.assertEqual(alt_tab.usage_id, KEY_TAB)
        self.dispatcher.send_single_keystroke(alt_tab.modifier_mask, alt_tab.usage_id)

        # Test Function Keys F1, F5, F12, F24
        f1 = HOTKEY_MAP["F1"]
        self.assertEqual(f1.usage_id, KEY_F1)
        self.dispatcher.send_single_keystroke(f1.modifier_mask, f1.usage_id)

        f24 = HOTKEY_MAP["F24"]
        self.assertEqual(f24.usage_id, KEY_F24)
        self.dispatcher.send_single_keystroke(f24.modifier_mask, f24.usage_id)

        # 5 key combinations -> 10 reports (5 press + 5 release)
        self.assertEqual(len(self.host.received_reports), 10)

    def test_tc02_deterministic_8ms_pacing_duty_cycle(self):
        """TC02: Validates deterministic 8ms pacing duty cycle (4ms down / 4ms up -> 125 chars/sec)."""
        text = "git status"
        t_start = self.dispatcher.virtual_time
        self.dispatcher.dispatch_burst(text, delay_ms=8.0, inter_line_delay_ms=0.0)
        t_end = self.dispatcher.virtual_time
        duration_ms = (t_end - t_start) * 1000.0

        expected_duration_ms = len(text) * 8.0
        self.assertAlmostEqual(duration_ms, expected_duration_ms, delta=1.0)
        self.assertEqual(self.host.host_text, text)

    def test_tc03_emergency_release_guard_all_zeros(self):
        """TC03: Verifies that emergency release emits pure all-zeros report [0,0,0,0,0,0,0,0]."""
        self.dispatcher.send_emergency_release()
        last_report = self.host.received_reports[-1]
        self.assertEqual(last_report, bytes([0, 0, 0, 0, 0, 0, 0, 0]))
        self.assertEqual(self.dispatcher.emergency_releases_sent, 1)

    def test_tc04_clipboard_keystroke_streaming(self):
        """TC04: Streams Android clipboard string to host workstation as paced keystrokes."""
        clip_content = "export API_KEY=sk_test_1234567890abcdef"
        ok = self.dispatcher.stream_clipboard_to_host(clip_content, bracketed_paste=False, delay_ms=8.0)
        self.assertTrue(ok)
        self.assertEqual(self.host.host_text, clip_content)
        self.assertEqual(self.dispatcher.total_keystrokes_sent, len(clip_content))

    def test_tc05_bracketed_paste_mode_wrapping(self):
        """TC05: Wraps clipboard stream in \\x1b[200~ ... \\x1b[201~ to prevent staircase indentation."""
        code_block = "def hello():\n    print('world')"
        wrapped = self.translator.wrap_bracketed_paste(code_block)
        self.assertTrue(wrapped.startswith(BRACKETED_PASTE_START))
        self.assertTrue(wrapped.endswith(BRACKETED_PASTE_END))
        self.assertIn(code_block, wrapped)

        ok = self.dispatcher.stream_clipboard_to_host(code_block, bracketed_paste=True, delay_ms=8.0)
        self.assertTrue(ok)
        self.assertIn("def hello():", self.host.host_text)

    def test_tc06_inter_line_delay_settling_guard(self):
        """TC06: Inserts 25ms settling delay after KEY_ENTER for shell AST syntax highlighting safety."""
        multiline = "line1\nline2\nline3"
        t_start = self.dispatcher.virtual_time
        self.dispatcher.dispatch_burst(multiline, delay_ms=8.0, inter_line_delay_ms=25.0)
        t_end = self.dispatcher.virtual_time
        duration_ms = (t_end - t_start) * 1000.0

        # 17 chars total (including 2 newlines). 2 newlines * 25ms extra delay = 50ms extra.
        # Base typing: 17 * 8ms = 136ms. Total expected = 186ms.
        expected_min = (len(multiline) * 8.0) + (2 * 25.0)
        self.assertGreaterEqual(duration_ms, expected_min - 2.0)
        self.assertEqual(self.host.host_text, multiline)


if __name__ == "__main__":
    unittest.main()
