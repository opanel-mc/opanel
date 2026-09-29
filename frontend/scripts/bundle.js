import fs from "node:fs";
import path from "node:path";

const distDir = path.resolve(process.cwd(), "dist/client");
const compatibilityIdFile = path.resolve(process.cwd(), "dist/vinext-rsc-compatibility-id");
const resourcesDir = path.resolve(process.cwd(), "../core/src/main/resources");
const targetDir = path.join(resourcesDir, "web");
const targetBuildIdFile = path.join(resourcesDir, "vinext-rsc-compatibility-id");

if(!fs.existsSync(compatibilityIdFile)) {
  throw new Error(`vinext RSC compatibility ID was not found at ${compatibilityIdFile}`);
}

fs.rmSync(targetDir, { recursive: true, force: true });
fs.mkdirSync(targetDir, { recursive: true });
fs.cpSync(distDir, targetDir, { recursive: true });
fs.copyFileSync(compatibilityIdFile, targetBuildIdFile);
