import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath, pathToFileURL } from "node:url";
import { createBuildMatrix } from "./build-matrix.mjs";

const repositoryDir = fileURLToPath(new URL("../../", import.meta.url));
const ignoredDirectories = [".agents/", ".claude/", ".idea/", ".vscode/", "images/", ".github/ISSUE_TEMPLATE/", "example-extension/"];
const publishingFiles = new Set([
  ".github/workflows/api-publish.yml",
  ".github/workflows/cos-publish.yml",
  ".github/workflows/curseforge-publish.yml",
  ".github/workflows/modrinth-publish.yml",
  ".github/scripts/prepare-cos-matrix.mjs",
  ".github/scripts/prepare-modrinth-matrix.mjs",
]);
const detectionFiles = new Set([
  ".github/workflows/ci.yml",
  ".github/scripts/detect-builds.mjs",
]);
const matrixFiles = new Set([
  ".github/scripts/build-matrix.mjs",
  ".github/scripts/build-matrix.test.mjs",
]);

function isIgnored(file) {
  return /\.md$/i.test(file) || path.posix.basename(file) === ".gitignore" ||
    ignoredDirectories.some((directory) => file.startsWith(directory)) ||
    ["LICENSE", "ThirdPartyNotices.txt", ".github/FUNDING.yml"].includes(file) || publishingFiles.has(file);
}

function booleanInput(value) {
  if(value === true || value === "true") return true;
  if(value === false || value === "false") return false;
  throw new Error(`Invalid manual build option: ${value}`);
}

export function selectBuilds(catalog, {
  eventName, files = [], fallbackReason, buildJar = true, buildPumpkin = true,
}) {
  const selected = new Set();
  const reasons = new Set();
  let pumpkin = false;
  let frontendChecks = false;
  const allJava = () => catalog.matrix.include.forEach(({ target }) => selected.add(target));
  const allBuilds = () => {
    allJava();
    pumpkin = frontendChecks = true;
  };

  if(eventName === "workflow_dispatch") {
    if(booleanInput(buildJar)) {
      allJava();
    }
    pumpkin = booleanInput(buildPumpkin);
    frontendChecks = selected.size > 0 || pumpkin;
    reasons.add(frontendChecks ? "Manual selection; enabled branches build in full." : "No build branches selected.");
  } else if(fallbackReason) {
    allBuilds();
    reasons.add(`Full build fallback: ${fallbackReason}`);
  } else {
    for(const file of files) {
      if(isIgnored(file)) continue;
      if(detectionFiles.has(file)) {
        allBuilds();
        reasons.add("Build workflow or change detection changed.");
      } else if(file.startsWith("frontend/")) {
        allJava();
        pumpkin = frontendChecks = true;
        reasons.add("Shared frontend inputs changed.");
      } else if(file.startsWith("pumpkin/") || ["Cargo.toml", "Cargo.lock"].includes(file)) {
        pumpkin = true;
        reasons.add("Pumpkin inputs changed.");
      } else if(file.startsWith("api/") || file.startsWith("core/")) {
        allJava();
        reasons.add("Shared Java code changed.");
      } else if(file.startsWith("gradle/") || matrixFiles.has(file) ||
        ["build.gradle", "settings.gradle", "frontend.gradle", "platform-modules.json", "gradle.properties", "gradlew", "gradlew.bat"].includes(file)) {
        allJava();
        if(file === "frontend.gradle") frontendChecks = true;
        reasons.add("Shared Java build configuration changed.");
      } else {
        const [platform, module] = file.split("/");
        const modules = Object.hasOwn(catalog.registry, platform) ? catalog.registry[platform] : undefined;
        if(modules) {
          const affected = Object.entries(modules)
            .filter(([target, helpers]) => target === module || helpers.includes(module))
            .map(([target]) => target);
          if(affected.length) {
            affected.forEach((target) => selected.add(target));
            reasons.add(`Changed module: ${platform}/${module}.`);
          } else {
            // New/deleted modules and files shared by a platform must not be
            // silently lost simply because they are absent from the registry.
            allJava();
            reasons.add(`Unclassified Java platform input: ${platform}.`);
          }
        } else {
          allBuilds();
          reasons.add(`Unclassified input: ${JSON.stringify(file)}.`);
        }
      }
    }
    if(reasons.size === 0) reasons.add("Only ignored files changed, or no file differences were found.");
  }

  const include = catalog.matrix.include.filter(({ target }) => selected.has(target));
  return {
    java_matrix: { include },
    java_required: include.length > 0,
    pumpkin_required: pumpkin,
    frontend_checks_required: frontendChecks,
    prepare_required: include.length > 0 || pumpkin,
    reasons: [...reasons],
  };
}

class ComparisonUnavailable extends Error {}

export function collectChangedFiles({ eventName, baseSha, headSha }, root = repositoryDir) {
  const git = (args) => spawnSync("git", args, {
    cwd: root, encoding: "utf8", maxBuffer: 32 * 1024 * 1024,
  });
  const ok = (result) => !result.error && result.status === 0;
  try {
    if(!["push", "pull_request"].includes(eventName)) throw new Error("Unsupported automatic event");
    if(![baseSha, headSha].every((sha) => /^[a-f0-9]{40}$/i.test(sha || "") && !/^0+$/.test(sha))) {
      throw new ComparisonUnavailable("Comparison commit is missing or invalid");
    }
    for(const sha of [baseSha, headSha]) {
      if(!ok(git(["cat-file", "-e", `${sha}^{commit}`])) &&
        !ok(git(["fetch", "--no-tags", "--depth=1", "origin", sha]))) {
        throw new ComparisonUnavailable("Could not fetch a comparison commit");
      }
    }
    let base = baseSha;
    if(eventName === "pull_request") {
      let mergeBase = git(["merge-base", baseSha, headSha]);
      if(!ok(mergeBase)) {
        // Checkout is shallow. Fetch bounded history instead of downloading
        // every old generated frontend merely to classify a PR.
        if(!ok(git(["fetch", "--no-tags", "--deepen=256", "origin", baseSha, headSha]))) {
          throw new ComparisonUnavailable("Could not fetch PR comparison history");
        }
        mergeBase = git(["merge-base", baseSha, headSha]);
      }
      if(!ok(mergeBase)) throw new ComparisonUnavailable("PR merge base is unavailable");
      base = mergeBase.stdout.trim();
    }
    // Disable rename detection so both the old and new paths participate.
    // NUL separation preserves spaces, tabs and newlines in filenames.
    const diff = git(["diff", "--name-only", "--no-renames", "-z", base, headSha, "--"]);
    if(!ok(diff)) throw new ComparisonUnavailable("Could not compare commits");
    return { files: diff.stdout.split("\0").filter(Boolean) };
  } catch(error) {
    if(!(error instanceof ComparisonUnavailable)) throw error;
    return { files: [], fallbackReason: error.message };
  }
}

export function detectBuilds(options, root = repositoryDir) {
  // Configuration errors must fail the job, not become an empty matrix or a
  // successful fallback. Only unavailable Git history falls back to all builds.
  const catalog = createBuildMatrix(root);
  const changes = options.eventName === "workflow_dispatch" ? {} : collectChangedFiles(options, root);
  return selectBuilds(catalog, { ...options, ...changes });
}

export function buildSummary(plan) {
  const targets = plan.java_matrix.include.map(({ target }) => `\`${target}\``).join(", ");
  return [
    "## Build selection", "",
    "| Work | Decision |", "| --- | --- |",
    `| Java targets | ${plan.java_required ? plan.java_matrix.include.length : "Skipped"} |`,
    `| Pumpkin | ${plan.pumpkin_required ? "Build all native targets" : "Skipped"} |`,
    `| Shared frontend preparation | ${plan.prepare_required ? "Run" : "Skipped"} |`,
    `| Frontend checks | ${plan.frontend_checks_required ? "Run" : "Skipped"} |`,
    "", ...(targets ? [`Selected Java targets: ${targets}`, ""] : []),
    ...plan.reasons.map((reason) => `- ${reason}`), "",
  ].join("\n");
}

if(process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) {
  const plan = detectBuilds({
    eventName: process.env.EVENT_NAME,
    baseSha: process.env.BASE_SHA,
    headSha: process.env.HEAD_SHA,
    buildJar: process.env.BUILD_JAR,
    buildPumpkin: process.env.BUILD_PUMPKIN,
  });
  if(process.env.GITHUB_OUTPUT) {
    const outputs = Object.entries(plan).filter(([name]) => name !== "reasons")
      .map(([name, value]) => `${name}=${JSON.stringify(value)}\n`).join("");
    fs.appendFileSync(process.env.GITHUB_OUTPUT, outputs);
  }
  const summary = buildSummary(plan);
  if(process.env.GITHUB_STEP_SUMMARY) fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, summary);
  console.log(summary);
}
