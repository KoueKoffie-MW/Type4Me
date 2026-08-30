"""
Workload Scenario W11: Enterprise Air-Gapped Workstation USB Fallback Workflow.
Simulates an enterprise environment with disabled Bluetooth, switching to
USB AOA 2.0 / Gadget HID transport, and sending secure credentials and source code.
"""
import unittest
from tests.e2e.harness.service_simulator import UsbAoaTransport, TransportType
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestWorkload11EnterpriseAirgapUsb(unittest.TestCase):
    """Tier 4: Workload Scenario 11 - Enterprise Air-Gapped USB Fallback"""

    def setUp(self):
        self.usb_transport = UsbAoaTransport(usb_connected=True)
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_wired_usb_secure_credential_dictation(self):
        """TC01: Dictates complex secure password / token over wired USB HID without Bluetooth."""
        self.assertTrue(self.usb_transport.initialize())
        self.assertEqual(self.usb_transport.transport_type, TransportType.USB_AOA_2_0)

        # Secure token with AltGr and symbols
        secure_token = "SecKey_2026!@#{[X_99]}\\€µ"
        self.dispatcher.dispatch_burst(secure_token)
        self.assertEqual(self.host.host_text, secure_token)
        self.assertEqual(self.host.error_count, 0)


if __name__ == "__main__":
    unittest.main()
