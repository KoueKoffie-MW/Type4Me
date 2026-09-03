import React, { useState } from 'react';
import { LayoutTemplate, Sparkles, Code2, Plus, Check } from 'lucide-react';
import { PromptModifierTemplate, DEFAULT_PROMPT_TEMPLATES } from '../engine/prompt/PromptModifierEngine';

interface TemplateMatrixProps {
  activeTemplate: PromptModifierTemplate;
  onSelectTemplate: (template: PromptModifierTemplate) => void;
}

export const TemplateMatrix: React.FC<TemplateMatrixProps> = ({
  activeTemplate,
  onSelectTemplate,
}) => {
  const [sampleVoice, setSampleVoice] = useState(
    'Hey look at that quaternion math function in the model and make sure it handles null pointers and update the gherkin test'
  );

  return (
    <div className="flex flex-col gap-6 max-w-5xl mx-auto p-4 select-text">
      {/* Header */}
      <div className="flex items-start justify-between bg-slate-900/90 border border-teal-800/40 rounded-xl p-5 shadow-lg">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <LayoutTemplate className="w-5 h-5 text-teal-400" />
            <h2 className="text-lg font-bold text-slate-100">Agentic Prompt Modifier Matrix</h2>
          </div>
          <p className="text-xs text-slate-400 max-w-2xl leading-relaxed">
            Autonomous coding agents need structured instructions rather than raw conversational speech. Choose or customize prompt modifiers that wrap your voice-dictated ideas into high-yield directives.
          </p>
        </div>
      </div>

      {/* Templates Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {DEFAULT_PROMPT_TEMPLATES.map((tpl) => {
          const isSelected = activeTemplate.id === tpl.id;
          return (
            <div
              key={tpl.id}
              onClick={() => onSelectTemplate(tpl)}
              className={`p-4 rounded-xl border cursor-pointer transition-all flex flex-col justify-between ${
                isSelected
                  ? 'bg-slate-900 border-teal-500 shadow-lg shadow-teal-950/60 ring-1 ring-teal-500'
                  : 'bg-slate-900/70 border-slate-800 hover:border-slate-700 hover:bg-slate-900'
              }`}
            >
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-mono font-bold bg-teal-950 text-teal-300 border border-teal-800 px-2 py-0.5 rounded">
                    {tpl.badge}
                  </span>
                  <span className="text-xs font-mono text-slate-500">Shortcut: Alt+{tpl.shortcut}</span>
                </div>
                <h3 className="text-sm font-semibold text-slate-100 mb-1">{tpl.name}</h3>
                <p className="text-xs text-slate-400 leading-relaxed mb-3">{tpl.description}</p>
              </div>

              <div className="pt-2 border-t border-slate-800/60 flex items-center justify-between text-xs">
                <span className="text-[11px] font-mono text-slate-500">Agent Directive</span>
                {isSelected ? (
                  <span className="text-teal-400 font-semibold flex items-center gap-1">
                    <Check className="w-3.5 h-3.5" /> Active
                  </span>
                ) : (
                  <span className="text-slate-500">Click to Select</span>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Live Template System Instruction Inspector */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 flex flex-col gap-3">
        <div className="flex items-center justify-between text-xs font-mono text-slate-400 pb-2 border-b border-slate-800">
          <span className="flex items-center gap-2">
            <Code2 className="w-4 h-4 text-teal-400" />
            <strong className="text-slate-200">System Instruction: {activeTemplate.name}</strong>
          </span>
          <span className="text-[11px] text-teal-400">Target Model: Gemini 3.7 Flash</span>
        </div>

        <pre className="p-4 bg-slate-950 rounded-lg border border-slate-800 text-xs font-mono text-slate-300 whitespace-pre-wrap leading-relaxed">
          {activeTemplate.systemInstruction}
        </pre>
      </div>
    </div>
  );
};
