"""
Tier 5 Adversarial Stress Test S01:
Bluetooth Sudden Disconnect During Active 100-Character Burst Typing & Emergency Release.

Stress-tests:
1. Mid-burst sudden disconnect during 100+ character bursts at arbitrary transmission checkpoints.
2. Verification of emergency all-zero key release report transmission upon disconnect/teardown (Edge Case E4).
3. Verification that host OS never retains stuck modifier keys or stuck key scancodes.
4. Transport state recovery: Reconnect after mid-burst drop and execute full burst with zero protocol errors.
5. Virtual cable unplug during active transmission.
6. Rapid 50-iteration connect-burst-disconnect loop under heavy load.
"""
import unittest
import time
from typing import List, Optional

from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, UsQwertyKeymap, KeyLayout, HidReport, HidKeyStroke
from tests.e2e.harness.hid_constants import (
    MOD_NONE, MOD_L_SHIFT, MOD_R_ALT, MOD_SHIFT_ALTGR,
    KEY_NONE, KEY_BACKSPACE, KEY_A, KEY_Z
)
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.service_simulator import BluetoothHidTransport, ConnectionState, BluetoothDeviceMock


class AdversarialBluetoothTransport(BluetoothHidTransport):
    """
    Instrumented Bluetooth HID transport that can simulate mid-flight link drop,
    emergency all-zero release reports, and report interception.
    """

    def __init__(self, permissions_granted: bool = True):
        super().__init__(permissions_granted=permissions_granted)
        self.emergency_release_sent = False
        self.disconnect_after_reports: Optional[int] = None
        self.reports_sent_count = 0
        self.host_simulator: Optional[HidHostSimulator] = None

    def set_host_simulator(self, host: HidHostSimulator):
        self.host_simulator = host

    def set_disconnect_trigger(self, after_reports: int):
        self.disconnect_after_reports = after_reports
        self.reports_sent_count = 0

    def send_report(self, report: bytes) -> bool:
        if self.connection_state != ConnectionState.CONNECTED or not self.is_registered:
            return False

        if len(report) != 8:
            return False

        self.reports_sent_count += 1
        self.transmitted_reports.append(report)

        # Deliver to host simulator if attached
        if self.host_simulator:
            if not self.host_simulator.receive_report(report):
                return False

        # Trigger sudden disconnect if threshold met
        if self.disconnect_after_reports is not None and self.reports_sent_count >= self.disconnect_after_reports:
            self.trigger_sudden_disconnect()
            return False

        return True

    def trigger_sudden_disconnect(self):
        """Simulates sudden Bluetooth link loss and emergency release handling."""
        # Send emergency all-zero key release report before teardown
        emergency_zero_report = bytes([0] * 8)
        self.transmitted_reports.append(emergency_zero_report)
        if self.host_simulator:
            # Emergency report received by host
            self.host_simulator.receive_report(emergency_zero_report)
        self.emergency_release_sent = True
        self.connection_state = ConnectionState.DISCONNECTED
        self.connected_device = None
        if self.callback:
            self.callback.on_connection_state_changed(BluetoothDeviceMock(), ConnectionState.DISCONNECTED)

    def trigger_virtual_cable_unplug(self):
        """Simulates Bluetooth HID Virtual Cable Unplug."""
        emergency_zero_report = bytes([0] * 8)
        self.transmitted_reports.append(emergency_zero_report)
        if self.host_simulator:
            self.host_simulator.receive_report(emergency_zero_report)
        self.emergency_release_sent = True
        self.connection_state = ConnectionState.DISCONNECTED
        self.connected_device = None
        if self.callback:
            self.callback.on_virtual_cable_unplug(BluetoothDeviceMock())


class TestS01BleDisconnectBurstEmergencyRelease(unittest.TestCase):
    """Adversarial Stress Test Suite S01: Bluetooth Disconnects & Emergency Release"""

    def setUp(self):
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.translator = GermanQwertzKeymap()
        self.transport = AdversarialBluetoothTransport(permissions_granted=True)
        self.transport.initialize()
        self.transport.set_host_simulator(self.host)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_s01_01_mid_burst_disconnect_during_100_char_german_text(self):
        """S01.1: Sudden disconnect midway through 100+ character German burst typing."""
        self.transport.simulate_host_connect()
        self.assertEqual(self.transport.connection_state, ConnectionState.CONNECTED)

        # Complex 120-character German test string with Umlauts, digits, and special symbols
        test_text = (
            "Zwölf Boxkämpfer jagen Viktor quer über den großen Sylter Deich! "
            "Äpfel, Öle, Übergrößen, Maßstab 12345 € @ {test} #100."
        )
        self.assertGreaterEqual(len(test_text), 100)

        # Translates to ~140 keystrokes (press + release = ~280 reports)
        strokes = self.translator.translate_string(test_text)
        self.assertGreaterEqual(len(strokes), 100)

        # Trigger disconnect at report #45 (mid-flight while transmitting)
        self.transport.set_disconnect_trigger(after_reports=45)

        # Dispatch burst using custom transport sender
        reports_sent = 0
        burst_succeeded = True
        for stroke in strokes:
            # Press
            ok_press = self.transport.send_report(stroke.to_key_down_report() if hasattr(stroke, 'to_key_down_report') else HidReport.press(stroke.modifier_mask, stroke.usage_id).to_bytes())
            if not ok_press:
                burst_succeeded = False
                break
            reports_sent += 1

            # Release
            ok_rel = self.transport.send_report(HidReport.release().to_bytes())
            if not ok_rel:
                burst_succeeded = False
                break
            reports_sent += 1

        # 1. Burst must fail cleanly without unhandled exception
        self.assertFalse(burst_succeeded)
        self.assertEqual(self.transport.connection_state, ConnectionState.DISCONNECTED)

        # 2. Emergency all-zero release report must have been transmitted
        self.assertTrue(self.transport.emergency_release_sent)

        # 3. Host simulator must NOT have stuck keys or stuck modifiers
        self.assertEqual(self.host.last_modifier, 0)
        self.assertEqual(self.host.last_keys, [0, 0, 0, 0, 0, 0])

    def test_s01_02_reconnect_and_resume_full_burst_recovery(self):
        """S01.2: Host reconnects after sudden mid-burst drop and completes burst cleanly."""
        self.transport.simulate_host_connect()

        # Step 1: Attempt burst with disconnect at report 30
        self.transport.set_disconnect_trigger(after_reports=30)
        test_text_p1 = "Dies ist ein unvollständiger Satz der mitten im Wort abbricht..."
        strokes = self.translator.translate_string(test_text_p1)
        for s in strokes:
            if not self.transport.send_report(HidReport.press(s.modifier_mask, s.usage_id).to_bytes()):
                break
            if not self.transport.send_report(HidReport.release().to_bytes()):
                break

        self.assertEqual(self.transport.connection_state, ConnectionState.DISCONNECTED)
        self.assertTrue(self.transport.emergency_release_sent)

        # Step 2: Reset disconnect trigger and Reconnect host
        self.transport.disconnect_after_reports = None
        self.transport.emergency_release_sent = False
        self.host.reset(KeyLayout.GERMAN_QWERTZ)
        self.transport.simulate_host_connect()
        self.assertEqual(self.transport.connection_state, ConnectionState.CONNECTED)

        # Step 3: Transmit a clean 100-character burst
        clean_text = "Vollständige Wiederherstellung nach Verbindungsabbruch: Äpfel, Öfen, Übungen und Maßstäbe 100% OK!"
        strokes_p2 = self.translator.translate_string(clean_text)
        for s in strokes_p2:
            ok_press = self.transport.send_report(HidReport.press(s.modifier_mask, s.usage_id).to_bytes())
            self.assertTrue(ok_press)
            ok_rel = self.transport.send_report(HidReport.release().to_bytes())
            self.assertTrue(ok_rel)

        # Step 4: Validate host received text with 100% integrity and zero errors
        self.assertEqual(self.host.host_text, clean_text)
        self.assertEqual(self.host.error_count, 0)
        self.assertEqual(self.host.last_keys, [0, 0, 0, 0, 0, 0])
        self.assertEqual(self.host.last_modifier, 0)

    def test_s01_03_virtual_cable_unplug_during_active_key_down(self):
        """S01.3: Virtual cable unplug occurs while a key is held down."""
        self.transport.simulate_host_connect()

        # Send Shift + 'A' key-down
        press_report = HidReport.press(MOD_L_SHIFT, KEY_A).to_bytes()
        self.transport.send_report(press_report)

        # Verify host received 'A' but key is currently held
        self.assertEqual(self.host.last_modifier, MOD_L_SHIFT)
        self.assertEqual(self.host.last_keys[0], KEY_A)

        # Sudden virtual cable unplug event occurs
        self.transport.trigger_virtual_cable_unplug()

        # Verify emergency zero report cleared the held key on host
        self.assertEqual(self.transport.connection_state, ConnectionState.DISCONNECTED)
        self.assertEqual(self.host.last_modifier, 0)
        self.assertEqual(self.host.last_keys, [0, 0, 0, 0, 0, 0])

    def test_s01_04_rapid_50_iteration_connect_burst_disconnect_loop(self):
        """S01.4: 50 rapid sequential cycles of connect -> burst -> sudden disconnect."""
        short_burst = "StressTest_#123! "

        for i in range(50):
            # Connect
            self.transport.disconnect_after_reports = None
            self.transport.simulate_host_connect()
            self.assertEqual(self.transport.connection_state, ConnectionState.CONNECTED)

            # Transmit 5 keystrokes
            strokes = self.translator.translate_string(short_burst)
            for s in strokes[:5]:
                self.transport.send_report(HidReport.press(s.modifier_mask, s.usage_id).to_bytes())
                self.transport.send_report(HidReport.release().to_bytes())

            # Sudden disconnect
            self.transport.trigger_sudden_disconnect()
            self.assertEqual(self.transport.connection_state, ConnectionState.DISCONNECTED)
            self.assertEqual(self.host.last_keys, [0, 0, 0, 0, 0, 0])
            self.assertEqual(self.host.last_modifier, 0)

        # Final full verification cycle
        self.transport.disconnect_after_reports = None
        self.host.reset(KeyLayout.GERMAN_QWERTZ)
        self.transport.simulate_host_connect()
        ok = self.dispatcher.dispatch_burst("Final Verification After 50 Cycles.")
        self.assertTrue(ok)
        self.assertEqual(self.host.host_text, "Final Verification After 50 Cycles.")
        self.assertEqual(self.host.error_count, 0)


if __name__ == "__main__":
    unittest.main()
