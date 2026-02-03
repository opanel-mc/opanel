import { type Item, versions } from "minecraft-textures";
import { coerce, compare } from "semver";

export async function getTextures(version: string): Promise<Item[] | null> {
  let suitableVersion: string | null = null;
  for(const textureVersion of versions) {
    if(compare(coerce(textureVersion) ?? "", coerce(version) ?? "") > 0) break;
    suitableVersion = textureVersion;
  }

  if(suitableVersion == null) return null;
  return (await import(`minecraft-textures/dist/textures/json/${suitableVersion}.json`)).items;
}
