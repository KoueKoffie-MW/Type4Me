"""
Android Bluetooth HID Service & Transport Layer Simulator.
Models Android 14/15 connectedDevice FGS, BluetoothProfile / BluetoothHidDevice callbacks,
runtime permission enforcement, and USB HID fallback transports.
"""
from dataclasses import dataclass
from enum import Enum
from typing import Optional, List, Dict, Any, Callable
import time

from .hid_constants import (
    HID_KEYBOARD_REPORT_DESCRIPTOR, SDP_SUBCLASS_KEYBOARD,
    LED_NUM_LOCK, LED_CAPS_LOCK, LED_SCROLL_LOCK
)


class ConnectionState(Enum):
    DISCONNECTED = 0
    CONNECTING = 1
    CONNECTED = 2
    DISCONNECTING = 3
    ERROR = 4


class TransportType(Enum):
    BLUETOOTH_HID = "BLUETOOTH_HID"
    USB_AOA_2_0 = "USB_AOA_2_0"
    USB_LINUX_GADGET = "USB_LINUX_GADGET"
    USB_ADB_SOCKET = "USB_ADB_SOCKET"


@dataclass
class BluetoothDeviceMock:
    address: str = "AA:BB:CC:DD:EE:FF"
    name: str = "Host-Workstation-PC"


class BluetoothHidDeviceCallbackMock:
    """Mock for BluetoothHidDevice.Callback events."""

    def on_app_status_changed(self, device: Optional[BluetoothDeviceMock], registered: bool):
        pass

    def on_connection_state_changed(self, device: BluetoothDeviceMock, state: ConnectionState):
        pass

    def on_get_report(self, device: BluetoothDeviceMock, report_type: int, report_id: int, buffer_size: int):
        pass

    def on_set_report(self, device: BluetoothDeviceMock, report_type: int, report_id: int, data: bytes):
        pass

    def on_virtual_cable_unplug(self, device: BluetoothDeviceMock):
        pass


class HidTransport:
    """Base abstraction for HID Transports (Bluetooth & USB)."""
    transport_type: TransportType

    def initialize(self) -> bool:
        raise NotImplementedError

    def send_report(self, report: bytes) -> bool:
        raise NotImplementedError

    def disconnect(self):
        raise NotImplementedError


class BluetoothHidTransport(HidTransport):
    """Simulates Android BluetoothHidDevice transport."""

    def __init__(self, permissions_granted: bool = True):
        self.transport_type = TransportType.BLUETOOTH_HID
        self.permissions_granted = permissions_granted
        self.is_registered = False
        self.connection_state = ConnectionState.DISCONNECTED
        self.connected_device: Optional[BluetoothDeviceMock] = None
        self.callback: Optional[BluetoothHidDeviceCallbackMock] = None
        self.transmitted_reports: List[bytes] = []

    def set_callback(self, callback: BluetoothHidDeviceCallbackMock):
        self.callback = callback

    def initialize(self) -> bool:
        if not self.permissions_granted:
            return False
        # Register SDP and Descriptor
        self.is_registered = True
        if self.callback:
            self.callback.on_app_status_changed(None, True)
        return True

    def simulate_host_connect(self, device: Optional[BluetoothDeviceMock] = None):
        if not self.is_registered:
            return
        dev = device or BluetoothDeviceMock()
        self.connected_device = dev
        self.connection_state = ConnectionState.CONNECTED
        if self.callback:
            self.callback.on_connection_state_changed(dev, ConnectionState.CONNECTED)

    def simulate_host_disconnect(self):
        if self.connected_device and self.callback:
            self.callback.on_connection_state_changed(self.connected_device, ConnectionState.DISCONNECTED)
        self.connected_device = None
        self.connection_state = ConnectionState.DISCONNECTED

    def simulate_virtual_cable_unplug(self):
        if self.connected_device and self.callback:
            self.callback.on_virtual_cable_unplug(self.connected_device)
        self.connected_device = None
        self.connection_state = ConnectionState.DISCONNECTED

    def send_report(self, report: bytes) -> bool:
        if not self.permissions_granted or not self.is_registered or self.connection_state != ConnectionState.CONNECTED:
            return False
        if len(report) != 8:
            return False
        self.transmitted_reports.append(report)
        return True

    def disconnect(self):
        self.simulate_host_disconnect()


class UsbAoaTransport(HidTransport):
    """Simulates Android Open Accessory (AOA) 2.0 HID Transport."""
    def __init__(self, usb_connected: bool = True):
        self.transport_type = TransportType.USB_AOA_2_0
        self.usb_connected = usb_connected
        self.registered = False
        self.transmitted_reports: List[bytes] = []

    def initialize(self) -> bool:
        if not self.usb_connected:
            return False
        self.registered = True
        return True

    def send_report(self, report: bytes) -> bool:
        if not self.registered or not self.usb_connected:
            return False
        if len(report) != 8:
            return False
        self.transmitted_reports.append(report)
        return True

    def disconnect(self):
        self.registered = False


class UsbGadgetTransport(HidTransport):
    """Simulates Linux USB Gadget /dev/hidg0 Transport (Rooted)."""
    def __init__(self, is_rooted: bool = True, dev_node_exists: bool = True):
        self.transport_type = TransportType.USB_LINUX_GADGET
        self.is_rooted = is_rooted
        self.dev_node_exists = dev_node_exists
        self.transmitted_reports: List[bytes] = []

    def initialize(self) -> bool:
        return self.is_rooted and self.dev_node_exists

    def send_report(self, report: bytes) -> bool:
        if not self.initialize():
            return False
        if len(report) != 8:
            return False
        self.transmitted_reports.append(report)
        return True

    def disconnect(self):
        pass


class UsbAdbSocketTransport(HidTransport):
    """Simulates ADB Reverse TCP Socket Companion Transport."""
    def __init__(self, adb_port_active: bool = True):
        self.transport_type = TransportType.USB_ADB_SOCKET
        self.adb_port_active = adb_port_active
        self.connected = False
        self.transmitted_reports: List[bytes] = []

    def initialize(self) -> bool:
        if not self.adb_port_active:
            return False
        self.connected = True
        return True

    def send_report(self, report: bytes) -> bool:
        if not self.connected:
            return False
        if len(report) != 8:
            return False
        self.transmitted_reports.append(report)
        return True

    def disconnect(self):
        self.connected = False


class AndroidForegroundServiceSimulator:
    """
    Simulates Android 14/15 Foreground Service lifecycle and checks.
    """

    def __init__(self, target_sdk: int = 35):
        self.target_sdk = target_sdk
        self.is_running = False
        self.foreground_service_type: Optional[str] = None
        self.notification_active = False
        self.wake_lock_held = False

    def start_foreground(self, fgs_type: str = "connectedDevice", has_notification: bool = True) -> bool:
        if self.target_sdk >= 34 and fgs_type != "connectedDevice":
            raise ValueError("Android 14+ requires foregroundServiceType='connectedDevice'")
        if not has_notification:
            raise ValueError("Foreground service must display ongoing notification")
        self.is_running = True
        self.foreground_service_type = fgs_type
        self.notification_active = True
        self.wake_lock_held = True
        return True

    def stop_foreground(self):
        self.is_running = False
        self.foreground_service_type = None
        self.notification_active = False
        self.wake_lock_held = False
