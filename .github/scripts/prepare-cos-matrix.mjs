import { appendFileSync, readdirSync, statSync } from "node:fs";

const assetsDir = process.env.RELEASE_ASSETS_DIR;
const releaseTag = process.env.RELEASE_TAG;

if(!assetsDir || !releaseTag) {
  throw new Error("RELEASE_ASSETS_DIR and RELEASE_TAG are required");
}
if(!statSync(assetsDir).isDirectory()) {
  throw new Error(`Release assets directory does not exist: ${assetsDir}`);
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

const matrix = JSON.stringify({
  include: assets.map((asset) => ({ asset })),
});

if(process.env.GITHUB_OUTPUT) {
  appendFileSync(process.env.GITHUB_OUTPUT, `matrix=${matrix}\n`);
}

console.log(`Prepared ${assets.length} COS publishing targets for ${releaseTag}.`);
