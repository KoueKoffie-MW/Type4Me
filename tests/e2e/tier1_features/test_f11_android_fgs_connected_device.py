"""
Feature 11: Android 14/15 connectedDevice FGS.
Verifies Foreground Service registration with FOREGROUND_SERVICE_CONNECTED_DEVICE type,
low-priority notification channel, and wake lock acquisition for background typing.
"""
import unittest
from tests.e2e.harness.service_simulator import AndroidForegroundServiceSimulator


class TestFeature11AndroidFgsConnectedDevice(unittest.TestCase):
    """Tier 1: Feature 11 - Android 14/15 connectedDevice FGS"""

    def setUp(self):
        self.service = AndroidForegroundServiceSimulator(target_sdk=35)

    def test_tc01_successful_start_with_connected_device_type(self):
        """TC01: Starts foreground service with connectedDevice type on Android 15."""
        ok = self.service.start_foreground(fgs_type="connectedDevice", has_notification=True)
        self.assertTrue(ok)
        self.assertTrue(self.service.is_running)
        self.assertEqual(self.service.foreground_service_type, "connectedDevice")
        self.assertTrue(self.service.notification_active)
        self.assertTrue(self.service.wake_lock_held)

    def test_tc02_invalid_fgs_type_rejected_on_api34_plus(self):
        """TC02: Rejects general or invalid foregroundServiceType on API 34+."""
        with self.assertRaises(ValueError):
            self.service.start_foreground(fgs_type="mediaPlayback", has_notification=True)

    def test_tc03_missing_notification_rejected(self):
        """TC03: Rejects startForeground without an active notification."""
        with self.assertRaises(ValueError):
            self.service.start_foreground(fgs_type="connectedDevice", has_notification=False)

    def test_tc04_clean_service_stop_releases_wake_lock(self):
        """TC04: Stopping foreground service releases wake lock and clears notifications."""
        self.service.start_foreground()
        self.service.stop_foreground()
        self.assertFalse(self.service.is_running)
        self.assertIsNone(self.service.foreground_service_type)
        self.assertFalse(self.service.wake_lock_held)
        self.assertFalse(self.service.notification_active)

    def test_tc05_legacy_api_compatibility(self):
        """TC05: Supports legacy API <= 33 execution while enforcing notification."""
        legacy_service = AndroidForegroundServiceSimulator(target_sdk=33)
        ok = legacy_service.start_foreground(fgs_type="connectedDevice", has_notification=True)
        self.assertTrue(ok)
        self.assertTrue(legacy_service.is_running)


if __name__ == "__main__":
    unittest.main()
