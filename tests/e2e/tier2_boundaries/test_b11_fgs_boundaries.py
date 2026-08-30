"""
Boundary Tests: Feature 11 - Android Foreground Service Boundaries.
Covers background start restrictions on Android 14/15, notification channel edge cases,
wake lock management under stress, and service restart semantics.
"""
import unittest
from tests.e2e.harness.service_simulator import AndroidForegroundServiceSimulator


class TestBoundary11Fgs(unittest.TestCase):
    """Tier 2: Boundary 11 - Android Foreground Service Boundaries"""

    def test_tc01_api35_strict_connected_device_type(self):
        """TC01: Android 15 requires strictly connectedDevice foreground service type."""
        svc = AndroidForegroundServiceSimulator(target_sdk=35)
        self.assertTrue(svc.start_foreground("connectedDevice", has_notification=True))
        with self.assertRaises(ValueError):
            svc.start_foreground("location", has_notification=True)

    def test_tc02_start_without_notification_fails(self):
        """TC02: Starting foreground service without ongoing notification raises ValueError."""
        svc = AndroidForegroundServiceSimulator(target_sdk=35)
        with self.assertRaises(ValueError):
            svc.start_foreground("connectedDevice", has_notification=False)

    def test_tc03_repeated_start_foreground_calls(self):
        """TC03: Multiple start_foreground calls keep service running with active notification."""
        svc = AndroidForegroundServiceSimulator(target_sdk=35)
        svc.start_foreground("connectedDevice", has_notification=True)
        # Update notification / restart
        svc.start_foreground("connectedDevice", has_notification=True)
        self.assertTrue(svc.is_running)
        self.assertTrue(svc.wake_lock_held)

    def test_tc04_stopping_unstarted_service_is_safe(self):
        """TC04: Calling stop_foreground on an unstarted service executes safely without error."""
        svc = AndroidForegroundServiceSimulator(target_sdk=35)
        self.assertFalse(svc.is_running)
        svc.stop_foreground()
        self.assertFalse(svc.is_running)

    def test_tc05_wake_lock_lifecycle_integrity(self):
        """TC05: Wake lock is acquired on start and released immediately on stop."""
        svc = AndroidForegroundServiceSimulator(target_sdk=35)
        self.assertFalse(svc.wake_lock_held)
        svc.start_foreground("connectedDevice", has_notification=True)
        self.assertTrue(svc.wake_lock_held)
        svc.stop_foreground()
        self.assertFalse(svc.wake_lock_held)


if __name__ == "__main__":
    unittest.main()
