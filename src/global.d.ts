export {};

declare module '*.css';

declare global {
  interface InstalledProduct {
    version: string;
    installedAt?: string;
    executablePath?: string;
  }

  interface Window {
    quremed?: {
      getAppMeta: () => Promise<{ version: string; platform: string; arch: string; packaged: boolean }>;
      getRegistry: () => Promise<Record<string, InstalledProduct>>;
      setRegistry: (registry: Record<string, InstalledProduct>) => Promise<boolean>;
      openExternal: (url: string) => Promise<boolean>;
      exportDiagnostics: (payload: unknown) => Promise<{ saved: boolean; filePath?: string }>;
      chooseExecutable: () => Promise<{ canceled: boolean; filePath?: string }>;
      launchExecutable: (filePath: string) => Promise<{ ok: boolean; error?: string }>;
    };
  }
}
