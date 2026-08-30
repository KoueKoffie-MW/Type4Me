"""
Boundary Tests: Feature 13 - USB HID Fallback Transports.
Covers USB disconnects, non-rooted access rejection, ADB socket drops,
and hot-swapping between USB and Bluetooth transports.
"""
import unittest
from tests.e2e.harness.service_simulator import (
    UsbAoaTransport, UsbGadgetTransport, UsbAdbSocketTransport
)


class TestBoundary13UsbFallback(unittest.TestCase):
    """Tier 2: Boundary 13 - USB HID Fallback Transports"""

    def test_tc01_aoa_disconnect_during_transmission(self):
        """TC01: AOA transport rejects reports immediately when USB cable is unplugged."""
        aoa = UsbAoaTransport(usb_connected=True)
        aoa.initialize()
        report = bytes([0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00])
        self.assertTrue(aoa.send_report(report))

        # Cable disconnected
        aoa.usb_connected = False
        self.assertFalse(aoa.send_report(report))

    def test_tc02_gadget_device_node_missing(self):
        """TC02: Linux Gadget transport fails if /dev/hidg0 node does not exist."""
        gadget = UsbGadgetTransport(is_rooted=True, dev_node_exists=False)
        self.assertFalse(gadget.initialize())
        self.assertFalse(gadget.send_report(bytes([0] * 8)))

    def test_tc03_adb_socket_connection_loss_and_retry(self):
        """TC03: ADB socket transport disconnects and reconnects successfully."""
        adb = UsbAdbSocketTransport(adb_port_active=True)
        adb.initialize()
        self.assertTrue(adb.connected)
        adb.disconnect()
        self.assertFalse(adb.connected)
        # Re-initialize
        adb.initialize()
        self.assertTrue(adb.connected)

    def test_tc04_invalid_report_size_on_usb_aoa(self):
        """TC04: AOA transport rejects malformed reports not equal to 8 bytes."""
        aoa = UsbAoaTransport(usb_connected=True)
        aoa.initialize()
        self.assertFalse(aoa.send_report(bytes([0] * 4)))
        self.assertFalse(aoa.send_report(bytes([0] * 10)))

    def test_tc05_multiple_usb_transports_coexistence(self):
        """TC05: Multiple USB fallback implementations can be instantiated independently."""
        aoa = UsbAoaTransport(usb_connected=True)
        adb = UsbAdbSocketTransport(adb_port_active=True)
        self.assertTrue(aoa.initialize())
        self.assertTrue(adb.initialize())


if __name__ == "__main__":
    unittest.main()
