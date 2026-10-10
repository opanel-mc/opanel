import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import nextEnv from "@next/env";
import { resolveTextureVersions } from "./texture-config.js";
import {
  frontendDir,
  stagingDir,
  resolveFrontendTarget,
  resolveFrontendOutput,
  writeCompatibilityId,
  publishFrontend,
} from "./build-config.js";

nextEnv.loadEnvConfig(frontendDir, false);
const env = process.env;
process.env.VITE_OPANEL_TARGET = resolveFrontendTarget(env);
process.env.TEXTURE_VERSIONS = resolveTextureVersions(env).join(",");
const outputDir = resolveFrontendOutput(env);

await import("./prelaunch.js");

// Next.js exports into out; retain the module packaging layout in dist/client.
const result = spawnSync(process.execPath, [
  path.join(frontendDir, "node_modules/next/dist/bin/next"),
  "build",
  ...process.argv.slice(2),
], { cwd: frontendDir, stdio: "inherit", env: process.env });
if(result.error) throw result.error;
if(result.status !== 0) process.exit(result.status || 1);

// Replace the staging directory so a prior vinext build cannot leave server
// bundles or other obsolete files in the published frontend.
fs.rmSync(stagingDir, { recursive: true, force: true });
fs.mkdirSync(stagingDir, { recursive: true });
fs.renameSync(path.join(frontendDir, "out"), path.join(stagingDir, "client"));
writeCompatibilityId(stagingDir);
publishFrontend(outputDir);
console.log(`Frontend ${process.env.VITE_OPANEL_TARGET} built at ${outputDir}`);
