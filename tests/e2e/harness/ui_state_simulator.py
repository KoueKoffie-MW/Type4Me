"""
UI & MVI State Machine Simulator for Jetpack Compose Single-Screen Architecture.
Models MainViewModel, immutable MainUiState, MainUiIntent, and Gboard IME Voice Typing Canvas.
"""
from dataclasses import dataclass, field
from typing import List, Optional, Callable, Dict, Any

from .keymap_engine import KeyLayout
from .ai_pipeline_simulator import (
    PromptPreset, BUILTIN_CLEAN_POLISH, BUILTIN_BUSINESS_GERMAN, BUILTIN_CODE_COMMENTS,
    TextRewriter, GeminiRemoteRewriter
)
from .persistence_simulator import PresetRepositorySimulator, SettingsRepositorySimulator
from .dispatcher_simulator import KeystrokeDispatcher
from .service_simulator import ConnectionState, BluetoothHidTransport


@dataclass(frozen=True)
class MainUiState:
    transcription_text: str = ""
    active_layout: KeyLayout = KeyLayout.GERMAN_QWERTZ
    connection_state: ConnectionState = ConnectionState.DISCONNECTED
    connected_device_name: Optional[str] = None
    live_diff_enabled: bool = False
    typing_delay_ms: int = 8
    presets: List[PromptPreset] = field(default_factory=list)
    selected_preset: Optional[PromptPreset] = None
    is_ai_rewriting: bool = False
    is_transmitting: bool = False
    error_message: Optional[str] = None
    undo_history: List[str] = field(default_factory=list)


class MainViewModelSimulator:
    """
    Simulates Android MainViewModel coordinating UI intents and emitting MainUiState.
    """

    def __init__(
        self,
        preset_repo: PresetRepositorySimulator,
        settings_repo: SettingsRepositorySimulator,
        rewriter: TextRewriter,
        dispatcher: KeystrokeDispatcher,
        transport: BluetoothHidTransport
    ):
        self.preset_repo = preset_repo
        self.settings_repo = settings_repo
        self.rewriter = rewriter
        self.dispatcher = dispatcher
        self.transport = transport

        # Initialize State
        presets = self.preset_repo.get_all_presets()
        self._state = MainUiState(
            transcription_text="",
            active_layout=self.settings_repo.get_key_layout(),
            connection_state=self.transport.connection_state,
            connected_device_name=self.transport.connected_device.name if self.transport.connected_device else None,
            live_diff_enabled=self.settings_repo.is_live_diff_enabled(),
            typing_delay_ms=self.settings_repo.get_typing_delay_ms(),
            presets=presets,
            selected_preset=presets[0] if presets else BUILTIN_CLEAN_POLISH,
            is_ai_rewriting=False,
            is_transmitting=False,
            error_message=None,
            undo_history=[]
        )
        self._listeners: List[Callable[[MainUiState], None]] = []

    @property
    def state(self) -> MainUiState:
        return self._state

    def _update_state(self, **kwargs):
        # Create new immutable copy
        current_dict = {
            "transcription_text": self._state.transcription_text,
            "active_layout": self._state.active_layout,
            "connection_state": self._state.connection_state,
            "connected_device_name": self._state.connected_device_name,
            "live_diff_enabled": self._state.live_diff_enabled,
            "typing_delay_ms": self._state.typing_delay_ms,
            "presets": self._state.presets,
            "selected_preset": self._state.selected_preset,
            "is_ai_rewriting": self._state.is_ai_rewriting,
            "is_transmitting": self._state.is_transmitting,
            "error_message": self._state.error_message,
            "undo_history": self._state.undo_history,
        }
        current_dict.update(kwargs)
        self._state = MainUiState(**current_dict)
        for l in self._listeners:
            l(self._state)

    def add_state_listener(self, listener: Callable[[MainUiState], None]):
        self._listeners.append(listener)
        listener(self._state)

    def on_text_changed(self, new_text: str):
        """Dispatched when Gboard or user modifies text."""
        old_text = self._state.transcription_text
        undo_list = list(self._state.undo_history)
        if old_text and (not undo_list or undo_list[-1] != old_text):
            undo_list.append(old_text)

        self._update_state(
            transcription_text=new_text,
            undo_history=undo_list,
            error_message=None
        )

        # If live diff mode is active, dispatch diff keystrokes to host in real-time
        if self._state.live_diff_enabled and self._state.connection_state == ConnectionState.CONNECTED:
            self._update_state(is_transmitting=True)
            self.dispatcher.dispatch_live_diff(new_text, delay_ms=self._state.typing_delay_ms)
            self._update_state(is_transmitting=False)

    def on_layout_selected(self, layout: KeyLayout):
        self.settings_repo.set_key_layout(layout)
        self._update_state(active_layout=layout)

    def on_live_diff_toggled(self, enabled: bool):
        self.settings_repo.set_live_diff_enabled(enabled)
        self._update_state(live_diff_enabled=enabled)

    def on_delay_changed(self, delay_ms: int):
        self.settings_repo.set_typing_delay_ms(delay_ms)
        self._update_state(typing_delay_ms=delay_ms)

    def on_preset_selected(self, preset: PromptPreset):
        self._update_state(selected_preset=preset)

    def on_trigger_ai_rewrite(self) -> bool:
        if not self._state.selected_preset or not self._state.transcription_text:
            return False

        self._update_state(is_ai_rewriting=True, error_message=None)
        success, transformed, error = self.rewriter.rewrite(
            self._state.transcription_text,
            self._state.selected_preset
        )

        if success:
            undo_list = list(self._state.undo_history)
            undo_list.append(self._state.transcription_text)
            self._update_state(
                transcription_text=transformed,
                is_ai_rewriting=False,
                undo_history=undo_list
            )
            return True
        else:
            self._update_state(
                is_ai_rewriting=False,
                error_message=error
            )
            return False

    def on_send_burst_to_host(self) -> bool:
        if self._state.connection_state != ConnectionState.CONNECTED:
            self._update_state(error_message="Host PC is not connected.")
            return False

        if not self._state.transcription_text:
            return False

        self._update_state(is_transmitting=True, error_message=None)
        ok = self.dispatcher.dispatch_burst(
            self._state.transcription_text,
            delay_ms=self._state.typing_delay_ms
        )
        self._update_state(is_transmitting=False)
        return ok

    def on_clear_text(self):
        old_text = self._state.transcription_text
        undo_list = list(self._state.undo_history)
        if old_text:
            undo_list.append(old_text)
        self._update_state(transcription_text="", undo_history=undo_list)

    def on_undo(self) -> bool:
        if not self._state.undo_history:
            return False
        undo_list = list(self._state.undo_history)
        prev_text = undo_list.pop()
        self._update_state(transcription_text=prev_text, undo_history=undo_list)
        return True

    def on_save_custom_preset(self, title: str, system_prompt: str) -> PromptPreset:
        import uuid
        preset = PromptPreset(
            id=f"custom_{uuid.uuid4().hex[:8]}",
            title=title,
            description=f"User preset: {title}",
            system_prompt=system_prompt,
            is_builtin=False,
            order_index=len(self._state.presets) + 1
        )
        self.preset_repo.save_preset(preset)
        updated_presets = self.preset_repo.get_all_presets()
        self._update_state(presets=updated_presets, selected_preset=preset)
        return preset

    def on_delete_custom_preset(self, preset: PromptPreset) -> bool:
        ok = self.preset_repo.delete_preset(preset)
        if ok:
            updated = self.preset_repo.get_all_presets()
            sel = updated[0] if updated else BUILTIN_CLEAN_POLISH
            self._update_state(presets=updated, selected_preset=sel)
        return ok
