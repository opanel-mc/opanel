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
  dragging?: boolean
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
  dragging?: boolean
}

export interface InventoryRightDragState {
  enabled: boolean
  active: boolean
  dragging: boolean
  suppressContextMenu: boolean
  visitedSlots: Set<string>
  startInteraction: ((dragging: boolean) => void) | null
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
  maxStackSize,
  dragging = false
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

  if(dragging && (!heldItem || source === "explorer")) {
    return { nextHeldItem: heldItem };
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

  if(dragging) return { nextHeldItem: heldItem };
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

function resetInventoryRightDrag(state: InventoryRightDragState) {
  state.active = false;
  state.dragging = false;
  state.visitedSlots.clear();
  state.startInteraction = null;
}

export function beginInventoryRightDrag(
  state: InventoryRightDragState,
  slot: string,
  interact: (dragging: boolean) => void
) {
  state.active = true;
  state.dragging = false;
  state.suppressContextMenu = false;
  state.visitedSlots.clear();
  state.visitedSlots.add(slot);
  state.startInteraction = interact;
}

export function visitInventoryRightDrag(
  state: InventoryRightDragState,
  slot: string,
  interact: () => void
) {
  if(!state.enabled || !state.active || state.visitedSlots.has(slot)) return;

  state.visitedSlots.add(slot);
  if(!state.dragging) {
    state.dragging = true;
    state.suppressContextMenu = true;
    state.startInteraction?.(true);
  }
  interact();
}

export function finishInventoryRightDrag(state: InventoryRightDragState) {
  if(!state.active) return;
  if(!state.dragging) state.startInteraction?.(false);
  resetInventoryRightDrag(state);
}

export function cancelInventoryRightDrag(state: InventoryRightDragState) {
  resetInventoryRightDrag(state);
  state.suppressContextMenu = false;
}
