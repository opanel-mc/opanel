import type { NbtBool, NbtNumber, NbtObject, NbtString } from "snbt-js";
import { potionColors } from "./potion-colors";
import {
  type RgbColor,
  type Enchantments,
  type PotionData,
  ItemNBTResolver,
} from "./resolver";

export class ComponentsResolver extends ItemNBTResolver {
  private enchantments: Enchantments = new Map();

  constructor(snbt: string) {
    super(snbt);

    // Enchantments
    for(const [id, level] of Object.entries(this.nbt.get<NbtObject>("minecraft:enchantments")?.childs ?? {})) {
      this.enchantments.set(id, (level as NbtNumber).value);
    }
  }

  private hasComponent(name: string): boolean {
    return this.nbt.get(name) !== undefined;
  }

  override isEmpty() {
    return !this.nbt || Object.keys(this.nbt).length === 0;
  }

  override getEnchantments() {
    return this.enchantments;
  }

  override hasEnchantments() {
    return this.enchantments.size > 0;
  }

  override shouldGlint() {
    const glintOverride = Boolean(this.nbt.get<NbtBool>("minecraft:enchantment_glint_override")?.value);
    const isLodestone = this.hasComponent("minecraft:lodestone_tracker");
    return this.hasEnchantments() || glintOverride || isLodestone;
  }

  override getDamage() {
    return this.nbt.get<NbtNumber>("minecraft:damage")?.value ?? null;
  }

  override isUnbreakable() {
    return this.hasComponent("minecraft:unbreakable");
  }

  override isPotion(): boolean {
    return this.hasComponent("minecraft:potion_contents");
  }

  override getPotionData(): PotionData | null {
    if(!this.isPotion()) return null;

    return {
      id: (this.nbt.get<NbtObject>("minecraft:potion_contents")?.childs.potion as NbtString).value,
      durationScale: this.nbt.get<NbtNumber>("minecraft:potion_duration_scale")?.value ?? 1
    };
  }

  override getPotionColor(): RgbColor | null {
    if(!this.isPotion()) return null;

    const potionNBT = this.nbt.get<NbtObject>("minecraft:potion_contents")?.childs;
    if(potionNBT?.custom_color !== undefined) {
      const hexStr = (potionNBT.custom_color as NbtNumber).value.toString(16).padStart(6, "0");
      const r = parseInt(hexStr.slice(0, 2), 16);
      const g = parseInt(hexStr.slice(2, 4), 16);
      const b = parseInt(hexStr.slice(4, 6), 16);
      return [r, g, b];
    }

    if(potionNBT?.potion) {
      return potionColors[(potionNBT.potion as NbtString).value] ?? potionColors["minecraft:water"];
    }
    return potionColors["minecraft:water"];
  }
}
