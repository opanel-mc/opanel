import { parseNbtString, type NbtObject } from "snbt-js";

// export type Color = "white" | "orange" | "magenta" | "light_blue" | "yellow" | "lime" | "pink" | "gray" | "light_gray" | "cyan" | "purple" | "blue" | "brown" | "green" | "red" | "black";
export type RgbColor = [number, number, number];

export type Enchantments = Map<string, number>;

export const glintItems = [
  "minecraft:enchanted_book",
  "minecraft:experience_bottle",
  "minecraft:enchanted_golden_apple",
  "minecraft:end_crystal",
  "minecraft:nether_star",
  "minecraft:written_book",
  "minecraft:debug_stick"
];

export abstract class ItemNBTResolver {
  protected nbt: NbtObject;

  constructor(protected id: string, snbt: string) {
    this.nbt = parseNbtString(snbt);
  }
  
  abstract isEmpty(): boolean;
  abstract getName(): string;
  abstract hasCustomName(): boolean;
  abstract getLore(): string[];
  abstract getEnchantments(): Enchantments;
  abstract hasEnchantments(): boolean;
  abstract shouldGlint(): boolean;
  abstract getDamage(): number | null;
  abstract isUnbreakable(): boolean;
  abstract isPotion(): boolean;
  abstract isTippedArrow(): boolean;
  abstract getPotionId(): string | null;
  abstract getPotionColor(): RgbColor | null;
  abstract getItemModel(): string | null;
  abstract getMapId(): number | null;
  abstract getBeeAmount(): number | null;
  abstract getHoneyLevel(): number | null;

  isDyedLeatherArmor(): boolean {
    return !!this.getDyedColor() && [
      "minecraft:leather_helmet",
      "minecraft:leather_chestplate",
      "minecraft:leather_leggings",
      "minecraft:leather_boots"
    ].includes(this.id);
  }

  abstract getDyedColor(): RgbColor | null;
}
