import React from 'react';
import { Sparkles, CheckCheck, ArrowRight, Bot, Cpu } from 'lucide-react';
import { PipelineExecutionResult } from '../engine/prompt/MultiPassPipeline';

interface DiffSplitViewProps {
  result: PipelineExecutionResult | null;
  onCopyPrompt: () => void;
  onDispatchToIde: () => void;
  targetWindow: string;
}

export const DiffSplitView: React.FC<DiffSplitViewProps> = ({
  result,
  onCopyPrompt,
  onDispatchToIde,
  targetWindow,
}) => {
  if (!result) return null;

  return (
    <div className="flex flex-col gap-4 mt-4 text-xs font-mono">
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-3">
        {/* Stage 1: Raw Voice */}
        <div className="bg-slate-900/80 border border-slate-800 rounded-lg p-3 flex flex-col">
          <div className="flex items-center justify-between text-slate-400 font-semibold mb-2 pb-1 border-b border-slate-800">
            <span className="flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse"></span>
              Stage 1: Raw Spoken Input
            </span>
            <span className="text-[10px] bg-slate-800 px-1.5 py-0.5 rounded text-slate-400">Win+H</span>
          </div>
          <div className="text-slate-300 whitespace-pre-wrap leading-relaxed flex-1">
            "{result.rawSpeech}"
          </div>
        </div>

        {/* Stage 2: Phonetic Accent Repair */}
        <div className="bg-slate-900/80 border border-teal-900/50 rounded-lg p-3 flex flex-col">
          <div className="flex items-center justify-between text-teal-400 font-semibold mb-2 pb-1 border-b border-slate-800">
            <span className="flex items-center gap-1.5">
              <Cpu className="w-3.5 h-3.5" />
              Stage 2: Accent Repair
            </span>
            <span className="text-[10px] bg-teal-950/80 text-teal-300 border border-teal-800 px-1.5 py-0.5 rounded">
              {result.deterministicReplacements.length} repaired
            </span>
          </div>
          <div className="text-teal-200 whitespace-pre-wrap leading-relaxed flex-1">
            "{result.phoneticallyRepaired}"
          </div>
          {result.deterministicReplacements.length > 0 && (
            <div className="mt-2 pt-2 border-t border-slate-800 flex flex-wrap gap-1">
              {result.deterministicReplacements.map((r, idx) => (
                <span key={idx} className="bg-teal-950 text-[10px] text-teal-300 px-1.5 py-0.5 rounded border border-teal-800/60">
                  <span className="line-through text-slate-500 mr-1">{r.from}</span>
                  <ArrowRight className="inline w-2.5 h-2.5 mx-0.5 text-teal-400" />
                  <span className="font-semibold text-teal-200">{r.to}</span>
                </span>
              ))}
            </div>
          )}
        </div>

        {/* Stage 3: Agentic Prompt Directives */}
        <div className="bg-slate-900/90 border border-blue-900/50 rounded-lg p-3 flex flex-col">
          <div className="flex items-center justify-between text-blue-400 font-semibold mb-2 pb-1 border-b border-slate-800">
            <span className="flex items-center gap-1.5">
              <Bot className="w-3.5 h-3.5" />
              Stage 3: Agent Prompt
            </span>
            <span className="text-[10px] bg-blue-950 text-blue-300 border border-blue-800 px-1.5 py-0.5 rounded">
              {result.source}
            </span>
          </div>
          <div className="text-slate-100 whitespace-pre-wrap leading-relaxed max-h-48 overflow-y-auto flex-1 font-mono text-[11px] bg-slate-950/60 p-2 rounded border border-slate-800/80">
            {result.finalPrompt}
          </div>
        </div>
      </div>

      {/* Action Footer */}
      <div className="flex items-center justify-between bg-slate-900/90 border border-slate-800 rounded-lg px-3 py-2">
        <div className="flex items-center gap-3 text-slate-400 text-[11px]">
          <span>Latency: <strong className="text-slate-200">{result.latencyMs}ms</strong></span>
          <span>•</span>
          <span>Context: <strong className="text-slate-200">~{result.contextTokensEstimated} tokens</strong></span>
          <span>•</span>
          <span>Target: <strong className="text-teal-300">{targetWindow || 'Active Editor'}</strong></span>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={onCopyPrompt}
            className="px-3 py-1 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded text-xs flex items-center gap-1.5 transition-colors"
          >
            <CheckCheck className="w-3.5 h-3.5 text-slate-400" />
            Copy Prompt
          </button>
          <button
            onClick={onDispatchToIde}
            className="px-3 py-1 bg-teal-600 hover:bg-teal-500 text-white font-medium rounded text-xs flex items-center gap-1.5 shadow-lg shadow-teal-900/30 transition-all active:scale-95"
          >
            <Sparkles className="w-3.5 h-3.5" />
            Dispatch & Paste to IDE (Enter)
          </button>
        </div>
      </div>
    </div>
  );
};
