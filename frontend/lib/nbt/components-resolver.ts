import {
  type NbtBool,
  type NbtNumber,
  NbtObject,
  NbtString
} from "snbt-js";
import { potionColors } from "./potion-colors";
import {
  type RgbColor,
  type Enchantments,
  ItemNBTResolver,
  glintItems,
} from "./resolver";
import { $, $mc } from "../i18n";

export class ComponentsResolver extends ItemNBTResolver {
  private enchantments: Enchantments = new Map();

  constructor(id: string, snbt: string) {
    super(id, snbt);

    // Enchantments
    const enchantmentsNBT = (
      this.nbt.get<NbtObject>(["minecraft:enchantments", "levels"]) ??
      this.nbt.get<NbtObject>("minecraft:enchantments")
    );
    for(const [id, level] of Object.entries(enchantmentsNBT?.childs ?? {})) {
      this.enchantments.set(id, (level as NbtNumber).value);
    }
  }

  private hasComponent(name: string): boolean {
    return this.nbt.get(name) !== undefined;
  }

  getComponentAmount(): number {
    return Object.keys(this.nbt.childs).length;
  }

  override isEmpty() {
    return !this.nbt || Object.keys(this.nbt).length === 0;
  }

  override getName() {
    const customName = this.nbt.get<NbtObject | NbtString>("minecraft:custom_name");
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
    return this.hasComponent("minecraft:custom_name");
  }

  override getEnchantments() {
    return this.enchantments;
  }

  override hasEnchantments() {
    return this.enchantments.size > 0;
  }

  override shouldGlint() {
    const glintOverride = this.nbt.get<NbtBool>("minecraft:enchantment_glint_override")?.value ?? false;
    const isLodestone = this.hasComponent("minecraft:lodestone_tracker");
    return glintItems.includes(this.id) || this.hasEnchantments() || glintOverride || isLodestone;
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

  override getPotionId(): string | null {
    if(!this.isPotion()) return null;

    const potionId = this.nbt.get<NbtString>(["minecraft:potion_contents", "potion"])?.value ?? "minecraft:empty";
    return potionId.replace(/long_|strong_/g, "");
  }

  override getPotionColor(): RgbColor | null {
    if(!this.isPotion()) return null;

    const customColor = this.nbt.get<NbtNumber>(["minecraft:potion_contents", "custom_color"]);
    if(customColor !== undefined) {
      const hexStr = customColor.value.toString(16).padStart(6, "0");
      const r = parseInt(hexStr.slice(0, 2), 16);
      const g = parseInt(hexStr.slice(2, 4), 16);
      const b = parseInt(hexStr.slice(4, 6), 16);
      return [r, g, b];
    }

    const id = this.getPotionId();
    return id ? potionColors[id] : potionColors["minecraft:water"];
  }
}
