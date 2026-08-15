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

interface ImportMeta {
  glob<T>(
    patterns: string | string[],
    options?: { import?: string; eager?: boolean }
  ): Record<string, () => Promise<T>>;
}

declare module '*.css' {
  const content = {};
  export default content;
}
