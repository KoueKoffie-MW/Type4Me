# 🗺️ Type4Me Hierarchical Documentation Map

This document serves as the master navigation index for all technical, architectural, and operational documentation across the Type4Me multi-platform ecosystem.

---

## 🏛️ Documentation Hierarchy

```
Type4Me Root
├── 📄 README.md                                  [L0: Ecosystem Overview]
├── 📄 PROJECT.md                                 [L0: Project Milestones & Feature Inventory]
│
├── 📂 docs/
│   ├── 📄 INDEX.md                               [L1: Master Documentation Map]
│   ├── 📄 ARCHITECTURE.md                        [L1: Multi-Platform System Architecture]
│   ├── 📄 GLOSSARY.md                            [L1: Ubiquitous Language & Domain Model]
│   │
│   ├── 📂 adr/                                   [L1: Architectural Decision Records]
│   │   ├── 📄 README.md                          (ADR Index & Governance)
│   │   ├── 📄 0001-hybrid-speech-capture-pipeline.md
│   │   ├── 📄 0002-context-distillation-and-token-budgeting.md
│   │   ├── 📄 0003-learn-my-accent-calibration-pipeline.md
│   │   ├── 📄 0004-dual-key-prompt-dispatch-and-safety-review.md
│   │   ├── 📄 0005-seamless-graceful-degradation-and-offline-fallback.md
│   │   ├── 📄 0006-target-window-binding-and-pinning.md
│   │   └── 📄 0007-context-workspace-auto-discovery.md
│   │
│   ├── 📄 PROMPT_ENGINEERING.md                  [L2: Agentic Prompt Modifier Matrix]
│   ├── 📄 RESEARCH_AND_FEATURE_ROADMAP.md        [L2: Technical Research & SOTA Benchmarks]
│   ├── 📄 NEXT_GEN_INNOVATIONS_AND_AUDIT.md      [L2: Codebase Audit & Innovations]
│   ├── 📄 HARDWARE_COMPATIBILITY.md              [L2: Mobile Bluetooth/USB HID Compatibility]
│   └── 📄 TOUCHPAD_GUIDE.md                      [L2: Mobile Trackpad & Gestures Guide]
│
├── 📂 desktop/                                    [Subsystem: Windows Desktop Suite]
│   ├── 📄 README.md                              [L2: Desktop Developer Guide & Hotkeys]
│   ├── 📂 electron/                              (Main process, Win32 helper, context watcher)
│   ├── 📂 src/engine/                            (Phonetic alignment, Trie, token budgeter, Gemini)
│   ├── 📂 src/components/                        (Floating HUD, Command Center, Calibration Studio)
│   └── 📂 tests/                                 (Phonetic engine & alignment unit tests)
│
└── 📂 app/                                        [Subsystem: Android Mobile HID Suite]
    ├── 📄 build.gradle.kts
    └── 📂 src/main/java/com/transcriptor/hid/     (Jetpack Compose, BluetoothProfile.HID_DEVICE, DAOs)
```

---

## 📑 Level-by-Level Reading Guides

### Level 0: Ecosystem & Governance
*   **[README.md](../README.md)**: High-level introduction to the Type4Me dual-mode ecosystem: Mobile Hardware HID bridge for air-gapped workstations + Desktop Windows Power Suite for ambient agentic prompt authoring.
*   **[PROJECT.md](../PROJECT.md)**: Production milestone tracker (M1–M6 Mobile Android Suite, M7 Desktop Windows Power Suite), interface contracts, and complete feature inventory.

### Level 1: System Architecture & Decisions
*   **[docs/ARCHITECTURE.md](ARCHITECTURE.md)**: Multi-platform system architecture detailing both the Android Bluetooth/USB HID subsystem and the Windows Desktop in-situ injection engine.
*   **[docs/GLOSSARY.md](GLOSSARY.md)**: Canonical ubiquitous language and domain model standardizing terms across speech recognition, phonetic alignment, context budgeting, and window management.
*   **[docs/adr/README.md](adr/README.md)**: Master index of all 7 Architectural Decision Records documenting consensus on speech capture, accent calibration, token budgeting, and window binding.

### Level 2: Subsystem Implementations & Guides
*   **[desktop/README.md](../desktop/README.md)**: Complete guide to Type4Me Desktop: Floating HUD shortcuts (`Ctrl+Shift+Space`), `Win+H` dictation, "Learn-My-Accent" teleprompter, and development commands.
*   **[docs/PROMPT_ENGINEERING.md](PROMPT_ENGINEERING.md)**: Deep dive into the prompt modifier matrix (*Bug Hunter*, *Architectural Refactor*, *Gherkin Spec*, *Direct Surgical Diff*, *Grill-Me Trigger*).
*   **[docs/HARDWARE_COMPATIBILITY.md](HARDWARE_COMPATIBILITY.md)**: Tested operating systems, Bluetooth controllers, and USB HID scancode tables for the mobile peripheral.
*   **[docs/TOUCHPAD_GUIDE.md](TOUCHPAD_GUIDE.md)**: Multi-touch gesture specifications, relative mouse packet structures, and cursor acceleration formulas.
*   **[docs/RESEARCH_AND_FEATURE_ROADMAP.md](RESEARCH_AND_FEATURE_ROADMAP.md)**: In-depth research papers, competitive benchmarks, and long-term feature roadmap.
