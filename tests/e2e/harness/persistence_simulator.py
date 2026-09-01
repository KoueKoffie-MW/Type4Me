"""
Persistence Layer Simulator: Room DB 2.6 and Jetpack DataStore Preferences.
Models Room V2 schema: CategoryEntity, SnippetEntity, MacroEntity, PairedHostEntity,
PresetEntity, DAOs with reactive Flow emissions, MIGRATION_1_2 DDL upgrade,
and idempotent DefaultToolPackProvider preloading 20+ developer snippets.
"""
from dataclasses import dataclass, field
from typing import List, Optional, Dict, Any, Callable
import time
import json
import uuid

from .keymap_engine import KeyLayout
from .ai_pipeline_simulator import (
    PromptPreset, BUILTIN_CLEAN_POLISH, BUILTIN_BUSINESS_GERMAN, BUILTIN_CODE_COMMENTS
)


# ---------------------------------------------------------------------------
# Entities
# ---------------------------------------------------------------------------

@dataclass
class CategoryEntity:
    id: str
    name: str
    icon_name: str
    order_index: int = 0


@dataclass
class SnippetEntity:
    id: str
    category_id: str
    title: str
    content: str
    syntax: str = "SHELL"   # SHELL, PYTHON, RUST, MARKDOWN, JSON, PROMPT
    is_favorite: bool = False
    tags: List[str] = field(default_factory=list)
    order_index: int = 0
    created_at: int = 0


@dataclass
class MacroEntity:
    id: str
    title: str
    description: str
    actions_json: str       # Serialized List<MacroAction>
    is_favorite: bool = False
    order_index: int = 0
    created_at: int = 0


@dataclass
class PairedHostEntity:
    id: str
    mac_address: str
    alias: str
    host_type: str = "WINDOWS"  # WINDOWS, MACOS, LINUX, ANDROID, EMBEDDED
    is_last_connected: bool = False
    custom_keymap: str = "US_QWERTY"
    bond_state: int = 12        # BluetoothDevice.BOND_BONDED = 12
    last_connected_at: int = 0


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


# ---------------------------------------------------------------------------
# DAOs
# ---------------------------------------------------------------------------

class CategoryDaoSimulator:
    def __init__(self, db_ref: Optional["AppDatabaseSimulator"] = None):
        self._db = db_ref
        self._table: Dict[str, CategoryEntity] = {}
        self._observers: List[Callable[[List[CategoryEntity]], None]] = []

    def _notify(self):
        cats = self.get_all_categories()
        for obs in self._observers:
            obs(cats)

    def add_observer(self, callback: Callable[[List[CategoryEntity]], None]):
        self._observers.append(callback)
        callback(self.get_all_categories())

    def get_all_categories(self) -> List[CategoryEntity]:
        res = list(self._table.values())
        res.sort(key=lambda c: c.order_index)
        return res

    def get_category_by_id(self, cat_id: str) -> Optional[CategoryEntity]:
        return self._table.get(cat_id)

    def insert_category(self, cat: CategoryEntity):
        self._table[cat.id] = cat
        self._notify()

    def update_category(self, cat: CategoryEntity):
        if cat.id in self._table:
            self._table[cat.id] = cat
            self._notify()

    def delete_category(self, cat_id: str) -> bool:
        if cat_id in self._table:
            del self._table[cat_id]
            # Foreign Key CASCADE: delete all snippets under this category
            if self._db and self._db.snippet_dao:
                self._db.snippet_dao.delete_by_category_id(cat_id)
            self._notify()
            return True
        return False


class SnippetDaoSimulator:
    def __init__(self, db_ref: Optional["AppDatabaseSimulator"] = None):
        self._db = db_ref
        self._table: Dict[str, SnippetEntity] = {}
        self._observers: List[Callable[[List[SnippetEntity]], None]] = []

    def _notify(self):
        snippets = self.get_all_snippets()
        for obs in self._observers:
            obs(snippets)

    def add_observer(self, callback: Callable[[List[SnippetEntity]], None]):
        self._observers.append(callback)
        callback(self.get_all_snippets())

    def get_all_snippets(self) -> List[SnippetEntity]:
        res = list(self._table.values())
        res.sort(key=lambda s: (not s.is_favorite, s.order_index, s.created_at))
        return res

    def get_snippets_by_category(self, category_id: str) -> List[SnippetEntity]:
        res = [s for s in self._table.values() if s.category_id == category_id]
        res.sort(key=lambda s: (not s.is_favorite, s.order_index))
        return res

    def get_favorites(self) -> List[SnippetEntity]:
        res = [s for s in self._table.values() if s.is_favorite]
        res.sort(key=lambda s: s.order_index)
        return res

    def search_snippets(self, query: str) -> List[SnippetEntity]:
        q = query.lower().strip()
        if not q:
            return self.get_all_snippets()
        return [
            s for s in self._table.values()
            if q in s.title.lower() or q in s.content.lower() or any(q in t.lower() for t in s.tags)
        ]

    def get_snippet_by_id(self, snippet_id: str) -> Optional[SnippetEntity]:
        return self._table.get(snippet_id)

    def insert_snippet(self, snippet: SnippetEntity):
        # Validate foreign key if db_ref is attached
        if self._db and self._db.category_dao:
            if snippet.category_id and snippet.category_id not in self._db.category_dao._table:
                raise ValueError(f"Foreign key violation: category_id '{snippet.category_id}' does not exist.")
        self._table[snippet.id] = snippet
        self._notify()

    def update_snippet(self, snippet: SnippetEntity):
        if snippet.id in self._table:
            self._table[snippet.id] = snippet
            self._notify()

    def toggle_favorite(self, snippet_id: str) -> bool:
        if snippet_id in self._table:
            self._table[snippet_id].is_favorite = not self._table[snippet_id].is_favorite
            self._notify()
            return True
        return False

    def delete_snippet(self, snippet_id: str) -> bool:
        if snippet_id in self._table:
            del self._table[snippet_id]
            self._notify()
            return True
        return False

    def delete_by_category_id(self, category_id: str):
        to_del = [sid for sid, s in self._table.items() if s.category_id == category_id]
        for sid in to_del:
            del self._table[sid]
        if to_del:
            self._notify()


class MacroDaoSimulator:
    def __init__(self, db_ref: Optional["AppDatabaseSimulator"] = None):
        self._db = db_ref
        self._table: Dict[str, MacroEntity] = {}
        self._observers: List[Callable[[List[MacroEntity]], None]] = []

    def _notify(self):
        macros = self.get_all_macros()
        for obs in self._observers:
            obs(macros)

    def add_observer(self, callback: Callable[[List[MacroEntity]], None]):
        self._observers.append(callback)
        callback(self.get_all_macros())

    def get_all_macros(self) -> List[MacroEntity]:
        res = list(self._table.values())
        res.sort(key=lambda m: (not m.is_favorite, m.order_index, m.created_at))
        return res

    def get_macro_by_id(self, macro_id: str) -> Optional[MacroEntity]:
        return self._table.get(macro_id)

    def insert_macro(self, macro: MacroEntity):
        self._table[macro.id] = macro
        self._notify()

    def update_macro(self, macro: MacroEntity):
        if macro.id in self._table:
            self._table[macro.id] = macro
            self._notify()

    def delete_macro(self, macro_id: str) -> bool:
        if macro_id in self._table:
            del self._table[macro_id]
            self._notify()
            return True
        return False


class PairedHostDaoSimulator:
    def __init__(self, db_ref: Optional["AppDatabaseSimulator"] = None):
        self._db = db_ref
        self._table: Dict[str, PairedHostEntity] = {}
        self._observers: List[Callable[[List[PairedHostEntity]], None]] = []

    def _notify(self):
        hosts = self.get_all_hosts()
        for obs in self._observers:
            obs(hosts)

    def add_observer(self, callback: Callable[[List[PairedHostEntity]], None]):
        self._observers.append(callback)
        callback(self.get_all_hosts())

    def get_all_hosts(self) -> List[PairedHostEntity]:
        res = list(self._table.values())
        res.sort(key=lambda h: (not h.is_last_connected, -h.last_connected_at))
        return res

    def get_host_by_id(self, host_id: str) -> Optional[PairedHostEntity]:
        return self._table.get(host_id)

    def get_host_by_mac(self, mac_address: str) -> Optional[PairedHostEntity]:
        for h in self._table.values():
            if h.mac_address.upper() == mac_address.upper():
                return h
        return None

    def get_last_connected_host(self) -> Optional[PairedHostEntity]:
        for h in self._table.values():
            if h.is_last_connected:
                return h
        return None

    def insert_or_update_host(self, host: PairedHostEntity):
        # Enforce unique MAC address constraint
        for hid, h in list(self._table.items()):
            if h.mac_address.upper() == host.mac_address.upper() and hid != host.id:
                host.id = hid
                break
        self._table[host.id] = host
        self._notify()

    def set_last_connected(self, host_id: str):
        now_ts = int(time.time() * 1000)
        for hid, h in self._table.items():
            if hid == host_id:
                h.is_last_connected = True
                h.last_connected_at = now_ts
            else:
                h.is_last_connected = False
        self._notify()

    def delete_host(self, host_id: str) -> bool:
        if host_id in self._table:
            del self._table[host_id]
            self._notify()
            return True
        return False


class PresetDaoSimulator:
    """Simulates Room SQLite Preset DAO (V1 Schema compatibility)."""
    def __init__(self, db_ref: Optional["AppDatabaseSimulator"] = None):
        self._db = db_ref
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
        if entity.is_builtin:
            return False
        del self._table[preset_id]
        self._notify()
        return True


# ---------------------------------------------------------------------------
# Default 20+ Developer Tool Pack Seeding
# ---------------------------------------------------------------------------

class DefaultToolPackProvider:
    """
    Idempotent seeding provider for 5 categories and 20+ developer snippets/macros.
    """
    @staticmethod
    def seed(db: "AppDatabaseSimulator"):
        # 1. Categories
        cat_git = CategoryEntity("cat_git", "Git Version Control", "ic_git", 1)
        cat_docker = CategoryEntity("cat_docker", "Docker & Containers", "ic_docker", 2)
        cat_k8s = CategoryEntity("cat_k8s", "Kubernetes", "ic_k8s", 3)
        cat_python = CategoryEntity("cat_python", "Python & Pytest", "ic_python", 4)
        cat_terminal = CategoryEntity("cat_terminal", "Terminal & AI Prompts", "ic_terminal", 5)

        for cat in [cat_git, cat_docker, cat_k8s, cat_python, cat_terminal]:
            db.category_dao.insert_category(cat)

        # 2. Snippets (22 production snippets)
        snippets = [
            # Git Category (5)
            SnippetEntity("snip_git_status", "cat_git", "Git Status Short", "git status -sb", "SHELL", is_favorite=True, tags=["git", "status"], order_index=1),
            SnippetEntity("snip_git_commit", "cat_git", "Git Commit with Message", 'git commit -m "{{prompt:Commit Message}}"', "SHELL", is_favorite=True, tags=["git", "commit"], order_index=2),
            SnippetEntity("snip_git_log", "cat_git", "Git Pretty Log", "git log --oneline --graph --decorate -n 15", "SHELL", tags=["git", "log"], order_index=3),
            SnippetEntity("snip_git_diff", "cat_git", "Git Staged Diff", "git diff --staged", "SHELL", tags=["git", "diff"], order_index=4),
            SnippetEntity("snip_git_push", "cat_git", "Git Push Current Branch", "git push origin HEAD", "SHELL", tags=["git", "push"], order_index=5),

            # Docker Category (5)
            SnippetEntity("snip_docker_up", "cat_docker", "Docker Compose Up Detached", "docker compose up -d", "SHELL", is_favorite=True, tags=["docker", "compose"], order_index=1),
            SnippetEntity("snip_docker_down", "cat_docker", "Docker Compose Down", "docker compose down --remove-orphans", "SHELL", tags=["docker", "compose"], order_index=2),
            SnippetEntity("snip_docker_ps", "cat_docker", "Docker Formatted PS", 'docker ps --format "table {{.ID}}\t{{.Names}}\t{{.Status}}"', "SHELL", tags=["docker", "ps"], order_index=3),
            SnippetEntity("snip_docker_logs", "cat_docker", "Docker Follow Logs", "docker logs -f --tail 100 {{prompt:Container Name}}", "SHELL", tags=["docker", "logs"], order_index=4),
            SnippetEntity("snip_docker_exec", "cat_docker", "Docker Shell Exec", "docker exec -it {{prompt:Container Name}} /bin/sh", "SHELL", tags=["docker", "exec"], order_index=5),

            # Kubernetes Category (4)
            SnippetEntity("snip_k8s_pods", "cat_k8s", "Kubectl Get All Pods", "kubectl get pods -A", "SHELL", is_favorite=True, tags=["k8s", "pods"], order_index=1),
            SnippetEntity("snip_k8s_logs", "cat_k8s", "Kubectl Follow Pod Logs", "kubectl logs -f {{prompt:Pod Name}} -n {{prompt:Namespace}}", "SHELL", tags=["k8s", "logs"], order_index=2),
            SnippetEntity("snip_k8s_describe", "cat_k8s", "Kubectl Describe Pod", "kubectl describe pod {{prompt:Pod Name}}", "SHELL", tags=["k8s", "describe"], order_index=3),
            SnippetEntity("snip_k8s_exec", "cat_k8s", "Kubectl Exec Interactive", "kubectl exec -it {{prompt:Pod Name}} -- /bin/bash", "SHELL", tags=["k8s", "exec"], order_index=4),

            # Python & Testing Category (4)
            SnippetEntity("snip_py_test", "cat_python", "Pytest Verbose by Keyword", 'pytest -v --tb=short -k "{{prompt:Test Keyword}}"', "PYTHON", is_favorite=True, tags=["pytest", "python"], order_index=1),
            SnippetEntity("snip_py_venv", "cat_python", "Create & Activate Virtualenv", "python -m venv .venv && source .venv/bin/activate", "SHELL", tags=["venv", "python"], order_index=2),
            SnippetEntity("snip_py_pip", "cat_python", "Pip Install Requirements", "pip install -r requirements.txt", "SHELL", tags=["pip", "python"], order_index=3),
            SnippetEntity("snip_py_ruff", "cat_python", "Ruff Check & Format", "ruff check --fix . && ruff format .", "PYTHON", tags=["ruff", "linter"], order_index=4),

            # Terminal & AI Category (4)
            SnippetEntity("snip_ai_refactor", "cat_terminal", "AI Code Refactor Prompt", "Refactor the following code for clean architecture and type safety:\n\n{{clipboard}}", "PROMPT", is_favorite=True, tags=["ai", "refactor"], order_index=1),
            SnippetEntity("snip_ai_doc", "cat_terminal", "AI Generate Docstrings", "Generate comprehensive Google-style docstrings for this function:\n\n{{clipboard}}", "PROMPT", tags=["ai", "docstrings"], order_index=2),
            SnippetEntity("snip_term_grep", "cat_terminal", "Recursive Ripgrep Search", 'rg -i "{{prompt:Search Pattern}}" .', "SHELL", tags=["grep", "rg"], order_index=3),
            SnippetEntity("snip_term_tar", "cat_terminal", "Extract Tarball", "tar -xzvf {{prompt:Archive File.tar.gz}}", "SHELL", tags=["tar", "extract"], order_index=4),
        ]

        for s in snippets:
            db.snippet_dao.insert_snippet(s)

        # 3. Sample Macros
        macro_actions_sample = json.dumps([
            {"type": "TypeString", "text": "git add -A\n"},
            {"type": "Delay", "durationMs": 50},
            {"type": "TypeString", "text": 'git commit -m "Auto-commit at {{iso_date}}"\n'},
            {"type": "Delay", "durationMs": 50},
            {"type": "TypeString", "text": "git push\n"}
        ])
        sample_macro = MacroEntity("macro_git_save_push", "Git Save & Push Workflow", "Stages all changes, creates timestamped commit, and pushes.", macro_actions_sample, is_favorite=True, order_index=1)
        db.macro_dao.insert_macro(sample_macro)


# ---------------------------------------------------------------------------
# AppDatabase Simulator & Migration MIGRATION_1_2
# ---------------------------------------------------------------------------

class AppDatabaseSimulator:
    """
    Simulates Room 2.6 AppDatabase with version management and DAO accessors.
    """
    def __init__(self, version: int = 2, seed_defaults: bool = True):
        self.version = version
        self.category_dao = CategoryDaoSimulator(self)
        self.snippet_dao = SnippetDaoSimulator(self)
        self.macro_dao = MacroDaoSimulator(self)
        self.paired_host_dao = PairedHostDaoSimulator(self)
        self.preset_dao = PresetDaoSimulator(self)

        if version >= 2 and seed_defaults:
            DefaultToolPackProvider.seed(self)

    def apply_migration_1_2(self):
        """
        Simulates Room MIGRATION_1_2:
        - Creates 'categories', 'snippets', 'macros', 'paired_hosts' tables.
        - Seeds the DefaultToolPackProvider.
        - Preserves existing custom presets in preset_dao.
        """
        if self.version == 1:
            self.version = 2
            DefaultToolPackProvider.seed(self)
            return True
        return False


class SettingsRepositorySimulator:
    """Simulates Jetpack DataStore Preferences repository."""

    def __init__(self):
        self._preferences: Dict[str, Any] = {
            "keymap_layout": KeyLayout.GERMAN_QWERTZ.value,
            "transmission_mode": "BURST",
            "inter_char_delay_ms": 8,
            "inter_line_delay_ms": 25,
            "live_diff_enabled": False,
            "enc_gemini_api_key": None,
            "ai_engine_type": "gemini-3.7-flash",
            "last_active_host_mac": None,
            "bracketed_paste_enabled": True
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
        self._preferences["inter_char_delay_ms"] = max(1, delay_ms)
        self._notify()

    def get_inter_line_delay_ms(self) -> int:
        return max(0, int(self._preferences.get("inter_line_delay_ms", 25)))

    def set_inter_line_delay_ms(self, delay_ms: int):
        self._preferences["inter_line_delay_ms"] = max(0, delay_ms)
        self._notify()

    def is_live_diff_enabled(self) -> bool:
        return bool(self._preferences.get("live_diff_enabled", False))

    def set_live_diff_enabled(self, enabled: bool):
        self._preferences["live_diff_enabled"] = enabled
        self._notify()

    def is_bracketed_paste_enabled(self) -> bool:
        return bool(self._preferences.get("bracketed_paste_enabled", True))

    def set_bracketed_paste_enabled(self, enabled: bool):
        self._preferences["bracketed_paste_enabled"] = enabled
        self._notify()

    def get_api_key(self) -> Optional[str]:
        return self._preferences.get("enc_gemini_api_key")

    def set_api_key(self, api_key: Optional[str]):
        self._preferences["enc_gemini_api_key"] = api_key
        self._notify()


class PresetRepositorySimulator:
    """Simulates Preset Repository wrapping PresetDaoSimulator."""

    def __init__(self, dao: Optional[PresetDaoSimulator] = None):
        if dao is None:
            self.dao = PresetDaoSimulator()
        else:
            self.dao = dao

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
