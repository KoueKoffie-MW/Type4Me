"""
Feature 13: USB HID Fallback Abstraction.
Verifies the HidTransport interface and implementations for wired USB fallback:
Android Open Accessory (AOA 2.0), Linux USB Gadget (/dev/hidg0), and ADB Reverse Bridge.
"""
import unittest
from tests.e2e.harness.service_simulator import (
    TransportType, UsbAoaTransport, UsbGadgetTransport, UsbAdbSocketTransport
)


class TestFeature13UsbHidFallback(unittest.TestCase):
    """Tier 1: Feature 13 - USB HID Fallback Abstraction"""

    def test_tc01_aoa_2_0_transport_success(self):
        """TC01: AOA 2.0 transport transmits 8-byte reports when USB accessory is connected."""
        aoa = UsbAoaTransport(usb_connected=True)
        self.assertTrue(aoa.initialize())
        self.assertEqual(aoa.transport_type, TransportType.USB_AOA_2_0)
        report = bytes([0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00])
        self.assertTrue(aoa.send_report(report))
        self.assertEqual(len(aoa.transmitted_reports), 1)

    def test_tc02_aoa_transport_disconnected(self):
        """TC02: AOA transport fails when USB cable is disconnected."""
        aoa = UsbAoaTransport(usb_connected=False)
        self.assertFalse(aoa.initialize())
        report = bytes([0] * 8)
        self.assertFalse(aoa.send_report(report))

    def test_tc03_linux_usb_gadget_rooted(self):
        """TC03: Linux USB Gadget (/dev/hidg0) succeeds on rooted hardware."""
        gadget = UsbGadgetTransport(is_rooted=True, dev_node_exists=True)
        self.assertTrue(gadget.initialize())
        self.assertEqual(gadget.transport_type, TransportType.USB_LINUX_GADGET)
        report = bytes([0x02, 0x00, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00])
        self.assertTrue(gadget.send_report(report))

    def test_tc04_linux_usb_gadget_non_rooted_fails(self):
        """TC04: Linux USB Gadget fails gracefully on stock (non-rooted) Android."""
        gadget = UsbGadgetTransport(is_rooted=False, dev_node_exists=False)
        self.assertFalse(gadget.initialize())
        report = bytes([0] * 8)
        self.assertFalse(gadget.send_report(report))

    def test_tc05_adb_socket_bridge(self):
        """TC05: ADB socket transport streams reports over reverse port forwarding."""
        adb = UsbAdbSocketTransport(adb_port_active=True)
        self.assertTrue(adb.initialize())
        self.assertEqual(adb.transport_type, TransportType.USB_ADB_SOCKET)
        report = bytes([0x40, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00])
        self.assertTrue(adb.send_report(report))
        self.assertEqual(len(adb.transmitted_reports), 1)


if __name__ == "__main__":
    unittest.main()
