// @vitest-environment node
import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { versions } from "minecraft-textures";
import { coerce, compare } from "semver";
import { build, normalizePath } from "vite";
import { afterEach, describe, expect, it, vi } from "vitest";
import { frontendDir } from "../scripts/build-config.js";
import { texturesPlugin } from "./textures-plugin.js";
import { resolveTexturePath, resolveTextureVersions } from "../scripts/texture-config.js";

const repositoryDir = fileURLToPath(new URL("../../", import.meta.url));
afterEach(() => vi.restoreAllMocks());

describe("texture configuration", () => {
  it("accepts one version and normalizes a list in semantic version order", () => {
    expect(resolveTextureVersions({ TEXTURE_VERSIONS: "26.3" })).toEqual(["26.3"]);
    expect(resolveTextureVersions({ TEXTURE_VERSIONS: " 1.21.11, 1.21.2,1.21, 1.21.2 " }))
      .toEqual(["1.21", "1.21.2", "1.21.11"]);
  });

  it("includes every catalog version for all and defaults to all in development", () => {
    const all = resolveTextureVersions({ TEXTURE_VERSIONS: "all" });
    expect(new Set(all)).toEqual(new Set(versions));
    expect(all).toContain("1.12");
    expect(resolveTextureVersions({}, false)).toEqual(all);
    expect(resolveTextureVersions({ TEXTURE_VERSIONS: " " }, false)).toEqual(all);
  });

  it.each([undefined, "", " "])("requires an explicit production selection: %s", (value) => {
    expect(() => resolveTextureVersions({ TEXTURE_VERSIONS: value })).toThrow(/TEXTURE_VERSIONS is required/);
  });

  it.each(["1.20.2", "1.21.id", "all,26.3", "26.3,", ",26.3", "26.3,,26.1", ">=1.21", "../26.3", "unknown"])(
    "rejects invalid configuration %s in both modes", (value) => {
      for(const production of [true, false]) {
        expect(() => resolveTextureVersions({ TEXTURE_VERSIONS: value }, production)).toThrow(/Invalid TEXTURE_VERSIONS/);
      }
    },
  );

  it("fails if a selected texture file is missing", () => {
    vi.spyOn(fs, "statSync").mockImplementationOnce(() => {
      throw new Error("ENOENT");
    });
    expect(() => resolveTextureVersions({ TEXTURE_VERSIONS: "26.3" })).toThrow(/Texture JSON is missing for 26.3/);
  });

  it("rejects a missing production selection before running prelaunch", () => {
    const result = spawnSync(process.execPath, ["scripts/build.js"], {
      cwd: frontendDir,
      encoding: "utf8",
      env: { ...process.env, VITE_OPANEL_TARGET: "paper-26.1", TEXTURE_VERSIONS: "" },
      timeout: 15_000,
    });
    expect(result.error).toBeUndefined();
    expect(result.status).toBe(1);
    expect(result.stderr).toContain("TEXTURE_VERSIONS is required");
  });
});

function javaTextures(platform: string, target: string) {
  const source = fs.readFileSync(path.join(repositoryDir, platform, target, "gradle.properties"), "utf8");
  const value = source.match(/^frontend_env_texture_versions=(.*)$/m)?.[1].trim();
  expect(value, target).toBeTruthy();
  expect(value, target).not.toBe("all");
  return resolveTextureVersions({ TEXTURE_VERSIONS: value });
}

describe("platform texture selections", () => {
  it("declares valid lists for every Java target and keeps helpers unconfigured", () => {
    const registry: Record<string, Record<string, string[]>> = JSON.parse(
      fs.readFileSync(path.join(repositoryDir, "platform-modules.json"), "utf8"),
    );
    for(const [platform, targets] of Object.entries(registry)) {
      for(const target of Object.keys(targets)) javaTextures(platform, target);
      for(const helper of new Set(Object.values(targets).flat())) {
        const file = path.join(repositoryDir, platform, helper, "gradle.properties");
        if(fs.existsSync(file)) expect(fs.readFileSync(file, "utf8")).not.toMatch(/^frontend_env_texture_versions=/m);
      }
    }
  });

  it.each([
    ["paper", "paper-1.21", ["1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8"]],
    ["paper", "paper-1.21.9", ["1.21.9", "1.21.10", "1.21.11"]],
    ["paper", "folia-1.21", ["1.21.4", "1.21.5", "1.21.6"]],
    ["neoforge", "neoforge-1.21.1", ["1.21.1", "1.21.2"]],
    ["fabric", "fabric-26.1", ["26.1", "26.1.1", "26.1.2", "26.2", "26.3"]],
    ["forge", "forge-1.21.8", ["1.21.8"]],
  ] as const)("covers supported Minecraft versions of %s/%s", (platform, target, supported) => {
    const selected = javaTextures(platform, target);
    const required = supported.map((mcVersion) => versions.filter((textureVersion) => (
      compare(coerce(textureVersion)!, coerce(mcVersion)!) <= 0
    )).at(-1));
    expect(new Set(selected)).toEqual(new Set(required));
  });

  it("selects only 26.3 for Pumpkin", () => {
    const source = fs.readFileSync(path.join(repositoryDir, "pumpkin/frontend.properties"), "utf8");
    const value = source.match(/^TEXTURE_VERSIONS=(.*)$/m)?.[1].trim();
    expect(resolveTextureVersions({ TEXTURE_VERSIONS: value })).toEqual(["26.3"]);
  });
});

describe("inventory texture bundling", () => {
  it.each(["26.3", "1.21.2,1.21.4", "all"])("bundles only %s and keeps textures lazy", async (value) => {
    const selected = resolveTextureVersions({ TEXTURE_VERSIONS: value });
    const result = await build({
      root: frontendDir,
      configFile: false,
      envFile: false,
      publicDir: false,
      logLevel: "silent",
      plugins: [texturesPlugin(selected), {
        name: "texture-test-entry",
        resolveId(id) {
          if(id === "virtual:texture-test") return `\0${id}`;
        },
        load(id) {
          if(id === "\0virtual:texture-test") {
            return 'import { textureLoaders } from "opanel-textures"; globalThis.textureTest = textureLoaders;';
          }
        },
      }],
      build: { write: false, minify: false, rolldownOptions: { input: "virtual:texture-test" } },
    });
    if(Array.isArray(result) || !("output" in result)) throw new Error("Expected one in-memory bundle");
    const chunks = result.output.filter((output) => output.type === "chunk");
    const isTexture = (id: string) => id.includes("/minecraft-textures/dist/textures/json/");
    const bundledTextures = chunks.flatMap((chunk) => Object.keys(chunk.modules)).filter(isTexture);
    expect(new Set(bundledTextures)).toEqual(new Set(selected.map((version) => normalizePath(resolveTexturePath(version)))));
    expect(chunks.filter((chunk) => chunk.isDynamicEntry)).toHaveLength(selected.length);
    expect(chunks.filter((chunk) => chunk.isEntry).flatMap((chunk) => Object.keys(chunk.modules)).some(isTexture)).toBe(false);
  }, 30_000);
});
