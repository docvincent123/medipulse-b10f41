const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('quremed', {
  getAppMeta: () => ipcRenderer.invoke('app:get-meta'),
  getRegistry: () => ipcRenderer.invoke('registry:get'),
  setRegistry: (registry) => ipcRenderer.invoke('registry:set', registry),
  openExternal: (url) => ipcRenderer.invoke('shell:open-external', url),
  exportDiagnostics: (payload) => ipcRenderer.invoke('support:export-diagnostics', payload),
  chooseExecutable: () => ipcRenderer.invoke('products:choose-executable'),
  launchExecutable: (filePath) => ipcRenderer.invoke('products:launch-executable', filePath),
});
