declare module 'opanel-textures' {
  import type { Item } from 'minecraft-textures';
  export const textureLoaders: Record<string, () => Promise<{ items: Item[] }>>;
}

declare module '*.css' {
  const content = {};
  export default content;
}
