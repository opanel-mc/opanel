import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";

const script = fileURLToPath(new URL("./prepare-cos-matrix.mjs", import.meta.url));

function prepareMatrix(t, {
  version = "2.3.0",
  tag = "v2.3.0",
  packageName = "opanel-pumpkin",
  assets = ["opanel-paper-26.1-build-2.3.0.jar", "opanel-fabric-26.1-build-2.3.0.jar"],
} = {}) {
  const tempRoot = path.resolve(tmpdir());
  const directory = fs.mkdtempSync(path.join(tempRoot, "opanel-cos-matrix-"));
  t.after(() => {
    assert.equal(path.dirname(path.resolve(directory)), tempRoot);
    assert(path.basename(directory).startsWith("opanel-cos-matrix-"));
    fs.rmSync(directory, { recursive: true, force: true });
  });
  const assetsDir = path.join(directory, "assets");
  const metadataFile = path.join(directory, "cargo-metadata.json");
  const outputFile = path.join(directory, "output");
  fs.mkdirSync(assetsDir);
  for(const asset of assets) fs.writeFileSync(path.join(assetsDir, asset), "fixture");
  fs.writeFileSync(metadataFile, JSON.stringify({ packages: [
    { name: "opanel-pumpkin-assets", version: "0.1.0" },
    { name: packageName, version },
  ] }));
  const result = spawnSync(process.execPath, [script], {
    encoding: "utf8",
    env: {
      ...process.env,
      RELEASE_ASSETS_DIR: assetsDir,
      RELEASE_REPOSITORY: "opanel-mc/opanel",
      RELEASE_TAG: tag,
      RELEASE_CARGO_METADATA: metadataFile,
      GITHUB_OUTPUT: outputFile,
    },
  });
  const outputs = fs.existsSync(outputFile) ? Object.fromEntries(
    fs.readFileSync(outputFile, "utf8").trim().split("\n").map((line) => {
      const separator = line.indexOf("=");
      return [line.slice(0, separator), line.slice(separator + 1)];
    }),
  ) : {};
  return { ...result, outputs };
}

test("combines release JARs with five correctly named Pumpkin build artifacts", (t) => {
  const result = prepareMatrix(t);
  assert.equal(result.status, 0, result.stderr);
  assert.equal(result.outputs.opanel_version, "2.3.0");
  const builds = JSON.parse(result.outputs.pumpkin_matrix).include;
  const uploads = JSON.parse(result.outputs.matrix).include;
  const expected = [
    ["ubuntu-24.04", "x86_64-unknown-linux-gnu", "libopanel_pumpkin.so", "so"],
    ["ubuntu-24.04-arm", "aarch64-unknown-linux-gnu", "libopanel_pumpkin.so", "so"],
    ["windows-latest", "x86_64-pc-windows-msvc", "opanel_pumpkin.dll", "dll"],
    ["macos-15-intel", "x86_64-apple-darwin", "libopanel_pumpkin.dylib", "dylib"],
    ["macos-latest", "aarch64-apple-darwin", "libopanel_pumpkin.dylib", "dylib"],
  ];
  assert.equal(builds.length, expected.length);
  assert.equal(uploads.length, 7);
  assert.equal(new Set(uploads.map(({ asset }) => asset)).size, 7);
  for(const [runner, target, library, extension] of expected) {
    const build = builds.find((entry) => entry.target === target);
    assert.equal(build.runner, runner);
    assert.equal(build.library, library);
    assert.equal(build.asset, `opanel-pumpkin-${target}-build-2.3.0.${extension}`);
    assert.deepEqual(uploads.find(({ asset }) => asset === build.asset), {
      asset: build.asset, source: "artifact", artifact: build.artifact,
    });
  }
  const jars = uploads.filter(({ source }) => source === "release");
  assert.equal(jars.length, 2);
  for(const jar of jars) {
    assert.equal(jar.downloadUrl, `https://github.com/opanel-mc/opanel/releases/download/v2.3.0/${jar.asset}`);
  }
});

test("preserves prerelease and build metadata versions with or without a v tag prefix", (t) => {
  const version = "2.3.0-rc.1+build.7";
  for(const tag of [version, `v${version}`]) {
    const result = prepareMatrix(t, { version, tag, assets: [`opanel-paper-26.1-build-${version}.jar`] });
    assert.equal(result.status, 0, result.stderr);
    const uploads = JSON.parse(result.outputs.matrix).include;
    assert(uploads.some(({ asset }) => asset === `opanel-pumpkin-x86_64-pc-windows-msvc-build-${version}.dll`));
    assert.equal(uploads[0].downloadUrl, `https://github.com/opanel-mc/opanel/releases/download/${encodeURIComponent(tag)}/${encodeURIComponent(uploads[0].asset)}`);
  }
});

test("rejects missing, unsafe, or release-mismatched Pumpkin versions", (t) => {
  for(const options of [
    { packageName: "another-package" },
    { version: "" },
    { version: "../../bad-version" },
    { version: "2.2.4" },
  ]) {
    const result = prepareMatrix(t, options);
    assert.notEqual(result.status, 0);
    assert.deepEqual(result.outputs, {});
    assert.match(result.stderr, /valid opanel-pumpkin version|does not match release tag/);
  }
});

test("requires JAR assets and rejects invalid release filenames", (t) => {
  for(const assets of [[], ["other.jar"], ["opanel bad.jar"]]) {
    const result = prepareMatrix(t, { assets });
    assert.notEqual(result.status, 0);
    assert.deepEqual(result.outputs, {});
    assert.match(result.stderr, /does not contain any jar assets|Invalid release asset names/);
  }
});
