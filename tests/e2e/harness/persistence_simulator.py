"""
Persistence Layer Simulator: Room DB and Jetpack DataStore Preferences.
Models reactive Flow emissions, CRUD operations, database constraints,
and encrypted settings storage.
"""
from dataclasses import dataclass
from typing import List, Optional, Dict, Any, Callable
import time

from .keymap_engine import KeyLayout
from .ai_pipeline_simulator import (
    PromptPreset, BUILTIN_CLEAN_POLISH, BUILTIN_BUSINESS_GERMAN, BUILTIN_CODE_COMMENTS
)


@dataclass
class PresetEntity:
    id: str
    name: str
    icon_name: str
    system_prompt: str
    user_prompt_template: str = "{INPUT_TEXT}"
    temperature: float = 0.2
    is_builtin: bool = False
    order_index: int = 0
    created_at: int = 0

    def to_domain(self) -> PromptPreset:
        return PromptPreset(
            id=self.id,
            title=self.name,
            description=f"Prompt: {self.system_prompt[:50]}...",
            system_prompt=self.system_prompt,
            user_prompt_template=self.user_prompt_template,
            temperature=self.temperature,
            is_builtin=self.is_builtin,
            order_index=self.order_index
        )

    @classmethod
    def from_domain(cls, preset: PromptPreset) -> "PresetEntity":
        return cls(
            id=preset.id,
            name=preset.title,
            icon_name="ic_preset_default",
            system_prompt=preset.system_prompt,
            user_prompt_template=preset.user_prompt_template,
            temperature=preset.temperature,
            is_builtin=preset.is_builtin,
            order_index=preset.order_index,
            created_at=int(time.time() * 1000)
        )


class PresetDaoSimulator:
    """Simulates Room SQLite Preset DAO with reactive observers."""

    def __init__(self):
        self._table: Dict[str, PresetEntity] = {}
        self._observers: List[Callable[[List[PresetEntity]], None]] = []

        # Seed built-ins
        for p in [BUILTIN_CLEAN_POLISH, BUILTIN_BUSINESS_GERMAN, BUILTIN_CODE_COMMENTS]:
            entity = PresetEntity.from_domain(p)
            self._table[entity.id] = entity

    def _notify(self):
        all_presets = self.get_all_presets()
        for obs in self._observers:
            obs(all_presets)

    def add_observer(self, callback: Callable[[List[PresetEntity]], None]):
        self._observers.append(callback)
        callback(self.get_all_presets())

    def get_all_presets(self) -> List[PresetEntity]:
        presets = list(self._table.values())
        # Order by is_builtin DESC, order_index ASC, created_at ASC
        presets.sort(key=lambda p: (not p.is_builtin, p.order_index, p.created_at))
        return presets

    def get_preset_by_id(self, preset_id: str) -> Optional[PresetEntity]:
        return self._table.get(preset_id)

    def insert_preset(self, preset: PresetEntity):
        self._table[preset.id] = preset
        self._notify()

    def update_preset(self, preset: PresetEntity):
        if preset.id in self._table:
            self._table[preset.id] = preset
            self._notify()

    def delete_preset(self, preset_id: str) -> bool:
        entity = self._table.get(preset_id)
        if entity is None:
            return False
        # Constraint: built-in presets cannot be deleted
        if entity.is_builtin:
            return False
        del self._table[preset_id]
        self._notify()
        return True


class PresetRepositorySimulator:
    """Simulates Preset Repository wrapping DAO."""

    def __init__(self, dao: Optional[PresetDaoSimulator] = None):
        self.dao = dao or PresetDaoSimulator()

    def get_all_presets(self) -> List[PromptPreset]:
        return [e.to_domain() for e in self.dao.get_all_presets()]

    def get_preset_by_id(self, preset_id: str) -> Optional[PromptPreset]:
        entity = self.dao.get_preset_by_id(preset_id)
        return entity.to_domain() if entity else None

    def save_preset(self, preset: PromptPreset):
        entity = PresetEntity.from_domain(preset)
        self.dao.insert_preset(entity)

    def delete_preset(self, preset: PromptPreset) -> bool:
        return self.dao.delete_preset(preset.id)


class SettingsRepositorySimulator:
    """Simulates Jetpack DataStore Preferences repository."""

    def __init__(self):
        self._preferences: Dict[str, Any] = {
            "keymap_layout": KeyLayout.GERMAN_QWERTZ.value,
            "transmission_mode": "BURST",
            "inter_char_delay_ms": 8,
            "live_diff_enabled": False,
            "enc_gemini_api_key": None,
            "ai_engine_type": "gemini-3.7-flash"
        }
        self._listeners: List[Callable[[Dict[str, Any]], None]] = []

    def _notify(self):
        for l in self._listeners:
            l(dict(self._preferences))

    def add_listener(self, listener: Callable[[Dict[str, Any]], None]):
        self._listeners.append(listener)
        listener(dict(self._preferences))

    def get_key_layout(self) -> KeyLayout:
        val = self._preferences.get("keymap_layout", KeyLayout.GERMAN_QWERTZ.value)
        return KeyLayout(val)

    def set_key_layout(self, layout: KeyLayout):
        self._preferences["keymap_layout"] = layout.value
        self._notify()

    def get_typing_delay_ms(self) -> int:
        return max(1, int(self._preferences.get("inter_char_delay_ms", 8)))

    def set_typing_delay_ms(self, delay_ms: int):
        # Boundary validation: clamp >= 1ms
        self._preferences["inter_char_delay_ms"] = max(1, delay_ms)
        self._notify()

    def is_live_diff_enabled(self) -> bool:
        return bool(self._preferences.get("live_diff_enabled", False))

    def set_live_diff_enabled(self, enabled: bool):
        self._preferences["live_diff_enabled"] = enabled
        self._notify()

    def get_api_key(self) -> Optional[str]:
        return self._preferences.get("enc_gemini_api_key")

    def set_api_key(self, api_key: Optional[str]):
        self._preferences["enc_gemini_api_key"] = api_key
        self._notify()
