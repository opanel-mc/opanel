import { appendFileSync, readFileSync, readdirSync, statSync } from "node:fs";
import { extname } from "node:path";

const pumpkinTargets = [
  { runner: "ubuntu-24.04", target: "x86_64-unknown-linux-gnu", library: "libopanel_pumpkin.so" },
  { runner: "ubuntu-24.04-arm", target: "aarch64-unknown-linux-gnu", library: "libopanel_pumpkin.so" },
  { runner: "windows-latest", target: "x86_64-pc-windows-msvc", library: "opanel_pumpkin.dll" },
  { runner: "macos-15-intel", target: "x86_64-apple-darwin", library: "libopanel_pumpkin.dylib" },
  { runner: "macos-latest", target: "aarch64-apple-darwin", library: "libopanel_pumpkin.dylib" },
];

const assetsDir = process.env.RELEASE_ASSETS_DIR;
const releaseRepository = process.env.RELEASE_REPOSITORY;
const releaseTag = process.env.RELEASE_TAG;
const cargoMetadataPath = process.env.RELEASE_CARGO_METADATA;

if(!assetsDir || !releaseRepository || !releaseTag || !cargoMetadataPath) {
  throw new Error("RELEASE_ASSETS_DIR, RELEASE_REPOSITORY, RELEASE_TAG and RELEASE_CARGO_METADATA are required");
}
if(!/^[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+$/.test(releaseRepository)) {
  throw new Error(`Invalid release repository: ${releaseRepository}`);
}
if(!statSync(assetsDir).isDirectory()) {
  throw new Error(`Release assets directory does not exist: ${assetsDir}`);
}

const metadata = JSON.parse(readFileSync(cargoMetadataPath, "utf8"));
const version = metadata.packages.find((pkg) => pkg.name === "opanel-pumpkin")?.version;
if(typeof version !== "string" || !/^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?(?:\+[0-9A-Za-z.-]+)?$/.test(version)) {
  throw new Error("Release Cargo metadata does not contain a valid opanel-pumpkin version");
}
if(version !== releaseTag.replace(/^v(?=\d)/, "")) {
  throw new Error(`Pumpkin version ${version} does not match release tag ${releaseTag}`);
}

const assets = readdirSync(assetsDir)
  .filter((name) => name.endsWith(".jar"))
  .sort();
const invalidAssets = assets.filter(
  (name) => !/^opanel-[A-Za-z0-9._+-]+\.jar$/.test(name),
);

if(invalidAssets.length > 0) {
  throw new Error(`Invalid release asset names: ${invalidAssets.join(", ")}`);
}
if(assets.length === 0) {
  throw new Error(`Release ${releaseTag} does not contain any jar assets`);
}

const pumpkinMatrix = {
  include: pumpkinTargets.map((target) => ({
    ...target,
    asset: `opanel-pumpkin-${target.target}-build-${version}${extname(target.library)}`,
    artifact: `cos-pumpkin-${target.target}`,
  })),
};
const matrix = {
  include: [...assets.map((asset) => ({
    asset,
    source: "release",
    downloadUrl: createDownloadUrl(releaseRepository, releaseTag, asset),
  })), ...pumpkinMatrix.include.map(({ asset, artifact }) => ({
    asset,
    source: "artifact",
    artifact,
  }))],
};

if(process.env.GITHUB_OUTPUT) {
  appendFileSync(process.env.GITHUB_OUTPUT, [
    `matrix=${JSON.stringify(matrix)}`,
    `pumpkin_matrix=${JSON.stringify(pumpkinMatrix)}`,
    `opanel_version=${version}`,
    "",
  ].join("\n"));
}

console.log(`Prepared ${assets.length} JARs and ${pumpkinMatrix.include.length} Pumpkin libraries for ${releaseTag}.`);

function createDownloadUrl(repository, tag, asset) {
  return `https://github.com/${repository}/releases/download/${encodeURIComponent(tag)}/${encodeURIComponent(asset)}`;
}
