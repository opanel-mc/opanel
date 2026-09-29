import fs from "node:fs";
import path from "node:path";

const distDir = path.resolve(process.cwd(), "dist/client");
const serverBuildIdFile = path.resolve(process.cwd(), "dist/server/BUILD_ID");
const resourcesDir = path.resolve(process.cwd(), "../core/src/main/resources");
const targetDir = path.join(resourcesDir, "web");
const targetBuildIdFile = path.join(resourcesDir, "vinext-rsc-compatibility-id");

const buildId = fs.readFileSync(serverBuildIdFile, "utf8").trim();
if(!buildId) {
  throw new Error("vinext did not generate a BUILD_ID");
}

fs.rmSync(targetDir, { recursive: true, force: true });
fs.mkdirSync(targetDir, { recursive: true });
fs.cpSync(distDir, targetDir, { recursive: true });
fs.writeFileSync(targetBuildIdFile, `${buildId}\n`, "utf8");
