const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  // Window & focus management (ADR-0004 & ADR-0006)
  getLastActiveWindow: () => ipcRenderer.invoke('get-last-active-window'),
  togglePinTarget: () => ipcRenderer.invoke('toggle-pin-target'),
  pasteToPreviousWindow: (text) => ipcRenderer.invoke('paste-to-previous-window', text),
  toggleWindowMode: () => ipcRenderer.invoke('toggle-window-mode'),
  minimizeWindow: () => ipcRenderer.invoke('minimize-window'),
  closeWindow: () => ipcRenderer.invoke('close-window'),
  
  // Context file watching & auto-discovery (ADR-0002 & ADR-0007)
  autoDiscoverContext: () => ipcRenderer.invoke('auto-discover-context'),
  startWatchingContext: (filePath) => ipcRenderer.invoke('start-watching-context', filePath),
  stopWatchingContext: () => ipcRenderer.invoke('stop-watching-context'),
  getContextSummary: (turnBudget) => ipcRenderer.invoke('get-context-summary', turnBudget),
  onContextUpdated: (callback) => {
    const handler = (event, data) => callback(data);
    ipcRenderer.on('context-updated', handler);
    return () => ipcRenderer.removeListener('context-updated', handler);
  },
  openFileDialog: () => ipcRenderer.invoke('open-file-dialog'),

  // Profiles & settings (ADR-0003 & ADR-0005)
  saveAccentProfile: (profile) => ipcRenderer.invoke('save-accent-profile', profile),
  loadAccentProfiles: () => ipcRenderer.invoke('load-accent-profiles'),
  saveApiKey: (key) => ipcRenderer.invoke('save-api-key', key),
  getApiKey: () => ipcRenderer.invoke('get-api-key'),
});
