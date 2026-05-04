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
import { textComponentToString } from "../utils";

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

  override getLore(): string[] {
    const loreNBT = this.nbt.get<NbtList>(["display", "Lore"]);
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
    return (this.hasTag("Potion") || this.hasTag("CustomPotionColor")) && (
      [
        "minecraft:potion",
        "minecraft:splash_potion",
        "minecraft:lingering_potion"
      ].includes(this.id)
    );
  }

  override isTippedArrow(): boolean {
    return (this.hasTag("Potion") || this.hasTag("CustomPotionColor")) && (
      this.id === "minecraft:tipped_arrow"
    );
  }

  override getPotionId(): string | null {
    if(!this.isPotion() && !this.isTippedArrow()) return null;

    const potionId = this.nbt.get<NbtString>("Potion")?.value ?? "minecraft:empty";
    return potionId.replace(/long_|strong_/g, "");
  }

  override getPotionColor(): RgbColor | null {
    if(!this.isPotion() && !this.isTippedArrow()) return null;

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

  override getItemModel(): string | null {
    return null;
  }

  override getMapId(): number | null {
    const mapId = this.nbt.get<NbtNumber>("map")?.value;
    return mapId !== undefined ? mapId : null;
  }

  override getBeeAmount(): number | null {
    const beeAmount = this.nbt.get<NbtList>(["BlockEntityTag", "Bees"])?.childs.length;
    return beeAmount !== undefined ? beeAmount : null;
  }

  override getHoneyLevel(): number | null {
    return null;
  }

  override getDyedColor(): RgbColor | null {
    const dyedColor = this.nbt.get<NbtNumber>(["display", "color"]);
    if(dyedColor === undefined) return null;

    const hexStr = dyedColor.value.toString(16).padStart(6, "0");
    const r = parseInt(hexStr.slice(0, 2), 16);
    const g = parseInt(hexStr.slice(2, 4), 16);
    const b = parseInt(hexStr.slice(4, 6), 16);
    return [r, g, b];
  }
}
