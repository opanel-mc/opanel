import { readdirSync } from "node:fs";
import path from "node:path";
import { DefaultArtifactClient } from "@actions/artifact";

const dir = path.join(process.env.GITHUB_WORKSPACE ?? process.cwd(), "build", "libs");
const client = new DefaultArtifactClient();
const jars = readdirSync(dir).filter((f) => f.endsWith(".jar"));

if(jars.length === 0) {
  console.error(`No jar files found in ${dir}`);
  process.exit(1);
}

for(const jar of jars) {
  const name = jar.replace(/\.jar$/, "");
  const { id, size } = await client.uploadArtifact(name, [path.join(dir, jar)], dir);
  console.log(`Uploaded ${jar} as artifact "${name}" (id=${id}, ${size} bytes)`);
}
