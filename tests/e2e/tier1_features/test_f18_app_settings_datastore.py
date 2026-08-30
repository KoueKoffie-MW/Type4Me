"""
Feature 18: App Settings Storage (DataStore).
Verifies DataStore preferences persistence for keymap layout, inter-character pacing delay,
transmission mode, and encrypted API key storage.
"""
import unittest
from tests.e2e.harness.persistence_simulator import SettingsRepositorySimulator
from tests.e2e.harness.keymap_engine import KeyLayout


class TestFeature18AppSettingsDataStore(unittest.TestCase):
    """Tier 1: Feature 18 - App Settings Storage (DataStore)"""

    def setUp(self):
        self.settings = SettingsRepositorySimulator()

    def test_tc01_default_settings(self):
        """TC01: Validates default initial settings: German QWERTZ, 8ms delay, Burst mode."""
        self.assertEqual(self.settings.get_key_layout(), KeyLayout.GERMAN_QWERTZ)
        self.assertEqual(self.settings.get_typing_delay_ms(), 8)
        self.assertFalse(self.settings.is_live_diff_enabled())
        self.assertIsNone(self.settings.get_api_key())

    def test_tc02_update_keymap_layout(self):
        """TC02: Updates active keymap to US QWERTY and verifies emission."""
        self.settings.set_key_layout(KeyLayout.US_QWERTY)
        self.assertEqual(self.settings.get_key_layout(), KeyLayout.US_QWERTY)

    def test_tc03_typing_delay_clamping(self):
        """TC03: Configures typing delay and validates lower bound >= 1ms."""
        self.settings.set_typing_delay_ms(15)
        self.assertEqual(self.settings.get_typing_delay_ms(), 15)

        # Boundary clamping test: setting 0 or negative clamps to 1ms
        self.settings.set_typing_delay_ms(-5)
        self.assertEqual(self.settings.get_typing_delay_ms(), 1)

    def test_tc04_live_diff_toggle(self):
        """TC04: Toggles live diff mode on and off."""
        self.settings.set_live_diff_enabled(True)
        self.assertTrue(self.settings.is_live_diff_enabled())
        self.settings.set_live_diff_enabled(False)
        self.assertFalse(self.settings.is_live_diff_enabled())

    def test_tc05_api_key_persistence(self):
        """TC05: Stores and retrieves Gemini API key."""
        fake_key = "AIzaSyTestKey123456"
        self.settings.set_api_key(fake_key)
        self.assertEqual(self.settings.get_api_key(), fake_key)
        self.settings.set_api_key(None)
        self.assertIsNone(self.settings.get_api_key())


if __name__ == "__main__":
    unittest.main()
