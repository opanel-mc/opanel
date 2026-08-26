import { type Item, versions } from "minecraft-textures";
import { coerce, compare } from "semver";

const textureModules = import.meta.glob<{ items: Item[] }>([
  "@minecraft-textures-json/*.json",
  "!@minecraft-textures-json/*.id.json",
  "!@minecraft-textures-json/1.12.json",
  "!@minecraft-textures-json/1.13.json",
  "!@minecraft-textures-json/1.14.json",
  "!@minecraft-textures-json/1.15.json",
  "!@minecraft-textures-json/1.16.json",
  "!@minecraft-textures-json/1.17.json",
  "!@minecraft-textures-json/1.18.json",
]);

export async function getTextures(version: string): Promise<Item[] | null> {
  let suitableVersion: string | null = null;
  for(const textureVersion of versions) {
    if(compare(coerce(textureVersion) ?? "", coerce(version) ?? "") > 0) break;
    suitableVersion = textureVersion;
  }

  if(suitableVersion == null) return null;

  const modulePath = Object.keys(textureModules).find((path) => (
    path.endsWith(`/${suitableVersion}.json`)
  ));
  if(modulePath == null) return null;

  return (await textureModules[modulePath]()).items;
}
