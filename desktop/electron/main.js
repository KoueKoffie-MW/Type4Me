const { app, BrowserWindow, globalShortcut, ipcMain, dialog, clipboard, Tray, Menu } = require('electron');
const path = require('path');
const fs = require('fs');
const win32Helper = require('./win32-helper');
const contextWatcher = require('./context-watcher');

let mainWindow = null;
let tray = null;
let currentMode = 'hud'; // 'hud' or 'commandCenter'

// Config paths
const userDataPath = app.getPath('userData');
const profilesPath = path.join(userDataPath, 'accent-profiles.json');
const configPath = path.join(userDataPath, 'config.json');

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 720,
    height: 300,
    frame: false,
    transparent: true,
    alwaysOnTop: true,
    skipTaskbar: false,
    resizable: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true,
    },
  });

  const isDev = !app.isPackaged && process.env.NODE_ENV !== 'production';
  if (isDev) {
    mainWindow.loadURL('http://localhost:5173').catch(() => {
      // If dev server not yet ready, load built dist or wait
      mainWindow.loadFile(path.join(__dirname, '../dist/index.html'));
    });
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'));
  }

  mainWindow.on('blur', () => {
    // Optional: auto-hide HUD on blur if configured
  });
}

function setWindowMode(mode) {
  if (!mainWindow) return;
  currentMode = mode;
  if (mode === 'hud') {
    mainWindow.setAlwaysOnTop(true, 'screen-saver');
    mainWindow.setSize(720, 320);
    mainWindow.setResizable(false);
  } else {
    mainWindow.setAlwaysOnTop(false);
    mainWindow.setSize(1100, 780);
    mainWindow.setResizable(true);
    mainWindow.center();
  }
}

async function summonHud() {
  if (!mainWindow) return;
  
  // 1. Capture the currently active IDE or terminal before stealing focus
  await win32Helper.captureForegroundWindow();

  // 2. Show & bring HUD to front
  setWindowMode('hud');
  mainWindow.show();
  mainWindow.focus();

  // 3. Inform renderer to set immediate caret focus so Win+H works effortlessly
  mainWindow.webContents.send('hud-summoned');
}

app.whenReady().then(() => {
  createWindow();

  // Register Global Shortcuts (Ctrl+Shift+Space and Alt+`)
  try {
    globalShortcut.register('CommandOrControl+Shift+Space', () => {
      summonHud();
    });
    globalShortcut.register('Alt+`', () => {
      summonHud();
    });
  } catch (err) {
    console.warn('Could not register global shortcuts:', err.message);
  }

  // Create Tray Icon
  try {
    const contextMenu = Menu.buildFromTemplate([
      { label: 'Summon Prompt HUD (Ctrl+Shift+Space)', click: () => summonHud() },
      { label: 'Open Command Center', click: () => { setWindowMode('commandCenter'); mainWindow.show(); } },
      { type: 'separator' },
      { label: 'Quit Type4Me Desktop', click: () => app.quit() },
    ]);
    // Create simple tray if icon available or fallback
  } catch (e) {
    console.warn('Tray setup note:', e.message);
  }

  // Context watcher event forwarding
  contextWatcher.on('context-updated', (summary) => {
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('context-updated', summary);
    }
  });

  // Automatically discover and attach to active agent transcript (ADR-0007)
  contextWatcher.autoDiscoverActiveTranscript();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('will-quit', () => {
  globalShortcut.unregisterAll();
  contextWatcher.stopWatching();
});

// -------------------------------------------------------------
// IPC Handlers
// -------------------------------------------------------------

ipcMain.handle('get-last-active-window', async () => {
  return {
    hwnd: win32Helper.isPinned ? win32Helper.pinnedHwnd : win32Helper.lastForegroundHwnd,
    title: win32Helper.isPinned ? win32Helper.pinnedTitle : win32Helper.lastWindowTitle,
    process: win32Helper.isPinned ? win32Helper.pinnedProcess : win32Helper.lastProcessName,
    isPinned: win32Helper.isPinned,
  };
});

ipcMain.handle('toggle-pin-target', () => {
  return win32Helper.togglePin();
});

ipcMain.handle('auto-discover-context', async () => {
  return await contextWatcher.autoDiscoverActiveTranscript();
});

ipcMain.handle('paste-to-previous-window', async (event, text) => {
  if (text) {
    clipboard.writeText(text);
  }
  if (mainWindow && currentMode === 'hud') {
    mainWindow.hide();
  }
  return await win32Helper.pasteToPreviousWindow(text);
});

ipcMain.handle('toggle-window-mode', () => {
  const newMode = currentMode === 'hud' ? 'commandCenter' : 'hud';
  setWindowMode(newMode);
  return newMode;
});

ipcMain.handle('minimize-window', () => {
  if (mainWindow) mainWindow.minimize();
});

ipcMain.handle('close-window', () => {
  if (mainWindow) {
    if (currentMode === 'hud') {
      mainWindow.hide();
    } else {
      mainWindow.close();
    }
  }
});

ipcMain.handle('start-watching-context', (event, filePath) => {
  return contextWatcher.startWatching(filePath);
});

ipcMain.handle('stop-watching-context', () => {
  contextWatcher.stopWatching();
  return true;
});

ipcMain.handle('get-context-summary', (event, turnBudget) => {
  return contextWatcher.getDistilledSummary(turnBudget);
});

ipcMain.handle('open-file-dialog', async () => {
  const result = await dialog.showOpenDialog(mainWindow, {
    properties: ['openFile'],
    filters: [
      { name: 'Context Files', extensions: ['jsonl', 'md', 'txt', 'log', 'diff'] },
      { name: 'All Files', extensions: ['*'] }
    ]
  });
  if (!result.canceled && result.filePaths.length > 0) {
    return result.filePaths[0];
  }
  return null;
});

// Profile persistence
ipcMain.handle('save-accent-profile', async (event, profile) => {
  try {
    let profiles = [];
    if (fs.existsSync(profilesPath)) {
      profiles = JSON.parse(await fs.promises.readFile(profilesPath, 'utf8'));
    }
    const idx = profiles.findIndex(p => p.id === profile.id);
    if (idx >= 0) {
      profiles[idx] = profile;
    } else {
      profiles.push(profile);
    }
    await fs.promises.writeFile(profilesPath, JSON.stringify(profiles, null, 2), 'utf8');
    return { success: true };
  } catch (err) {
    return { success: false, error: err.message };
  }
});

ipcMain.handle('load-accent-profiles', async () => {
  try {
    if (fs.existsSync(profilesPath)) {
      return JSON.parse(await fs.promises.readFile(profilesPath, 'utf8'));
    }
  } catch (err) {
    console.warn('Error loading accent profiles:', err.message);
  }
  return [];
});

ipcMain.handle('save-api-key', async (event, key) => {
  try {
    let cfg = {};
    if (fs.existsSync(configPath)) {
      cfg = JSON.parse(await fs.promises.readFile(configPath, 'utf8'));
    }
    cfg.geminiApiKey = key;
    await fs.promises.writeFile(configPath, JSON.stringify(cfg, null, 2), 'utf8');
    return true;
  } catch (e) {
    return false;
  }
});

ipcMain.handle('get-api-key', async () => {
  try {
    if (fs.existsSync(configPath)) {
      const cfg = JSON.parse(await fs.promises.readFile(configPath, 'utf8'));
      return cfg.geminiApiKey || process.env.GEMINI_API_KEY || '';
    }
  } catch (e) {}
  return process.env.GEMINI_API_KEY || '';
});
