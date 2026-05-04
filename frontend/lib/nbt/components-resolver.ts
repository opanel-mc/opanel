import {
  type NbtBool,
  type NbtValue,
  NbtList,
  NbtNumber,
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
import { textComponentToString } from "../utils";

/**
 * Converts the item model's model string to a texture item ID
 *
 * @example
 * - minecraft:stone -> minecraft:stone
 * - minecraft:item/stone -> minecraft:stone
 * - stone -> minecraft:stone
 */
export function itemModelToTextureId(model: string | null): string | null {
  if(!model) return null;

  const colon = model.indexOf(":");
  const namespace = colon === -1 ? "minecraft" : model.slice(0, colon);
  const path = colon === -1 ? model : model.slice(colon + 1);
  const pathParts = path.split("/");
  if(pathParts.length < 2) return `${namespace}:${path}`;
  return `${namespace}:${pathParts.slice(1).join("/")}`;
}

export class ComponentsResolver extends ItemNBTResolver {
  private blockState: Map<string, NbtValue> = new Map();
  private enchantments: Enchantments = new Map();

  constructor(id: string, snbt: string) {
    super(id, snbt);

    // Block State
    const blockStateNBT = this.nbt.get<NbtObject>("minecraft:block_state");
    if(blockStateNBT) {
      for(const [key, value] of Object.entries(blockStateNBT.childs)) {
        this.blockState.set(key, value);
      }
    }

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

  private getBlockState<T extends NbtValue>(state: string): T | null {
    const value = this.blockState.get(state);
    if(value === undefined) return null;
    return value as T;
  }

  getComponentAmount(): number {
    return Object.keys(this.nbt.childs).length;
  }

  override isEmpty() {
    return !this.nbt || Object.keys(this.nbt).length === 0;
  }

  override getName() {
    const customName = this.nbt.get<NbtObject | NbtString>("minecraft:custom_name");
    if(customName instanceof NbtString || customName instanceof NbtObject) {
      return textComponentToString(customName) ?? $mc(this.id);
    }
    if(this.getPotionId()) {
      return $(`item.minecraft.potion.effect.${this.getPotionId()?.replace("minecraft:", "")}` as any);
    }
    return $mc(this.id);
  }

  override hasCustomName(): boolean {
    return this.hasComponent("minecraft:custom_name");
  }

  override getLore(): string[] {
    const loreNBT = this.nbt.get<NbtList>("minecraft:lore");
    if(!loreNBT) return [];

    const lore: string[] = [];
    for(const item of loreNBT.childs) {
      const loreStr = textComponentToString(item as NbtObject | NbtString);
      if(loreStr !== null) {
        lore.push(loreStr);
      }
    }
    return lore;
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
    return this.hasComponent("minecraft:potion_contents") && (
      [
        "minecraft:potion",
        "minecraft:splash_potion",
        "minecraft:lingering_potion"
      ].includes(this.id)
    );
  }

  override isTippedArrow(): boolean {
    return this.hasComponent("minecraft:potion_contents") && this.id === "minecraft:tipped_arrow";
  }

  override getPotionId(): string | null {
    if(!this.isPotion() && !this.isTippedArrow()) return null;

    const potionId = this.nbt.get<NbtString>(["minecraft:potion_contents", "potion"])?.value ?? "minecraft:empty";
    return potionId.replace(/long_|strong_/g, "");
  }

  override getPotionColor(): RgbColor | null {
    if(!this.isPotion() && !this.isTippedArrow()) return null;

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

  override getItemModel(): string | null {
    const model = this.nbt.get<NbtString>("minecraft:item_model")?.value;
    return model ?? null;
  }

  override getMapId(): number | null {
    const mapId = this.nbt.get<NbtNumber>("minecraft:map_id")?.value;
    return mapId !== undefined ? mapId : null;
  }

  override getBeeAmount(): number | null {
    const beeAmount = this.nbt.get<NbtList>("minecraft:bees")?.childs.length;
    return beeAmount !== undefined ? beeAmount : null;
  }

  override getHoneyLevel(): number | null {
    return this.getBlockState<NbtNumber>("honey_level")?.value ?? null;
  }

  override getDyedColor(): RgbColor | null {
    const dyedColor = this.nbt.get<NbtNumber | NbtList>("minecraft:dyed_color");
    if(dyedColor instanceof NbtNumber) {
      const hexStr = dyedColor.value.toString(16).padStart(6, "0");
      const r = parseInt(hexStr.slice(0, 2), 16);
      const g = parseInt(hexStr.slice(2, 4), 16);
      const b = parseInt(hexStr.slice(4, 6), 16);
      return [r, g, b];
    }
    if(dyedColor instanceof NbtList) {
      if(dyedColor.childs.length < 3) return null;
      const r = Math.min(255, (dyedColor.childs[0] as NbtNumber).value * 255);
      const g = Math.min(255, (dyedColor.childs[1] as NbtNumber).value * 255);
      const b = Math.min(255, (dyedColor.childs[2] as NbtNumber).value * 255);
      return [r, g, b];
    }
    return null;
  }
}
