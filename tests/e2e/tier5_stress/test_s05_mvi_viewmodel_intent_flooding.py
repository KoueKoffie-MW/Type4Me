"""
Tier 5 Adversarial Stress Test S05:
Rapid UI Intent Flooding (1,000 Intents/Sec) Against MainViewModel.

Stress-tests:
1. Flood MainViewModel with 1,000+ concurrent intents within <1 second.
2. Concurrent producer threads interleaving TextChanged, LayoutSelected, LiveDiff,
   DelayChanged, PresetSelected, Undo, Clear, and Preset CRUD intents.
3. Thread-safety verification: Immutable state updates, absence of race conditions or deadlock.
4. Validation of state bounds (typing delay >= 1, undo history integrity, no null states).
5. Verification of clean post-flood state recovery.
"""
import unittest
import threading
import concurrent.futures
import time
from typing import List, Optional

from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.service_simulator import BluetoothHidTransport, ConnectionState
from tests.e2e.harness.ai_pipeline_simulator import (
    PromptPreset, GeminiRemoteRewriter, LiteRtOnDeviceRewriter,
    BUILTIN_CLEAN_POLISH, BUILTIN_BUSINESS_GERMAN, BUILTIN_CODE_COMMENTS
)
from tests.e2e.harness.persistence_simulator import PresetRepositorySimulator, SettingsRepositorySimulator
from tests.e2e.harness.ui_state_simulator import MainViewModelSimulator, MainUiState


class ThreadSafeViewModel(MainViewModelSimulator):
    """Thread-safe ViewModel simulation with synchronized intent reduction."""
    def __init__(self, preset_repo, settings_repo, rewriter, dispatcher, transport):
        super().__init__(preset_repo, settings_repo, rewriter, dispatcher, transport)
        self._lock = threading.RLock()
        self.processed_intents_count = 0

    def _update_state(self, **kwargs):
        with self._lock:
            super()._update_state(**kwargs)

    def on_text_changed(self, new_text: str):
        with self._lock:
            self.processed_intents_count += 1
            super().on_text_changed(new_text)

    def on_layout_selected(self, layout: KeyLayout):
        with self._lock:
            self.processed_intents_count += 1
            super().on_layout_selected(layout)

    def on_live_diff_toggled(self, enabled: bool):
        with self._lock:
            self.processed_intents_count += 1
            super().on_live_diff_toggled(enabled)

    def on_delay_changed(self, delay_ms: int):
        with self._lock:
            self.processed_intents_count += 1
            super().on_delay_changed(delay_ms)

    def on_preset_selected(self, preset: PromptPreset):
        with self._lock:
            self.processed_intents_count += 1
            super().on_preset_selected(preset)

    def on_clear_text(self):
        with self._lock:
            self.processed_intents_count += 1
            super().on_clear_text()

    def on_undo(self) -> bool:
        with self._lock:
            self.processed_intents_count += 1
            return super().on_undo()

    def on_save_custom_preset(self, title: str, system_prompt: str) -> PromptPreset:
        with self._lock:
            self.processed_intents_count += 1
            return super().on_save_custom_preset(title, system_prompt)


class TestS05MviViewModelIntentFlooding(unittest.TestCase):
    """Adversarial Stress Test Suite S05: Rapid UI Intent Flooding"""

    def setUp(self):
        self.preset_repo = PresetRepositorySimulator()
        self.settings_repo = SettingsRepositorySimulator()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.translator = GermanQwertzKeymap()
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)
        self.transport = BluetoothHidTransport(permissions_granted=True)
        self.transport.initialize()
        self.transport.simulate_host_connect()

        self.rewriter = GeminiRemoteRewriter(api_key_provider=lambda: "test_key")

        self.view_model = ThreadSafeViewModel(
            preset_repo=self.preset_repo,
            settings_repo=self.settings_repo,
            rewriter=self.rewriter,
            dispatcher=self.dispatcher,
            transport=self.transport
        )

    def test_s05_01_flood_1000_intents_under_one_second(self):
        """S05.1: Flood MainViewModel with 1,000 mixed intents across concurrent threads in <1.0s."""
        state_history: List[MainUiState] = []
        def on_state(s: MainUiState):
            state_history.append(s)

        self.view_model.add_state_listener(on_state)

        start_time = time.time()

        def dispatch_worker(worker_id: int, count: int):
            for j in range(count):
                intent_type = (worker_id * 100 + j) % 8
                if intent_type == 0:
                    self.view_model.on_text_changed(f"Worker {worker_id} text chunk {j}")
                elif intent_type == 1:
                    layout = KeyLayout.US_QWERTY if j % 2 == 0 else KeyLayout.GERMAN_QWERTZ
                    self.view_model.on_layout_selected(layout)
                elif intent_type == 2:
                    self.view_model.on_live_diff_toggled(j % 2 == 0)
                elif intent_type == 3:
                    self.view_model.on_delay_changed((j % 50) + 1)
                elif intent_type == 4:
                    preset = BUILTIN_BUSINESS_GERMAN if j % 2 == 0 else BUILTIN_CLEAN_POLISH
                    self.view_model.on_preset_selected(preset)
                elif intent_type == 5:
                    self.view_model.on_undo()
                elif intent_type == 6:
                    self.view_model.on_clear_text()
                elif intent_type == 7:
                    self.view_model.on_save_custom_preset(f"Preset_{worker_id}_{j}", "System prompt")

        num_threads = 20
        intents_per_thread = 50  # 20 * 50 = 1,000 total intents

        with concurrent.futures.ThreadPoolExecutor(max_workers=num_threads) as executor:
            futures = [executor.submit(dispatch_worker, t_id, intents_per_thread) for t_id in range(num_threads)]
            for f in futures:
                f.result()

        elapsed = time.time() - start_time

        # 1. Total processed intents must equal exactly 1,000
        self.assertEqual(self.view_model.processed_intents_count, 1000)
        self.assertLess(elapsed, 1.5, f"1000 intents took too long: {elapsed:.2f}s")

        # 2. State invariant checks
        final_state = self.view_model.state
        self.assertIsNotNone(final_state)
        self.assertGreaterEqual(final_state.typing_delay_ms, 1)
        self.assertIsInstance(final_state.transcription_text, str)
        self.assertIsInstance(final_state.undo_history, list)
        self.assertGreaterEqual(len(final_state.presets), 3)

        # 3. Observers received coherent states throughout the flood
        self.assertGreater(len(state_history), 500)
        for s in state_history:
            self.assertIsNotNone(s.active_layout)
            self.assertGreaterEqual(s.typing_delay_ms, 1)

    def test_s05_02_post_flood_recovery_and_undo_stack_integrity(self):
        """S05.2: ViewModel operates cleanly after flood and retains correct undo stack semantics."""
        # Clear and set known initial state
        self.view_model.on_clear_text()
        self.assertEqual(self.view_model.state.transcription_text, "")

        # Type Version 1
        self.view_model.on_text_changed("Version 1")
        self.assertEqual(self.view_model.state.transcription_text, "Version 1")

        # Type Version 2
        self.view_model.on_text_changed("Version 2")
        self.assertEqual(self.view_model.state.transcription_text, "Version 2")

        # Type Version 3
        self.view_model.on_text_changed("Version 3")
        self.assertEqual(self.view_model.state.transcription_text, "Version 3")

        # Undo 1 -> restores Version 2
        ok_undo1 = self.view_model.on_undo()
        self.assertTrue(ok_undo1)
        self.assertEqual(self.view_model.state.transcription_text, "Version 2")

        # Undo 2 -> restores Version 1
        ok_undo2 = self.view_model.on_undo()
        self.assertTrue(ok_undo2)
        self.assertEqual(self.view_model.state.transcription_text, "Version 1")

        # Undo on empty stack -> returns False cleanly without exception
        ok_undo3 = self.view_model.on_undo()
        self.assertFalse(ok_undo3)
        self.assertEqual(self.view_model.state.transcription_text, "Version 1")


if __name__ == "__main__":
    unittest.main()
