import fs from "node:fs";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const repositoryDir = fileURLToPath(new URL("../../", import.meta.url));

export function createBuildMatrix(root = repositoryDir) {
  const registry = JSON.parse(fs.readFileSync(path.join(root, "platform-modules.json"), "utf8"));
  const version = fs.readFileSync(path.join(root, "gradle.properties"), "utf8")
    .match(/^version=(.+)$/m)?.[1].trim();
  if(!version) throw new Error("Missing project version");
  const include = [];
  const seen = new Set();
  for(const [platform, targets] of Object.entries(registry)) {
    for(const [target, helpers] of Object.entries(targets)) {
      if(seen.has(target) || !/^(fabric|forge|neoforge|paper|folia)-\d[\d.]*$/.test(target)) {
        throw new Error(`Invalid or duplicate target: ${target}`);
      }
      seen.add(target);
      for(const module of [target, ...helpers]) {
        if(!fs.existsSync(path.join(root, platform, module, "build.gradle"))) {
          throw new Error(`Missing module: ${platform}/${module}`);
        }
      }
      const properties = fs.readFileSync(path.join(root, platform, target, "gradle.properties"), "utf8");
      const baseName = properties.match(/^baseName=(.+)$/m)?.[1].trim();
      if(!baseName) throw new Error(`Missing baseName for ${target}`);
      include.push({ target, artifact: `${baseName}-build-${version}` });
    }
  }
  if(!include.length) throw new Error("No Java build targets registered");
  return { matrix: { include }, version, registry };
}

if(process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) {
  const { matrix, version } = createBuildMatrix();
  if(process.env.GITHUB_OUTPUT) {
    fs.appendFileSync(process.env.GITHUB_OUTPUT, `matrix=${JSON.stringify(matrix)}\nversion=${version}\n`);
  }
  console.log(JSON.stringify(matrix, null, 2));
}
