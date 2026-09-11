export {};

declare global {
  interface Window {
    quremed?: {
      getAppMeta: () => Promise<{
        version: string;
        platform: string;
        arch: string;
        packaged: boolean;
      }>;
      openExternal: (url: string) => Promise<boolean>;
    };
  }
}
