"""
UI & MVI State Machine Simulator for Jetpack Compose Architecture.
Models MainViewModel, immutable MainUiState, MainUiIntent, Gboard IME Voice Canvas,
HotkeyDockBar, SnippetsPadScreen, VariablePromptBottomSheet, and Multi-Host ConnectionHeader.
"""
from dataclasses import dataclass, field
from typing import List, Optional, Callable, Dict, Any

from .keymap_engine import KeyLayout, HOTKEY_MAP
from .ai_pipeline_simulator import (
    PromptPreset, BUILTIN_CLEAN_POLISH, BUILTIN_BUSINESS_GERMAN, BUILTIN_CODE_COMMENTS,
    TextRewriter, GeminiRemoteRewriter
)
from .persistence_simulator import (
    PresetRepositorySimulator, SettingsRepositorySimulator,
    AppDatabaseSimulator, SnippetEntity, CategoryEntity, PairedHostEntity
)
from .dispatcher_simulator import KeystrokeDispatcher
from .service_simulator import ConnectionState, BluetoothHidTransport
from .multi_host_simulator import MultiHostTransportSimulator, MultiHostState
from .variable_parser_simulator import VariableParser, PromptDescriptor, InterpolationContext


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

    # Next-Gen Developer Power Suite UI States
    categories: List[CategoryEntity] = field(default_factory=list)
    selected_category_id: Optional[str] = None
    snippets: List[SnippetEntity] = field(default_factory=list)
    search_query: str = ""
    selected_snippet: Optional[SnippetEntity] = None
    is_prompt_sheet_visible: bool = False
    prompt_descriptors: List[PromptDescriptor] = field(default_factory=list)
    prompt_answers: Dict[str, str] = field(default_factory=dict)
    paired_hosts: List[PairedHostEntity] = field(default_factory=list)
    active_host: Optional[PairedHostEntity] = None
    is_switching_host: bool = False


class MainViewModelSimulator:
    """
    Simulates Android MainViewModel coordinating UI intents and emitting MainUiState.
    """

    def __init__(
        self,
        preset_repo: Optional[PresetRepositorySimulator] = None,
        settings_repo: Optional[SettingsRepositorySimulator] = None,
        rewriter: Optional[TextRewriter] = None,
        dispatcher: Optional[KeystrokeDispatcher] = None,
        transport: Optional[BluetoothHidTransport] = None,
        db: Optional[AppDatabaseSimulator] = None,
        multi_host_transport: Optional[MultiHostTransportSimulator] = None
    ):
        self.db = db or AppDatabaseSimulator(version=2, seed_defaults=True)
        self.preset_repo = preset_repo or PresetRepositorySimulator(self.db.preset_dao)
        self.settings_repo = settings_repo or SettingsRepositorySimulator()
        self.rewriter = rewriter
        self.dispatcher = dispatcher
        self.transport = transport
        self.multi_host_transport = multi_host_transport

        # Initialize State
        presets = self.preset_repo.get_all_presets()
        categories = self.db.category_dao.get_all_categories()
        snippets = self.db.snippet_dao.get_all_snippets()
        paired_hosts = self.db.paired_host_dao.get_all_hosts()
        active_host = self.db.paired_host_dao.get_last_connected_host() or (paired_hosts[0] if paired_hosts else None)

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
            undo_history=[],
            categories=categories,
            selected_category_id=categories[0].id if categories else None,
            snippets=snippets,
            search_query="",
            selected_snippet=None,
            is_prompt_sheet_visible=False,
            prompt_descriptors=[],
            prompt_answers={},
            paired_hosts=paired_hosts,
            active_host=active_host,
            is_switching_host=False
        )
        self._listeners: List[Callable[[MainUiState], None]] = []

    @property
    def state(self) -> MainUiState:
        return self._state

    def _update_state(self, **kwargs):
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
            "categories": self._state.categories,
            "selected_category_id": self._state.selected_category_id,
            "snippets": self._state.snippets,
            "search_query": self._state.search_query,
            "selected_snippet": self._state.selected_snippet,
            "is_prompt_sheet_visible": self._state.is_prompt_sheet_visible,
            "prompt_descriptors": self._state.prompt_descriptors,
            "prompt_answers": self._state.prompt_answers,
            "paired_hosts": self._state.paired_hosts,
            "active_host": self._state.active_host,
            "is_switching_host": self._state.is_switching_host
        }
        current_dict.update(kwargs)
        self._state = MainUiState(**current_dict)
        for l in self._listeners:
            l(self._state)

    def add_state_listener(self, listener: Callable[[MainUiState], None]):
        self._listeners.append(listener)
        listener(self._state)

    # -----------------------------------------------------------------------
    # Transcription & Live Diff Intents
    # -----------------------------------------------------------------------
    def on_text_changed(self, new_text: str):
        old_text = self._state.transcription_text
        undo_list = list(self._state.undo_history)
        if old_text and (not undo_list or undo_list[-1] != old_text):
            undo_list.append(old_text)

        self._update_state(
            transcription_text=new_text,
            undo_history=undo_list,
            error_message=None
        )

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

    # -----------------------------------------------------------------------
    # Hotkey Dock Bar Intents
    # -----------------------------------------------------------------------
    def on_hotkey_pressed(self, hotkey_name: str) -> bool:
        """Dispatched when a hotkey button on HotkeyDockBar is tapped."""
        stroke = HOTKEY_MAP.get(hotkey_name.upper())
        if not stroke:
            self._update_state(error_message=f"Unknown hotkey: {hotkey_name}")
            return False

        self._update_state(is_transmitting=True, error_message=None)
        ok = self.dispatcher.send_single_keystroke(stroke.modifier_mask, stroke.usage_id)
        self._update_state(is_transmitting=False)
        return ok

    def on_clipboard_stream_pressed(self, clipboard_content: str, bracketed: bool = True) -> bool:
        """Dispatched when Clipboard Stream button is tapped."""
        if not clipboard_content:
            return False

        self._update_state(is_transmitting=True, error_message=None)
        ok = self.dispatcher.stream_clipboard_to_host(
            clipboard_content,
            bracketed_paste=bracketed,
            delay_ms=self._state.typing_delay_ms
        )
        self._update_state(is_transmitting=False)
        return ok

    # -----------------------------------------------------------------------
    # Snippets Pad & Variable Prompt Intents
    # -----------------------------------------------------------------------
    def on_category_selected(self, category_id: Optional[str]):
        if category_id:
            filtered = self.db.snippet_dao.get_snippets_by_category(category_id)
        else:
            filtered = self.db.snippet_dao.get_all_snippets()
        self._update_state(selected_category_id=category_id, snippets=filtered)

    def on_search_query_changed(self, query: str):
        filtered = self.db.snippet_dao.search_snippets(query)
        self._update_state(search_query=query, snippets=filtered)

    def on_snippet_clicked(self, snippet: SnippetEntity, clipboard_content: str = ""):
        """Checks for prompts. If template contains prompts, opens VariablePromptBottomSheet."""
        prompts = VariableParser.extract_prompts(snippet.content)
        if prompts:
            # Need user input -> open Bottom Sheet modal
            self._update_state(
                selected_snippet=snippet,
                is_prompt_sheet_visible=True,
                prompt_descriptors=prompts,
                prompt_answers={p.key: p.default_value for p in prompts}
            )
        else:
            # No prompts -> execute immediately
            self.on_dispatch_snippet(snippet, clipboard_content=clipboard_content)

    def on_prompt_answer_changed(self, key: str, value: str):
        current_answers = dict(self._state.prompt_answers)
        current_answers[key] = value
        self._update_state(prompt_answers=current_answers)

    def on_confirm_prompt_and_dispatch(self, clipboard_content: str = "") -> bool:
        snippet = self._state.selected_snippet
        if not snippet:
            return False

        ctx = InterpolationContext(
            clipboard_text=clipboard_content,
            prompt_values=self._state.prompt_answers
        )
        interpolated = VariableParser.parse(snippet.content, ctx)
        self._update_state(is_prompt_sheet_visible=False, selected_snippet=None)

        self._update_state(is_transmitting=True)
        ok = self.dispatcher.dispatch_burst(interpolated, delay_ms=self._state.typing_delay_ms)
        self._update_state(is_transmitting=False)
        return ok

    def on_dismiss_prompt_sheet(self):
        self._update_state(is_prompt_sheet_visible=False, selected_snippet=None, prompt_answers={})

    def on_dispatch_snippet(self, snippet: SnippetEntity, clipboard_content: str = "") -> bool:
        ctx = InterpolationContext(clipboard_text=clipboard_content)
        interpolated = VariableParser.parse(snippet.content, ctx)
        self._update_state(is_transmitting=True)
        ok = self.dispatcher.dispatch_burst(interpolated, delay_ms=self._state.typing_delay_ms)
        self._update_state(is_transmitting=False)
        return ok

    # -----------------------------------------------------------------------
    # Multi-Host Switching Intents
    # -----------------------------------------------------------------------
    def on_select_host_to_switch(self, target_host: PairedHostEntity) -> bool:
        if self.multi_host_transport is None:
            self._update_state(active_host=target_host)
            return True

        self._update_state(is_switching_host=True, error_message=None)
        ok = self.multi_host_transport.switch_host(target_host)
        self._update_state(
            is_switching_host=False,
            active_host=target_host if ok else self._state.active_host,
            error_message=None if ok else "Host switch failed or timed out"
        )
        return ok

    # -----------------------------------------------------------------------
    # AI Presets
    # -----------------------------------------------------------------------
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

