import React, { useState, useEffect, useRef } from 'react';
import { FloatingHud } from './components/FloatingHud';
import { CommandCenter } from './components/CommandCenter';
import { DEFAULT_ACCENT_PROFILES } from './engine/accent/AccentProfiles';
import { UserAccentProfile } from './engine/accent/ConfusionMatrix';
import { DistilledContextSummary } from './engine/context/TokenBudgeter';
import { MultiPassPipeline, PipelineExecutionResult } from './engine/prompt/MultiPassPipeline';
import { PromptModifierTemplate } from './engine/prompt/PromptModifierEngine';

// Declare Electron API on window
declare global {
  interface Window {
    electronAPI?: {
      getLastActiveWindow: () => Promise<{ hwnd: number; title: string; process: string }>;
      pasteToPreviousWindow: (text: string) => Promise<boolean>;
      toggleWindowMode: () => Promise<string>;
      minimizeWindow: () => Promise<void>;
      closeWindow: () => Promise<void>;
      startWatchingContext: (filePath: string) => Promise<{ success: boolean; error?: string }>;
      stopWatchingContext: () => Promise<boolean>;
      getContextSummary: (turnBudget?: number) => Promise<DistilledContextSummary>;
      onContextUpdated: (callback: (data: DistilledContextSummary) => void) => () => void;
      openFileDialog: () => Promise<string | null>;
      togglePinTarget: () => Promise<{ isPinned: boolean; hwnd: number; title: string; process: string }>;
      autoDiscoverContext: () => Promise<string | null>;
      saveAccentProfile: (profile: UserAccentProfile) => Promise<{ success: boolean }>;
      loadAccentProfiles: () => Promise<UserAccentProfile[]>;
      saveApiKey: (key: string) => Promise<boolean>;
      getApiKey: () => Promise<string>;
    };
  }
}

export const App: React.FC = () => {
  const [windowMode, setWindowMode] = useState<'hud' | 'commandCenter'>('hud');
  const [profiles, setProfiles] = useState<UserAccentProfile[]>(DEFAULT_ACCENT_PROFILES);
  const [activeProfile, setActiveProfile] = useState<UserAccentProfile>(DEFAULT_ACCENT_PROFILES[0]);
  const [contextSummary, setContextSummary] = useState<DistilledContextSummary | null>(null);
  const [activeBudget, setActiveBudget] = useState<'lean' | 'balanced' | 'deep'>('balanced');
  const [targetWindow, setTargetWindow] = useState<{ title: string; process: string; isPinned?: boolean }>({
    title: '',
    process: 'Antigravity IDE',
    isPinned: false,
  });
  const [apiKey, setApiKey] = useState<string>('');

  const pipelineRef = useRef<MultiPassPipeline>(new MultiPassPipeline(DEFAULT_ACCENT_PROFILES[0]));

  useEffect(() => {
    // 1. Load saved profiles & API key if available
    if (window.electronAPI) {
      window.electronAPI.loadAccentProfiles().then((saved) => {
        if (saved && saved.length > 0) {
          setProfiles(saved);
          setActiveProfile(saved[0]);
          pipelineRef.current.updateProfile(saved[0]);
        }
      });

      window.electronAPI.getApiKey().then((k) => {
        if (k) setApiKey(k);
      });

      // 2. Listen to context updates
      const unsub = window.electronAPI.onContextUpdated((summary) => {
        setContextSummary(summary);
      });

      // 3. Poll active window every 2s
      const pollWindow = async () => {
        try {
          const win = await window.electronAPI!.getLastActiveWindow();
          if (win && (win.process || win.title)) {
            setTargetWindow({
              title: win.title,
              process: win.process,
              isPinned: (win as any).isPinned || false,
            });
          }
        } catch (e) {}
      };
      const interval = setInterval(pollWindow, 2000);
      pollWindow();

      return () => {
        unsub();
        clearInterval(interval);
      };
    }
  }, []);

  const handleTogglePin = async () => {
    if (window.electronAPI?.togglePinTarget) {
      try {
        const res = await window.electronAPI.togglePinTarget();
        setTargetWindow({
          title: res.title,
          process: res.process,
          isPinned: res.isPinned,
        });
      } catch (err) {
        console.warn('Could not toggle window pin:', err);
      }
    }
  };

  const handleProfileChange = (profile: UserAccentProfile) => {
    setActiveProfile(profile);
    pipelineRef.current.updateProfile(profile);
  };

  const handleSaveProfile = async (profile: UserAccentProfile) => {
    handleProfileChange(profile);
    const updatedList = profiles.map((p) => (p.id === profile.id ? profile : p));
    if (!updatedList.some((p) => p.id === profile.id)) {
      updatedList.push(profile);
    }
    setProfiles(updatedList);
    if (window.electronAPI) {
      await window.electronAPI.saveAccentProfile(profile);
    }
  };

  const handleSaveApiKey = async (key: string) => {
    setApiKey(key);
    if (window.electronAPI) {
      await window.electronAPI.saveApiKey(key);
    }
  };

  const handleSelectContextFile = async () => {
    if (window.electronAPI) {
      const file = await window.electronAPI.openFileDialog();
      if (file) {
        await window.electronAPI.startWatchingContext(file);
        const summary = await window.electronAPI.getContextSummary(4);
        setContextSummary(summary);
      }
    }
  };

  const handleRefreshContext = async () => {
    if (window.electronAPI && contextSummary?.filePath) {
      const summary = await window.electronAPI.getContextSummary(4);
      setContextSummary(summary);
    }
  };

  const handleExecutePrompt = async (
    rawText: string,
    template: PromptModifierTemplate
  ): Promise<PipelineExecutionResult> => {
    return await pipelineRef.current.execute(
      rawText,
      template,
      activeProfile,
      contextSummary,
      activeBudget,
      apiKey
    );
  };

  const handleDispatchToIde = async (promptText: string): Promise<void> => {
    if (window.electronAPI) {
      await window.electronAPI.pasteToPreviousWindow(promptText);
    } else {
      await navigator.clipboard.writeText(promptText);
    }
  };

  const toggleMode = async () => {
    if (window.electronAPI) {
      const newMode = (await window.electronAPI.toggleWindowMode()) as 'hud' | 'commandCenter';
      setWindowMode(newMode);
    } else {
      setWindowMode((prev) => (prev === 'hud' ? 'commandCenter' : 'hud'));
    }
  };

  return (
    <div className="w-full h-full">
      {windowMode === 'hud' ? (
        <div className="p-2">
          <FloatingHud
            onExpandCommandCenter={toggleMode}
            onClose={() => {
              if (window.electronAPI) window.electronAPI.closeWindow();
            }}
            onExecutePrompt={handleExecutePrompt}
            onDispatchToIde={handleDispatchToIde}
            targetWindow={targetWindow}
            contextSummary={contextSummary}
            activeProfile={activeProfile}
            onTogglePin={handleTogglePin}
          />
        </div>
      ) : (
        <CommandCenter
          onMinimizeToHud={toggleMode}
          activeProfile={activeProfile}
          profilesList={profiles}
          onSelectProfile={handleProfileChange}
          onSaveProfile={handleSaveProfile}
          contextSummary={contextSummary}
          activeBudget={activeBudget}
          onBudgetChange={setActiveBudget}
          onSelectContextFile={handleSelectContextFile}
          onRefreshContext={handleRefreshContext}
          targetWindow={targetWindow}
          apiKey={apiKey}
          onSaveApiKey={handleSaveApiKey}
          onExecutePrompt={handleExecutePrompt}
          onDispatchToIde={handleDispatchToIde}
        />
      )}
    </div>
  );
};
