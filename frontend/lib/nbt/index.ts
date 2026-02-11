import type { ItemNBTResolver } from "./resolver";
import { coerce, compare } from "semver";
import { ComponentsResolver } from "./components-resolver";
import { TagResolver } from "./tag-resolver";

export function createResolver(version: string, snbt: string): ItemNBTResolver {
  return (
    compare(coerce(version) ?? "", coerce("1.20.5") ?? "") >= 0
    ? new ComponentsResolver(snbt)
    : new TagResolver(snbt)
  );
}
