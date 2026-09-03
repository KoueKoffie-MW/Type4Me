import React, { useState } from 'react';
import { Sparkles, Mic, Layers, Cpu, LayoutTemplate, Settings, Minimize2, Check, RefreshCw, Send } from 'lucide-react';
import { PromptModifierTemplate, DEFAULT_PROMPT_TEMPLATES } from '../engine/prompt/PromptModifierEngine';
import { UserAccentProfile } from '../engine/accent/ConfusionMatrix';
import { DistilledContextSummary } from '../engine/context/TokenBudgeter';
import { PipelineExecutionResult } from '../engine/prompt/MultiPassPipeline';
import { AccentCalibrationStudio } from './AccentCalibrationStudio';
import { ContextInspector } from './ContextInspector';
import { TemplateMatrix } from './TemplateMatrix';
import { DiffSplitView } from './DiffSplitView';

interface CommandCenterProps {
  onMinimizeToHud: () => void;
  activeProfile: UserAccentProfile;
  profilesList: UserAccentProfile[];
  onSelectProfile: (profile: UserAccentProfile) => void;
  onSaveProfile: (profile: UserAccentProfile) => Promise<void>;
  contextSummary: DistilledContextSummary | null;
  activeBudget: 'lean' | 'balanced' | 'deep';
  onBudgetChange: (budget: 'lean' | 'balanced' | 'deep') => void;
  onSelectContextFile: () => Promise<void>;
  onRefreshContext: () => Promise<void>;
  targetWindow: { title: string; process: string };
  apiKey: string;
  onSaveApiKey: (key: string) => Promise<void>;
  onExecutePrompt: (text: string, template: PromptModifierTemplate) => Promise<PipelineExecutionResult>;
  onDispatchToIde: (promptText: string) => Promise<void>;
}

export const CommandCenter: React.FC<CommandCenterProps> = ({
  onMinimizeToHud,
  activeProfile,
  profilesList,
  onSelectProfile,
  onSaveProfile,
  contextSummary,
  activeBudget,
  onBudgetChange,
  onSelectContextFile,
  onRefreshContext,
  targetWindow,
  apiKey,
  onSaveApiKey,
  onExecutePrompt,
  onDispatchToIde,
}) => {
  const [activeTab, setActiveTab] = useState<'prompt' | 'accent' | 'context' | 'templates' | 'settings'>('prompt');
  const [inputText, setInputText] = useState('');
  const [selectedTemplate, setSelectedTemplate] = useState<PromptModifierTemplate>(DEFAULT_PROMPT_TEMPLATES[0]);
  const [isProcessing, setIsProcessing] = useState(false);
  const [currentResult, setCurrentResult] = useState<PipelineExecutionResult | null>(null);
  const [tempApiKey, setTempApiKey] = useState(apiKey);
  const [keySaved, setKeySaved] = useState(false);

  const handleRunRefinement = async () => {
    if (!inputText.trim()) return;
    setIsProcessing(true);
    try {
      const res = await onExecutePrompt(inputText, selectedTemplate);
      setCurrentResult(res);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleSaveKey = async () => {
    await onSaveApiKey(tempApiKey);
    setKeySaved(true);
    setTimeout(() => setKeySaved(false), 2500);
  };

  return (
    <div className="flex flex-col h-screen w-screen bg-slate-950 text-slate-100 font-sans select-none overflow-hidden">
      {/* Top Application Bar */}
      <header className="flex items-center justify-between px-5 py-3 bg-slate-900 border-b border-slate-800">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-teal-500 to-blue-600 flex items-center justify-center shadow-md shadow-teal-900/40">
            <Sparkles className="w-4 h-4 text-white" />
          </div>
          <div>
            <h1 className="text-sm font-bold tracking-tight text-slate-100 flex items-center gap-2">
              Type4Me Desktop
              <span className="text-[10px] bg-teal-950 text-teal-300 border border-teal-800 px-1.5 py-0.2 rounded font-mono font-normal">
                Windows SOTA
              </span>
            </h1>
            <span className="text-[11px] font-mono text-slate-400">
              Accent-Aware Voice Prompt Suite for Coding Agents
            </span>
          </div>
        </div>

        {/* Navigation Tabs */}
        <nav className="flex items-center gap-1 bg-slate-950/80 p-1 rounded-xl border border-slate-800">
          <button
            onClick={() => setActiveTab('prompt')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium flex items-center gap-1.5 transition-all ${
              activeTab === 'prompt' ? 'bg-teal-600 text-white shadow-sm' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Mic className="w-3.5 h-3.5" />
            Prompt Studio
          </button>
          <button
            onClick={() => setActiveTab('accent')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium flex items-center gap-1.5 transition-all ${
              activeTab === 'accent' ? 'bg-teal-600 text-white shadow-sm' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Cpu className="w-3.5 h-3.5" />
            Learn-My-Accent
          </button>
          <button
            onClick={() => setActiveTab('context')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium flex items-center gap-1.5 transition-all ${
              activeTab === 'context' ? 'bg-teal-600 text-white shadow-sm' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Layers className="w-3.5 h-3.5" />
            Context Inspector
          </button>
          <button
            onClick={() => setActiveTab('templates')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium flex items-center gap-1.5 transition-all ${
              activeTab === 'templates' ? 'bg-teal-600 text-white shadow-sm' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <LayoutTemplate className="w-3.5 h-3.5" />
            Templates
          </button>
          <button
            onClick={() => setActiveTab('settings')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium flex items-center gap-1.5 transition-all ${
              activeTab === 'settings' ? 'bg-teal-600 text-white shadow-sm' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Settings className="w-3.5 h-3.5" />
            Settings
          </button>
        </nav>

        {/* Window controls */}
        <div className="flex items-center gap-2">
          <button
            onClick={onMinimizeToHud}
            title="Minimize to Floating HUD (Ctrl+Shift+Space)"
            className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white rounded-lg text-xs font-mono flex items-center gap-1.5 border border-slate-700 transition-colors"
          >
            <Minimize2 className="w-3.5 h-3.5" />
            Floating HUD
          </button>
        </div>
      </header>

      {/* Main Tab Content */}
      <main className="flex-1 overflow-y-auto p-6">
        {activeTab === 'prompt' && (
          <div className="max-w-5xl mx-auto flex flex-col gap-5 select-text">
            {/* Input Card */}
            <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 flex flex-col gap-4 shadow-xl">
              <div className="flex items-center justify-between text-xs font-mono text-slate-400 pb-2 border-b border-slate-800">
                <span className="flex items-center gap-2">
                  <Mic className="w-4 h-4 text-teal-400 animate-pulse" />
                  <span>
                    Spoken Prompt (Press <strong className="text-teal-300">Win+H</strong> in textbox to dictate):
                  </span>
                </span>
                <span className="text-slate-400">
                  Target Window: <strong className="text-teal-400">{targetWindow.process || 'IDE'}</strong>
                </span>
              </div>

              <textarea
                value={inputText}
                onChange={(e) => setInputText(e.target.value)}
                placeholder="Speak into this textbox using Win+H or type your raw thought... (e.g. 'Hey look at that quaternion math function in the model and make sure it handles null pointers and update the gherkin test')"
                rows={4}
                className="w-full bg-slate-950 border border-slate-800 focus:border-teal-600 rounded-lg p-3 text-sm text-slate-200 placeholder-slate-600 outline-none leading-relaxed"
              />

              {/* Template & Run Row */}
              <div className="flex items-center justify-between pt-1">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-mono text-slate-500">Agent Modifier:</span>
                  <select
                    value={selectedTemplate.id}
                    onChange={(e) => {
                      const t = DEFAULT_PROMPT_TEMPLATES.find((x) => x.id === e.target.value);
                      if (t) setSelectedTemplate(t);
                    }}
                    className="bg-slate-950 border border-slate-800 text-xs font-mono text-teal-300 rounded px-2.5 py-1.5 outline-none"
                  >
                    {DEFAULT_PROMPT_TEMPLATES.map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.badge} - {t.name}
                      </option>
                    ))}
                  </select>
                </div>

                <button
                  onClick={handleRunRefinement}
                  disabled={isProcessing || !inputText.trim()}
                  className="px-4 py-2 bg-teal-600 hover:bg-teal-500 disabled:opacity-50 text-white font-medium rounded-lg text-xs flex items-center gap-2 shadow-lg shadow-teal-900/40 transition-all active:scale-95"
                >
                  {isProcessing ? (
                    <>
                      <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                      Refining Prompt...
                    </>
                  ) : (
                    <>
                      <Sparkles className="w-3.5 h-3.5" />
                      Synthesize Agent Prompt
                    </>
                  )}
                </button>
              </div>
            </div>

            {/* 3-Way Diff View */}
            <DiffSplitView
              result={currentResult}
              onCopyPrompt={() => {
                if (currentResult) navigator.clipboard.writeText(currentResult.finalPrompt);
              }}
              onDispatchToIde={() => {
                if (currentResult) onDispatchToIde(currentResult.finalPrompt);
              }}
              targetWindow={targetWindow.process}
            />
          </div>
        )}

        {activeTab === 'accent' && (
          <AccentCalibrationStudio activeProfile={activeProfile} onSaveProfile={onSaveProfile} />
        )}

        {activeTab === 'context' && (
          <ContextInspector
            contextSummary={contextSummary}
            activeBudget={activeBudget}
            onBudgetChange={onBudgetChange}
            onSelectContextFile={onSelectContextFile}
            onRefreshContext={onRefreshContext}
          />
        )}

        {activeTab === 'templates' && (
          <TemplateMatrix activeTemplate={selectedTemplate} onSelectTemplate={setSelectedTemplate} />
        )}

        {activeTab === 'settings' && (
          <div className="max-w-2xl mx-auto flex flex-col gap-6 bg-slate-900/90 border border-slate-800 rounded-xl p-6 select-text">
            <h2 className="text-base font-bold text-slate-100 flex items-center gap-2 pb-2 border-b border-slate-800">
              <Settings className="w-4 h-4 text-teal-400" />
              Settings & Model Configuration
            </h2>

            {/* Accent Profile Selection */}
            <div className="flex flex-col gap-2">
              <label className="text-xs font-mono text-slate-400">Active Speaker Accent Profile:</label>
              <select
                value={activeProfile.id}
                onChange={(e) => {
                  const p = profilesList.find((x) => x.id === e.target.value);
                  if (p) onSelectProfile(p);
                }}
                className="bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-xs font-mono text-teal-300 outline-none"
              >
                {profilesList.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name} ({p.detailedRules.length} learned rules)
                  </option>
                ))}
              </select>
            </div>

            {/* Gemini API Key */}
            <div className="flex flex-col gap-2">
              <label className="text-xs font-mono text-slate-400 flex items-center justify-between">
                <span>Gemini API Key (for Gemini 3.7 Flash & Gemini 3.5 Transcribe):</span>
                <span className="text-[11px] text-teal-400">Model: gemini-3.7-flash (GA Aug 2026)</span>
              </label>
              <input
                type="password"
                value={tempApiKey}
                onChange={(e) => setTempApiKey(e.target.value)}
                placeholder="AIzaSy..."
                className="bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-xs font-mono text-slate-200 outline-none"
              />
              <span className="text-[11px] text-slate-500">
                If omitted or offline, Type4Me falls back automatically to its local deterministic phonetic Trie engine with zero network calls.
              </span>
              <button
                onClick={handleSaveKey}
                className="self-end mt-2 px-4 py-1.5 bg-teal-600 hover:bg-teal-500 text-white rounded-lg text-xs flex items-center gap-1.5 transition-colors"
              >
                {keySaved ? <Check className="w-3.5 h-3.5" /> : null}
                {keySaved ? 'Saved!' : 'Save Key'}
              </button>
            </div>

            {/* System Hotkeys Reference */}
            <div className="bg-slate-950 p-4 rounded-lg border border-slate-800/80 flex flex-col gap-2 text-xs font-mono">
              <span className="text-slate-400 font-semibold mb-1">Registered System Hotkeys:</span>
              <div className="flex items-center justify-between text-slate-300">
                <span>Summon Floating HUD:</span>
                <span className="bg-slate-900 px-2 py-0.5 rounded border border-slate-800 text-teal-300">
                  Ctrl + Shift + Space
                </span>
              </div>
              <div className="flex items-center justify-between text-slate-300">
                <span>Quick Toggle HUD:</span>
                <span className="bg-slate-900 px-2 py-0.5 rounded border border-slate-800 text-teal-300">
                  Alt + `
                </span>
              </div>
              <div className="flex items-center justify-between text-slate-300">
                <span>Windows Voice Typing:</span>
                <span className="bg-slate-900 px-2 py-0.5 rounded border border-slate-800 text-amber-400">
                  Win + H
                </span>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Global Status Bar */}
      <footer className="px-5 py-2 bg-slate-900/90 border-t border-slate-800 flex items-center justify-between text-xs font-mono text-slate-500">
        <div className="flex items-center gap-3">
          <span className="flex items-center gap-1.5 text-teal-400">
            <span className="w-2 h-2 rounded-full bg-teal-500 animate-pulse"></span>
            Profile: {activeProfile.name.split('/')[0].trim()}
          </span>
          <span>•</span>
          <span>
            Context:{' '}
            <strong className="text-slate-300">
              {contextSummary?.filePath ? 'Watching transcript.jsonl' : 'No file'}
            </strong>
          </span>
        </div>
        <div className="flex items-center gap-2">
          <span>Engine: <strong className="text-teal-400">Gemini 3.7 Flash + Double Metaphone</strong></span>
        </div>
      </footer>
    </div>
  );
};
