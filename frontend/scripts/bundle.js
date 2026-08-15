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

function moveRouteHtmlToIndex(dir) {
  for(const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const entryPath = path.join(dir, entry.name);
    if(entry.isDirectory()) {
      moveRouteHtmlToIndex(entryPath);
      continue;
    }
    if(!entry.isFile() || !entry.name.endsWith(".html")) continue;
    if(entry.name === "index.html" || entry.name === "404.html") continue;

    const routeDir = entryPath.slice(0, -".html".length);
    fs.mkdirSync(routeDir, { recursive: true });
    fs.renameSync(entryPath, path.join(routeDir, "index.html"));
  }
}

moveRouteHtmlToIndex(targetDir);
