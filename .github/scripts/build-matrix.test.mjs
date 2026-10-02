import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";
import { createBuildMatrix } from "./build-matrix.mjs";

const root = fileURLToPath(new URL("../../", import.meta.url));
const registry = JSON.parse(fs.readFileSync(path.join(root, "platform-modules.json"), "utf8"));

test("matrix covers every version module with unique final artifact names", () => {
  const { matrix, version } = createBuildMatrix();
  const onDisk = Object.keys(registry).flatMap((platform) => fs.readdirSync(path.join(root, platform))
    .filter((name) => /-\d/.test(name)));
  assert.deepEqual(matrix.include.map((entry) => entry.target).sort(), onDisk.sort());
  assert.equal(new Set(matrix.include.map((entry) => entry.artifact)).size, onDisk.length);
  for(const entry of matrix.include) {
    assert.equal(entry.artifact, `opanel-${entry.target}-build-${version}`);
  }
});

test("each selected target includes all referenced helpers and declares its frontend target", () => {
  for(const [platform, targets] of Object.entries(registry)) {
    for(const [target, helpers] of Object.entries(targets)) {
      const included = new Set([target, ...helpers, "core", "api"]);
      for(const module of [target, ...helpers]) {
        const source = fs.readFileSync(path.join(root, platform, module, "build.gradle"), "utf8");
        for(const [, dependency] of source.matchAll(/project\(["']:([^"']+)["']\)/g)) {
          assert.ok(included.has(dependency), `${target} is missing ${dependency}`);
        }
      }
      const properties = fs.readFileSync(path.join(root, platform, target, "gradle.properties"), "utf8");
      assert.equal(properties.match(/^frontend_env_vite_opanel_target=(.+)$/m)?.[1].trim(), target);
    }
  }
});
