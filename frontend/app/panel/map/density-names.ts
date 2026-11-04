import type { TranslationKey } from "@/lang";
import { $, $mc, localize } from "@/lib/i18n";

const legacyEntityIds: Record<string, string> = {
  dropped_item: "item", mushroom_cow: "mooshroom", snowman: "snow_golem",
  leash_hitch: "leash_knot", primed_tnt: "tnt", firework: "firework_rocket",
  fishing_hook: "fishing_bobber", ender_signal: "eye_of_ender", ender_crystal: "end_crystal",
  thrown_exp_bottle: "experience_bottle", splash_potion: "potion", lingering_potion: "potion",
  lightning: "lightning_bolt", minecart_chest: "chest_minecart", minecart_furnace: "furnace_minecart",
  minecart_tnt: "tnt_minecart", minecart_hopper: "hopper_minecart",
  minecart_mob_spawner: "spawner_minecart", minecart_command: "command_block_minecart",
};
export function densityEntityName(type: string): string {
  const id = type.toLowerCase();
  const key = `entity.minecraft.${legacyEntityIds[id] ?? id}`;
  const name = localize(key as TranslationKey);
  return name === key ? $("map.density.unknown-type", type) : name;
}
export function densityBlockName(type: string): string {
  const id = type.toLowerCase().replace("_wall_hanging_sign", "_hanging_sign").replace("_wall_sign", "_sign").replace("_wall_banner", "_banner");
  const key = `minecraft:${id}`;
  const name = $mc(key);
  return name === key ? $("map.density.unknown-type", type) : name;
}
