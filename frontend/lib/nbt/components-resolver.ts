import {
  type NbtValue,
  NbtBool,
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
  if(typeof model !== "string" || !model) return null;

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
    const blockStateNBT = this.nbt.get("minecraft:block_state");
    if(blockStateNBT instanceof NbtObject) {
      for(const [key, value] of Object.entries(blockStateNBT.childs)) {
        this.blockState.set(key, value);
      }
    }

    // Enchantments
    const enchantmentsComponent = this.nbt.get("minecraft:enchantments");
    if(enchantmentsComponent instanceof NbtObject) {
      const levels = enchantmentsComponent.get("levels");
      const enchantmentsNBT = levels instanceof NbtObject ? levels : enchantmentsComponent;
      for(const [id, level] of Object.entries(enchantmentsNBT.childs)) {
        if(level instanceof NbtNumber) {
          this.enchantments.set(id, level.value);
        }
      }
    }
  }

  private hasComponent(name: string): boolean {
    return this.nbt.get(name) !== undefined;
  }

  private getBlockState(state: string): NbtValue | null {
    const value = this.blockState.get(state);
    if(value === undefined) return null;
    return value;
  }

  private getPotionContents(): NbtObject | null {
    const potionContents = this.nbt.get("minecraft:potion_contents");
    return potionContents instanceof NbtObject ? potionContents : null;
  }

  getComponentAmount(): number {
    return Object.keys(this.nbt.childs).length;
  }

  override isEmpty() {
    return this.nbt.isempty();
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
    const customName = this.nbt.get("minecraft:custom_name");
    return customName instanceof NbtString || customName instanceof NbtObject;
  }

  override getLore(): string[] {
    const loreNBT = this.nbt.get("minecraft:lore");
    if(!(loreNBT instanceof NbtList)) return [];

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
    const glintOverrideNBT = this.nbt.get("minecraft:enchantment_glint_override");
    const glintOverride = (
      glintOverrideNBT instanceof NbtBool
      ? glintOverrideNBT.value
      : glintOverrideNBT instanceof NbtNumber && glintOverrideNBT.value !== 0
    );
    const isLodestone = this.hasComponent("minecraft:lodestone_tracker");
    return glintItems.includes(this.id) || this.hasEnchantments() || glintOverride || isLodestone;
  }

  override getDamage() {
    const damage = this.nbt.get("minecraft:damage");
    return damage instanceof NbtNumber ? damage.value : null;
  }

  override isUnbreakable() {
    return this.hasComponent("minecraft:unbreakable");
  }

  override isPotion(): boolean {
    return this.getPotionContents() !== null && (
      [
        "minecraft:potion",
        "minecraft:splash_potion",
        "minecraft:lingering_potion"
      ].includes(this.id)
    );
  }

  override isTippedArrow(): boolean {
    return this.getPotionContents() !== null && this.id === "minecraft:tipped_arrow";
  }

  override getPotionId(): string | null {
    if(!this.isPotion() && !this.isTippedArrow()) return null;

    const potion = this.getPotionContents()?.get("potion");
    const potionId = potion instanceof NbtString ? potion.value : "minecraft:empty";
    return potionId.replace(/long_|strong_/g, "");
  }

  override getPotionColor(): RgbColor | null {
    if(!this.isPotion() && !this.isTippedArrow()) return null;

    const customColor = this.getPotionContents()?.get("custom_color");
    if(customColor instanceof NbtNumber) {
      const hexStr = customColor.value.toString(16).padStart(6, "0");
      const r = parseInt(hexStr.slice(0, 2), 16);
      const g = parseInt(hexStr.slice(2, 4), 16);
      const b = parseInt(hexStr.slice(4, 6), 16);
      return [r, g, b];
    }

    const id = this.getPotionId();
    return id ? (potionColors[id] ?? potionColors["minecraft:water"]) : potionColors["minecraft:water"];
  }

  override getItemModel(): string | null {
    const model = this.nbt.get("minecraft:item_model");
    return model instanceof NbtString ? model.value : null;
  }

  override getMapId(): number | null {
    const mapId = this.nbt.get("minecraft:map_id");
    return mapId instanceof NbtNumber ? mapId.value : null;
  }

  override getBeeAmount(): number | null {
    const bees = this.nbt.get("minecraft:bees");
    return bees instanceof NbtList ? bees.childs.length : null;
  }

  override getHoneyLevel(): number | null {
    const honeyLevel = this.getBlockState("honey_level");
    return honeyLevel instanceof NbtNumber ? honeyLevel.value : null;
  }

  override getDyedColor(): RgbColor | null {
    const dyedColor = this.nbt.get("minecraft:dyed_color");
    if(dyedColor instanceof NbtNumber) {
      const hexStr = dyedColor.value.toString(16).padStart(6, "0");
      const r = parseInt(hexStr.slice(0, 2), 16);
      const g = parseInt(hexStr.slice(2, 4), 16);
      const b = parseInt(hexStr.slice(4, 6), 16);
      return [r, g, b];
    }
    if(dyedColor instanceof NbtList) {
      if(dyedColor.childs.length < 3) return null;
      const [red, green, blue] = dyedColor.childs;
      if(
        !(red instanceof NbtNumber)
        || !(green instanceof NbtNumber)
        || !(blue instanceof NbtNumber)
      ) return null;

      const r = Math.min(255, red.value * 255);
      const g = Math.min(255, green.value * 255);
      const b = Math.min(255, blue.value * 255);
      return [r, g, b];
    }
    return null;
  }
}
