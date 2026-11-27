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

declare module '*?raw' {
  const content: string;
  export default content;
}

declare module 'virtual:textures' {
  import type { Item } from 'minecraft-textures';
  export const textureLoaders: Record<string, () => Promise<{ items: Item[] }>>;
}

interface ImportMetaEnv {
  readonly VITE_OPANEL_VERSION: string;
  readonly VITE_OPANEL_TARGET: string;
  readonly VITE_BULLETIN_BASE_URL?: string;
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
