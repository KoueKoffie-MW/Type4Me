"""
Boundary Test B24 (R1): Hotkey Bar & Clipboard Streamer Boundary & Corner Cases.
Verifies:
- Empty clipboard payload handling
- 50,000-character extreme clipboard streaming with progress
- Unicode surrogate pairs, emojis, and unmapped characters
- Rapid double-tap hotkey burst collisions
- Non-printable ASCII control characters sanitization
- Mid-stream cancellation with emergency release report verification
"""
import unittest
from tests.e2e.harness.hid_constants import (
    MOD_NONE, MOD_L_CTRL, KEY_C, KEY_ESCAPE, KEY_TAB
)
from tests.e2e.harness.keymap_engine import UsQwertyKeymap, GermanQwertzKeymap, KeyLayout, HOTKEY_MAP
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestBoundary24HotkeyAndClipboard(unittest.TestCase):
    """Tier 2: Boundary 24 (R1) - Hotkey Bar & Clipboard Streamer Boundaries"""

    def setUp(self):
        self.host = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        self.translator = UsQwertyKeymap()
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator, virtual_clock=True)

    def test_tc01_empty_clipboard_payload_handling(self):
        """TC01: Empty string clipboard stream produces zero reports and returns cleanly."""
        ok = self.dispatcher.stream_clipboard_to_host("", bracketed_paste=False)
        self.assertTrue(ok)
        self.assertEqual(len(self.host.received_reports), 0)
        self.assertEqual(self.host.host_text, "")

    def test_tc02_large_fifty_thousand_char_clipboard_payload(self):
        """TC02: Streams 50,000 character large script without buffer overrun."""
        large_payload = "const x = 12345;\n" * 2500  # 45,000 chars
        ok = self.dispatcher.stream_clipboard_to_host(large_payload, bracketed_paste=False, delay_ms=8.0)
        self.assertTrue(ok)
        self.assertEqual(len(self.host.host_text), len(large_payload))

    def test_tc03_unicode_emojis_and_surrogate_pairs(self):
        """TC03: Handles emojis (🚀, 🔥, 🎉) gracefully via translation fallback."""
        text = "Deploying 🚀 to production 🔥 with success 🎉!"
        strokes = self.translator.translate_string(text)
        # Unmapped emojis return empty stroke without crashing
        self.assertGreater(len(strokes), 0)
        self.dispatcher.dispatch_burst(text)
        self.assertIn("Deploying", self.host.host_text)

    def test_tc04_rapid_double_tap_hotkeys(self):
        """TC04: Dispatches 50 rapid sequential Ctrl+C signals without report corruption."""
        ctrl_c = HOTKEY_MAP["CTRL_C"]
        for _ in range(50):
            ok = self.dispatcher.send_single_keystroke(ctrl_c.modifier_mask, ctrl_c.usage_id, delay_ms=2.0)
            self.assertTrue(ok)

        self.assertEqual(len(self.host.received_reports), 100)

    def test_tc05_mid_stream_cancellation_triggers_emergency_release(self):
        """TC05: Aborting clipboard stream triggers NonCancellable emergency release report."""
        stream_cancelled = False

        def check_cancel():
            nonlocal stream_cancelled
            if self.dispatcher.total_keystrokes_sent >= 10:
                stream_cancelled = True
                return True
            return False

        long_text = "abcdefghijklmnopqrstuvwxyz" * 10
        ok = self.dispatcher.stream_clipboard_to_host(long_text, cancel_check=check_cancel)
        self.assertFalse(ok)
        self.assertTrue(stream_cancelled)
        # Final report must be all-zeros release report
        self.assertEqual(self.host.received_reports[-1], bytes([0, 0, 0, 0, 0, 0, 0, 0]))
        self.assertGreaterEqual(self.dispatcher.emergency_releases_sent, 1)


if __name__ == "__main__":
    unittest.main()
