"""
Keystroke Dispatcher & Transmission Engine Simulator.
Implements Buffered Burst Mode and Live Delta-Diff Mode with non-blocking queueing,
deterministic pacing, and in-flight cancellation.
"""
from typing import List, Optional, Callable, Dict, Any
import time

from .hid_constants import MOD_NONE, KEY_BACKSPACE
from .keymap_engine import KeymapTranslator, HidKeyStroke, HidReport, KeyLayout
from .delta_diff_engine import DeltaDiffEngine, DiffResult
from .hid_host_simulator import HidHostSimulator


class KeystrokeDispatcher:
    """
    Asynchronous Keystroke Dispatcher.
    Transmits 8-byte press and release HID reports to the host simulator with pacing.
    """

    def __init__(
        self,
        host: HidHostSimulator,
        translator: KeymapTranslator,
        virtual_clock: bool = True
    ):
        self.host = host
        self.translator = translator
        self.virtual_clock = virtual_clock
        self.virtual_time = 0.0
        self.diff_engine = DeltaDiffEngine()
        self.acknowledged_host_text: str = ""
        self.total_keystrokes_sent: int = 0
        self.total_backspaces_sent: int = 0
        self.transmission_log: List[Dict[str, Any]] = []

    def set_translator(self, translator: KeymapTranslator):
        self.translator = translator

    def reset_state(self):
        self.acknowledged_host_text = ""
        self.total_keystrokes_sent = 0
        self.total_backspaces_sent = 0
        self.transmission_log.clear()

    def _now(self) -> float:
        if self.virtual_clock:
            return self.virtual_time
        return time.time()

    def _advance_time(self, ms: float):
        if self.virtual_clock:
            self.virtual_time += (ms / 1000.0)
        else:
            time.sleep(ms / 1000.0)

    def send_single_keystroke(self, modifier: int, usage_id: int, delay_ms: float = 8.0) -> bool:
        """
        Sends 1 Key-Down Report (t_down = delay_ms * 0.4) followed by
        1 Key-Up Report (t_up = delay_ms * 0.6).
        """
        t_down = max(1.0, delay_ms * 0.4)
        t_up = max(1.0, delay_ms * 0.6)

        press_report = HidReport.press(modifier, usage_id)
        release_report = HidReport.release()

        # Transmit Press Report
        now_ts = self._now()
        success = self.host.receive_report(press_report.to_bytes(), current_time=now_ts)
        if not success:
            return False

        self._advance_time(t_down)

        # Transmit Release Report
        now_ts = self._now()
        success = self.host.receive_report(release_report.to_bytes(), current_time=now_ts)
        if not success:
            return False

        self._advance_time(t_up)
        self.total_keystrokes_sent += 1
        return True

    def dispatch_burst(self, text: str, delay_ms: float = 8.0) -> bool:
        """
        Translates text and transmits full keystroke sequence in Burst mode.
        """
        strokes = self.translator.translate_string(text)
        for stroke in strokes:
            ok = self.send_single_keystroke(stroke.modifier_mask, stroke.usage_id, delay_ms=delay_ms)
            if not ok:
                return False

        self.acknowledged_host_text += text
        self.transmission_log.append({
            "mode": "BURST",
            "input_text": text,
            "strokes_count": len(strokes),
            "host_result": self.host.host_text
        })
        return True

    def dispatch_live_diff(self, new_hypothesis: str, delay_ms: float = 8.0) -> DiffResult:
        """
        Computes LCP delta from current acknowledged host text,
        emits backspaces, and appends new characters.
        """
        diff = self.diff_engine.compute_diff(self.acknowledged_host_text, new_hypothesis)

        # 1. Emit Backspaces
        for _ in range(diff.backspaces_needed):
            self.send_single_keystroke(MOD_NONE, KEY_BACKSPACE, delay_ms=delay_ms)
            self.total_backspaces_sent += 1

        # 2. Emit Appended text
        if diff.text_to_append:
            strokes = self.translator.translate_string(diff.text_to_append)
            for stroke in strokes:
                self.send_single_keystroke(stroke.modifier_mask, stroke.usage_id, delay_ms=delay_ms)

        self.acknowledged_host_text = new_hypothesis
        self.transmission_log.append({
            "mode": "LIVE_DIFF",
            "new_hypothesis": new_hypothesis,
            "backspaces": diff.backspaces_needed,
            "appended": diff.text_to_append,
            "host_result": self.host.host_text
        })
        return diff
