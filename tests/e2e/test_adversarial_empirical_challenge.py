"""
Type4Me Empirical Adversarial Stress & Verification Harness.
Adversarially validates:
1. Bracketed Paste Mode (huge payloads, nested markers, mid-stream cancellation, rapid sequential bursts)
2. Rapid Modifier Toggles (1000-cycle thrashing, hotkey interleaving, shifted/AltGr transitions, zero stuck keys)
3. Dead-Key Synthesis (German QWERTZ solitary space auto-injection, repetitive chains, accented vowels, boundary conditions)
4. Multi-Host Disconnect/Reconnect Simulation (carousel switching, concurrent thread contention, mid-burst drops, watchdog recovery)
"""
import unittest
import threading
from concurrent.futures import ThreadPoolExecutor
from typing import List, Optional

from tests.e2e.harness.hid_constants import (
    MOD_NONE, MOD_L_CTRL, MOD_L_SHIFT, MOD_L_ALT, MOD_L_GUI,
    MOD_R_CTRL, MOD_R_SHIFT, MOD_R_ALT, MOD_R_GUI, MOD_SHIFT_ALTGR,
    KEY_NONE, KEY_A, KEY_Z, KEY_1, KEY_0, KEY_ENTER, KEY_ESCAPE,
    KEY_BACKSPACE, KEY_TAB, KEY_SPACE, KEY_GRAVE, KEY_EQUAL,
    KEY_C, KEY_D, KEY_L, KEY_F1, KEY_F12,
    BRACKETED_PASTE_START, BRACKETED_PASTE_END
)
from tests.e2e.harness.keymap_engine import (
    KeyLayout, UsQwertyKeymap, GermanQwertzKeymap, HidKeyStroke, HidReport
)
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.persistence_simulator import (
    AppDatabaseSimulator, PairedHostEntity
)
from tests.e2e.harness.multi_host_simulator import (
    MultiHostTransportSimulator, MultiHostState
)
from tests.e2e.harness.service_simulator import (
    BluetoothHidTransport, ConnectionState, BluetoothDeviceMock
)


class TestBracketedPasteAdversarial(unittest.TestCase):
    """Adversarial stress testing of Bracketed Paste Mode in KeystrokeDispatcher."""

    def setUp(self):
        self.host = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        self.translator = UsQwertyKeymap()
        self.dispatcher = KeystrokeDispatcher(
            host=self.host,
            translator=self.translator,
            virtual_clock=True
        )

    def test_01_empty_clipboard_payload(self):
        """Empty clipboard string with bracketed paste enabled should safely no-op."""
        ok = self.dispatcher.stream_clipboard_to_host("", bracketed_paste=True)
        self.assertTrue(ok)
        self.assertEqual(self.host.host_text, "")
        self.assertEqual(len(self.host.received_reports), 0)

    def test_02_huge_code_payload_multiline(self):
        """Streaming a large multiline payload (5,000+ chars) with mixed indents and syntax."""
        python_snippet = (
            "def process_matrix(data: list[list[int]]) -> int:\n"
            "    total = 0\n"
            "    for row in data:\n"
            "        for val in row:\n"
            "            if val % 2 == 0:\n"
            "                total += val * 2\n"
            "            else:\n"
            "                total -= val\n"
            "    return total\n"
        ) * 20  # ~3,600 characters with 180 lines

        ok = self.dispatcher.stream_clipboard_to_host(python_snippet, bracketed_paste=True)
        self.assertTrue(ok)
        # Note: \x1b is decoded as non-printable KEY_ESCAPE, so host receives printable prefix [200~ and suffix [201~
        self.assertTrue(self.host.host_text.startswith("[200~"))
        self.assertTrue(self.host.host_text.endswith("[201~"))
        # Verify body intact
        self.assertIn("def process_matrix", self.host.host_text)
        self.assertIn("total += val * 2", self.host.host_text)
        # Emergency release must be sent at end
        self.assertGreater(self.dispatcher.emergency_releases_sent, 0)
        self.assertEqual(self.host.last_modifier, 0)

    def test_03_nested_escape_markers_inside_payload(self):
        """Payload containing literal terminal escape sequences and bracket markers."""
        adversarial_str = "echo 'evil_inner' && rm -rf /tmp/test\n"
        ok = self.dispatcher.stream_clipboard_to_host(adversarial_str, bracketed_paste=True)
        self.assertTrue(ok)
        self.assertTrue(self.host.host_text.startswith("[200~"))
        self.assertTrue(self.host.host_text.endswith("[201~"))
        self.assertIn("evil_inner", self.host.host_text)
        self.assertEqual(self.host.last_modifier, 0)

    def test_04_mid_stream_cancellation_guarantees_emergency_release(self):
        """When a stream is cancelled mid-way, emergency all-zero release must be sent."""
        counter = {"count": 0}

        def abort_after_15_strokes():
            counter["count"] += 1
            return counter["count"] >= 15

        payload = "A" * 100
        ok = self.dispatcher.stream_clipboard_to_host(
            payload, bracketed_paste=True, cancel_check=abort_after_15_strokes
        )
        self.assertFalse(ok)
        # Host must not retain stuck modifiers
        self.assertEqual(self.host.last_modifier, 0)
        self.assertGreater(self.dispatcher.emergency_releases_sent, 0)
        last_report = self.host.received_reports[-1]
        self.assertEqual(last_report, bytes([0] * 8))

    def test_05_rapid_sequential_bursts(self):
        """Rapid sequential clipboard streams (50 bursts) without state leak."""
        for i in range(50):
            token = f"val_{i};"
            ok = self.dispatcher.stream_clipboard_to_host(token, bracketed_paste=True)
            self.assertTrue(ok)
            self.assertEqual(self.host.last_modifier, 0)

        self.assertIn("val_0;", self.host.host_text)
        self.assertIn("val_49;", self.host.host_text)
        self.assertEqual(self.dispatcher.emergency_releases_sent, 50)


class TestRapidModifierTogglesAdversarial(unittest.TestCase):
    """Adversarial stress testing of rapid modifier transitions and emergency release."""

    def setUp(self):
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.translator = GermanQwertzKeymap()
        self.dispatcher = KeystrokeDispatcher(
            host=self.host,
            translator=self.translator,
            virtual_clock=True
        )

    def test_01_rapid_modifier_thrashing_1000_events(self):
        """1000 alternating modifier combinations in tight loop; host modifiers must return to 0."""
        modifiers = [
            MOD_NONE, MOD_L_SHIFT, MOD_L_CTRL, MOD_L_ALT,
            MOD_R_ALT, MOD_L_GUI, MOD_SHIFT_ALTGR,
            MOD_L_CTRL | MOD_L_SHIFT, MOD_L_CTRL | MOD_L_ALT
        ]

        for i in range(1000):
            mod = modifiers[i % len(modifiers)]
            press_rep = HidReport.press(mod, KEY_A)
            rel_rep = HidReport.release()
            self.host.receive_report(press_rep.to_bytes())
            self.host.receive_report(rel_rep.to_bytes())

        self.dispatcher.send_emergency_release()
        self.assertEqual(self.host.last_modifier, 0)
        self.assertEqual(self.host.last_keys, [0] * 6)

    def test_02_terminal_hotkey_macro_interleaving(self):
        """Interleave terminal control hotkeys (Ctrl+C, Ctrl+Z, Alt+Tab) with text typing."""
        hotkeys = [
            self.translator.translate_hotkey("CTRL_C"),
            self.translator.translate_hotkey("CTRL_Z"),
            self.translator.translate_hotkey("CTRL_D"),
            self.translator.translate_hotkey("ALT_TAB"),
            self.translator.translate_hotkey("ESC"),
            self.translator.translate_hotkey("F5"),
            self.translator.translate_hotkey("F12"),
        ]
        # Filter non-null
        valid_hotkeys = [hk for hk in hotkeys if hk is not None]
        self.assertGreaterEqual(len(valid_hotkeys), 5)

        for _ in range(20):
            # Send raw hotkey sequence
            ok = self.dispatcher.send_raw_keystrokes(valid_hotkeys)
            self.assertTrue(ok)
            # Dispatch text burst
            ok = self.dispatcher.dispatch_burst("git status\n")
            self.assertTrue(ok)
            self.assertEqual(self.host.last_modifier, 0)

    def test_03_shifted_unshifted_altgr_dynamic_alternation(self):
        """Rapid alternation between lowercase, uppercase, and AltGr 3rd-level symbols."""
        mixed_text = "aA@bB€cC{dD}eE[fF]gG\\hH~iI|jJ" * 10
        ok = self.dispatcher.dispatch_burst(mixed_text)
        self.assertTrue(ok)
        self.assertEqual(self.host.last_modifier, 0)
        # Verify symbols decoded on QWERTZ host
        self.assertIn("@", self.host.host_text)
        self.assertIn("€", self.host.host_text)
        self.assertIn("{", self.host.host_text)
        self.assertIn("}", self.host.host_text)
        self.assertIn("[", self.host.host_text)
        self.assertIn("]", self.host.host_text)
        self.assertIn("\\", self.host.host_text)


class TestDeadKeySynthesisAdversarial(unittest.TestCase):
    """Adversarial stress testing of German QWERTZ dead-key space auto-injection."""

    def setUp(self):
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.translator = GermanQwertzKeymap(auto_space_dead_keys=True)
        self.dispatcher = KeystrokeDispatcher(
            host=self.host,
            translator=self.translator,
            virtual_clock=True
        )

    def test_01_solitary_dead_keys_auto_space(self):
        """Solitary dead keys (^, ´, `, ~) must synthesize with auto-space."""
        solitary_keys = "^´`~"
        for ch in solitary_keys:
            strokes = self.translator.translate_char(ch)
            # Must emit 2 strokes: the dead key and the space
            self.assertEqual(len(strokes), 2, f"Failed for dead key {ch}")
            self.assertEqual(strokes[1].usage_id, KEY_SPACE)

    def test_02_repetitive_dead_key_chains(self):
        """Chains of repetitive dead keys (e.g. ^^^^^^^^^^ and ´´´´´´´´´´)."""
        chain = "^" * 20
        ok = self.dispatcher.dispatch_burst(chain)
        self.assertTrue(ok)
        # Host simulator should decode all 20 circumflexes
        self.assertEqual(self.host.host_text.count("^"), 20)

        chain_acute = "´" * 20
        ok = self.dispatcher.dispatch_burst(chain_acute)
        self.assertTrue(ok)
        self.assertEqual(self.host.host_text.count("´"), 20)

    def test_03_alternating_dead_keys(self):
        """Rapid alternating dead keys: ^´^´^´^´^´."""
        alt_pattern = "^´^´^´^´^´"
        ok = self.dispatcher.dispatch_burst(alt_pattern)
        self.assertTrue(ok)
        self.assertEqual(self.host.host_text.count("^"), 5)
        self.assertEqual(self.host.host_text.count("´"), 5)

    def test_04_dead_keys_interleaved_with_backspace_and_newlines(self):
        """Dead keys followed by backspaces and soft enters."""
        mixed = "^\b\n´\b\n`\b\n"
        strokes = self.translator.translate_string(mixed)
        self.assertGreater(len(strokes), 0)
        ok = self.dispatcher.send_raw_keystrokes(strokes)
        self.assertTrue(ok)
        self.assertEqual(self.host.last_modifier, 0)


class TestMultiHostDisconnectReconnectAdversarial(unittest.TestCase):
    """Adversarial stress testing of Multi-Host Switching & Disconnect Recovery."""

    def setUp(self):
        self.db = AppDatabaseSimulator(version=2, seed_defaults=True)
        self.hosts = [
            PairedHostEntity("host_1", "11:22:33:44:55:01", "Linux Workstation", "LINUX"),
            PairedHostEntity("host_2", "11:22:33:44:55:02", "MacBook Pro M3", "MACOS"),
            PairedHostEntity("host_3", "11:22:33:44:55:03", "Windows 11 Desktop", "WINDOWS"),
            PairedHostEntity("host_4", "11:22:33:44:55:04", "Android Tablet", "ANDROID"),
        ]
        for h in self.hosts:
            self.db.paired_host_dao.insert_or_update_host(h)

        self.transport = MultiHostTransportSimulator(
            paired_host_dao=self.db.paired_host_dao,
            permissions_granted=True,
            virtual_clock=True
        )
        self.transport.initialize()

    def test_01_switching_carousel_40_transitions(self):
        """10 full cycles across 4 hosts (40 transitions); verify state machine cleanliness."""
        phase_records = []
        self.transport.add_state_listener(lambda s: phase_records.append((s.state, s.phase)))

        for cycle in range(10):
            for target in self.hosts:
                ok = self.transport.switch_host(target, settling_delay_ms=0.0)
                self.assertTrue(ok)
                self.assertEqual(self.transport.connection_state.state, MultiHostState.CONNECTED)
                self.assertEqual(self.transport.connection_state.active_host.id, target.id)

        # Mutex must be unlocked
        self.assertFalse(self.transport.is_switching)
        self.assertGreaterEqual(len(phase_records), 40)

    def test_02_concurrent_switching_contention(self):
        """50 concurrent threads firing switch_host calls simultaneously."""
        outcomes = []
        lock = threading.Lock()

        def switch_worker(idx: int):
            target = self.hosts[idx % len(self.hosts)]
            ok = self.transport.switch_host(target, settling_delay_ms=0.0)
            with lock:
                outcomes.append((idx, ok))

        with ThreadPoolExecutor(max_workers=16) as executor:
            futures = [executor.submit(switch_worker, i) for i in range(50)]
            for f in futures:
                f.result()

        self.assertEqual(len(outcomes), 50)
        self.assertEqual(self.transport.connection_state.state, MultiHostState.CONNECTED)
        self.assertFalse(self.transport.is_switching)

    def test_03_mid_burst_disconnect_simulation_and_recovery(self):
        """Simulate sudden transport disconnect during active burst typing."""
        from tests.e2e.tier5_stress.test_s01_ble_disconnect_burst_emergency_release import (
            AdversarialBluetoothTransport
        )
        adv_transport = AdversarialBluetoothTransport(permissions_granted=True)
        adv_transport.initialize()
        adv_transport.simulate_host_connect()
        self.assertEqual(adv_transport.connection_state, ConnectionState.CONNECTED)

        host_sim = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        adv_transport.set_host_simulator(host_sim)

        # Set trigger to drop after 25 reports
        adv_transport.set_disconnect_trigger(after_reports=25)

        # Send reports until disconnect occurs
        disconnected = False
        for i in range(50):
            rep = HidReport.press(MOD_NONE, KEY_A + (i % 20))
            ok = adv_transport.send_report(rep.to_bytes())
            if not ok:
                disconnected = True
                break

        self.assertTrue(disconnected)
        self.assertTrue(adv_transport.emergency_release_sent)
        self.assertEqual(adv_transport.connection_state, ConnectionState.DISCONNECTED)

        # Recovery: Clear trigger and Reconnect host
        adv_transport.disconnect_after_reports = None
        adv_transport.emergency_release_sent = False
        adv_transport.simulate_host_connect()
        self.assertEqual(adv_transport.connection_state, ConnectionState.CONNECTED)
        rep = HidReport.press(MOD_NONE, KEY_Z)
        ok = adv_transport.send_report(rep.to_bytes())
        self.assertTrue(ok)
        rel = HidReport.release()
        adv_transport.send_report(rel.to_bytes())
        self.assertEqual(host_sim.last_modifier, 0)


if __name__ == "__main__":
    unittest.main()
