import { createHash } from "node:crypto";
import { appendFileSync, createReadStream, readdirSync, statSync } from "node:fs";
import path from "node:path";

const PLATFORM_NAMES = {
  fabric: "Fabric",
  folia: "Folia",
  forge: "Forge",
  neoforge: "NeoForge",
  paper: "Paper",
};

const assetsDir = process.env.RELEASE_ASSETS_DIR;
const releaseTag = process.env.RELEASE_TAG;
const releaseRepository = process.env.RELEASE_REPOSITORY;
const releaseName = process.env.RELEASE_NAME?.trim() || `OPanel ${releaseTag}`;
const supportedVersionListUrl = process.env.SUPPORTED_VERSION_LIST_URL;

if(!assetsDir || !releaseTag || !releaseRepository || !supportedVersionListUrl) {
  throw new Error("RELEASE_ASSETS_DIR, RELEASE_TAG, RELEASE_REPOSITORY and SUPPORTED_VERSION_LIST_URL are required");
}

if(!statSync(assetsDir).isDirectory()) {
  throw new Error(`Release assets directory does not exist: ${assetsDir}`);
}

const supportedVersions = await fetchJson(supportedVersionListUrl);
const expectedTargets = parseSupportedTargets(supportedVersions);
const releaseVersion = releaseTag.replace(/^v(?=\d)/, "");
const channel = getReleaseChannel(releaseTag, process.env.RELEASE_PRERELEASE);
const entries = [];
const foundTargets = new Set();
const errors = [];

const jarNames = readdirSync(assetsDir)
  .filter((name) => name.endsWith(".jar"))
  .sort();

if(jarNames.length === 0) {
  throw new Error(`No jar files found in ${assetsDir}`);
}

for(const asset of jarNames) {
  if(asset === `opanel-${releaseVersion}.jar`) {
    console.log(`Ignoring root project jar ${asset}`);
    continue;
  }

  const match = /^opanel-(fabric|folia|forge|neoforge|paper)-(.+?)-build-(.+)\.jar$/.exec(asset);
  if(!match) {
    errors.push(`Unrecognised release jar: ${asset}`);
    continue;
  }

  const [, platform, buildVersion, artifactVersion] = match;
  const targetKey = `${platform}:${buildVersion}`;
  const gameVersions = expectedTargets.get(targetKey);

  if(artifactVersion !== releaseVersion) {
    errors.push(`${asset} contains version ${artifactVersion}, expected ${releaseVersion}`);
  }
  if(!gameVersions) {
    errors.push(`No supported-version-list entry for ${platform} ${buildVersion}`);
    continue;
  }
  if(foundTargets.has(targetKey)) {
    errors.push(`Multiple release jars found for ${platform} ${buildVersion}`);
    continue;
  }

  foundTargets.add(targetKey);
  entries.push({
    asset,
    buildVersion,
    channel,
    downloadUrl: createDownloadUrl(releaseRepository, releaseTag, asset),
    gameVersions: JSON.stringify(gameVersions),
    loader: platform,
    platform: PLATFORM_NAMES[platform],
    sha512: await hashFile(path.join(assetsDir, asset)),
    versionName: `${releaseName} - ${PLATFORM_NAMES[platform]} ${buildVersion}`,
    versionNumber: `${releaseTag}-${platform}-${buildVersion}`,
  });
}

for(const targetKey of expectedTargets.keys()) {
  if(!foundTargets.has(targetKey)) {
    errors.push(`Release jar is missing for ${targetKey.replace(":", " ")}`);
  }
}

if(errors.length > 0) {
  throw new Error(`Release assets and supported-version-list do not match:\n- ${errors.join("\n- ")}`);
}

if(entries.length === 0) {
  throw new Error("Publishing matrix is empty");
}

const matrix = JSON.stringify({ include: entries });
if(process.env.GITHUB_OUTPUT) {
  appendFileSync(process.env.GITHUB_OUTPUT, `matrix=${matrix}\n`);
}

console.log(`Prepared ${entries.length} Modrinth publishing targets with channel ${channel}.`);

function parseSupportedTargets(value) {
  if(!value || typeof value !== "object" || Array.isArray(value)) {
    throw new Error("Supported version list must be a JSON object");
  }

  const targets = new Map();
  for(const [platform, versions] of Object.entries(value)) {
    if(!(platform in PLATFORM_NAMES)) {
      throw new Error(`Unsupported platform in supported-version-list: ${platform}`);
    }
    if(!versions || typeof versions !== "object" || Array.isArray(versions)) {
      throw new Error(`Supported versions for ${platform} must be a JSON object`);
    }

    for(const [buildVersion, gameVersions] of Object.entries(versions)) {
      if(!Array.isArray(gameVersions) || gameVersions.length === 0) {
        throw new Error(`${platform} ${buildVersion} must contain at least one game version`);
      }
      if(gameVersions.some((version) => typeof version !== "string" || version.length === 0)) {
        throw new Error(`${platform} ${buildVersion} contains an invalid game version`);
      }
      if(new Set(gameVersions).size !== gameVersions.length) {
        throw new Error(`${platform} ${buildVersion} contains duplicate game versions`);
      }

      targets.set(`${platform}:${buildVersion}`, gameVersions);
    }
  }

  return targets;
}

async function fetchJson(url) {
  let lastError;
  for(let attempt = 1; attempt <= 3; attempt++) {
    try {
      const response = await fetch(url, {
        headers: { Accept: "application/json" },
      });
      if(!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return await response.json();
    } catch(error) {
      lastError = error;
      if(attempt < 3) {
        await new Promise((resolve) => setTimeout(resolve, attempt * 1000));
      }
    }
  }

  throw new Error(`Unable to fetch supported version list from ${url}: ${lastError}`);
}

function getReleaseChannel(tag, prerelease) {
  if(tag.toLowerCase().includes("alpha")) {
    return "alpha";
  }
  if(String(prerelease).toLowerCase() === "true" || /(?:beta|rc|pre)/i.test(tag)) {
    return "beta";
  }
  return "release";
}

function createDownloadUrl(repository, tag, asset) {
  return `https://github.com/${repository}/releases/download/${encodeURIComponent(tag)}/${encodeURIComponent(asset)}`;
}

function hashFile(file) {
  return new Promise((resolve, reject) => {
    const hash = createHash("sha512");
    createReadStream(file)
      .on("data", (chunk) => hash.update(chunk))
      .on("error", reject)
      .on("end", () => resolve(hash.digest("hex")));
  });
}
