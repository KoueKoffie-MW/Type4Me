# ADR-0005: Seamless Graceful Degradation and Deterministic Offline Fallback

## Status
**Accepted** (2026-09-03)

## Context
Developers frequently operate under enterprise VPN restrictions, in air-gapped corporate environments, or on transit networks with intermittent connectivity. Furthermore, third-party API keys may face rate limits, network timeouts, or may not be immediately configured upon first install. 

If the application hard-blocks execution on cloud availability, it renders the developer utility fragile and unreliable. Conversely, mandating a heavy 4–8GB local GGUF model consumes excessive VRAM alongside resource-intensive workloads like MATLAB, Simulink, and Docker containers.

## Decision
We implement a **Dual-Tier Inference and Graceful Degradation Architecture**:
1. **Primary Online Tier (Gemini 3.7 Flash)**:
   - When a valid Gemini API key is configured and the network is active, prompt synthesis is executed via `gemini-3.7-flash` (GA August 2026), providing frontier instruction following, context pruning, and acoustic in-context reasoning.
2. **Deterministic Local Fallback Tier (<1ms)**:
   - If offline, if an API call exceeds a 1,200ms timeout threshold, or if no API key is set, the pipeline **instantly falls back to local execution**:
     - *Phonetic Repair*: Executed by the in-memory **Phonetic Trie** using the active speaker profile's substitution dictionary.
     - *Agent Directive Synthesis*: Structured deterministically using the **Rule-Based Template Engine**, formatting the objective, context header, and verification constraints without external network calls.
3. **Transparent Provenance Telemetry**:
   - The UI displays an explicit provenance badge (`Source: Gemini 3.7 Flash` vs `Source: Deterministic Local Fallback`) so developers always know which tier produced the prompt.

## Consequences
### Positive
*   100% operational uptime: the tool never crashes or blocks on network drops.
*   Zero additional memory or VRAM overhead on developer workstations.
*   Instantaneous (<1ms) response times in air-gapped or offline modes.

### Negative / Trade-offs
*   In offline fallback mode, prompt synthesis is structural and template-driven rather than generative/semantic.
