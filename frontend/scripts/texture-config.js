import fs from "node:fs";
import path from "node:path";
import { createRequire } from "node:module";
import { versions } from "minecraft-textures";
import { coerce, compare } from "semver";
import { frontendDir } from "./build-config.js";

const require = createRequire(import.meta.url);
const compareVersions = (left, right) => compare(coerce(left), coerce(right));
const availableVersions = [...versions].sort(compareVersions);

export function resolveTexturePath(version) {
  try {
    const file = require.resolve(`minecraft-textures/dist/textures/json/${version}.json`);
    if(!fs.statSync(file).isFile()) throw new Error("Not a regular file");
    return file;
  } catch(error) {
    throw new Error(`Texture JSON is missing for ${version}. Reinstall frontend dependencies.`, { cause: error });
  }
}

/** @param {string[]} versions */
export function createTextureModule(versions) {
  // Literal imports preserve lazy loading and exclude unselected versions.
  const entries = versions.map((version) => {
    // Package specifiers also work on Windows, where Turbopack cannot import
    // absolute drive-letter paths in generated modules.
    const file = `minecraft-textures/dist/textures/json/${version}.json`;
    return `${JSON.stringify(version)}: () => import(${JSON.stringify(file)})`;
  });
  return `export const textureLoaders = {\n${entries.join(",\n")}\n};\n`;
}

/** @param {string[]} versions */
export function writeTextureLoaders(versions) {
  const file = path.join(frontendDir, "build/textures.js");
  const source = createTextureModule(versions);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  if(!fs.existsSync(file) || fs.readFileSync(file, "utf8") !== source) {
    fs.writeFileSync(file, source, "utf8");
  }
  return file;
}

/**
 * @param {Record<string, string | undefined>} [env]
 * @param {boolean} [production]
 * @returns {string[]}
 */
export function resolveTextureVersions(env = process.env, production = true) {
  const value = env.TEXTURE_VERSIONS?.trim();
  if(!value && production) {
    throw new Error("TEXTURE_VERSIONS is required for a production frontend build");
  }

  const selected = !value || value === "all"
    ? availableVersions
    : value.split(",").map((version) => version.trim());
  const invalid = selected.filter((version) => !availableVersions.includes(version));
  if(invalid.length) {
    throw new Error(`Invalid TEXTURE_VERSIONS entries: ${invalid.map((version) => JSON.stringify(version)).join(", ")}. Use "all" alone or a comma-separated list of texture versions: ${availableVersions.join(", ")}`);
  }

  const resolved = [...new Set(selected)].sort(compareVersions);
  for(const version of resolved) {
    resolveTexturePath(version);
  }
  return resolved;
}
