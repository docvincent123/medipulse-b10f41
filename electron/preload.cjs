const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('quremed', {
  getAppMeta: () => ipcRenderer.invoke('app:get-meta'),
  openExternal: (url) => ipcRenderer.invoke('shell:open-external', url),
});
