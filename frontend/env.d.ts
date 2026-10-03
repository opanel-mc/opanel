declare module '*?worker' {
  const WorkerConstructor: {
    new(options?: WorkerOptions): Worker;
  };
  export default WorkerConstructor;
}

declare module '*?url' {
  const url: string;
  export default url;
}

interface ImportMetaEnv {
  readonly VITE_OPANEL_VERSION: string;
  readonly VITE_OPANEL_TARGET: string;
}

interface Window {
  __OPANEL_BUILD_INFO__: Readonly<{ target: string }>;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
  glob<T>(
    patterns: string | string[],
    options?: { import?: string; eager?: boolean }
  ): Record<string, () => Promise<T>>;
}

declare module '*.css' {
  const content = {};
  export default content;
}
