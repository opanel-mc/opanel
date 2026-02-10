import { potionColors } from "./potion-colors";
import {
  type RgbColor,
  type Enchantments,
  type PotionData,
  ItemNBTResolver,
} from "./resolver";

const minecraftNamespace = "minecraft:";

export class ComponentsResolver extends ItemNBTResolver {
  private enchantments: Enchantments = new Map();

  constructor(nbt?: any) {
    super(nbt);

    // Enchantments
    for(const [id, level] of Object.entries(this.getComponentNBT("enchantments", {}))) {
      this.enchantments.set(id, level as number);
    }
  }

  private getComponentNBT<V>(name: string, defaultValue: V): V {
    const componentKey = minecraftNamespace + name;
    if(!this.nbt || !this.nbt[componentKey]) {
      return defaultValue;
    }
    return this.nbt[componentKey] as V;
  }

  private hasComponentNBT(name: string): boolean {
    const componentKey = minecraftNamespace + name;
    return !!this.nbt && !!this.nbt[componentKey];
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
    const glintOverride = this.getComponentNBT<boolean>("enchantment_glint_override", false);
    const isLodestone = this.hasComponentNBT("lodestone_tracker");
    return this.hasEnchantments() || glintOverride || isLodestone;
  }

  override getDamage() {
    return this.getComponentNBT<number | null>("damage", null);
  }

  override isUnbreakable() {
    return this.getComponentNBT<boolean>("unbreakable", false);
  }

  override isPotion(): boolean {
    return this.hasComponentNBT("potion_contents");
  }

  override getPotionData(): PotionData | null {
    if(!this.isPotion()) return null;

    return {
      id: this.getComponentNBT<any>("potion_contents", null).potion,
      durationScale: this.getComponentNBT<number>("potion_duration_scale", 1)
    };
  }

  override getPotionColor(): RgbColor | null {
    if(!this.isPotion()) return null;

    const potionNBT = this.getComponentNBT<any>("potion_contents", null);
    if(potionNBT.custom_color) {
      const hexStr = potionNBT.custom_color.toString(16).padStart(6, "0");
      const r = parseInt(hexStr.slice(0, 2), 16);
      const g = parseInt(hexStr.slice(2, 4), 16);
      const b = parseInt(hexStr.slice(4, 6), 16);
      return [r, g, b];
    }

    if(potionNBT.potion) {
      return potionColors[potionNBT.potion] ?? potionColors["minecraft:water"];
    }
    return potionColors["minecraft:water"];
  }
}
