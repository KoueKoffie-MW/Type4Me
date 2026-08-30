# 🏛️ Type4Me System Architecture

**Type4Me** is a native Android application designed to bridge human speech, agentic AI prompt structuring, and pure hardware-level keystroke / mouse injection into any host workstation over Bluetooth.

---

## 🧭 High-Level System Architecture

```mermaid
graph TB
    subgraph Input Layer [🎙️ Voice & Touch Input Layer]
        IME[Gboard / System IME<br/>On-Device Speech-to-Text] -->|Text Stream| Canvas[Transcription Canvas]
        Touch[Capacitive Touch Surface<br/>1-Finger / 2-Finger Gestures] -->|Motion Events| Trackpad[Touchpad Engine]
    end

    subgraph AI Intelligence Pipeline [🤖 Agentic AI Pipeline]
        Canvas -->|Raw Transcription| Rewriter[Gemini Remote Rewriter]
        Presets[(Room Database<br/>Prompt Presets)] -->|System Prompts| Rewriter
        Settings[(DataStore<br/>API Key & Model)] -->|Config| Rewriter
        Rewriter -->|Structured Prompt| Canvas
    end

    subgraph HID Translation & Protocol Layer [⌨️ Keystroke & Mouse Engine]
        Canvas -->|Buffered Text| KeymapEngine[Keymap Translator<br/>DE QWERTZ / US QWERTY]
        Canvas -->|Live Diff Text| LcpEngine[LCP Delta-Diff Engine]
        Trackpad -->|dX, dY, Buttons, Scroll| MouseEngine[Mouse Report Formatter]

        KeymapEngine --> KeystrokeDispatcher[Keystroke Dispatcher<br/>Burst / Pacing / Modifiers]
        LcpEngine --> KeystrokeDispatcher
    end

    subgraph Bluetooth HID Peripheral Transport [📡 Transport Layer]
        KeystrokeDispatcher -->|Report ID 1: 8B Keyboard| BtHid[Bluetooth HID Transport<br/>BluetoothProfile.HID_DEVICE]
        MouseEngine -->|Report ID 2: 4B Mouse| BtHid
        Service[HidDeviceService<br/>Android 14/15 FGS] --> BtHid
    end

    subgraph Host Workstation [💻 Host Workstation (Win / Mac / Linux)]
        BtHid -->|Standard HID Packets| HostBt[Host Bluetooth Stack]
        HostBt --> HostOs[OS Kernel HID Driver<br/>Zero Client Software]
        HostOs --> WorkstationApps[IDE / Terminal / Claude / Antigravity / Cursor]
    end
```

---

## 🧩 Architectural Components

### 1. Presentation Layer (Jetpack Compose & MVI)
- **Unidirectional Data Flow**: The UI is entirely driven by `MainViewModel` observing a single immutable `MainUiState` flow and processing structured `MainUiIntent` actions.
- **Adaptive Layout**: Features `verticalScroll` handling and responsive layout scaling across Portrait and Landscape orientations, with soft-keyboard inset awareness.
- **Component Decomposition**:
  - `ConnectionHeader`: Displays real-time Bluetooth connection status, connected host name, and LED indicators (Caps Lock, Num Lock).
  - `ControlBar`: Instant layout toggling (DE QWERTZ vs. US QWERTY), live-diff toggle, and typing speed slider (2ms–50ms).
  - `PresetSelector`: Horizontal scrolling chip bar for built-in and custom AI prompt templates.
  - `TranscriptionCanvas`: Live speech transcription and text editing canvas with word/character telemetry.
  - `TouchpadCanvas`: Full-screen trackpad with multi-touch gestures, scroll strip, and physical click buttons.

---

### 2. Composite Bluetooth HID Engine
The application emulates a **Composite Bluetooth Human Interface Device (HID)** conforming to the USB HID 1.11 specification.

#### HID Report Descriptor Layout (129 Bytes)
```
+------------------------------------------------------------------------+
|                      COMPOSITE HID REPORT DESCRIPTOR                   |
+------------------------------------+-----------------------------------+
|  KEYBOARD REPORT (Report ID 1)     |  MOUSE REPORT (Report ID 2)       |
|  - Usage: Generic Desktop Keyboard |  - Usage: Generic Desktop Mouse   |
|  - Input Report: 8 Bytes           |  - Input Report: 4 Bytes          |
|    * Byte 0: Modifier Bitmask      |    * Byte 0: Button Bitmask (L/R/M|
|    * Byte 1: Reserved OEM Byte     |    * Byte 1: Relative dX (-127..+127)
|    * Bytes 2-7: 6-Key Rollover     |    * Byte 2: Relative dY (-127..+127)
|  - Output Report: 1 Byte (LEDs)    |    * Byte 3: Relative Wheel       |
+------------------------------------+-----------------------------------+
```

#### Bluetooth SDP Record Configuration
- **Device Name**: `Type4Me Keyboard`
- **Device Provider**: `Type4Me`
- **Device Description**: `Voice-to-HID Speech Input & Touchpad Companion`
- **SDP Subclass**: `0xC0` (`0x40` Keyboard | `0x80` Mouse = Combo Peripheral)

---

### 3. Keymap Translation & Typography Subsystem
Translates UTF-8 strings into hardware HID scancodes and modifier bitmasks (`MOD_LSHIFT`, `MOD_RALT` / `AltGr`).

- **German QWERTZ (DIN 2137-1)**:
  - Umlauts (`ä`, `ö`, `ü`, `ß`) mapped to dedicated German key scancodes (`0x34`, `0x33`, `0x2F`, `0x2D`).
  - Swapped `Z` (`0x1D`) and `Y` (`0x1C`).
  - `AltGr` symbols: `@`, `€`, `\`, `{`, `}`, `[`, `]`, `~`, `|` mapped with `MOD_RALT`.
  - **Dead Keys**: Acute (`´`), Grave (`` ` ``), and Circumflex (`^`) automatically inject a trailing Space keystroke (`0x2C`) to prevent accidental host dead-key combinations.
- **US QWERTY**:
  - Full ASCII symbol map (shifted numbers, punctuation, curly brackets).
  - Shifted symbols correctly synthesized with `MOD_LSHIFT`.
- **Chat-Safe Soft-Enters**:
  - `\n` and `\r` newline characters are translated into `Shift + Enter` (`MOD_LSHIFT | KEY_ENTER`). This ensures multiline prompts can be streamed into AI chat interfaces (ChatGPT, Claude, Antigravity, Discord) without triggering premature message submissions.

---

### 4. Live Delta-Diff Synchronization (LCP Engine)
When **Live Diff Mode** is active:
1. Every partial hypothesis from voice typing is compared against the previously transmitted text.
2. The engine computes the **Longest Common Prefix (LCP)** between previous and new text.
3. It emits `N = len(previous) - LCP` `KEY_BACKSPACE` keystrokes to erase divergent suffixes.
4. It types the new suffix characters dynamically.
5. In-flight coroutine jobs are conflated and serialized to guarantee deterministic ordering during rapid speech.

---

### 5. Dynamic AI Rewrite Pipeline
- **Google GenAI REST Integration**: Built with direct REST payload architecture to bypass gRPC/SDK threading bottlenecks.
- **Sub-Second Execution**: Uses `gemini-3.5-flash-lite` by default for ultra-low latency (~0.6s).
- **Dynamic Key & Model Resolution**: Reads API keys and model selections dynamically on every rewrite request without requiring app restarts or service recreation.
- **Room Database Presets**: Built-in presets are seeded automatically and can be extended by user-defined custom prompts stored locally.
