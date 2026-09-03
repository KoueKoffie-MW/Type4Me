# 📜 Architecture Decision Records (ADRs)

This directory contains the immutable Architectural Decision Records documenting key design choices, trade-offs, and consensus for the Type4Me ecosystem.

---

## 📑 ADR Master Index

| # | Date | Title | Status | Primary Decision |
| :--- | :--- | :--- | :--- | :--- |
| **[ADR-0001](0001-hybrid-speech-capture-pipeline.md)** | 2026-09-03 | Hybrid Speech Capture Pipeline | **Accepted** | Dual-mode capture: caret focus for `Win+H` + Chromium Web Audio for native push-to-talk streaming to Gemini 3.5 Transcribe Live (1,000-word vocabulary biasing) and offline `whisper.cpp`. |
| **[ADR-0002](0002-context-distillation-and-token-budgeting.md)** | 2026-09-03 | Context Distillation & Token Budgeting | **Accepted** | 4-tier streaming parser: sliding turn window (last 2–4 turns) + diagnostic error trap + lexical symbol matcher with hard token caps (*Lean: 500*, *Balanced: 2k*, *Deep: 8k*). |
| **[ADR-0003](0003-learn-my-accent-calibration-pipeline.md)** | 2026-09-03 | Hierarchical Accent Calibration & Phonetic Repair | **Accepted** | Preloaded regional priors (SA English, German English) + diagnostic teleprompter reading passages with Needleman-Wunsch alignment + personal confusion matrix delta + sub-millisecond Phonetic Trie fast-path. |
| **[ADR-0004](0004-dual-key-prompt-dispatch-and-safety-review.md)** | 2026-09-03 | Dual-Key Prompt Dispatch & Safety Review | **Accepted** | `Enter` triggers instant auto-paste via Win32 simulated `Ctrl+V` (<500ms); `Ctrl+Enter` opens the 3-Way Split Diff View for surgical visual inspection. |
| **[ADR-0005](0005-seamless-graceful-degradation-and-offline-fallback.md)** | 2026-09-03 | Seamless Graceful Degradation & Offline Fallback | **Accepted** | Gemini 3.7 Flash online + instant local fallback to the in-memory Phonetic Trie and rule-based template engine (<1ms, 0 downtime, 0 extra VRAM, clear provenance badge). |
| **[ADR-0006](0006-target-window-binding-and-pinning.md)** | 2026-09-03 | Target Window Binding & 1-Click Pinning | **Accepted** | Dynamic foreground window tracking with 1-click Pin lock to dispatch prompts into Antigravity IDE while browsing external documentation on secondary monitors. |
| **[ADR-0007](0007-context-workspace-auto-discovery.md)** | 2026-09-03 | Context Workspace Auto-Discovery Daemon | **Accepted** | Automatic background discovery of newest active `transcript.jsonl` in `~/.gemini/antigravity/brain/` and active `git diff`, with manual override and drag-and-drop support. |

---

## 🏷️ Standards & Governance
All ADRs follow the standard Michael Nygard template:
* **Status**: Proposed, Accepted, Rejected, Deprecated, or Superseded.
* **Context**: The forces, user requirements, and constraints driving the decision.
* **Decision**: The specific architectural strategy chosen.
* **Consequences**: Explicit documentation of positive benefits and negative trade-offs.
