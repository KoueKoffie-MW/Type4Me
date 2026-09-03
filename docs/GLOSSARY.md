# Type4Me Desktop: Ubiquitous Language & Domain Glossary

This document establishes the canonical ubiquitous language and domain model for the Type4Me Desktop suite.

---

## Core Domain Entities

### 1. Speech & Phonetics
*   **Phonetic Alignment**: The global dynamic programming alignment (Needleman-Wunsch algorithm) between reference calibration text and speech-to-text (ASR) transcription tokens, computing Word Error Rate (WER) and phoneme-level substitutions.
*   **Double Metaphone Key**: A 4-character phonetic representation of an English word (primary and secondary keys) that normalizes acoustic homophones (e.g., plosive devoicing `/d/ \rightarrow /t/`, dental fricatives `/θ/ \rightarrow /s/`, and vowel raising `/ɛ/ \rightarrow /ɪ/`).
*   **Phonetic Confusion Matrix**: A learned mapping of acoustic/phonetic substitutions ($\text{misrecognized} \rightarrow \text{intended}$) capturing speaker-specific pronunciation idiosyncrasies with occurrence frequencies and confidence weights.
*   **Phonetic Trie**: A high-speed, multi-word prefix tree executing deterministic phrase substitutions in sub-millisecond time before LLM reasoning.
*   **Diagnostic Calibration Passage**: Curated, phonetically balanced reading scripts designed to deliberately trigger regional accent phoneme shifts and developer jargon misrecognitions (*Simulink, Stateflow, quaternions, scancodes, mutexes, Gherkin*).
*   **Speaker Accent Profile**: A persistent JSON/SQLite document encapsulating base regional priors (e.g., South African English, German English) combined with personal calibration delta rules.

### 2. Context & Workspace Monitoring
*   **Context Source**: A local workstation file (e.g., `transcript.jsonl`, `PROJECT.md`, active git diff) monitored for agent dialogue momentum and active tasks.
*   **Shared Non-Blocking Watcher**: A file watcher configured with Windows `FILE_SHARE_READ | FILE_SHARE_WRITE` semantics to prevent `ERROR_SHARING_VIOLATION` crashes when the IDE actively appends to the log.
*   **Context Distillation**: The 4-tier filtering process that extracts sliding turn windows, unresolved error diagnostics, and active file paths from massive raw transcripts into a compact context header.
*   **Token Budget Allocator**: A configurable token quota (*Lean: 500*, *Balanced: 2,000*, *Deep: 8,000*) governing the maximum context injected into prompt synthesis requests.

### 3. Agentic Synthesis & Window Integration
*   **Prompt Modifier Template**: A structured transformation directive (e.g., *Bug Hunter*, *Architectural Refactor*, *Gherkin Spec*, *Direct Surgical Diff*, *Grill-Me Trigger*) converting colloquial human speech into actionable AI coding agent prompts.
*   **Target Window (`HWND`)**: The Win32 handle of the active IDE or terminal recorded prior to summoning the Type4Me Floating HUD.
*   **Target Window Pinning**: The ability to lock prompt dispatch to a specific IDE process handle, enabling ambient voice prompting while reading external docs on secondary displays.
*   **Simulated Injection**: The Win32 `SendInput` or `keybd_event` sequence that restores focus to the target window and dispatches synthesized prompts via simulated `Ctrl+V`.
*   **Floating HUD**: A frameless, semi-transparent, `WS_EX_TOPMOST` desktop overlay designed to capture immediate caret focus so Windows Voice Typing (`Win+H`) can stream text directly into the application.
*   **3-Way Split Diff View**: An inspection interface displaying `[Raw Speech]` $\rightarrow$ `[Accent Repaired]` $\rightarrow$ `[Agent Directive]` for surgical visual verification.
*   **Dual-Key Dispatch**: A keystroke pattern where `Enter` triggers instant auto-paste into the active editor, while `Ctrl+Enter` opens the 3-Way Split Diff View for inspection.
*   **Deterministic Local Fallback Tier**: An air-gapped offline engine executing Trie repairs and structural template wrapping with 0ms interruption when cloud APIs are unreachable.
*   **Autonomous Context Discovery Daemon**: A background service that identifies the active agent session's `transcript.jsonl` and repository `git diff` based on window activity and file modification timestamps.
