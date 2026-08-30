# Project: Type4Me (Hardware Speech-to-Keystroke & Touchpad Mouse Bridge)

## Architecture
- **Presentation Layer (Jetpack Compose & MVI)**: `MainScreen`, `TranscriptionCanvas`, `TouchpadCanvas`, `ConnectionHeader`, `ControlBar`, `PresetSelector`, `SettingsDialog`, `HostConnectDialog`, `ErrorBanner`, driven by `MainViewModel` using unidirectional data flow (`MainUiState`, `MainUiIntent`).
- **Core Services & Transport**: `HidDeviceService` (foreground service), `BluetoothHidTransport` (Android Bluetooth HID Composite Device profile, singleton instance with Keyboard Report ID 1 + Mouse Report ID 2), `UsbHidTransport`, `KeystrokeDispatcher` (deterministic key pacing, modifiers, soft-enter formatting, Unicode mapping), `DeltaDiffEngine`.
- **AI Pipeline**: `TextRewriter` interface, `GeminiRemoteRewriter` (Google GenAI SDK / REST, dynamic `apiKeyProvider` and `modelProvider` from DataStore without restart, default `gemini-3.5-flash-lite`), `PromptEngine`.
- **Data Layer**: `AppDatabase` (Room database with `PresetDao`, `PresetEntity`), `PresetRepository`, `SettingsRepository` (Jetpack DataStore Preferences).

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Singleton HID Transport | Unify `BluetoothHidTransport` between `TranscriptorApp` and `HidDeviceService` to eliminate disconnected dual instances | M1 | Survey |
| 2 | Dynamic AI Model & Key Pickup | Refactor `GeminiRemoteRewriter` with `modelProvider` lambda to dynamically read model & API key from DataStore on every rewrite without app restart | M1 | Survey |
| 3 | Live Diff Concurrency Safety | Conflate / cancel preceding live-diff coroutine jobs in `MainViewModel` to eliminate out-of-order execution during rapid voice typing | M1 | Survey |
| 4 | Room Database Callback Safety | Ensure safe fallback for preset seeding if database callback instance is not yet populated | M1 | Survey |
| 5 | Settings Dialog & Entry Point | Create `SettingsDialog` in Compose for Gemini API key (with visibility toggle) and Gemini model selection dropdown (`gemini-3.5-flash-lite`, `gemini-3-flash-preview`, `gemini-3.7-flash`); add Settings button to `ConnectionHeader` | M2 | Survey |
| 6 | Responsive Landscape Layout | Add `verticalScroll` to `MainScreen` root column and adaptive height to `TranscriptionCanvas` to eliminate clipping in landscape & IME open states | M2 | Survey |
| 7 | Accessibility & Ergonomics | Enforce >= 48dp touch targets on badges/buttons, fix WCAG AA color contrast (>= 4.5:1), add TalkBack semantics | M2 | Survey |
| 8 | Persistent Actionable Error Banners | Implement prominent inline `ErrorBanner` for API key errors, quota exceeded, or HID disconnects with retry/settings actions | M2 | Survey |
| 9 | JVM Unit Test Fix (MockK) | Replace reflection in `BluetoothHidTransportTest.kt` with MockK mocks (`mockk<BluetoothDevice>(relaxed = true)`) | M3 | Survey |
| 10 | Windows E2E Console UTF-8 Fix | Fix `UnicodeEncodeError` on Windows console in `tests/e2e/run_e2e_tests.py` via `sys.stdout.reconfigure(encoding="utf-8", errors="replace")` | M3 | Survey |
| 11 | Manifest & Branding Modernization | Rebrand to **Type4Me**, add Concept 2 adaptive neon glyph launcher icons, configure GPLv3 license | M3 | User Request |
| 12 | Chat-Safe Soft-Enters | Map `\n` and `\r` to hardware `Shift + Enter` to prevent premature execution in AI chat boxes | M3 | User Request |
| 13 | Composite HID Touchpad Mouse | Add Report ID 2 (Relative Mouse) to HID descriptor, create `TouchpadCanvas` with 1-finger glide, tap-click, scroll strip, and physical buttons | M4 | User Request |
| 14 | Technical Documentation Suite | Author `docs/ARCHITECTURE.md`, `docs/TOUCHPAD_GUIDE.md`, `docs/PROMPT_ENGINEERING.md`, and `docs/HARDWARE_COMPATIBILITY.md` | M4 | User Request |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Core Architecture, Services & Dynamic AI Pipeline | Singleton transport injection, dynamic `modelProvider` in `GeminiRemoteRewriter`, live-diff coroutine mutex/job serialization, DB seeding guard | None | DONE |
| M2 | Responsive UI, Settings Screen & Accessibility Remediation | `SettingsDialog`, `ConnectionHeader` settings button, `MainUiState`/`MainUiIntent` wiring, landscape scrollability, >=48dp touch targets, WCAG AA contrast, error banners | M1 | DONE |
| M3 | Rebranding, Soft-Enters & GPLv3 Licensing | "Type4Me" branding, Concept 2 adaptive icon suite, `Shift + Enter` translation, GNU GPLv3 license | M2 | DONE |
| M4 | Touchpad Mouse, Documentation & E2E Validation | Composite HID descriptor (129B), `TouchpadCanvas`, `docs/` technical documentation suite, 135 JVM + 301 Python tests passing | M3 | DONE |

## Code Layout
- `app/src/main/java/com/transcriptor/hid/`
  - `TranscriptorApp.kt` (Application class, singleton DI container)
  - `ai/` (`TextRewriter.kt`, `GeminiRemoteRewriter.kt`, `PromptEngine.kt`)
  - `data/` (`AppDatabase.kt`, `PresetDao.kt`, `PresetEntity.kt`, `PresetRepository.kt`, `SettingsRepository.kt`)
  - `service/` (`HidDeviceService.kt`, `BluetoothHidTransport.kt`, `UsbHidTransport.kt`, `KeystrokeDispatcher.kt`, `DeltaDiffEngine.kt`, `HidConnectionState.kt`)
  - `ui/` (`MainActivity.kt`, `MainScreen.kt`, `MainViewModel.kt`, `MainUiState.kt`, `MainUiIntent.kt`, `components/`)
- `docs/` (`ARCHITECTURE.md`, `TOUCHPAD_GUIDE.md`, `PROMPT_ENGINEERING.md`, `HARDWARE_COMPATIBILITY.md`)
- `app/src/test/java/com/transcriptor/hid/` (135 JVM Unit tests)
- `tests/e2e/` (301 Python E2E protocol tests)
