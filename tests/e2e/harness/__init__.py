"""
Transcriptor HID — E2E Test Harness Package
Comprehensive simulation models for HID reports, keymaps, dispatching,
Room 2.6 persistence, variable AST parsing, polymorphic macros,
multi-host switching, and desktop context companion.
"""
from .hid_constants import *
from .keymap_engine import (
    KeyLayout, HidKeyStroke, HidReport, HOTKEY_MAP,
    KeymapTranslator, UsQwertyKeymap, GermanQwertzKeymap
)
from .delta_diff_engine import DeltaDiffEngine, DiffResult
from .hid_host_simulator import HidHostSimulator
from .dispatcher_simulator import KeystrokeDispatcher
from .ai_pipeline_simulator import (
    PromptPreset, BUILTIN_CLEAN_POLISH, BUILTIN_BUSINESS_GERMAN, BUILTIN_CODE_COMMENTS,
    TextRewriter, GeminiRemoteRewriter, LiteRtOnDeviceRewriter, HybridAiOrchestrator
)
from .persistence_simulator import (
    CategoryEntity, SnippetEntity, MacroEntity, PairedHostEntity, PresetEntity,
    CategoryDaoSimulator, SnippetDaoSimulator, MacroDaoSimulator, PairedHostDaoSimulator,
    PresetDaoSimulator, AppDatabaseSimulator, DefaultToolPackProvider,
    PresetRepositorySimulator, SettingsRepositorySimulator
)
from .service_simulator import (
    ConnectionState, TransportType, BluetoothDeviceMock,
    BluetoothHidDeviceCallbackMock, HidTransport, BluetoothHidTransport,
    UsbAoaTransport, UsbGadgetTransport, UsbAdbSocketTransport,
    AndroidForegroundServiceSimulator
)
from .variable_parser_simulator import (
    PromptDescriptor, InterpolationContext, VariableParser
)
from .macro_runner_simulator import (
    MacroExecutionStatus, MacroExecutionState, MacroAction,
    TypeStringAction, KeyCombinationAction, DelayAction,
    PromptVariableAction, ClipboardPasteAction, MacroRunner
)
from .multi_host_simulator import (
    MultiHostState, MultiHostConnectionState, MultiHostTransportSimulator
)
from .companion_simulator import (
    DesktopContext, DesktopCompanionServerSimulator, CompanionClientSimulator
)
from .ui_state_simulator import (
    MainUiState, MainViewModelSimulator
)
