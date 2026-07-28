import {
  NbtBool,
  NbtList,
  NbtNumber,
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
    const enchantmentsNBT = this.nbt.get("Enchantments");
    if(enchantmentsNBT instanceof NbtList) {
      for(const enchantment of enchantmentsNBT.childs) {
        if(!(enchantment instanceof NbtObject)) continue;

        const enchantmentId = enchantment.get("id");
        const enchantmentLevel = enchantment.get("lvl");
        if(enchantmentId instanceof NbtString && enchantmentLevel instanceof NbtNumber) {
          this.enchantments.set(enchantmentId.value, enchantmentLevel.value);
        }
      }
    }
  }

  private hasTag(name: string): boolean {
    return this.nbt.get(name) !== undefined;
  }

  override isEmpty() {
    return this.nbt.isempty();
  }

  override getName() {
    const displayNBT = this.nbt.get("display");
    const customName = displayNBT instanceof NbtObject ? displayNBT.get("Name") : undefined;
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
    const displayNBT = this.nbt.get("display");
    if(!(displayNBT instanceof NbtObject)) return false;

    const customName = displayNBT.get("Name");
    return customName instanceof NbtString || customName instanceof NbtObject;
  }

  override getLore(): string[] {
    const displayNBT = this.nbt.get("display");
    if(!(displayNBT instanceof NbtObject)) return [];

    const loreNBT = displayNBT.get("Lore");
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
    const isLodestone = this.hasTag("LodestoneTracked");
    return glintItems.includes(this.id) || this.hasEnchantments() || isLodestone;
  }

  override getDamage() {
    const damage = this.nbt.get("Damage");
    return damage instanceof NbtNumber ? damage.value : null;
  }

  override isUnbreakable() {
    const unbreakable = this.nbt.get("Unbreakable");
    if(unbreakable instanceof NbtBool) return unbreakable.value;
    return unbreakable instanceof NbtNumber && unbreakable.value !== 0;
  }

  override isPotion(): boolean {
    const potion = this.nbt.get("Potion");
    const customColor = this.nbt.get("CustomPotionColor");
    return (potion instanceof NbtString || customColor instanceof NbtNumber) && (
      [
        "minecraft:potion",
        "minecraft:splash_potion",
        "minecraft:lingering_potion"
      ].includes(this.id)
    );
  }

  override isTippedArrow(): boolean {
    const potion = this.nbt.get("Potion");
    const customColor = this.nbt.get("CustomPotionColor");
    return (potion instanceof NbtString || customColor instanceof NbtNumber) && (
      this.id === "minecraft:tipped_arrow"
    );
  }

  override getPotionId(): string | null {
    if(!this.isPotion() && !this.isTippedArrow()) return null;

    const potion = this.nbt.get("Potion");
    const potionId = potion instanceof NbtString ? potion.value : "minecraft:empty";
    return potionId.replace(/long_|strong_/g, "");
  }

  override getPotionColor(): RgbColor | null {
    if(!this.isPotion() && !this.isTippedArrow()) return null;

    const customColor = this.nbt.get("CustomPotionColor");
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
    return null;
  }

  override getMapId(): number | null {
    const mapId = this.nbt.get("map");
    return mapId instanceof NbtNumber ? mapId.value : null;
  }

  override getBeeAmount(): number | null {
    const blockEntityTag = this.nbt.get("BlockEntityTag");
    if(!(blockEntityTag instanceof NbtObject)) return null;

    const bees = blockEntityTag.get("Bees");
    return bees instanceof NbtList ? bees.childs.length : null;
  }

  override getHoneyLevel(): number | null {
    return null;
  }

  override getDyedColor(): RgbColor | null {
    const displayNBT = this.nbt.get("display");
    if(!(displayNBT instanceof NbtObject)) return null;

    const dyedColor = displayNBT.get("color");
    if(!(dyedColor instanceof NbtNumber)) return null;

    const hexStr = dyedColor.value.toString(16).padStart(6, "0");
    const r = parseInt(hexStr.slice(0, 2), 16);
    const g = parseInt(hexStr.slice(2, 4), 16);
    const b = parseInt(hexStr.slice(4, 6), 16);
    return [r, g, b];
  }
}
