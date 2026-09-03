<div align="center">

<img src="art/logo.png" alt="Type4Me Logo" width="160" height="160" />

# 🎙️ Type4Me

**Dual-Platform Developer Suite: Mobile Hardware HID Bridge + Windows AI Voice Prompt & Accent Engine**

[![Download APK](https://img.shields.io/badge/Download%20APK-v1.0.0%20(4.9%20MB)-success.svg?logo=android&logoColor=white)](release/Type4Me-v1.0.0.apk)
[![Download Windows App](https://img.shields.io/badge/Download%20Windows%20App-v1.0.0%20(96%20MB)-0078D4.svg?logo=windows&logoColor=white)](release/Type4Me-Desktop-v1.0.0.exe)
[![Platform](https://img.shields.io/badge/Platform-Android%2014%20%7C%2015%20%2B%20Windows-3DDC84.svg?logo=android&logoColor=white)](https://android.com)
[![Protocol](https://img.shields.io/badge/Protocol-Bluetooth%20%7C%20USB%20HID%20%7C%20Win32-0082FC.svg?logo=bluetooth&logoColor=white)](https://www.bluetooth.com)
[![AI Engine](https://img.shields.io/badge/AI%20Engine-Gemini%203.7%20Flash%20%26%203.5%20Transcribe-8E75FF.svg?logo=google&logoColor=white)](https://deepmind.google/technologies/gemini/)
[![Tests](https://img.shields.io/badge/Tests-446%20Passing-brightgreen.svg)](tests/)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

*Speak naturally, navigate effortlessly. Transform stream-of-consciousness developer dictation into high-agency AI coding directives, repair regional accent distortions via Needleman-Wunsch alignment, ingest active agent transcripts with token budgeting, and stream pure keystrokes or simulated Win32 input directly into any workstation.*

</div>

---

## 🌟 Dual-Platform Ecosystem

Type4Me is architected across two complementary developer workflows:

1. **📱 Mobile Hardware HID Bridge (`app/`)**: An air-gapped Android hardware peripheral that emulates a physical Bluetooth & USB keyboard/mouse, typing blind electrical scancodes into corporate virtual desktops, remote SSH terminals, and locked workstations with zero host software.
2. **🖥️ Windows Desktop Power Suite (`desktop/`)**: An ambient developer overlay (Electron + React + Tailwind) featuring a Spotlight-style Floating HUD (`Ctrl+Shift+Space`), native `Win+H` focus management, the world-first "Learn-My-Accent" calibration studio, live `transcript.jsonl` context watching with token budgeting, and Gemini 3.7 Flash agentic prompt synthesis.

---

## 🌟 Key Highlights

- 🖥️ **Spotlight-Style Windows HUD & Win+H Caret Focus**: Summon a frameless, semi-transparent prompt HUD with `Ctrl+Shift+Space` or `Alt+\``; caret focus is set instantly so pressing `Win+H` streams native voice dictation directly into the box.
- 🇿🇦 **"Learn-My-Accent" Calibration Studio**: Curated teleprompter passages targeting tricky phoneme transitions (Afrikaans vowel shifts `/ɛ/ \rightarrow /ɪ/`, final plosive devoicing `/d/ \rightarrow /t/`, German dental fricatives `/θ/ \rightarrow /s/`, and developer jargon like *Simulink*, *Gherkin*, *quaternions*). Dynamically aligns via Needleman-Wunsch DP and learns your personal confusion matrix.
- ⚡ **Sub-Millisecond Deterministic Phonetic Trie**: Pre-loaded with South African English, German English, and General ESL priors to instantly repair jargon misrecognitions before LLM reasoning.
- 📚 **Agent Transcript Watcher & Token Budgeting**: Non-blocking shared file monitoring for `transcript.jsonl` and git diffs, with sliding turn windows (last 2–4 turns), active diagnostic error traps, and strict token caps (*Lean: 500*, *Balanced: 2,000*, *Deep: 8,000* tokens).
- 🤖 **Agentic Prompt Modifier Matrix**: 6 specialized developer templates (*Bug Hunter*, *Architectural Refactor*, *Gherkin Test Spec*, *Direct Surgical Diff*, *Grill-Me Trigger*, *Clean Voice*) powered by **Gemini 3.7 Flash** (GA August 2026) with offline deterministic fallback.
- 🪟 **Target Window Pinning & Simulated Injection**: 1-click Pin lock enables reading external docs or web consoles on secondary monitors while always auto-pasting synthesized prompts back into your pinned Antigravity IDE.
- ⌨️ **Pure Hardware Bluetooth & USB HID (Mobile)**: Your workstation detects your Android device as a genuine physical Bluetooth/USB keyboard and mouse. Zero host background daemons required.
- 🔒 **Zero Host Screen Exposure & Absolute Privacy**: Operates without screen-scraping or telemetry hooks, keeping sensitive codebases, classified CAD models, and private screens completely unexposed.
- 🚀 **Voice Typing for ANYTHING**: Because it operates strictly at the hardware HID layer, Type4Me brings instantaneous voice typing and AI prompt structuring to **any software on earth** — including locked-down corporate virtual desktops (Citrix, RDP, VMware), air-gapped terminal consoles (SSH, vim, tmux), IDEs (VS Code, Android Studio, Cursor), CAD/Simscape/MATLAB environments, and game engines.
- 🖱️ **Tactile Touchpad & Mouse Combo**: Switch instantly to trackpad mode to navigate your workstation screen. Features 1-finger smooth cursor glide, 1-finger tap (left click), long-press / 2-finger tap (right click), dedicated vertical scroll wheel strip, and adjustable speed multiplier ($0.5\times$ to $3.0\times$).
- 🤖 **Agentic Prompt Engineering**: Dictate unstructured, stream-of-consciousness thoughts and instantly transform them into structured, high-agency prompts (*Context, Objective, Constraints & Rules, Required Output Format*) tailored for autonomous AI agents like **Antigravity**, **Claude Code**, **ChatGPT**, and **Hermes**.
- 🇿🇦 **Speaker Accent & Phonetic ASR Repair**: Select your native accent (Afrikaans, German, Dutch, French, Indian, Spanish, or Custom) — Gemini dynamically contextualizes and repairs regional vowel shifts, dropped consonants, and phonetic mis-transcriptions based on sentence context.
- ⚡ **Sub-Second Gemini 3.5 Flash-Lite**: Blazing-fast (~0.6s) intelligent speech cleanup, punctuation restoration, tone adjustment, and multilingual translation directly via Google GenAI REST API.
- ⏎ **Chat-Safe Soft-Enters**: Automatically translates multiline prompts into `Shift + Enter` keystrokes, ensuring chat windows, terminal prompts, and AI agents don't submit intermediate text prematurely before the entire message has finished typing.
- 🔴 **Live Delta-Diff Streaming**: Real-time voice typing synchronization that calculates the Longest Common Prefix (LCP) against the host text, emitting backspaces and typing additions dynamically as you speak.
- 🌐 **DIN 2137-1 German QWERTZ & US QWERTY**: Flawless hardware translation for German umlauts (`ä`, `ö`, `ü`, `ß`), `AltGr` symbols (`@`, `€`, `\`, `{`, `}`), dead keys with auto-space injection (`^`, `´`, `` ` ``), and smart typography.

---

## 🔒 Radical Privacy & Universal Compatibility

### 1. Zero On-Screen Exposure
Traditional desktop AI tools, accessibility bridges, or telemetry daemons require elevated privileges to monitor and scrape what is displayed on your monitor. 

**Type4Me takes the opposite approach**:
* All speech capture and AI prompt rewriting take place **entirely on your mobile device**.
* The phone only transmits outgoing hardware electrical scancodes (keystrokes and mouse packets) to your workstation.
* Your proprietary source code, classified engineering models, confidential emails, and sensitive screens remain **100% invisible and unexposed**.

### 2. Universal Software Voice Typing
Because your operating system treats Type4Me as a standard physical USB/Bluetooth peripheral:
* **No Host Software or Admin Rights**: Works on strictly managed enterprise corporate laptops without requiring IT approval or background service installation.
* **Air-Gapped & Secure Environments**: Compatible with air-gapped development machines, secure virtualization layers, remote desktop sessions, and Linux headless servers.
* **Universal App Support**: Works seamlessly inside terminal editors (`vim`, `nano`, `emacs`), IDEs (`VS Code`, `IntelliJ`, `MATLAB`), chat apps (`Slack`, `Discord`, `Teams`), and browser prompt fields.

---

## 🎙️ Lightweight Architecture & Voice Input Philosophy

Type4Me is intentionally designed to be **featherweight (<10 MB)** by avoiding bulky 500MB embedded ASR neural networks. Instead, it leverages the state-of-the-art voice typing already built into your Android operating system:

* **Leverage Existing On-Device Speech Engines**: Tap the transcription canvas and press the microphone icon (`🎙️`) on your on-screen keyboard.
* **Recommended Keyboard**: **Gboard (Google Keyboard)** is strongly recommended for its superior accuracy, multi-language speech recognition, seamless code-switching (e.g. English, Afrikaans, German), and offline on-device speech transcription.
* **Separation of Concerns**: Your system keyboard handles raw acoustic transcription; **Type4Me** handles **AI prompt orchestration, layout translation, and hardware keystroke injection**.

---

## 🔌 Dual Transport: Wireless Bluetooth & Wired USB

Type4Me supports two independent hardware transport pipelines:

1. **Wireless Bluetooth HID Profile (`BluetoothProfile.HID_DEVICE`)**:
   - Standard 2.4 GHz wireless connection for untethered flexibility across laptops, desktop workstations, tablets, and phones.
2. **Wired USB Transport (`UsbHidTransport`)**:
   - **Android Open Accessory (AOA) 2.0**: Direct USB cable connection emulating a physical wired USB HID keyboard and mouse.
   - **ADB Reverse Socket Bridge**: Fallback TCP socket bridging over standard USB debugging cables.
   - **Linux USB Gadget (`/dev/hidg0`)**: Direct ConfigFS kernel gadget integration for embedded devices and rooted environments.
   - *Use cases*: RF-restricted defense/government zones, high-interference industrial labs, or desktop PCs without Bluetooth hardware.

---

## 📱 Device Compatibility & Hardware Matrix

Type4Me operates via the Android **Bluetooth HID Device Profile** (`BluetoothProfile.HID_DEVICE`), allowing the phone to act as a true Bluetooth peripheral.

### 🧪 Verified Reference Device
* **Google Pixel 10 Pro** *(Android 15 / API 35)* — *100% verified with low-latency Bluetooth HID peripheral registration, mouse trackpad tracking, live LED sync, and Gemini REST rewriting.*

### 📋 Full Compatibility Matrix by Manufacturer
| OEM / Brand | Supported Models & Series | Minimum OS Version |
| :--- | :--- | :---: |
| **Google** | **Pixel 10, 10 Pro (Reference Device)**<br>Pixel 9, 9 Pro, 9 Pro XL, 9 Pro Fold<br>Pixel 8, 8 Pro, 8a<br>Pixel 7, 7 Pro, 7a<br>Pixel 6, 6 Pro, 6a<br>Pixel 5, 5a, 4, 4 XL, 4a, 3, 3 XL, 2 | Android 9.0+ |
| **Samsung** | **Galaxy S Series:** S25, S24, S23, S22, S21, S20, S10, S9<br>**Galaxy Z Series:** Z Fold (1–6), Z Flip (1–6)<br>**Galaxy Note Series:** Note 20, Note 10, Note 9<br>**Galaxy A Series:** A55, A54, A53, A52, A73, A72, A35, A34<br>**Galaxy Tab Series:** Tab S10, S9, S8, S7 | One UI 2.0+<br>(Android 10+) |
| **OnePlus** | OnePlus 13, 12, 11, 10 Pro, 9 Pro, 8 Pro, 7 Pro, 6T<br>OnePlus Nord (Nord 2, 3, 4, CE series) | OxygenOS 11+ |
| **Sony** | Xperia 1 (Mark I through VI)<br>Xperia 5 (Mark I through V)<br>Xperia 10 (Mark I through VI)<br>Xperia PRO / PRO-I | Android 10+ |
| **Motorola** | Edge series (30, 40, 50 Pro/Ultra)<br>Razr series (2022, 40 Ultra, 50 Ultra)<br>Moto G Stylus 5G, Moto G Power 5G | Android 12+ |
| **Nothing** | Phone (1), Phone (2), Phone (2a) | Nothing OS 1.5+ |
| **ASUS** | ROG Phone (3, 5, 6, 7, 8 Pro)<br>Zenfone (8, 9, 10, 11 Ultra) | Android 11+ |
| **Xiaomi / POCO** | Xiaomi 15, 14, 13, 12, 11 series<br>POCO F6, F5, F4, X6 Pro, X5 Pro<br>Redmi Note 12, 13, 14 Pro/Pro+ | HyperOS /<br>MIUI 13+ |
| **Android Go / Stripped OEM ROMs** | Budget chipsets with stripped Bluetooth HAL stacks | ❌ Unsupported |

> [!NOTE]
> **Minimum Requirement**: Android 9.0 (API 28+) with Bluetooth Low Energy (BLE) and HID Device role enabled by the device manufacturer. Android 14/15 is recommended for full `FOREGROUND_SERVICE_CONNECTED_DEVICE` background typing stability.

---

## 🏗️ Architecture

```mermaid
graph LR
    subgraph Android Device [📱 Android Device]
        A[🎙️ Gboard Voice Typing] --> B[📝 Transcription Canvas]
        B --> C{✨ AI Rewrite?}
        C -- Yes --> D[⚡ Gemini 3.5 Flash-Lite]
        D --> B
        B --> E[⌨️ Keymap Translator<br/>DE QWERTZ / US QWERTY]
        E --> F[⏱️ Keystroke Dispatcher<br/>Burst / Live-Diff Engine]
        F --> G[📡 Bluetooth / USB HID Engine]
    end

    subgraph Host Workstation [💻 Host Workstation (Zero Software)]
        G -- Standard 8B Keyboard / 4B Mouse Reports --> H[🔌 Host In-Box HID Subsystem]
        H --> I[🖥️ Any App: Terminal / IDE / CAD / Citrix / AI Agent]
    end
```

---

## 🚀 Getting Started

### 1. Requirements
* Compatible Android device with Bluetooth HID Peripheral or USB OTG support.
* Gboard (Google Keyboard) installed for high-speed voice dictation.
* Google Gemini API Key (get one free at [Google AI Studio](https://aistudio.google.com/)).

### 2. Pairing with your PC (Bluetooth)
1. Open **Type4Me** on your phone.
2. Tap the connection card in the top header or select **"Pair Host"** to make your device discoverable.
3. On your PC/Mac, open Bluetooth Settings, scan for **"Type4Me Keyboard"**, and pair.
4. The header on Type4Me will turn green (**Connected**) with live Caps Lock / Num Lock LED sync.

### 3. Setting your API Key
1. Tap the **Settings (⚙️)** icon in the top header.
2. Paste your Gemini API key and tap **"Test Gemini Connection"** (verifies in <1s).
3. Select your preferred model (Default: **Gemini 3.5 Flash-Lite** for ultra-low latency).

---

## 📚 Technical Documentation & Guides

Comprehensive technical guides and architecture specifications are available in the [`docs/`](docs/) directory:

* 🏛️ **[System Architecture](docs/ARCHITECTURE.md)**: Unidirectional MVI data flow, Composite Bluetooth/USB HID descriptor (129B), LCP delta-diff streaming, and typography engine.
* 🔒 **[Privacy & Security Model](docs/ARCHITECTURE.md#security--screen-privacy-model)**: Air-gap assurance, zero host inspection, and physical keystroke isolation.
* 🖱️ **[Touchpad & Mouse Guide](docs/TOUCHPAD_GUIDE.md)**: Multi-touch trackpad gestures, scroll strip, tactile click buttons, and sensitivity curve tuning.
* 🤖 **[Agentic Prompt Engineering](docs/PROMPT_ENGINEERING.md)**: Transforming stream-of-consciousness speech into structured coding prompts for Antigravity, Claude, and ChatGPT.
* 📱 **[Hardware & Bluetooth HAL Matrix](docs/HARDWARE_COMPATIBILITY.md)**: Full vendor breakdown, Bluetooth HID Device profile requirements, and ADB diagnostics.

---

## 🧪 Testing & Verification

Type4Me is verified with a comprehensive multi-tier test suite:

### 1. Android JVM Unit Tests (135 Tests)
```bash
./gradlew test
```

### 2. Standalone Python E2E & Protocol Verification (301 Tests)
```bash
python tests/e2e/run_e2e_tests.py
```

* **Tier 1**: Core Feature Coverage (Report generation, Keymaps, AltGr, Dead keys, Mouse reports)
* **Tier 2**: Boundary & Corner Cases (Empty strings, max buffers, rapid bursts, mouse clamping)
* **Tier 3**: Cross-Feature Integration (AI rewrite + keymap translation + HID dispatch)
* **Tier 4**: Real-World Workload Scenarios (Full coding prompts, German emails)
* **Tier 5**: Adversarial Stress & Jitter (Simulated packet drops, timing jitter)

---

## 📄 License

This project is licensed under the GNU General Public License v3.0 (GPLv3) - see the [LICENSE](LICENSE) file for details.
