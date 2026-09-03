# ADR-0007: Context Workspace Auto-Discovery Daemon with Manual Override

## Status
**Accepted** (2026-09-03)

## Context
AI coding agents store live conversation transcripts in deep, environment-specific paths (e.g., `%USERPROFILE%\.gemini\antigravity\brain\<conversation-id>\.system_generated\logs\transcript.jsonl`). Manually opening file dialogs and navigating through hidden directories every time a developer switches tasks or opens a new session creates friction that discourages active context utilization.

## Decision
We implement an **Autonomous Context Discovery Daemon with Manual Override**:
1. **Automated Session Discovery**:
   - The daemon monitors standard agent runtime paths (e.g. `~/.gemini/antigravity/brain/`) and identifies the most recently updated active transcript based on file modification timestamps (`mtime`).
   - If a target IDE window is pinned or active, the daemon scans the working directory for active `git diff` and repository markers.
2. **Ambient HUD Status**:
   - The Floating HUD and Command Center display the detected active session: `Context: [Auto-Detected: nifty-galileo / 9ffca67c...]`.
3. **Manual Override & Drag-and-Drop**:
   - Developers retain full control to override auto-discovery by dropping any file directly onto the HUD/Command Center or using the manual file selector.

## Consequences
### Positive
*   Zero configuration: the app immediately grounds voice prompts in the active agent conversation upon opening.
*   Seamless task switching across multiple repos and conversation sessions.
*   Retains explicit control for custom files (`PROJECT.md`, specs, diff files).

### Negative / Trade-offs
*   Requires background directory polling across agent brain folders (mitigated by throttling checks to window-activation events).
