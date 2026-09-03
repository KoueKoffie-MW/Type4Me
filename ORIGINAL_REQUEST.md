# Original User Request

## Initial Request — 2026-08-30T19:50:45Z

Comprehensive evaluation, defect discovery, and end-to-end remediation of the Transcriptor HID Android codebase, covering UI/UX responsiveness across orientations, settings accessibility, AI rewriting integration, unit test compilation, and verification pipelines.

Working directory: C:\Users\Jan\Documents\antigravity\nifty-galileo
Integrity mode: development

## Requirements

### R1. Deep Codebase Evaluation & Defect Identification
Perform an exhaustive multi-agent audit across the entire codebase to detect all bugs, architectural bottlenecks, unhandled edge cases, race conditions, deprecated APIs, and design flaws across all application components (Jetpack Compose UI, ViewModels, AI Pipelines, HID Services, Room DB, and DataStore).

### R2. Responsive UI & Accessibility Remediation
- **Settings Screen/Dialog**: Provide a clean, accessible interface to view and edit the Google Gemini API key, select the Gemini model (`gemini-3.7-flash`, `gemini-3.5-flash-lite`, `gemini-2.5-flash`), and configure typing pacing/keymap defaults.
- **Landscape & Responsive Layouts**: Refactor the main screen layouts, headers, control bars, preset chips, and transcription canvas so that both portrait and landscape orientations display adaptively with zero clipped elements, overflow glitches, or awkward spacing.
- **Ergonomics & Usability**: Ensure intuitive keyboard actions, scannable connection state indicators, and direct prompts if AI rewrite is triggered without credentials.

### R3. Dynamic AI Pipeline & Configuration
- Ensure the text rewriter dynamically picks up active model selection and API key changes from the repository without requiring app restarts.
- Provide clear, user-friendly error banners and prompts when network requests fail or credentials are missing.

### R4. Test Infrastructure & Platform Stability
- Fix JVM/JDK unit test compilation issues in `BluetoothHidTransportTest` and any other test classes by using clean MockK instances instead of low-level `sun.misc.Unsafe` reflection.
- Fix console Unicode stdout encoding in `tests/e2e/run_e2e_tests.py` to prevent Windows charmap encoding crashes.
- Resolve any deprecated manifest attributes or Gradle build warnings.

## Verification Resources
- Android Unit Test Suite: `./gradlew test`
- Gradle Build Validation: `./gradlew assembleDebug`
- Standalone E2E Test Runner: `python tests/e2e/run_e2e_tests.py`

## Acceptance Criteria

### Build & Test Health
- [ ] `./gradlew test` executes cleanly and 100% of unit tests pass with zero compilation or runtime errors.
- [ ] `python tests/e2e/run_e2e_tests.py` passes 100% across Tiers 1-5 on Windows without encoding exceptions.
- [ ] `./gradlew assembleDebug` builds successfully without manifest or compilation warnings.

### Functional & UI Verification
- [ ] Settings configuration is fully accessible via UI, allowing the user to view/save the Gemini API key and switch models.
- [ ] UI renders adaptively and legibly in both Portrait and Landscape orientations without squashed or clipped controls.
- [ ] All newly discovered bugs during the audit are documented and resolved.

## Follow-up — 2026-09-01T21:55:44Z

Implementation of the **Type4Me Next-Gen Developer Power Suite**: Virtual Developer Hotkey Bar, Android Clipboard-to-HID Keystroke Streamer, Persistent Snippets & Macro Engine (Room DB), Multi-Host Quick Switcher, and Dual-Tier Zero-Install Desktop Context Companion.

Working directory: c:\Users\Jan\Documents\antigravity\nifty-galileo
Integrity mode: development

## Requirements

### R1. Developer & Terminal Hotkey Bar (HID Page 0x07 Control Keys)
Implement an accessible, responsive Hotkey Dock Bar directly above or integrated with the Voice Keyboard canvas:
- Hardware terminal & navigation keys: `Esc`, `Tab`, `Ctrl+C`, `Ctrl+Z`, `Ctrl+D`, `Ctrl+L`, `Arrow Up/Down/Left/Right`, `Alt+Tab`, and expandable `F-Keys (F1-F12)`.
- Keystroke Dispatcher integration sending pure 8-byte HID reports with deterministic 8ms pacing and `NonCancellable` emergency modifier release guard.
- Clipboard Streamer button: reads Android clipboard and converts strings into hardware HID keystroke packets, supporting terminal bracketed paste mode (`\x1b[200~` ... `\x1b[201~`) to prevent staircase indentation in Vim/Bash/Zsh.

### R2. Quick Snippets & Action Macros Pad (Room DB 2.6)
Implement a persistent Snippets & Macros engine:
- SQLite/Room database entities (`CategoryEntity`, `SnippetEntity`, `MacroEntity`, `PairedHostEntity`) with migration/seeding.
- Pre-loaded Developer Tool Pack with 20+ categorized production snippets (Git, Docker, Pytest, Terminal, AI Coding Prompts).
- Jetpack Compose Snippets Pad UI with category filter chips, search/filter, and 1-tap instant keystroke transmission.
- Single-pass template variable interpolation (`{{timestamp}}`, `{{clipboard}}`, `{{prompt}}`).

### R3. Multi-Host Quick-Switching UI & Registry
Implement persistent paired host management:
- Persistent `PairedHostEntity` storage with custom alias naming (e.g., "Work PC", "Private Laptop", "Home Server").
- Fast 1-tap switching dropdown widget integrated into the `ConnectionHeader`.
- Graceful disconnect-and-reconnect state flow.

### R4. Dual-Tier Zero-Install Desktop Context Companion
Provide a lightweight zero-install desktop companion script (`tools/companion/type4me_companion.py` / `.ps1`):
- Broadcasts current active window title / selected text to the phone over local BLE/HTTP socket when permitted on developer workstations.
- Allows Type4Me on Android to ingest host context into Gemini prompt engineering without compromising the primary zero-host air-gap fallback.

## Acceptance Criteria

### Build & Test Health
- [ ] `./gradlew test` passes 100% with all new and existing JVM unit tests.
- [ ] `python tests/e2e/run_e2e_tests.py` passes 100% across Tiers 1-5.
- [ ] `./gradlew assembleRelease` compiles cleanly without warnings.

### Functional & UI Verification
- [ ] Developer Hotkey Bar renders responsively in portrait and landscape with functional terminal key injection.
- [ ] Snippets Pad renders preloaded templates and dispatches 1-tap keystrokes over Bluetooth HID.
- [ ] Clipboard Streamer converts mobile clipboard into cleanly paced host keystrokes.
- [ ] Multi-Host switcher displays bonded hosts and allows switching.


## Follow-up — 2026-09-03T20:46:28Z

Comprehensive defect audit and remediation of the current Type4Me v1.4.0 codebase, combined with deep architectural design and prototyping of next-generation capabilities (Air-Gapped Screen Lens OCR, Direct Push-to-Talk Audio Pipeline, Bluetooth Connection Watchdog, and Gyroscope Air Mouse).

Working directory: c:\Users\Jan\Documents\antigravity\nifty-galileo
Integrity mode: development

## Core Directives & SOTA Research Rule
- **Live State-of-the-Art Research**: Actively search the web and inspect current (2026) Android platform documentation, CameraX / MLKit OCR capabilities, Bluetooth HID profile specifications, and modern developer tooling. Do NOT rely on static assumptions or dated paradigms.

---

## Requirements

### R1. Deep Codebase Defect Discovery & Immediate Remediation (Bug Hunter Team)
Exhaustively audit the active codebase (`app/src/main/java`, Compose UI, Room DB 2.6, HID Services, and Companion Client) to identify and fix:
- Any UI clipping, recomposition lags, or orientation layout flaws across Portrait/Landscape.
- Potential race conditions or deadlocks in `KeystrokeDispatcher`, `MacroRunner`, and `BluetoothHidTransport`.
- Corner cases in `VariableParser` (unescaped brackets, nested variables, null safety).
- Any unhandled exceptions or connection leak risks in `CompanionClient` and `BluetoothHidDeviceAdapter`.
- Fix any deprecation warnings or lint defects identified during compilation.

### R2. Air-Gapped Optical Vision Context (Screen Lens / OCR Snapshot)
Design and prototype an in-app "Scan Screen" Camera Lens for 100% zero-host air-gapped context ingestion:
- Lightweight CameraX preview sheet / dialog that captures monitor screen regions (terminal errors, code diffs, logs).
- On-device MLKit Text Recognition (OCR) or direct Gemini Vision multimodal pass to extract stack traces and code snippets without any host-side software.
- Seamless injection of extracted text into `transcriptionText` or background context for Gemini prompt rewriting.

### R3. Continuous In-App Audio & Push-To-Talk Pipeline
Research and design a direct in-app voice capture pipeline bypassing Gboard silence timeouts:
- Direct `AudioRecord` / `SpeechRecognizer` integration with a dedicated Push-to-Talk (hold-to-speak) floating action trigger.
- Audio level / waveform visualization during dictation.
- Roadmap for local on-device Whisper / LiteRT Speech ASR running on the phone NPU.

### R4. Bluetooth L2CAP Connection Watchdog & Fast Auto-Reconnect
Design an autonomous connection resilience watchdog:
- Periodic heartbeat monitoring and active state detection for host sleep/wake cycles.
- Fast auto-reconnect state machine recovering lost L2CAP HID channels in <1.5s when the PC wakes up, without manual user intervention.

### R5. Gyroscope Air Mouse & Presentation Pointer Exploration
Investigate feasibility of using the phone's IMU (gyroscope + accelerometer) as a 3D air mouse / laser pointer:
- Mathematical fusion mapping pitch/yaw angular velocity to relative HID mouse deltas (`dX`, `dY`).
- Ergonomic trigger button (hold to aim, release to freeze) and click triggers.

---

## Deliverables & Acceptance Criteria

### Build & Test Health
- [ ] `./gradlew test` executes cleanly and 100% of JVM unit tests pass with zero regressions.
- [ ] `python tests/e2e/run_e2e_tests.py` passes 100% across Tiers 1-5.
- [ ] `./gradlew assembleRelease` compiles cleanly without warnings or errors.

### Defect Remediation & Quality
- [ ] All newly discovered bugs, race conditions, and deprecations in R1 are documented, fixed, and covered by automated regression tests.

### Architectural & Prototype Deliverables
- [ ] Comprehensive architectural report published to `docs/NEXT_GEN_INNOVATIONS_AND_AUDIT.md` covering R1-R5 with concrete CameraX/OCR pipelines, Watchdog state machines, and Air Mouse sensor math.
