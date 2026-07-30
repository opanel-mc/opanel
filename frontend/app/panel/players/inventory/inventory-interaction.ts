import type {
  InventoryType,
  ItemStack,
  PlayerInventory
} from "@/lib/types";

export const AIR = "minecraft:air";

export type InventoryInteractionButton = "left" | "right" | "middle";
export type InventoryInteractionSource = "inventory" | "explorer" | "container";

export interface InventoryInteractionInput {
  button: InventoryInteractionButton
  source: InventoryInteractionSource
  clickedItem: ItemStack
  heldItem: ItemStack | null
  maxStackSize: number
}

export interface InventoryInteractionResult {
  nextHeldItem: ItemStack | null
  replacementItem?: ItemStack
}

export interface InventoryInteractionRequest {
  button: InventoryInteractionButton
  source: InventoryInteractionSource
  clickedItem: ItemStack
  maxStackSize: number
  inventoryType?: InventoryType
}

function emptyItem(slot: number): ItemStack {
  return { slot, id: AIR, count: 0 };
}

function sameItem(a: ItemStack, b: ItemStack) {
  return a.id === b.id && a.snbt === b.snbt;
}

function transferableCount(targetCount: number, requestedCount: number, maxStackSize: number) {
  return Math.max(0, Math.min(requestedCount, maxStackSize - targetCount));
}

function subtractHeldItem(heldItem: ItemStack, count: number) {
  const nextCount = heldItem.count - count;
  return nextCount > 0 ? { ...heldItem, count: nextCount } : null;
}

function swapWithHeldItem(clickedItem: ItemStack, heldItem: ItemStack): InventoryInteractionResult {
  return {
    nextHeldItem: clickedItem.id === AIR ? null : clickedItem,
    replacementItem: { ...heldItem, slot: clickedItem.slot }
  };
}

export function resolveInventoryInteraction({
  button,
  source,
  clickedItem,
  heldItem,
  maxStackSize
}: InventoryInteractionInput): InventoryInteractionResult {
  if(button === "middle") {
    if(source !== "inventory" || clickedItem.id === AIR) {
      return { nextHeldItem: heldItem };
    }
    return {
      nextHeldItem: { ...clickedItem, count: maxStackSize }
    };
  }

  if(button === "left") {
    if(!heldItem) {
      if(clickedItem.id === AIR) return { nextHeldItem: null };
      return {
        nextHeldItem: clickedItem,
        replacementItem: source === "explorer"
          ? undefined
          : emptyItem(clickedItem.slot)
      };
    }

    if(source === "explorer") {
      if(heldItem.slot === -1 && sameItem(clickedItem, heldItem)) {
        const count = transferableCount(heldItem.count, 1, maxStackSize);
        return {
          nextHeldItem: count > 0
            ? { ...heldItem, count: heldItem.count + count }
            : heldItem
        };
      }
      return { nextHeldItem: null };
    }

    if(sameItem(clickedItem, heldItem)) {
      const count = transferableCount(clickedItem.count, heldItem.count, maxStackSize);
      if(count <= 0) return { nextHeldItem: heldItem };
      return {
        nextHeldItem: subtractHeldItem(heldItem, count),
        replacementItem: { ...clickedItem, count: clickedItem.count + count }
      };
    }

    return swapWithHeldItem(clickedItem, heldItem);
  }

  if(!heldItem) {
    if(source === "explorer") {
      return {
        nextHeldItem: { ...clickedItem, count: maxStackSize }
      };
    }
    if(clickedItem.id === AIR) return { nextHeldItem: null };

    const remainingCount = Math.floor(clickedItem.count / 2);
    return {
      nextHeldItem: { ...clickedItem, count: Math.ceil(clickedItem.count / 2) },
      replacementItem: remainingCount > 0
        ? { ...clickedItem, count: remainingCount }
        : emptyItem(clickedItem.slot)
    };
  }

  if(source === "explorer") {
    return { nextHeldItem: null };
  }

  if(sameItem(clickedItem, heldItem)) {
    const count = transferableCount(clickedItem.count, 1, maxStackSize);
    if(count <= 0) return { nextHeldItem: heldItem };
    return {
      nextHeldItem: subtractHeldItem(heldItem, count),
      replacementItem: { ...clickedItem, count: clickedItem.count + count }
    };
  }

  if(clickedItem.id === AIR) {
    return {
      nextHeldItem: subtractHeldItem(heldItem, 1),
      replacementItem: {
        ...heldItem,
        slot: clickedItem.slot,
        count: 1
      }
    };
  }

  return swapWithHeldItem(clickedItem, heldItem);
}

export function replaceInventoryItem(
  inventory: PlayerInventory,
  inventoryType: InventoryType,
  item: ItemStack
): PlayerInventory {
  const inventoryData = inventory[inventoryType];
  if(
    item.slot < 0
    || item.slot >= inventoryData.size
    || item.slot >= inventoryData.items.length
  ) {
    return inventory;
  }

  const replacement = item.id === AIR || item.count <= 0
    ? { slot: item.slot, id: AIR, count: 0 }
    : { ...item };
  const currentItem = inventoryData.items[item.slot];

  if(
    currentItem?.slot === replacement.slot
    && currentItem.id === replacement.id
    && currentItem.count === replacement.count
    && currentItem.snbt === replacement.snbt
  ) {
    return inventory;
  }

  const items = [...inventoryData.items];
  items[item.slot] = replacement;

  return {
    ...inventory,
    [inventoryType]: {
      ...inventoryData,
      items
    }
  };
}
