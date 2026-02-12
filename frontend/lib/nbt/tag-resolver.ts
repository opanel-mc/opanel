import {
  type NbtBool,
  type NbtList,
  type NbtNumber,
  NbtObject,
  NbtString
} from "snbt-js";
import {
  type RgbColor,
  type Enchantments,
  ItemNBTResolver,
  glintItems,
} from "./resolver";
import { $, $mc } from "../i18n";
import { potionColors } from "./potion-colors";

export class TagResolver extends ItemNBTResolver {
  private enchantments: Enchantments = new Map();

  constructor(id: string, snbt: string) {
    super(id, snbt);

    // Enchantments
    for(const { childs } of (this.nbt.get<NbtList>("Enchantments")?.childs ?? []) as NbtObject[]) {
      const id = (childs.id as NbtString).value;
      const level = (childs.lvl as NbtNumber).value;
      this.enchantments.set(id, level);
    }
  }

  private hasTag(name: string): boolean {
    return this.nbt.get(name) !== undefined;
  }

  override isEmpty() {
    return !this.nbt || Object.keys(this.nbt).length === 0;
  }

  override getName() {
    const displayNBT = this.nbt.get<NbtObject>("display");
    const customName = displayNBT?.get<NbtObject | NbtString>("Name");
    if(customName instanceof NbtString) {
      return customName.value;
    }
    if(customName instanceof NbtObject) {
      return customName.get<NbtString>("text")?.value ?? $mc(this.id);
    }
    if(this.getPotionId()) {
      return $(`item.minecraft.potion.effect.${this.getPotionId()?.replace("minecraft:", "")}` as any);
    }
    return $mc(this.id);
  }

  override hasCustomName(): boolean {
    return this.hasTag("display") && this.nbt.get<NbtObject>("display")?.get("Name") !== undefined;
  }

  override getEnchantments() {
    return this.enchantments;
  }

  override hasEnchantments() {
    return this.enchantments.size > 0;
  }

  override shouldGlint() {
    const isLodestone = this.hasTag("LodestoneTracked");
    return glintItems.includes(this.id) || this.hasEnchantments() || isLodestone;
  }

  override getDamage() {
    return this.nbt.get<NbtNumber>("Damage")?.value ?? null;
  }

  override isUnbreakable() {
    return this.nbt.get<NbtBool>("Unbreakable")?.value ?? false;
  }

  override isPotion(): boolean {
    return this.hasTag("Potion") || this.hasTag("CustomPotionColor");
  }

  override getPotionId(): string | null {
    if(!this.isPotion()) return null;

    const potionId = this.nbt.get<NbtString>("Potion")?.value ?? "minecraft:empty";
    return potionId.replace(/long_|strong_/g, "");
  }

  override getPotionColor(): RgbColor | null {
    if(!this.isPotion()) return null;

    const customColor = this.nbt.get<NbtNumber>("CustomPotionColor")?.value;
    if(customColor !== undefined) {
      const hexStr = customColor.toString(16).padStart(6, "0");
      const r = parseInt(hexStr.slice(0, 2), 16);
      const g = parseInt(hexStr.slice(2, 4), 16);
      const b = parseInt(hexStr.slice(4, 6), 16);
      return [r, g, b];
    }
    
    const id = this.getPotionId();
    return id ? potionColors[id] : potionColors["minecraft:water"];
  }
}
