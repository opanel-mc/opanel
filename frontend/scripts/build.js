import path from "node:path";
import { spawnSync } from "node:child_process";
import { loadEnv } from "vite";
import { resolveTextureVersions } from "./texture-config.js";
import {
  frontendDir,
  stagingDir,
  resolveFrontendTarget,
  resolveFrontendOutput,
  writeCompatibilityId,
  publishFrontend,
} from "./build-config.js";

const env = { ...loadEnv("production", frontendDir, ""), ...process.env };
process.env.VITE_OPANEL_TARGET = resolveFrontendTarget(env);
process.env.TEXTURE_VERSIONS = resolveTextureVersions(env).join(",");
const outputDir = resolveFrontendOutput(env);

await import("./prelaunch.js");

// vinext's static exporter still uses dist internally. Keep the entire build
// together there, then publish it to the requested module's output directory.
const result = spawnSync(process.execPath, [
  path.join(frontendDir, "node_modules/vinext/dist/cli.js"),
  "build",
  ...process.argv.slice(2),
], { cwd: frontendDir, stdio: "inherit", env: process.env });
if(result.error) throw result.error;
if(result.status !== 0) process.exit(result.status || 1);

writeCompatibilityId(stagingDir);
publishFrontend(outputDir);
console.log(`Frontend ${process.env.VITE_OPANEL_TARGET} built at ${outputDir}`);
