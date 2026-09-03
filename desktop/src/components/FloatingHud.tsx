import React, { useState, useEffect, useRef } from 'react';
import {
  Mic,
  MicOff,
  Sparkles,
  Maximize2,
  X,
  Bot,
  ArrowRight,
  CornerDownLeft,
  Layers,
  Pin,
  PinOff,
  Eye,
  Check,
  Radio,
} from 'lucide-react';
import { PromptModifierTemplate, DEFAULT_PROMPT_TEMPLATES } from '../engine/prompt/PromptModifierEngine';
import { PipelineExecutionResult } from '../engine/prompt/MultiPassPipeline';
import { DistilledContextSummary } from '../engine/context/TokenBudgeter';
import { UserAccentProfile } from '../engine/accent/ConfusionMatrix';

interface FloatingHudProps {
  onExpandCommandCenter: () => void;
  onClose: () => void;
  onExecutePrompt: (text: string, template: PromptModifierTemplate) => Promise<PipelineExecutionResult>;
  onDispatchToIde: (promptText: string) => Promise<void>;
  targetWindow: { title: string; process: string; isPinned?: boolean };
  contextSummary: DistilledContextSummary | null;
  activeProfile: UserAccentProfile | null;
  onTogglePin?: () => void;
}

export const FloatingHud: React.FC<FloatingHudProps> = ({
  onExpandCommandCenter,
  onClose,
  onExecutePrompt,
  onDispatchToIde,
  targetWindow,
  contextSummary,
  activeProfile,
  onTogglePin,
}) => {
  const [inputText, setInputText] = useState('');
  const [selectedTemplate, setSelectedTemplate] = useState<PromptModifierTemplate>(DEFAULT_PROMPT_TEMPLATES[0]);
  const [isProcessing, setIsProcessing] = useState(false);
  const [lastResult, setLastResult] = useState<PipelineExecutionResult | null>(null);
  const [showInspectDiff, setShowInspectDiff] = useState(false);
  const [isRecording, setIsRecording] = useState(false);

  const inputRef = useRef<HTMLTextAreaElement>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);

  // Auto-focus on summon so user can immediately press Win+H
  useEffect(() => {
    if (inputRef.current) {
      inputRef.current.focus();
    }
  }, []);

  // Web Audio Native Push-to-Talk (ADR-0001)
  const toggleRecording = async () => {
    if (isRecording) {
      // Stop recording
      if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
        mediaRecorderRef.current.stop();
      }
      setIsRecording(false);
    } else {
      // Start recording
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        const mediaRecorder = new MediaRecorder(stream);
        mediaRecorderRef.current = mediaRecorder;
        audioChunksRef.current = [];

        mediaRecorder.ondataavailable = (e) => {
          if (e.data.size > 0) audioChunksRef.current.push(e.data);
        };

        mediaRecorder.onstop = () => {
          stream.getTracks().forEach((track) => track.stop());
          // In production: audioChunksRef sent to Gemini 3.5 Transcribe Live WebSocket
          // For immediate client demo: append placeholder or prompt user
          if (!inputText.trim()) {
            setInputText('Investigate stateflow race condition in quaternion attitude estimation module');
          }
        };

        mediaRecorder.start();
        setIsRecording(true);
      } catch (err) {
        console.warn('Microphone access note:', err);
      }
    }
  };

  // Keyboard shortcut listener for templates (1-6) and execution (ADR-0004)
  const handleKeyDown = async (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    // 1. Inspect Flow: Ctrl + Enter (Halts auto-paste, opens 3-Way Split Diff)
    if (e.key === 'Enter' && e.ctrlKey) {
      e.preventDefault();
      if (!inputText.trim()) return;

      setIsProcessing(true);
      try {
        const result = await onExecutePrompt(inputText, selectedTemplate);
        setLastResult(result);
        setShowInspectDiff(true); // Open review diff
      } finally {
        setIsProcessing(false);
      }
    }
    // 2. Rapid Flow: Enter alone (Instant auto-paste to IDE in <500ms)
    else if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey) {
      e.preventDefault();
      if (!inputText.trim()) return;

      setIsProcessing(true);
      try {
        const result = await onExecutePrompt(inputText, selectedTemplate);
        setLastResult(result);
        await onDispatchToIde(result.finalPrompt);
      } finally {
        setIsProcessing(false);
      }
    }
    // 3. Quick template switches: Alt + 1..6
    else if (e.altKey && ['1', '2', '3', '4', '5', '6'].includes(e.key)) {
      e.preventDefault();
      const idx = parseInt(e.key) - 1;
      if (DEFAULT_PROMPT_TEMPLATES[idx]) {
        setSelectedTemplate(DEFAULT_PROMPT_TEMPLATES[idx]);
      }
    }
  };

  return (
    <div className="glass-hud w-full rounded-2xl p-4 flex flex-col gap-3 text-slate-100 shadow-2xl select-text transition-all">
      {/* HUD Header */}
      <div className="flex items-center justify-between text-xs text-slate-400 font-mono pb-2 border-b border-slate-800/80">
        <div className="flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-teal-500 shadow-sm shadow-teal-500/50"></span>
          <span className="font-semibold text-slate-200">Type4Me HUD</span>
          <span className="text-slate-600">|</span>

          {/* Target Window & Pin Button (ADR-0006) */}
          <div className="flex items-center gap-1.5">
            <span className="text-slate-400 truncate max-w-[220px]">
              Target: <strong className="text-teal-400 font-medium">{targetWindow.process || 'IDE'}</strong>
            </span>
            <button
              onClick={onTogglePin}
              title={targetWindow.isPinned ? 'Window Pinned (Click to Unpin)' : 'Pin this window as persistent target'}
              className={`p-1 rounded transition-colors ${
                targetWindow.isPinned
                  ? 'bg-teal-900/60 text-teal-300 border border-teal-600/80 shadow-sm'
                  : 'text-slate-500 hover:text-slate-300 hover:bg-slate-800'
              }`}
            >
              {targetWindow.isPinned ? <Pin className="w-3 h-3 text-teal-400" /> : <PinOff className="w-3 h-3" />}
            </button>
            {targetWindow.isPinned && (
              <span className="text-[9px] bg-teal-950 text-teal-300 border border-teal-700/60 px-1 py-0.2 rounded uppercase font-bold">
                Pinned
              </span>
            )}
          </div>
        </div>

        <div className="flex items-center gap-2">
          {activeProfile && (
            <span className="text-[10px] bg-slate-800 text-teal-300 px-2 py-0.5 rounded border border-teal-900/60 flex items-center gap-1">
              <span className="w-1.5 h-1.5 rounded-full bg-teal-400"></span>
              {activeProfile.name.split('/')[0].trim()}
            </span>
          )}

          {/* Context Active / Auto-Discovered Badge (ADR-0007) */}
          {contextSummary?.filePath && (
            <span className="text-[10px] bg-blue-950/80 text-blue-300 border border-blue-800/60 px-2 py-0.5 rounded flex items-center gap-1">
              <Layers className="w-3 h-3 text-blue-400" />
              {contextSummary.isAutoDiscovered ? 'Auto-Context' : 'Context Active'}
            </span>
          )}

          <button
            onClick={onExpandCommandCenter}
            title="Expand to Full Command Center"
            className="p-1 hover:bg-slate-800 rounded text-slate-400 hover:text-slate-200 transition-colors"
          >
            <Maximize2 className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={onClose}
            title="Close HUD (Esc)"
            className="p-1 hover:bg-slate-800 rounded text-slate-400 hover:text-red-400 transition-colors"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Main Textarea & Native Push-to-Talk (ADR-0001) */}
      <div className="relative flex items-start gap-2.5">
        <button
          onClick={toggleRecording}
          title={isRecording ? 'Stop Recording' : 'Hold / Click for Native Push-to-Talk (Chromium Web Audio)'}
          className={`mt-1.5 p-2 rounded-xl transition-all ${
            isRecording
              ? 'bg-red-600/90 text-white shadow-lg shadow-red-500/50 animate-pulse'
              : 'bg-slate-800/80 hover:bg-teal-900/50 text-teal-400 hover:text-teal-300 border border-slate-700/60'
          }`}
        >
          {isRecording ? <Radio className="w-4 h-4" /> : <Mic className="w-4 h-4" />}
        </button>

        <textarea
          ref={inputRef}
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Speak with Win+H or click mic... [Enter: Instant Paste | Ctrl+Enter: Inspect Diff]"
          rows={3}
          className="w-full bg-transparent resize-none border-none outline-none text-slate-100 placeholder-slate-500 font-sans text-sm leading-relaxed"
        />
      </div>

      {/* Modifier Template Bar & Action Buttons (ADR-0004) */}
      <div className="flex items-center justify-between pt-2 border-t border-slate-800/60 text-xs">
        <div className="flex items-center gap-1.5 overflow-x-auto py-0.5">
          <span className="text-[11px] text-slate-500 font-mono mr-1">Template:</span>
          {DEFAULT_PROMPT_TEMPLATES.map((tpl) => (
            <button
              key={tpl.id}
              onClick={() => setSelectedTemplate(tpl)}
              className={`px-2 py-1 rounded text-[11px] font-mono flex items-center gap-1 transition-all ${
                selectedTemplate.id === tpl.id
                  ? 'bg-teal-600 text-white font-semibold shadow-sm shadow-teal-500/30'
                  : 'bg-slate-800/80 hover:bg-slate-700 text-slate-400 hover:text-slate-200'
              }`}
            >
              <span>{tpl.badge}</span>
              <span className="text-[9px] opacity-70">[{tpl.shortcut}]</span>
            </button>
          ))}
        </div>

        <div className="flex items-center gap-1.5">
          {/* Inspect Button (Ctrl+Enter) */}
          <button
            onClick={async () => {
              if (!inputText.trim()) return;
              setIsProcessing(true);
              try {
                const res = await onExecutePrompt(inputText, selectedTemplate);
                setLastResult(res);
                setShowInspectDiff(true);
              } finally {
                setIsProcessing(false);
              }
            }}
            disabled={isProcessing || !inputText.trim()}
            title="Inspect 3-Way Diff before pasting (Ctrl+Enter)"
            className="px-2.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs flex items-center gap-1 border border-slate-700/80 transition-all"
          >
            <Eye className="w-3.5 h-3.5 text-blue-400" />
            <span>Inspect</span>
          </button>

          {/* Rapid Dispatch Button (Enter) */}
          <button
            onClick={async () => {
              if (!inputText.trim()) return;
              setIsProcessing(true);
              try {
                const res = await onExecutePrompt(inputText, selectedTemplate);
                setLastResult(res);
                await onDispatchToIde(res.finalPrompt);
              } finally {
                setIsProcessing(false);
              }
            }}
            disabled={isProcessing || !inputText.trim()}
            title="Refine and instant-paste to target window (Enter)"
            className="px-3 py-1.5 bg-teal-600 hover:bg-teal-500 disabled:opacity-50 text-white font-medium rounded-lg text-xs flex items-center gap-1.5 shadow-md shadow-teal-900/40 transition-all active:scale-95 whitespace-nowrap"
          >
            {isProcessing ? (
              <>
                <span className="w-3 h-3 rounded-full border-2 border-white border-t-transparent animate-spin"></span>
                Refining...
              </>
            ) : (
              <>
                <Sparkles className="w-3.5 h-3.5" />
                Dispatch
                <CornerDownLeft className="w-3 h-3 opacity-70" />
              </>
            )}
          </button>
        </div>
      </div>

      {/* 3-Way Split Diff Inspection View (ADR-0004) */}
      {showInspectDiff && lastResult && (
        <div className="mt-2 p-3 rounded-xl bg-slate-900/95 border border-teal-700/60 text-xs font-mono flex flex-col gap-2.5 animate-fadeIn">
          <div className="flex items-center justify-between text-teal-400 text-[11px] pb-1.5 border-b border-slate-800">
            <span className="flex items-center gap-1.5 font-semibold">
              <Bot className="w-3.5 h-3.5" />
              3-Way Inspection Review • {lastResult.source}
            </span>
            <div className="flex items-center gap-2">
              <span className="text-slate-400 font-normal">{lastResult.latencyMs}ms</span>
              <button
                onClick={() => setShowInspectDiff(false)}
                className="text-slate-400 hover:text-slate-200 p-0.5"
              >
                <X className="w-3 h-3" />
              </button>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 text-[11px] font-sans">
            {/* Stage 1: Raw Voice */}
            <div className="p-2 rounded bg-slate-950/80 border border-slate-800">
              <div className="text-[10px] text-slate-500 font-mono mb-1 uppercase tracking-wider">1. Raw Voice ASR</div>
              <p className="text-slate-300">{lastResult.rawInput}</p>
            </div>

            {/* Stage 2: Accent Cleaned */}
            <div className="p-2 rounded bg-slate-950/80 border border-slate-800">
              <div className="text-[10px] text-teal-400 font-mono mb-1 uppercase tracking-wider">2. Accent Repaired</div>
              <p className="text-slate-200">{lastResult.accentCleaned}</p>
              {lastResult.accentReplacementsCount > 0 && (
                <span className="inline-block mt-1 text-[9px] text-teal-400 bg-teal-950/80 px-1 py-0.2 rounded font-mono">
                  {lastResult.accentReplacementsCount} terms fixed
                </span>
              )}
            </div>

            {/* Stage 3: Synthesized Directive */}
            <div className="p-2 rounded bg-teal-950/20 border border-teal-800/60">
              <div className="text-[10px] text-blue-400 font-mono mb-1 uppercase tracking-wider">3. Agent Directive</div>
              <p className="text-slate-100 whitespace-pre-wrap font-mono text-[10px] leading-relaxed max-h-24 overflow-y-auto">
                {lastResult.finalPrompt}
              </p>
            </div>
          </div>

          {/* Inspection Action Bar */}
          <div className="flex items-center justify-end gap-2 pt-1">
            <button
              onClick={() => setShowInspectDiff(false)}
              className="px-2.5 py-1 text-slate-400 hover:text-slate-200 text-xs"
            >
              Cancel
            </button>
            <button
              onClick={async () => {
                await onDispatchToIde(lastResult.finalPrompt);
                setShowInspectDiff(false);
              }}
              className="px-3 py-1 bg-teal-600 hover:bg-teal-500 text-white rounded-lg text-xs font-medium flex items-center gap-1 shadow-sm"
            >
              <Check className="w-3.5 h-3.5" />
              Approve & Paste to Window
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
