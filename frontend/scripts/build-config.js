import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

export const frontendDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
export const stagingDir = path.join(frontendDir, "dist");

export function resolveFrontendTarget(env = process.env, production = true) {
  const target = env.VITE_OPANEL_TARGET;
  if(!target?.trim()) {
    if(production) throw new Error("VITE_OPANEL_TARGET is required for a production frontend build");
    return "paper";
  }
  return target;
}

export function resolveFrontendOutput(env = process.env) {
  const output = path.resolve(frontendDir, env.OPANEL_FRONTEND_OUTPUT || "dist");
  // Publishing replaces the output directory. Never allow it to replace source
  // directories or overlap the staging directory.
  const contains = (parent, child) => {
    const relative = path.relative(parent, child);
    return relative !== ".." && !relative.startsWith(`..${path.sep}`) && !path.isAbsolute(relative);
  };
  if(output !== stagingDir && (contains(output, frontendDir) || contains(frontendDir, output))) {
    throw new Error("OPANEL_FRONTEND_OUTPUT must not contain frontend sources or overlap frontend/dist");
  }
  return output;
}

export function validatePreparedFrontend(root = frontendDir) {
  const requiredFiles = [
    ...["en_us", "zh_cn", "zh_hk"] // only validating the three languages is ok
      .map((lang) => `assets/minecraft/${lang}.json`),
    "assets/minecraft/textures/stone.png",
    "assets/minecraft/models/stone.json",
    "assets/minecraft/blockstates/stone.json",
    "wasm-lib/pkg/wasm_lib.js",
    "wasm-lib/pkg/wasm_lib.d.ts",
    "wasm-lib/pkg/wasm_lib_bg.wasm",
  ];
  for(const file of requiredFiles) {
    const fullPath = path.join(root, file);
    if(!fs.existsSync(fullPath) || !fs.statSync(fullPath).isFile() || fs.statSync(fullPath).size === 0) {
      throw new Error(`Prepared frontend input is missing or empty: ${file}. Run npm run prelaunch without OPANEL_FRONTEND_PREPARED=1 first.`);
    }
  }
}

export function writeCompatibilityId(outputDir) {
  const buildId = fs.readFileSync(path.join(frontendDir, ".next/BUILD_ID"), "utf8").trim();
  if(!buildId) throw new Error("Next.js generated an empty BUILD_ID");
  // Retain the resource name consumed by both Java and Pumpkin. The backends
  // also send it as x-nextjs-deployment-id, matching Next.js's deployment ID.
  fs.writeFileSync(path.join(outputDir, "vinext-rsc-compatibility-id"), `${buildId}\n`, "utf8");
}

export function publishFrontend(outputDir, sourceDir = stagingDir) {
  for(const file of ["client/index.html", "client/404.html", "vinext-rsc-compatibility-id"]) {
    if(!fs.statSync(path.join(sourceDir, file)).size) throw new Error(`Empty frontend output: ${file}`);
  }
  if(outputDir === sourceDir) return;
  fs.rmSync(outputDir, { recursive: true, force: true });
  fs.cpSync(sourceDir, outputDir, { recursive: true });
}
