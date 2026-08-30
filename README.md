<div align="center">

<img src="art/logo.png" alt="Type4Me Logo" width="160" height="160" />

# 🎙️ Type4Me

**Hardware-Level Speech-to-Keystroke & AI Prompt Bridge for Android**

[![Platform](https://img.shields.io/badge/Platform-Android%2014%20%7C%2015-3DDC84.svg?logo=android&logoColor=white)](https://android.com)
[![Protocol](https://img.shields.io/badge/Protocol-Bluetooth%20HID%20Profile-0082FC.svg?logo=bluetooth&logoColor=white)](https://www.bluetooth.com)
[![AI Engine](https://img.shields.io/badge/AI%20Engine-Google%20Gemini%20Flash--Lite-8E75FF.svg?logo=google&logoColor=white)](https://deepmind.google/technologies/gemini/)
[![Tests](https://img.shields.io/badge/Tests-434%20Passing-brightgreen.svg)](tests/)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

*Speak naturally. Let Gemini structure your thoughts or engineer agentic prompts. Transmit pure hardware keystrokes into any workstation with zero host-side drivers.*

</div>

---

## 🌟 Key Highlights

- ⌨️ **Pure Hardware Bluetooth HID**: Your PC, Mac, or Linux workstation detects your phone as a genuine physical Bluetooth keyboard. No background software, companion daemons, or browser extensions needed on the host.
- 🤖 **Agentic Prompt Engineering**: Dictate unstructured, stream-of-consciousness thoughts and instantly transform them into structured, high-agency prompts (*Context, Objective, Constraints & Rules, Required Output Format*) tailored for autonomous AI agents like **Antigravity**, **Claude**, and **Hermes**.
- ⚡ **Sub-Second Gemini 3.5 Flash-Lite**: Blazing-fast (~0.6s) intelligent speech cleanup, punctuation restoration, tone adjustment, and multilingual translation directly via Google GenAI REST API.
- ⏎ **Chat-Safe Soft-Enters**: Automatically translates multiline prompts into `Shift + Enter` keystrokes, ensuring chat windows, terminal prompts, and AI agents don't submit intermediate text prematurely before the entire message has finished typing.
- 🔴 **Live Delta-Diff Streaming**: Real-time voice typing synchronization that calculates the Longest Common Prefix (LCP) against the host text, emitting backspaces and typing additions dynamically as you speak.
- 🌐 **DIN 2137-1 German QWERTZ & US QWERTY**: Flawless hardware translation for German umlauts (`ä`, `ö`, `ü`, `ß`), `AltGr` symbols (`@`, `€`, `\`, `{`, `}`), dead keys with auto-space injection (`^`, `´`, `` ` ``), and smart typography.

---

## 🎙️ Lightweight Architecture & Voice Input Philosophy

Type4Me is intentionally designed to be **featherweight (<10 MB)** by avoiding bulky 500MB embedded ASR neural networks. Instead, it leverages the state-of-the-art voice typing already built into your Android operating system:

* **Leverage Existing On-Device Speech Engines**: Tap the transcription canvas and press the microphone icon (`🎙️`) on your on-screen keyboard.
* **Recommended Keyboard**: **Gboard (Google Keyboard)** is strongly recommended for its superior accuracy, multi-language speech recognition, seamless code-switching (e.g. English, Afrikaans, German), and offline on-device speech transcription.
* **Separation of Concerns**: Your system keyboard handles raw acoustic transcription; **Type4Me** handles **AI prompt orchestration, layout translation, and hardware keystroke injection**.

---

## 📱 Device Compatibility & Hardware Requirements

Type4Me operates via the Android **Bluetooth HID Device Profile** (`BluetoothProfile.HID_DEVICE`), allowing the phone to act as a true Bluetooth peripheral.

### 🧪 Verified Devices
* **Tested Primary Reference Device**: **Google Pixel 10 Pro** *(Android 15 / API 35)* — *100% verified with low-latency Bluetooth HID peripheral registration, live LED sync, and Gemini REST rewriting.*

### 📋 Compatibility Overview
| Category | Compatibility Status | Notes |
| :--- | :---: | :--- |
| **Google Pixel** *(Pixel 4 to 10 Pro / Fold)* | ✅ **Full Support** | Native Google Bluetooth stack with complete HID peripheral role. |
| **Samsung Galaxy** *(S20–S25, Note, Z Fold/Flip)* | ✅ **Full Support** | One UI 4.0+ (Android 12+) includes full Bluetooth HID Device HAL. |
| **OnePlus / OPPO / Realme** | ✅ **Full Support** | OxygenOS / ColorOS 11+ supports HID Device mode. |
| **Sony Xperia** *(Xperia 1, 5, 10 series)* | ✅ **Full Support** | Clean Android implementation with full HID stack. |
| **Motorola** *(Edge, Razr series)* | ✅ **Full Support** | Android 12+ stock Bluetooth stack. |
| **Xiaomi / POCO / Redmi** | ⚠️ **Most Devices** | Supported on HyperOS / MIUI builds with standard Bluetooth HAL. |
| **Android Go / Stripped OEM ROMs** | ❌ **Unsupported** | Some budget chipsets disable Bluetooth HID peripheral mode in kernel/HAL. |

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
        F --> G[📡 Android Bluetooth HID Profile]
    end

    subgraph Host Workstation [💻 Host Workstation]
        G -- Standard 8-Byte HID Reports --> H[🔌 Host Bluetooth Subsystem]
        H --> I[🖥️ IDE / Terminal / AI Chat / Agent]
    end
```

---

## 🚀 Getting Started

### 1. Requirements
* Compatible Android device with Bluetooth HID Peripheral support.
* Gboard (Google Keyboard) installed for high-speed voice dictation.
* Google Gemini API Key (get one free at [Google AI Studio](https://aistudio.google.com/)).

### 2. Pairing with your PC
1. Open **Type4Me** on your phone.
2. Tap the connection card in the top header or select **"Pair Host"** to make your device discoverable.
3. On your PC/Mac, open Bluetooth Settings, scan for **"Type4Me Keyboard"**, and pair.
4. The header on Type4Me will turn green (**Connected**) with live Caps Lock / Num Lock LED sync.

### 3. Setting your API Key
1. Tap the **Settings (⚙️)** icon in the top header.
2. Paste your Gemini API key and tap **"Test Gemini Connection"** (verifies in <1s).
3. Select your preferred model (Default: **Gemini 3.5 Flash-Lite** for ultra-low latency).

---

## 🧪 Testing & Verification

Type4Me is verified with a comprehensive multi-tier test suite:

### 1. Android JVM Unit Tests (133 Tests)
```bash
./gradlew test
```

### 2. Standalone Python E2E & Protocol Verification (301 Tests)
```bash
python tests/e2e/run_e2e_tests.py
```

* **Tier 1**: Core Feature Coverage (Report generation, Keymaps, AltGr, Dead keys)
* **Tier 2**: Boundary & Corner Cases (Empty strings, max buffers, rapid bursts)
* **Tier 3**: Cross-Feature Integration (AI rewrite + keymap translation + HID dispatch)
* **Tier 4**: Real-World Workload Scenarios (Full coding prompts, German emails)
* **Tier 5**: Adversarial Stress & Jitter (Simulated packet drops, timing jitter)

---

## 📄 License

This project is licensed under the GNU General Public License v3.0 (GPLv3) - see the [LICENSE](LICENSE) file for details.
