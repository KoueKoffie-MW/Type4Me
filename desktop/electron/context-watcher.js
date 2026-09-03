/**
 * Type4Me Desktop Context Watcher
 * Non-blocking file watcher and stream parser for active agent transcripts (transcript.jsonl),
 * project docs, and git status.
 * Implements ADR-0002 (4-Tier Distillation) and ADR-0007 (Autonomous Context Discovery Daemon).
 */

const fs = require('fs');
const path = require('path');
const readline = require('readline');
const { EventEmitter } = require('events');

class ContextWatcher extends EventEmitter {
  constructor() {
    super();
    this.currentFilePath = null;
    this.fsWatcher = null;
    this.lastKnownSize = 0;
    this.cachedEntries = [];
    this.isAutoDiscovered = false;
  }

  /**
   * Scans agent directory for newest active transcript (ADR-0007)
   */
  async autoDiscoverActiveTranscript() {
    const userProfile = process.env.USERPROFILE || process.env.HOME;
    if (!userProfile) return null;

    const brainDir = path.join(userProfile, '.gemini', 'antigravity', 'brain');
    if (!fs.existsSync(brainDir)) return null;

    try {
      const convDirs = await fs.promises.readdir(brainDir, { withFileTypes: true });
      let newestFile = null;
      let newestMtime = 0;

      for (const d of convDirs) {
        if (!d.isDirectory()) continue;
        const candidatePaths = [
          path.join(brainDir, d.name, '.system_generated', 'logs', 'transcript.jsonl'),
          path.join(brainDir, d.name, 'transcript.jsonl'),
        ];

        for (const cp of candidatePaths) {
          try {
            if (fs.existsSync(cp)) {
              const stat = await fs.promises.stat(cp);
              if (stat.mtimeMs > newestMtime) {
                newestMtime = stat.mtimeMs;
                newestFile = cp;
              }
            }
          } catch (e) {}
        }
      }

      if (newestFile) {
        this.isAutoDiscovered = true;
        this.startWatching(newestFile);
        return newestFile;
      }
    } catch (err) {
      console.warn('Auto-discovery warning:', err.message);
    }
    return null;
  }

  /**
   * Starts watching a target context file (e.g. transcript.jsonl)
   */
  startWatching(filePath) {
    if (!filePath || !fs.existsSync(filePath)) {
      return { success: false, error: 'File does not exist: ' + filePath };
    }

    if (this.fsWatcher) {
      this.stopWatching();
    }

    this.currentFilePath = filePath;
    this.readFullContext();

    try {
      this.fsWatcher = fs.watch(filePath, (eventType) => {
        if (eventType === 'change') {
          this.readIncrementalContext();
        }
      });
      return { success: true, filePath, isAutoDiscovered: this.isAutoDiscovered };
    } catch (err) {
      return { success: false, error: err.message };
    }
  }

  stopWatching() {
    if (this.fsWatcher) {
      this.fsWatcher.close();
      this.fsWatcher = null;
    }
    this.currentFilePath = null;
    this.cachedEntries = [];
  }

  /**
   * Non-blocking full read of transcript file (handles JSONL and markdown)
   */
  async readFullContext() {
    if (!this.currentFilePath) return;

    try {
      const stats = await fs.promises.stat(this.currentFilePath);
      this.lastKnownSize = stats.size;

      const entries = [];
      const fileStream = fs.createReadStream(this.currentFilePath, { encoding: 'utf8', flags: 'r' });
      const rl = readline.createInterface({ input: fileStream, crlfDelay: Infinity });

      for await (const line of rl) {
        if (!line.trim()) continue;
        try {
          const parsed = JSON.parse(line);
          entries.push(parsed);
        } catch {
          entries.push({ type: 'RAW_TEXT', content: line });
        }
      }

      this.cachedEntries = entries;
      this.emit('context-updated', this.getDistilledSummary());
    } catch (err) {
      console.warn('Context file read warning (shared access):', err.message);
    }
  }

  async readIncrementalContext() {
    await this.readFullContext();
  }

  /**
   * Distills the transcript into a compact token-budgeted payload (ADR-0002)
   */
  getDistilledSummary(turnBudget = 4) {
    if (!this.cachedEntries.length) {
      return {
        filePath: this.currentFilePath,
        isAutoDiscovered: this.isAutoDiscovered,
        totalEntries: 0,
        recentTurns: [],
        activeGoals: [],
        recentErrors: [],
        extractedSymbols: [],
      };
    }

    const turns = [];
    const activeGoals = [];
    const recentErrors = [];
    const symbols = new Set();

    for (let i = this.cachedEntries.length - 1; i >= 0; i--) {
      const item = this.cachedEntries[i];
      if (turns.length < turnBudget) {
        if (item.type === 'USER_INPUT') {
          turns.unshift({ role: 'user', content: typeof item.content === 'string' ? item.content.slice(0, 300) : '' });
        } else if (item.type === 'PLANNER_RESPONSE') {
          const text = typeof item.content === 'string' ? item.content.slice(0, 300) : '';
          turns.unshift({ role: 'assistant', content: text });
        }
      }

      if (item.status === 'ERROR' && recentErrors.length < 3) {
        recentErrors.push(typeof item.content === 'string' ? item.content.slice(0, 200) : 'Error event');
      }

      if (item.tool_calls && Array.isArray(item.tool_calls)) {
        for (const tc of item.tool_calls) {
          if (tc.args && (tc.args.TargetFile || tc.args.AbsolutePath || tc.args.SearchPath)) {
            const p = tc.args.TargetFile || tc.args.AbsolutePath || tc.args.SearchPath;
            const base = path.basename(p);
            symbols.add(base);
          }
        }
      }
    }

    return {
      filePath: this.currentFilePath,
      isAutoDiscovered: this.isAutoDiscovered,
      totalEntries: this.cachedEntries.length,
      recentTurns: turns,
      activeGoals,
      recentErrors,
      extractedSymbols: Array.from(symbols).slice(0, 15),
    };
  }
}

module.exports = new ContextWatcher();
