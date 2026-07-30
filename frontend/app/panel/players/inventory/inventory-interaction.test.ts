import { describe, expect, it } from "vitest";
import { InventoryType } from "@/lib/types";
import {
  createInventory,
  createInventoryData,
  createItem
} from "@/test/inventory-helper";
import {
  AIR,
  replaceInventoryItem,
  resolveInventoryInteraction
} from "./inventory-interaction";

function interact(overrides: Partial<Parameters<typeof resolveInventoryInteraction>[0]> = {}) {
  return resolveInventoryInteraction({
    button: "left",
    source: "inventory",
    clickedItem: createItem({ slot: 0, count: 8 }),
    heldItem: null,
    maxStackSize: 64,
    ...overrides
  });
}

describe("resolve inventory interaction", () => {
  it("picks up a normal item and empties its slot", () => {
    const clickedItem = createItem({ slot: 5, count: 8 });

    expect(interact({ clickedItem })).toEqual({
      nextHeldItem: clickedItem,
      replacementItem: { slot: 5, id: AIR, count: 0 }
    });
  });

  it("places a held item into an empty slot", () => {
    expect(interact({
      clickedItem: createItem({ slot: 5, id: AIR, count: 0 }),
      heldItem: createItem({ slot: 2, count: 8 })
    })).toEqual({
      nextHeldItem: null,
      replacementItem: createItem({ slot: 5, count: 8 })
    });
  });

  it("swaps different items", () => {
    const clickedItem = createItem({ slot: 5, id: "minecraft:diamond", count: 2 });
    const heldItem = createItem({ slot: 2, count: 8 });

    expect(interact({ clickedItem, heldItem })).toEqual({
      nextHeldItem: clickedItem,
      replacementItem: createItem({ slot: 5, count: 8 })
    });
  });

  it("merges equal stacks up to the maximum stack size", () => {
    expect(interact({
      clickedItem: createItem({ slot: 5, count: 60 }),
      heldItem: createItem({ slot: 2, count: 8 })
    })).toEqual({
      nextHeldItem: createItem({ slot: 2, count: 4 }),
      replacementItem: createItem({ slot: 5, count: 64 })
    });
  });

  it("keeps stacks with different SNBT separate", () => {
    const clickedItem = createItem({ slot: 5, count: 2, snbt: "{foo:1b}" });
    const heldItem = createItem({ slot: 2, count: 3, snbt: "{bar:1b}" });

    expect(interact({ clickedItem, heldItem })).toEqual({
      nextHeldItem: clickedItem,
      replacementItem: { ...heldItem, slot: 5 }
    });
  });

  it("takes the larger half with right click", () => {
    expect(interact({
      button: "right",
      clickedItem: createItem({ slot: 5, count: 9 })
    })).toEqual({
      nextHeldItem: createItem({ slot: 5, count: 5 }),
      replacementItem: createItem({ slot: 5, count: 4 })
    });
  });

  it("places one item with right click", () => {
    expect(interact({
      button: "right",
      clickedItem: createItem({ slot: 5, id: AIR, count: 0 }),
      heldItem: createItem({ slot: 2, count: 3 })
    })).toEqual({
      nextHeldItem: createItem({ slot: 2, count: 2 }),
      replacementItem: createItem({ slot: 5, count: 1 })
    });
  });

  it("places one item into an equal stack without exceeding its maximum", () => {
    expect(interact({
      button: "right",
      clickedItem: createItem({ slot: 5, count: 15 }),
      heldItem: createItem({ slot: 2, count: 3 }),
      maxStackSize: 16
    })).toEqual({
      nextHeldItem: createItem({ slot: 2, count: 2 }),
      replacementItem: createItem({ slot: 5, count: 16 })
    });
    expect(interact({
      button: "right",
      clickedItem: createItem({ slot: 5, count: 16 }),
      heldItem: createItem({ slot: 2, count: 3 }),
      maxStackSize: 16
    })).toEqual({
      nextHeldItem: createItem({ slot: 2, count: 3 })
    });
  });

  it("swaps different items with right click", () => {
    const clickedItem = createItem({ slot: 5, id: "minecraft:diamond", count: 2 });
    const heldItem = createItem({ slot: 2, count: 3 });

    expect(interact({
      button: "right",
      clickedItem,
      heldItem
    })).toEqual({
      nextHeldItem: clickedItem,
      replacementItem: { ...heldItem, slot: 5 }
    });
  });

  it("gets one or a full stack from the explorer", () => {
    const explorerItem = createItem({ slot: -1, count: 1 });

    expect(interact({
      source: "explorer",
      clickedItem: explorerItem
    })).toEqual({ nextHeldItem: explorerItem });
    expect(interact({
      button: "right",
      source: "explorer",
      clickedItem: explorerItem
    })).toEqual({
      nextHeldItem: createItem({ slot: -1, count: 64 })
    });
  });

  it("adds explorer items one at a time only to a matching explorer stack", () => {
    const explorerItem = createItem({ slot: -1, count: 1, snbt: "{foo:1b}" });

    expect(interact({
      source: "explorer",
      clickedItem: explorerItem,
      heldItem: createItem({ slot: -1, count: 15, snbt: "{foo:1b}" }),
      maxStackSize: 16
    })).toEqual({
      nextHeldItem: createItem({ slot: -1, count: 16, snbt: "{foo:1b}" })
    });
    expect(interact({
      source: "explorer",
      clickedItem: explorerItem,
      heldItem: createItem({ slot: 2, count: 15, snbt: "{foo:1b}" }),
      maxStackSize: 16
    })).toEqual({ nextHeldItem: null });
  });

  it("copies a full stack with middle click only from the main inventory", () => {
    const clickedItem = createItem({ slot: 5, count: 3 });

    expect(interact({
      button: "middle",
      clickedItem
    })).toEqual({
      nextHeldItem: createItem({ slot: 5, count: 64 })
    });
    expect(interact({
      button: "middle",
      source: "container",
      clickedItem
    })).toEqual({ nextHeldItem: null });
  });
});

describe("replace inventory item", () => {
  it.each([
    InventoryType.MAIN,
    InventoryType.EQUIPMENTS,
    InventoryType.ENDER_CHEST
  ])("replaces an item in %s", (inventoryType) => {
    const inventory = createInventory();
    const replacement = createItem({ slot: 1, id: "minecraft:diamond", count: 3 });
    const nextInventory = replaceInventoryItem(inventory, inventoryType, replacement);

    expect(nextInventory[inventoryType].items[1]).toEqual(replacement);
  });

  it("normalizes air and non-positive item counts", () => {
    const inventory = createInventory({
      main: createInventoryData(36, {
        items: Array.from({ length: 36 }, (_, slot) => createItem({
          slot,
          id: slot === 2 ? "minecraft:stone" : AIR,
          count: slot === 2 ? 3 : 0
        }))
      })
    });

    expect(replaceInventoryItem(
      inventory,
      InventoryType.MAIN,
      createItem({ slot: 2, id: "minecraft:stone", count: 0, snbt: "{}" })
    ).main.items[2]).toEqual({ slot: 2, id: AIR, count: 0 });
  });

  it("returns the original inventory for an invalid slot", () => {
    const inventory = createInventory();

    expect(replaceInventoryItem(
      inventory,
      InventoryType.MAIN,
      createItem({ slot: -1 })
    )).toBe(inventory);
    expect(replaceInventoryItem(
      inventory,
      InventoryType.MAIN,
      createItem({ slot: inventory.main.size })
    )).toBe(inventory);
  });

  it("only copies the changed branch, items array, and item", () => {
    const inventory = createInventory();
    const unchangedItem = inventory.main.items[2];
    const replacement = createItem({ slot: 1, id: "minecraft:diamond", count: 3 });
    const nextInventory = replaceInventoryItem(inventory, InventoryType.MAIN, replacement);

    expect(nextInventory).not.toBe(inventory);
    expect(nextInventory.main).not.toBe(inventory.main);
    expect(nextInventory.main.items).not.toBe(inventory.main.items);
    expect(nextInventory.main.items[1]).not.toBe(replacement);
    expect(nextInventory.main.items[2]).toBe(unchangedItem);
    expect(nextInventory.equipments).toBe(inventory.equipments);
    expect(nextInventory.enderChest).toBe(inventory.enderChest);
  });
});
