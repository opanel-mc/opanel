import type { ItemNBTResolver } from "../nbt/resolver";
import { describe, expect, it } from "vitest";
import {
  ComponentsResolver,
  itemModelToTextureId
} from "../nbt/components-resolver";
import { TagResolver } from "../nbt/tag-resolver";

function exerciseResolver(resolver: ItemNBTResolver) {
  resolver.isEmpty();
  resolver.getName();
  resolver.hasCustomName();
  resolver.getLore();
  resolver.getEnchantments();
  resolver.hasEnchantments();
  resolver.shouldGlint();
  resolver.getDamage();
  resolver.isUnbreakable();
  resolver.isPotion();
  resolver.isTippedArrow();
  resolver.getPotionId();
  resolver.getPotionColor();
  resolver.getItemModel();
  resolver.getMapId();
  resolver.getBeeAmount();
  resolver.getHoneyLevel();
  resolver.isDyedLeatherArmor();
  resolver.getDyedColor();
}

describe("ComponentsResolver", () => {
  it.each([
    "",
    "1",
    "true",
    "[]",
    `{"minecraft:block_state":1}`,
    `{"minecraft:enchantments":1}`,
    `{"minecraft:lore":1}`,
    `{"minecraft:potion_contents":1}`,
    `{"minecraft:potion_contents":{potion:1}}`,
    `{"minecraft:potion_contents":{custom_color:{}}}`,
    `{"minecraft:bees":1}`,
    `{"minecraft:item_model":1}`,
    `{"minecraft:dyed_color":[{},0.5f,0.5f]}`
  ])("safely ignores malformed NBT: %s", (snbt) => {
    expect(() => {
      const resolver = new ComponentsResolver("minecraft:potion", snbt);
      exerciseResolver(resolver);
      resolver.getComponentAmount();
    }).not.toThrow();
  });

  it("falls back to empty NBT when parsing fails or the root is not a compound", () => {
    expect(new ComponentsResolver("minecraft:stone", "").isEmpty()).toBe(true);
    expect(new ComponentsResolver("minecraft:stone", "1").isEmpty()).toBe(true);
    expect(new ComponentsResolver("minecraft:stone", "true").isEmpty()).toBe(true);
    expect(new ComponentsResolver("minecraft:stone", "[]").isEmpty()).toBe(true);
  });

  it("keeps resolving valid components", () => {
    const resolver = new ComponentsResolver(
      "minecraft:diamond_sword",
      `{
        "minecraft:enchantments":{levels:{"minecraft:sharpness":5}},
        "minecraft:damage":2,
        "minecraft:enchantment_glint_override":1b
      }`
    );

    expect(resolver.getEnchantments()).toEqual(new Map([["minecraft:sharpness", 5]]));
    expect(resolver.getDamage()).toBe(2);
    expect(resolver.shouldGlint()).toBe(true);
  });

  it("rejects a non-string item model at runtime", () => {
    const resolver = new ComponentsResolver("minecraft:stone", `{"minecraft:item_model":1}`);

    expect(resolver.getItemModel()).toBeNull();
    expect(itemModelToTextureId(1 as never)).toBeNull();
  });
});

describe("TagResolver", () => {
  it.each([
    "",
    "1",
    "true",
    "[]",
    `{Enchantments:{}}`,
    `{Enchantments:[{}]}`,
    `{Enchantments:[{id:1,lvl:1}]}`,
    `{display:1}`,
    `{display:{Lore:1}}`,
    `{Potion:1}`,
    `{BlockEntityTag:1}`,
    `{BlockEntityTag:{Bees:1}}`,
    `{display:{color:{}}}`
  ])("safely ignores malformed NBT: %s", (snbt) => {
    expect(() => {
      const resolver = new TagResolver("minecraft:potion", snbt);
      exerciseResolver(resolver);
    }).not.toThrow();
  });

  it("falls back to empty NBT when parsing fails or the root is not a compound", () => {
    expect(new TagResolver("minecraft:stone", "").isEmpty()).toBe(true);
    expect(new TagResolver("minecraft:stone", "1").isEmpty()).toBe(true);
    expect(new TagResolver("minecraft:stone", "true").isEmpty()).toBe(true);
    expect(new TagResolver("minecraft:stone", "[]").isEmpty()).toBe(true);
  });

  it("keeps resolving valid legacy tags", () => {
    const resolver = new TagResolver(
      "minecraft:diamond_sword",
      `{
        Enchantments:[{id:"minecraft:sharpness",lvl:5s}],
        display:{Name:"Sword",Lore:["Line"]},
        Unbreakable:1b
      }`
    );

    expect(resolver.getEnchantments()).toEqual(new Map([["minecraft:sharpness", 5]]));
    expect(resolver.getName()).toBe("Sword");
    expect(resolver.getLore()).toEqual(["Line"]);
    expect(resolver.isUnbreakable()).toBe(true);
  });
});
