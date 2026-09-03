import React from 'react';
import { Layers, FileText, CheckCircle2, RefreshCw, FolderOpen, Sliders, AlertCircle, Bot, User } from 'lucide-react';
import { DistilledContextSummary, BUDGET_PROFILES, TokenBudgeter } from '../engine/context/TokenBudgeter';

interface ContextInspectorProps {
  contextSummary: DistilledContextSummary | null;
  activeBudget: 'lean' | 'balanced' | 'deep';
  onBudgetChange: (budget: 'lean' | 'balanced' | 'deep') => void;
  onSelectContextFile: () => Promise<void>;
  onRefreshContext: () => Promise<void>;
}

export const ContextInspector: React.FC<ContextInspectorProps> = ({
  contextSummary,
  activeBudget,
  onBudgetChange,
  onSelectContextFile,
  onRefreshContext,
}) => {
  const estimatedTokens = contextSummary
    ? TokenBudgeter.estimateTokens(TokenBudgeter.formatContextPrompt(contextSummary, activeBudget))
    : 0;
  const budgetCap = BUDGET_PROFILES[activeBudget].maxTokens;
  const budgetPercentage = Math.min(100, Math.round((estimatedTokens / budgetCap) * 100));

  return (
    <div className="flex flex-col gap-6 max-w-5xl mx-auto p-4 select-text">
      {/* Header Banner */}
      <div className="flex items-start justify-between bg-slate-900/90 border border-blue-900/40 rounded-xl p-5 shadow-lg">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <Layers className="w-5 h-5 text-blue-400" />
            <h2 className="text-lg font-bold text-slate-100">Agent Context & Token Budget Inspector</h2>
          </div>
          <p className="text-xs text-slate-400 max-w-2xl leading-relaxed">
            Point Type4Me to your active AI agent conversation transcript (e.g. <code className="text-teal-300">transcript.jsonl</code>) or project diff. The engine continuously watches the file, extracts active tasks and errors, and synthesizes a token-budgeted prompt context.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={onSelectContextFile}
            className="px-3 py-2 bg-blue-600 hover:bg-blue-500 text-white font-medium rounded-lg text-xs flex items-center gap-2 shadow-md shadow-blue-900/40 transition-all active:scale-95"
          >
            <FolderOpen className="w-4 h-4" />
            Select Context File
          </button>
          <button
            onClick={onRefreshContext}
            title="Reload Context Stream"
            className="p-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs transition-colors"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* File Status & Budget Slider */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* File Card */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between text-xs font-mono text-slate-400 mb-2">
              <span className="flex items-center gap-1.5">
                <FileText className="w-3.5 h-3.5 text-teal-400" />
                Active Context File
              </span>
              {contextSummary?.filePath ? (
                <span className="text-[10px] bg-emerald-950 text-emerald-300 border border-emerald-800 px-1.5 py-0.5 rounded flex items-center gap-1">
                  <CheckCircle2 className="w-2.5 h-2.5" /> Watching
                </span>
              ) : (
                <span className="text-[10px] bg-slate-800 text-slate-500 px-1.5 py-0.5 rounded">
                  None Selected
                </span>
              )}
            </div>
            <p className="text-xs font-mono text-slate-200 break-all bg-slate-950 p-2 rounded border border-slate-800">
              {contextSummary?.filePath || 'No context file selected (Click Select Context File above)'}
            </p>
          </div>
          <div className="text-[11px] font-mono text-slate-500 mt-2">
            Entries parsed: <strong className="text-slate-300">{contextSummary?.totalEntries || 0}</strong>
          </div>
        </div>

        {/* Token Budget Card */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 md:col-span-2 flex flex-col justify-between">
          <div className="flex items-center justify-between text-xs font-mono text-slate-400 mb-2">
            <span className="flex items-center gap-1.5">
              <Sliders className="w-3.5 h-3.5 text-blue-400" />
              Token Budget Allocation
            </span>
            <span className="text-xs font-mono text-slate-200">
              ~<strong>{estimatedTokens}</strong> / {budgetCap} tokens ({budgetPercentage}%)
            </span>
          </div>

          {/* Progress Bar */}
          <div className="w-full bg-slate-950 rounded-full h-2.5 overflow-hidden border border-slate-800 my-2">
            <div
              className={`h-full transition-all duration-300 ${
                budgetPercentage > 90 ? 'bg-amber-500' : 'bg-teal-500'
              }`}
              style={{ width: `${Math.min(100, budgetPercentage)}%` }}
            ></div>
          </div>

          {/* Preset Buttons */}
          <div className="flex items-center gap-2 mt-2">
            {(['lean', 'balanced', 'deep'] as const).map((b) => (
              <button
                key={b}
                onClick={() => onBudgetChange(b)}
                className={`flex-1 py-1.5 rounded-lg text-xs font-mono uppercase font-semibold transition-all ${
                  activeBudget === b
                    ? 'bg-blue-600 text-white shadow-md shadow-blue-900/40'
                    : 'bg-slate-950 border border-slate-800 text-slate-400 hover:text-slate-200'
                }`}
              >
                {b} ({BUDGET_PROFILES[b].maxTokens} tok)
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Referenced Symbols & Files */}
      {contextSummary && contextSummary.extractedSymbols.length > 0 && (
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
          <span className="text-xs font-mono text-slate-400 block mb-2">
            Referenced Workstation Files & AST Identifiers:
          </span>
          <div className="flex flex-wrap gap-1.5">
            {contextSummary.extractedSymbols.map((sym, idx) => (
              <span
                key={idx}
                className="bg-slate-950 text-xs font-mono text-teal-300 border border-slate-800 px-2.5 py-1 rounded-md"
              >
                {sym}
              </span>
            ))}
          </div>
        </div>
      )}

      {/* Recent Errors */}
      {contextSummary && contextSummary.recentErrors.length > 0 && (
        <div className="bg-slate-900/90 border border-rose-900/40 rounded-xl p-4">
          <span className="text-xs font-mono text-rose-400 flex items-center gap-1.5 mb-2">
            <AlertCircle className="w-4 h-4 text-rose-400" />
            Detected Errors in Session (Injected for Agent Diagnosis):
          </span>
          <div className="flex flex-col gap-2">
            {contextSummary.recentErrors.map((err, idx) => (
              <div
                key={idx}
                className="p-2.5 rounded bg-rose-950/40 border border-rose-900/50 text-rose-200 font-mono text-xs"
              >
                {err}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Recent Dialogue Turns */}
      {contextSummary && contextSummary.recentTurns.length > 0 && (
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 flex flex-col gap-3">
          <span className="text-xs font-mono text-slate-400 block pb-1 border-b border-slate-800">
            Recent Context Dialogue Turns:
          </span>
          <div className="flex flex-col gap-2.5 max-h-64 overflow-y-auto pr-1">
            {contextSummary.recentTurns.map((turn, idx) => (
              <div
                key={idx}
                className={`p-3 rounded-lg border text-xs font-mono ${
                  turn.role === 'user'
                    ? 'bg-slate-950/80 border-slate-800 text-slate-200'
                    : 'bg-blue-950/30 border-blue-900/50 text-blue-200'
                }`}
              >
                <div className="flex items-center gap-1.5 text-[10px] text-slate-500 font-semibold mb-1">
                  {turn.role === 'user' ? <User className="w-3 h-3" /> : <Bot className="w-3 h-3 text-blue-400" />}
                  <span>{turn.role === 'user' ? 'USER' : 'AGENT'}</span>
                </div>
                <p className="line-clamp-3 leading-relaxed">{turn.content}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
