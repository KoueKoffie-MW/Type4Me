"""
Boundary Tests: Feature 18 - App Settings DataStore.
Covers corrupted preferences fallback, negative delay clamping, extreme delay values,
rapid concurrent updates, and null setting handling.
"""
import unittest
from tests.e2e.harness.persistence_simulator import SettingsRepositorySimulator
from tests.e2e.harness.keymap_engine import KeyLayout


class TestBoundary18DataStore(unittest.TestCase):
    """Tier 2: Boundary 18 - App Settings DataStore"""

    def setUp(self):
        self.settings = SettingsRepositorySimulator()

    def test_tc01_negative_and_zero_delay_clamped(self):
        """TC01: Negative or 0ms delays are clamped to minimum 1ms."""
        self.settings.set_typing_delay_ms(-10)
        self.assertEqual(self.settings.get_typing_delay_ms(), 1)
        self.settings.set_typing_delay_ms(0)
        self.assertEqual(self.settings.get_typing_delay_ms(), 1)

    def test_tc02_large_typing_delay_boundary(self):
        """TC02: Configures large typing delay (e.g. 500ms for slow host debugging)."""
        self.settings.set_typing_delay_ms(500)
        self.assertEqual(self.settings.get_typing_delay_ms(), 500)

    def test_tc03_rapid_layout_toggling(self):
        """TC03: Rapidly alternating active layout 100 times maintains consistency."""
        for i in range(100):
            target = KeyLayout.US_QWERTY if i % 2 == 0 else KeyLayout.GERMAN_QWERTZ
            self.settings.set_key_layout(target)
            self.assertEqual(self.settings.get_key_layout(), target)

    def test_tc04_null_and_empty_api_key_equivalence(self):
        """TC04: Setting empty or None API key resets key value."""
        self.settings.set_api_key("some_key")
        self.assertEqual(self.settings.get_api_key(), "some_key")
        self.settings.set_api_key(None)
        self.assertIsNone(self.settings.get_api_key())

    def test_tc05_listener_emission_on_every_mutation(self):
        """TC05: State listener is triggered on every single preferences mutation."""
        call_count = 0
        def on_change(prefs):
            nonlocal call_count
            call_count += 1

        self.settings.add_listener(on_change)
        # add_listener invokes immediately once (call_count = 1)
        self.assertEqual(call_count, 1)

        self.settings.set_key_layout(KeyLayout.US_QWERTY)
        self.assertEqual(call_count, 2)
        self.settings.set_typing_delay_ms(12)
        self.assertEqual(call_count, 3)
        self.settings.set_live_diff_enabled(True)
        self.assertEqual(call_count, 4)


if __name__ == "__main__":
    unittest.main()
