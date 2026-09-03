# Project: Type4Me Next-Gen Developer Power Suite

## Architecture
- **Presentation Layer (Jetpack Compose & MVI)**:
  - `MainScreen` (root scaffold, scrollable landscape & portrait layouts).
  - `HotkeyDockBar` (hardware terminal keys: `Esc`, `Tab`, `Ctrl+C`, `Ctrl+Z`, `Ctrl+D`, `Ctrl+L`, `Alt+Tab`, `Arrow keys`, `F1-F12`, clipboard stream button).
  - `SnippetsPadScreen` (category filter chips, favorites bar, staggered grid, search bar, syntax badges, 1-tap typing).
  - `VariablePromptBottomSheet` (dynamic template prompt parameter entry modal).
  - `ConnectionHeader` (multi-host quick switching dropdown widget, status indicator dot, settings button).
  - `TranscriptionCanvas` & `TouchpadCanvas` (voice editing canvas and precision trackpad).
  - Driven by `MainViewModel` with unidirectional data flow (`MainUiState`, `MainUiIntent`).
- **Core Services & Transport**:
  - `BluetoothHidTransport`: Composite HID descriptor (Report ID 1 Keyboard + F1-F24 + LEDs, Report ID 2 Relative Mouse, Report ID 3 Consumer Control Media).
  - 6-Phase Atomic Multi-Host Switching Protocol (`switchingMutex`, 150ms settling delay, dead-link watchdog, zero-modifier release).
  - `KeystrokeDispatcher`: Deterministic 8ms pacing ($t_{down}=4\text{ms}, t_{up}=4\text{ms}$), 25-50ms inter-line delay for AST highlighters, `NonCancellable` emergency release guard, bracketed paste mode (`\x1b[200~` ... `\x1b[201~`), German dead-key synthesis, configurable `NewlineSubmissionMode`.
  - `UsbHidTransport`: Zero-config wired USB OTG fallback.
- **Data & Persistence Layer (Room 2.6)**:
  - `AppDatabase` (Room V2: `categories`, `snippets`, `macros`, `paired_hosts`, `presets`).
  - DAOs: `CategoryDao`, `SnippetDao`, `MacroDao`, `PairedHostDao`, `PresetDao`.
  - Migration & Seeding: `MIGRATION_1_2` DDL upgrade and idempotent `DefaultToolPackProvider` preloading 20+ production developer snippets.
- **Engines & Tooling**:
  - `VariableParser`: Single-pass mustache AST parser with `\{\{` escaping, code-point aware cursor backtracking, and interactive prompt extraction.
  - `MacroRunner`: Polymorphic `MacroAction` coroutine execution engine.
  - Desktop Context Companion: `tools/companion/type4me_companion.py` (Python stdlib) and `tools/companion/type4me_companion.ps1` (pure PowerShell) serving `GET /context` on port 8765 for optional Gemini prompt enrichment with 100% air-gap fallback.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Room 2.6 Database Schema & Entities | `CategoryEntity`, `SnippetEntity`, `MacroEntity`, `PairedHostEntity`, `Converters` (JSON serialization) | M1 | Survey |
| 2 | Room DAOs & Reactive Flows | `CategoryDao`, `SnippetDao`, `MacroDao`, `PairedHostDao` with reactive `Flow`, full-text search, and CRUD | M1 | Survey |
| 3 | Database Migration MIGRATION_1_2 | SQLite DDL upgrading AppDatabase from V1 to V2 with foreign keys, cascading deletes, and composite indices | M1 | Survey |
| 4 | Preloaded 20+ Developer Tool Pack | Idempotent seeding of 5 categories, 20+ production snippets (Git, Docker, K8s, Rust, Python, AI prompts), and macros | M1 | Survey |
| 5 | Extended Page 0x07 Scancodes & Modifiers | Full HID Usage table (`Esc`, `Tab`, arrows, navigation, `F1-F24`) and 8-bit modifier bitmask engine in `HidConstants` | M2 | Survey |
| 6 | Deterministic 8ms Pacing & Release Guard | 8-byte input report lifecycle, 8ms duty cycle, 25-50ms newline delay, and `NonCancellable` emergency release report | M2 | Survey |
| 7 | Bracketed Paste & Dead-Key Synthesis | Foreground clipboard streaming wrapped in `\x1b[200~` ... `\x1b[201~` and QWERTZ dead-key space-synthesis | M2 | Survey |
| 8 | Virtual Hotkey Dock Bar Compose UI | Responsive horizontal scrollable dock pinned above voice canvas with 1-tap terminal keys and F-key expansion | M2 | Survey |
| 9 | Single-Pass Variable AST Parser | Parser supporting `{{timestamp}}`, `{{iso_date}}`, `{{clipboard}}`, `{{prompt:LABEL}}`, code-point cursor backtrack, `\{\{` escaping | M3 | Survey |
| 10 | Polymorphic MacroAction & Runner Engine | Polymorphic JSON actions (`TypeString`, `KeyCombination`, `Delay`, `PromptVariable`, `ClipboardPaste`) & `MacroRunner` | M3 | Survey |
| 11 | Snippets Pad Screen & Card UI | Jetpack Compose `SnippetsPadScreen` with category filter chips, favorites bar, staggered grid, search, and syntax badges | M3 | Survey |
| 12 | Variable Prompt Modal Bottom Sheet | `VariablePromptBottomSheet` for dynamic user parameter entry before snippet/macro dispatch | M3 | Survey |
| 13 | 154-Byte Composite HID Descriptor | SDP descriptor supporting Report ID 1 (Keyboard up to F24), Report ID 2 (Mouse), Report ID 3 (Consumer Media) | M4 | Survey |
| 14 | 6-Phase Atomic Host Switching Protocol | Mutex-serialized host switching with 150ms settling guard, 1000ms dead-link watchdog, and Report ID 1 zero release | M4 | Survey |
| 15 | Multi-Host Dropdown in ConnectionHeader | Top bar widget showing active host icon, custom alias, connection status dot, and 1-tap switching dropdown | M4 | Survey |
| 16 | Zero-Install Python Companion Script | Standalone `tools/companion/type4me_companion.py` (stdlib only) serving active window/selection context on `GET /context` | M5 | Survey |
| 17 | Zero-Install PowerShell Companion Script | Standalone `tools/companion/type4me_companion.ps1` (pure Win32 P/Invoke) serving active window/selection context | M5 | Survey |
| 18 | Dual-Tier Prompt Context Ingestion | Android `CompanionClient` ingesting workstation context into Gemini prompt system prefix with 100% air-gap fallback | M5 | Survey |
| 19 | E2E Protocol & Test Suite Tiers 1-5 | 300+ simulation tests in `tests/e2e/` covering HID reports, pacing, Room DB, switching protocol, and companion | Test Track / M6 | Survey |
| 20 | JVM Unit Test Suite Expansion | Full unit test coverage across all newly introduced modules and repository layers in `app/src/test/` | M1-M5 / M6 | Survey |
| 21 | Clean Release Build & Shrinking | Verification that `./gradlew assembleRelease` compiles cleanly without warnings with R8 obfuscation | M6 | Survey |
| 22 | Desktop Floating HUD & Win+H Caret Focus | Frameless, semi-transparent HUD summoned via `Ctrl+Shift+Space` with auto-focus for instant `Win+H` dictation | M7 | Desktop |
| 23 | Needleman-Wunsch Alignment & WER Telemetry | Global DP alignment between reference teleprompter scripts and spoken ASR calculating Word Error Rate | M7 | Desktop |
| 24 | Personal Confusion Matrix & Regional Priors | Learning speaker phonetic substitutions (e.g. `pin \rightarrow pen`) layered over SA/German English base priors | M7 | Desktop |
| 25 | Deterministic Phonetic Trie Fast-Path | Sub-millisecond in-memory Trie executing multi-word engineering jargon repairs before LLM reasoning | M7 | Desktop |
| 26 | 4-Tier Context Distillation & Budget Allocator | Streaming `transcript.jsonl` watcher extracting recent turns, active errors, and symbols with 500/2k/8k token caps | M7 | Desktop |
| 27 | Gemini 3.7 Flash Agentic Modifier Matrix | Generative prompt transformation across 6 developer presets with deterministic offline rule-based fallback | M7 | Desktop |
| 28 | Win32 Target Binding, Pinning & Injection | Foreground `HWND` tracking, 1-click Pin lock, and simulated `Ctrl+V` dispatch into active coding agent | M7 | Desktop |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Room 2.6 Persistence Layer & Tool Pack Seeding | `CategoryEntity`, `SnippetEntity`, `MacroEntity`, `PairedHostEntity`, DAOs, `MIGRATION_1_2`, `DefaultToolPackProvider` | None | PLANNED |
| M2 | HID Keystroke Dispatcher, Hotkey Bar & Clipboard Streamer | `HidConstants`, `KeystrokeDispatcher` emergency guard & bracketed paste, `HotkeyDockBar` UI, dead-key synthesis | None | PLANNED |
| M3 | Snippets Pad UI, Variable Parser & Macro Engine | `VariableParser`, `VariablePromptBottomSheet`, `MacroAction`, `MacroRunner`, `SnippetsPadScreen`, `MainViewModel` wiring | M1, M2 | PLANNED |
| M4 | Multi-Host Quick Switching & Paired Host Registry | 154B Composite Descriptor, 6-Phase Switching Protocol, `BluetoothHidTransport` release fix, `ConnectionHeader` dropdown | M1, M2 | PLANNED |
| M5 | Dual-Tier Zero-Install Desktop Context Companion | `tools/companion/type4me_companion.py`, `type4me_companion.ps1`, `CompanionClient`, Gemini context ingestion | None | PLANNED |
| M6 | Final Verification: 100% E2E Pass, Tier 5 Hardening & Release Build | Full verification of Tiers 1-4 E2E tests, Tier 5 adversarial hardening, JVM unit tests, and `./gradlew assembleRelease` | M1, M2, M3, M4, M5, Test Track | PLANNED |
| M7 | Type4Me Desktop: Windows SOTA Voice Prompt Suite & Accent Engine | `desktop/` suite: Floating HUD, Learn-My-Accent studio, Needleman-Wunsch alignment, Trie, context watcher, Gemini 3.7 Flash | None | COMPLETE |


## Interface Contracts
### Hotkey & Keystroke Dispatcher ↔ UI (`HotkeyDockBar`)
- `KeystrokeDispatcher.sendRawKeyStrokes(strokes: List<HidKeyStroke>, delayMs: Long = 8L)`
- `KeystrokeDispatcher.streamClipboardToHost(clipText: String, bracketedPaste: Boolean = false, delayMs: Long = 8L)`
- `HidKeyStroke(modifiers: Byte, usageId: Byte)`

### Room Database ↔ Snippets & Multi-Host Repositories
- `AppDatabase.categoryDao(): CategoryDao`
- `AppDatabase.snippetDao(): SnippetDao`
- `AppDatabase.macroDao(): MacroDao`
- `AppDatabase.pairedHostDao(): PairedHostDao`
- `Converters`: `List<String>` and `SyntaxType` JSON / string serializers.

### Variable Parser & Macro Runner ↔ Snippets UI
- `VariableParser.parse(template: String, context: InterpolationContext): VariableParseResult`
- `VariableParser.extractPrompts(template: String): List<VariableDescriptor.Prompt>`
- `MacroRunner.execute(macro: MacroEntity, context: InterpolationContext): Flow<MacroExecutionState>`

### Multi-Host Transport ↔ ConnectionHeader
- `BluetoothHidTransport.switchHost(target: PairedHostEntity): Flow<MultiHostConnectionState>`
- `BluetoothHidTransport.getPairedHosts(): Flow<List<PairedHostEntity>>`
- `BluetoothHidTransport.savePairedHost(host: PairedHostEntity): Unit`

### Desktop Companion ↔ Android AI Engine
- `CompanionClient.fetchActiveContext(hostIp: String, port: Int = 8765): Result<DesktopContext>`
- `DesktopContext(windowTitle: String, selectedText: String, processName: String, timestamp: Long)`

## Code Layout
- `app/src/main/java/com/transcriptor/hid/`
  - `TranscriptorApp.kt` (Application singleton & repository locator)
  - `ai/` (`TextRewriter.kt`, `GeminiRemoteRewriter.kt`, `LiteRtOnDeviceRewriter.kt`, `PromptPreset.kt`, `CompanionClient.kt`)
  - `data/`
    - `db/` (`AppDatabase.kt`, `CategoryEntity.kt`, `SnippetEntity.kt`, `MacroEntity.kt`, `PairedHostEntity.kt`, `CategoryDao.kt`, `SnippetDao.kt`, `MacroDao.kt`, `PairedHostDao.kt`, `Converters.kt`, `MIGRATION_1_2.kt`, `DefaultToolPackProvider.kt`)
    - `SnippetRepository.kt`, `MacroRepository.kt`, `PairedHostRepository.kt`, `PresetRepository.kt`, `SettingsRepository.kt`
  - `engine/`
    - `HidConstants.kt`, `HidKeyStroke.kt`, `HidReport.kt`, `KeyLayout.kt`, `KeymapTranslator.kt`, `UsQwertyKeymap.kt`, `GermanQwertzKeymap.kt`, `KeystrokeDispatcher.kt`, `DeltaDiffEngine.kt`
    - `VariableParser.kt`, `VariableDescriptor.kt`, `MacroAction.kt`, `MacroRunner.kt`
  - `service/`
    - `HidDeviceService.kt`, `HidTransport.kt`, `BluetoothHidTransport.kt`, `UsbHidTransport.kt`, `HidConnectionState.kt`, `MultiHostConnectionState.kt`
  - `ui/`
    - `MainActivity.kt`, `MainScreen.kt`, `MainViewModel.kt`, `MainUiState.kt`, `MainUiIntent.kt`
    - `components/` (`ConnectionHeader.kt`, `HotkeyDockBar.kt`, `SnippetsPadScreen.kt`, `SnippetCard.kt`, `VariablePromptBottomSheet.kt`, `ControlBar.kt`, `PresetSelector.kt`, `TranscriptionCanvas.kt`, `TouchpadCanvas.kt`, `ErrorBanner.kt`, `SettingsDialog.kt`, `HostConnectDialog.kt`)
- `tools/companion/`
  - `type4me_companion.py` (Zero-dependency Python 3 desktop companion script)
  - `type4me_companion.ps1` (Zero-dependency PowerShell desktop companion script)
- `tests/e2e/`
  - `run_e2e_tests.py` (E2E simulation test runner)
  - `tier1_features/`, `tier2_boundaries/`, `tier3_combinations/`, `tier4_realworld/`, `tier5_adversarial/`
- `app/src/test/java/com/transcriptor/hid/` (JVM Unit test suites)
