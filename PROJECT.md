# Project: Transcriptor HID Android Codebase Overhaul

## Architecture
- **Presentation Layer (Jetpack Compose & MVI)**: `MainScreen`, `TranscriptionCanvas`, `ConnectionHeader`, `ControlBar`, `PresetSelector`, `SettingsDialog`, `ErrorBanner`, driven by `MainViewModel` using unidirectional data flow (`MainUiState`, `MainUiIntent`).
- **Core Services & Transport**: `HidDeviceService` (foreground service), `BluetoothHidTransport` (Android Bluetooth HID Device profile, singleton instance), `UsbHidTransport`, `KeystrokeDispatcher` (deterministic key pacing, modifiers, Unicode mapping), `DeltaDiffEngine`.
- **AI Pipeline**: `TextRewriter` interface, `GeminiRemoteRewriter` (Google GenAI SDK, dynamic `apiKeyProvider` and `modelProvider` from DataStore without restart), `PromptEngine`.
- **Data Layer**: `AppDatabase` (Room database with `PresetDao`, `PresetEntity`), `PresetRepository`, `SettingsRepository` (Jetpack DataStore Preferences).

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Singleton HID Transport | Unify `BluetoothHidTransport` between `TranscriptorApp` and `HidDeviceService` to eliminate disconnected dual instances | M1 | Survey (Explorer 1) |
| 2 | Dynamic AI Model & Key Pickup | Refactor `GeminiRemoteRewriter` with `modelProvider` lambda to dynamically read model & API key from DataStore on every rewrite without app restart | M1 | Survey (Explorer 1, 3) |
| 3 | Live Diff Concurrency Safety | Conflate / cancel preceding live-diff coroutine jobs in `MainViewModel` to eliminate out-of-order execution during rapid voice typing | M1 | Survey (Explorer 1, 2) |
| 4 | Room Database Callback Safety | Ensure safe fallback for preset seeding if database callback instance is not yet populated | M1 | Survey (Explorer 1) |
| 5 | Settings Dialog & Entry Point | Create `SettingsDialog` in Compose for Gemini API key (with visibility toggle) and Gemini model selection dropdown (`gemini-3.7-flash`, `gemini-3.5-flash-lite`, `gemini-2.5-flash`); add Settings button to `ConnectionHeader` | M2 | Survey (Explorer 2, 3) |
| 6 | Responsive Landscape Layout | Add `verticalScroll` to `MainScreen` root column and adaptive height to `TranscriptionCanvas` to eliminate clipping in landscape & IME open states | M2 | Survey (Explorer 2) |
| 7 | Accessibility & Ergonomics | Enforce >= 48dp touch targets on badges/buttons, fix WCAG AA color contrast (>= 4.5:1), add `Role.RadioButton`/`selectableGroup` TalkBack semantics | M2 | Survey (Explorer 2) |
| 8 | Persistent Actionable Error Banners | Implement prominent inline `ErrorBanner` for API key errors, quota exceeded, or HID disconnects with retry/settings actions | M2 | Survey (Explorer 2) |
| 9 | JVM Unit Test Fix (MockK) | Replace forbidden `sun.misc.Unsafe` in `BluetoothHidTransportTest.kt` with MockK mocks (`mockk<BluetoothDevice>(relaxed = true)`) | M3 | Survey (Explorer 1, 3) |
| 10 | Windows E2E Console UTF-8 Fix | Fix `UnicodeEncodeError` on Windows console in `tests/e2e/run_e2e_tests.py` via `sys.stdout.reconfigure(encoding="utf-8", errors="replace")` | M3 | Survey (Explorer 3) |
| 11 | Manifest & Gradle Modernization | Remove deprecated `package` attribute from `AndroidManifest.xml` and verify clean build configurations | M3 | Survey (Explorer 3) |
| 12 | End-to-End Test Suite & Build Verification | Full execution and validation of `./gradlew test`, `./gradlew assembleDebug`, and `python tests/e2e/run_e2e_tests.py` (Tiers 1-5) | M4 | Survey (All) |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Core Architecture, Services & Dynamic AI Pipeline | Singleton transport injection, dynamic `modelProvider` in `GeminiRemoteRewriter`, live-diff coroutine mutex/job serialization, DB seeding guard | None | DONE |
| M2 | Responsive UI, Settings Screen & Accessibility Remediation | `SettingsDialog`, `ConnectionHeader` settings button, `MainUiState`/`MainUiIntent` wiring, landscape scrollability, >=48dp touch targets, WCAG AA contrast, error banners | M1 | DONE |
| M3 | Test Infrastructure, MockK Unit Tests & Windows Stability | `BluetoothHidTransportTest` MockK refactoring, `run_e2e_tests.py` Windows console UTF-8 stream fix, `AndroidManifest.xml` cleanups | None | DONE |
| M4 | Full Verification & Forensic Audit | Execution of `./gradlew test`, `./gradlew assembleDebug`, `python tests/e2e/run_e2e_tests.py`, multi-agent review, challenger validation, forensic integrity audit | M1, M2, M3 | DONE |

## Interface Contracts
### `SettingsRepository` ↔ `GeminiRemoteRewriter`
- `GeminiRemoteRewriter(apiKeyProvider: suspend () -> String?, modelProvider: suspend () -> String = { MODEL_GEMINI_3_7_FLASH }, generator: GeminiContentGenerator = DefaultGenAiSdkGenerator())`
- On `rewrite(text: String, prompt: String)`:
  - Invokes `apiKey = apiKeyProvider()`
  - Invokes `currentModel = modelProvider()`
  - Calls `generator.generate(apiKey, currentModel, systemPrompt, userPrompt)`

### `TranscriptorApp` ↔ `HidDeviceService` ↔ `KeystrokeDispatcher`
- `BluetoothHidTransport` is instantiated once in `TranscriptorApp` (singleton) and accessed by `HidDeviceService` via application context / shared reference.

### `MainViewModel` ↔ `SettingsDialog`
- `MainUiState`: `isSettingsOpen: Boolean`, `apiKeyInput: String`, `isApiKeyVisible: Boolean`, `selectedModel: String`, `isApiKeyTesting: Boolean`, `settingsMessage: String?`
- `MainUiIntent`: `OpenSettings`, `CloseSettings`, `UpdateApiKey(String)`, `ToggleApiKeyVisibility`, `SelectModel(String)`, `SaveSettings`, `TestApiKey`

## Code Layout
- `app/src/main/java/com/transcriptor/hid/`
  - `TranscriptorApp.kt` (Application class, singleton DI container)
  - `ai/` (`TextRewriter.kt`, `GeminiRemoteRewriter.kt`, `PromptEngine.kt`)
  - `data/` (`AppDatabase.kt`, `PresetDao.kt`, `PresetEntity.kt`, `PresetRepository.kt`, `SettingsRepository.kt`)
  - `service/` (`HidDeviceService.kt`, `BluetoothHidTransport.kt`, `UsbHidTransport.kt`, `KeystrokeDispatcher.kt`, `DeltaDiffEngine.kt`, `HidConnectionState.kt`)
  - `ui/` (`MainActivity.kt`, `MainScreen.kt`, `MainViewModel.kt`, `MainUiState.kt`, `MainUiIntent.kt`, `components/`)
- `app/src/test/java/com/transcriptor/hid/` (Unit tests)
- `tests/e2e/` (Python E2E tests)
