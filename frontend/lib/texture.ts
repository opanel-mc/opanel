import { type Item, versions } from "minecraft-textures";
import { coerce, compare } from "semver";
import { textureLoaders } from "opanel-textures";

export async function getTextures(version: string): Promise<Item[] | null> {
  let suitableVersion: string | null = null;
  for(const textureVersion of versions) {
    if(compare(coerce(textureVersion) ?? "", coerce(version) ?? "") > 0) break;
    suitableVersion = textureVersion;
  }

  if(suitableVersion == null) return null;

  const loadTextures = textureLoaders[suitableVersion];
  if(loadTextures == null) return null;

  return (await loadTextures()).items;
}
