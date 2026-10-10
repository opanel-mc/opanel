import { createTextureModule } from "../scripts/texture-config.js";

const moduleId = "opanel-textures";
const resolvedModuleId = `\0${moduleId}`;

/**
 * @param {string[]} versions
 * @returns {import("vite").Plugin}
 */
export function texturesPlugin(versions) {
  // Vitest uses the same module source as Next.js.
  const source = createTextureModule(versions);

  return {
    name: "textures-plugin",
    resolveId(id) {
      if(id === moduleId) return resolvedModuleId;
    },
    load(id) {
      if(id === resolvedModuleId) {
        return source;
      }
    },
  };
}
