import { normalizePath } from "vite";
import { resolveTexturePath } from "../scripts/texture-config.js";

const moduleId = "virtual:textures";
const resolvedModuleId = `\0${moduleId}`;

/**
 * @param {string[]} versions
 * @returns {import("vite").Plugin}
 */
export function texturesPlugin(versions) {
  // Literal imports keep unselected JSONs out of every build environment while
  // preserving one lazy-loaded module per texture version.
  const entries = versions.map((version) => {
    const file = normalizePath(resolveTexturePath(version));
    return `${JSON.stringify(version)}: () => import(${JSON.stringify(file)})`;
  });

  return {
    name: "textures-plugin",
    resolveId(id) {
      if(id === moduleId) return resolvedModuleId;
    },
    load(id) {
      if(id === resolvedModuleId) {
        return `export const textureLoaders = {\n${entries.join(",\n")}\n};`;
      }
    },
  };
}
