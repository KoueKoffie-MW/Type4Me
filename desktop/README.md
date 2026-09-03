# 🖥️ Type4Me Desktop

**Windows SOTA Voice Prompt Suite & Accent-Aware Context Engine for AI Coding Agents**

---

## 🌟 Overview

Type4Me Desktop is a native Windows power utility designed for engineers and developers who dictate complex prompts, face speech recognition errors due to non-native or regional accents (e.g. South African / Afrikaans, German), and need real-time context from local agent conversation transcripts (`transcript.jsonl`) transformed into high-potency agentic directives.

### Key Capabilities

1. **Spotlight-Style Floating HUD (`Ctrl+Shift+Space` or `Alt+\``)**:
   - Frameless, semi-transparent, `WS_EX_TOPMOST` overlay.
   - Automatically focuses the input textbox upon summon so you can immediately press **`Win+H`** to dictate via Windows Voice Typing.
   - Dual-action dispatch: `Enter` executes instant auto-paste (<500ms) into your active editor; `Ctrl+Enter` opens the 3-Way Split Diff View for inspection.
2. **"Learn-My-Accent" Calibration Studio**:
   - Curated diagnostic teleprompter reading scripts loaded with phonemes and developer jargon (*Simulink, Stateflow, quaternions, scancodes, mutexes, Gherkin*).
   - Dynamic programming **Needleman-Wunsch global alignment** calculating Word Error Rate (WER) and acoustic substitutions.
   - Personal confusion matrix generator that learns your pronunciation shifts (e.g. `pin \rightarrow pen`, `sim you link \rightarrow Simulink`, `wariable \rightarrow variable`) and updates your speaker profile.
   - Sub-millisecond deterministic **Phonetic Trie** fast-path.
3. **Agent Context File Watcher & Token Budgeting**:
   - Non-blocking shared file reading (`FILE_SHARE_READ`) monitoring `transcript.jsonl` or `PROJECT.md` without file-lock collisions.
   - 4-tier distillation: sliding turn window (last 2–4 turns), diagnostic error trap, and lexical symbol matching.
   - User-configurable budget sliders (*Lean: 500 tokens*, *Balanced: 2,000 tokens*, *Deep: 8,000 tokens*).
4. **Agentic Prompt Modifier Matrix**:
   - 6 specialized developer templates: *Bug Hunter*, *Architectural Refactor*, *Gherkin Test Spec*, *Direct Surgical Diff*, *Grill-Me Trigger*, and *Clean Voice*.
   - Generative synthesis powered by **Gemini 3.7 Flash** (`gemini-3.7-flash`, GA August 2026) with offline deterministic rule-based fallback.
5. **Win32 Window Binding & Pinning**:
   - Automatically tracks foreground window handle (`HWND`), title, and process.
   - 1-click Pin lock allows you to read external documentation or web consoles on secondary monitors while always dispatching prompts into your pinned IDE.

---

## 🚀 Quickstart

### Prerequisites
* **Node.js**: v20+ (tested on Node v24.14.0)
* **npm**: v10+ (tested on npm 11.9.0)
* **Windows**: Windows 10/11 64-bit

### Installation & Launch

```powershell
# Navigate to desktop directory
cd c:\Users\Jan\Documents\antigravity\nifty-galileo\desktop

# Run development mode (Vite + Electron)
npm run dev

# Or build production assets
npm run build

# Run unit tests
npm test
```

---

## ⌨️ System Hotkeys

| Hotkey | Action | Description |
| :--- | :--- | :--- |
| `Ctrl + Shift + Space` | **Summon / Toggle HUD** | Brings Floating HUD to front and focuses textarea |
| `Alt + \`` | **Quick Toggle HUD** | Alternative one-handed toggle shortcut |
| `Win + H` | **Windows Voice Typing** | Native Windows dictation bar into focused textarea |
| `Enter` | **Rapid Auto-Dispatch** | Repairs accent, synthesizes prompt, and auto-pastes to IDE |
| `Ctrl + Enter` | **Inspect 3-Way Diff** | Halts auto-paste to preview raw, repaired, and agent output |
| `Alt + 1..6` | **Template Select** | Quick-select modifier template (Bug, Arch, Test, Diff, Grill, Clean) |

---

## 📁 Code Layout

```
desktop/
├── electron/
│   ├── main.js                  # Electron main: window modes, global hotkeys, tray, IPC
│   ├── preload.js               # Secure IPC bridge
│   ├── win32-helper.js          # Win32 window tracking & SendInput Ctrl+V simulation
│   └── context-watcher.js       # Non-blocking file watcher & stream parser for transcripts
├── src/
│   ├── engine/
│   │   ├── accent/              # Double Metaphone, Needleman-Wunsch, Confusion Matrix, Trie, Priors
│   │   ├── context/             # TokenBudgeter, sliding window, entity biasing
│   │   └── prompt/              # PromptModifierEngine, GeminiClient (3.7 Flash), MultiPassPipeline
│   ├── components/              # FloatingHud, CommandCenter, AccentStudio, ContextInspector, DiffSplitView
│   ├── App.tsx                  # Root state & mode switching
│   └── main.tsx                 # React entry point
└── tests/                       # Unit tests (Double Metaphone, Trie, WER, profile updates)
```
