import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const frontendDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const buildIdFile = path.join(frontendDir, "dist/server/BUILD_ID");
const compatibilityIdFile = path.join(frontendDir, "dist/vinext-rsc-compatibility-id");

if(!fs.existsSync(buildIdFile)) {
  throw new Error(`vinext BUILD_ID was not found at ${buildIdFile}`);
}

const buildId = fs.readFileSync(buildIdFile, "utf8").trim();
if(!buildId) {
  throw new Error("vinext generated an empty BUILD_ID");
}

fs.writeFileSync(compatibilityIdFile, `${buildId}\n`, "utf8");
