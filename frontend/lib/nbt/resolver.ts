// export type Color = "white" | "orange" | "magenta" | "light_blue" | "yellow" | "lime" | "pink" | "gray" | "light_gray" | "cyan" | "purple" | "blue" | "brown" | "green" | "red" | "black";
export type RgbColor = [number, number, number];

export type Enchantments = Map<string, number>;

export interface PotionData {
  id: string
  durationScale: number
}

export abstract class ItemNBTResolver {
  constructor(protected nbt?: any) { }
  abstract isEmpty(): boolean;
  abstract getEnchantments(): Enchantments;
  abstract hasEnchantments(): boolean;
  abstract shouldGlint(): boolean;
  abstract getDamage(): number | null;
  abstract isUnbreakable(): boolean;
  abstract isPotion(): boolean;
  abstract getPotionData(): PotionData | null;
  abstract getPotionColor(): RgbColor | null;
}
