const { app, BrowserWindow, dialog, ipcMain, shell } = require('electron');
const fs = require('fs');
const path = require('path');

const isDev = !app.isPackaged;

function registryPath() {
  return path.join(app.getPath('userData'), 'product-registry.json');
}

function readRegistry() {
  try {
    const raw = fs.readFileSync(registryPath(), 'utf8');
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
}

function writeRegistry(registry) {
  const file = registryPath();
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(registry ?? {}, null, 2), 'utf8');
}

function createWindow() {
  const win = new BrowserWindow({
    width: 1460,
    height: 920,
    minWidth: 1120,
    minHeight: 720,
    backgroundColor: '#061017',
    title: 'QureMED Studio',
    titleBarStyle: 'hidden',
    titleBarOverlay: {
      color: '#061017',
      symbolColor: '#dff7f1',
      height: 44,
    },
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
    },
  });

  if (isDev) win.loadURL('http://127.0.0.1:5173');
  else win.loadFile(path.join(__dirname, '..', 'dist', 'index.html'));

  win.webContents.setWindowOpenHandler(({ url }) => {
    if (/^(https:\/\/|mailto:)/i.test(url)) shell.openExternal(url);
    return { action: 'deny' };
  });
}

ipcMain.handle('app:get-meta', () => ({
  version: app.getVersion(),
  platform: process.platform,
  arch: process.arch,
  packaged: app.isPackaged,
}));

ipcMain.handle('registry:get', () => readRegistry());

ipcMain.handle('registry:set', (_event, registry) => {
  if (!registry || typeof registry !== 'object' || Array.isArray(registry)) throw new Error('Invalid registry payload');
  writeRegistry(registry);
  return true;
});

ipcMain.handle('shell:open-external', async (_event, url) => {
  if (typeof url !== 'string' || !/^(https:\/\/|mailto:)/i.test(url)) throw new Error('Only HTTPS and mailto URLs are allowed');
  await shell.openExternal(url);
  return true;
});

ipcMain.handle('products:choose-executable', async () => {
  const result = await dialog.showOpenDialog({
    title: 'Choose installed QureMED application',
    properties: ['openFile'],
    filters: [{ name: 'Windows applications', extensions: ['exe'] }],
  });
  if (result.canceled || !result.filePaths[0]) return { canceled: true };
  return { canceled: false, filePath: result.filePaths[0] };
});

ipcMain.handle('products:launch-executable', async (_event, filePath) => {
  if (typeof filePath !== 'string' || !filePath.toLowerCase().endsWith('.exe')) return { ok: false, error: 'Invalid executable path' };
  if (!fs.existsSync(filePath)) return { ok: false, error: 'Executable not found' };
  const error = await shell.openPath(filePath);
  return error ? { ok: false, error } : { ok: true };
});

ipcMain.handle('support:export-diagnostics', async (_event, payload) => {
  const result = await dialog.showSaveDialog({
    title: 'Save QureMED Studio diagnostics',
    defaultPath: `QureMED-Studio-Diagnostics-${new Date().toISOString().slice(0, 10)}.json`,
    filters: [{ name: 'JSON', extensions: ['json'] }],
  });
  if (result.canceled || !result.filePath) return { saved: false };
  const safePayload = payload && typeof payload === 'object' ? payload : {};
  fs.writeFileSync(result.filePath, JSON.stringify(safePayload, null, 2), 'utf8');
  return { saved: true, filePath: result.filePath };
});

app.whenReady().then(() => {
  createWindow();
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
