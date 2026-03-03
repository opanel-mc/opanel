import type { ReactNode } from "react";
import type { ItemStack } from "@/lib/types";
import { fireEvent, render } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { VersionContext } from "@/contexts/api-context";
import { InventoryContext } from "@/contexts/inventory-context";
import { createMockVersionContext } from "@/test/contexts-helper";
import { createItem, createMockInventoryContextValue } from "@/test/inventory-helper";
import { AIR, InventoryItem } from "./inventory-item";

vi.mock("@/style/item-effect.css", () => ({}));

vi.mock("./item-dialog", () => ({
  ItemDialog: ({ children }: { children: ReactNode }) => <>{children}</>
}));

function renderInventoryItem(itemStack: ItemStack, options?: {
  held?: boolean,
  ctxOverrides?: Partial<ReturnType<typeof createMockInventoryContextValue>>
}) {
  const ctx = createMockInventoryContextValue(options?.ctxOverrides);
  const elem = render(
    <VersionContext.Provider value={createMockVersionContext()}>
      <InventoryContext.Provider value={ctx}>
        <InventoryItem
          itemStack={itemStack}
          held={options?.held}/>
      </InventoryContext.Provider>
    </VersionContext.Provider>
  );
  const itemElem = elem.container.querySelector("[data-slot='inventory-item']") as HTMLElement;
  expect(itemElem).toBeInTheDocument();

  return { ...elem, itemElem, ctx };
}

function fireMiddleClick(elem: HTMLElement) {
  fireEvent(elem, new MouseEvent("auxclick", { bubbles: true, cancelable: true, button: 1 }));
}

describe("test inventory item", () => {
  it("should pick up and remove clicked item when left-clicking a normal slot with empty hand", () => {
    const item = createItem({ slot: 10, id: "minecraft:stone", count: 8 });
    const { itemElem, ctx } = renderInventoryItem(item);

    fireEvent.click(itemElem);

    expect(ctx.setCurrentlyHeldItem).toHaveBeenCalledWith(item);
    expect(ctx.removeClickedItem).toHaveBeenCalledWith(item);
  });

  it("should pick up clicked item without removing when left-clicking an explorer slot", () => {
    const item = createItem({ slot: -1, id: "minecraft:stone", count: 1 });
    const { itemElem, ctx } = renderInventoryItem(item);

    fireEvent.click(itemElem);

    expect(ctx.setCurrentlyHeldItem).toHaveBeenCalledWith(item);
    expect(ctx.removeClickedItem).not.toHaveBeenCalled();
  });

  it("should merge held stack into clicked stack when left-clicking same item", () => {
    const heldItem = createItem({ slot: 2, id: "minecraft:stone", count: 3, snbt: "{foo:1b}" });
    const clickedItem = createItem({ slot: 12, id: "minecraft:stone", count: 9, snbt: "{foo:1b}" });
    const { itemElem, ctx } = renderInventoryItem(clickedItem, {
      ctxOverrides: {
        currentlyHeldItem: heldItem
      }
    });

    fireEvent.click(itemElem);

    expect(ctx.addClickedWithHeldItem).toHaveBeenCalledWith(clickedItem, 3);
  });

  it("should swap held item with clicked item when left-clicking different item", () => {
    const clickedItem = createItem({ slot: 12, id: "minecraft:diamond", count: 1 });
    const { itemElem, ctx } = renderInventoryItem(clickedItem, {
      ctxOverrides: {
        currentlyHeldItem: createItem({ slot: 2, id: "minecraft:stone", count: 3 })
      }
    });

    fireEvent.click(itemElem);

    expect(ctx.swapClickedWithHeldItem).toHaveBeenCalledWith(clickedItem);
  });

  it("should swap held item with clicked item when ids are same but nbt is different", () => {
    const heldItem = createItem({ slot: 2, id: "minecraft:stone", count: 3, snbt: "{foo:1b}" });
    const clickedItem = createItem({ slot: 12, id: "minecraft:stone", count: 9, snbt: "{bar:1b}" });
    const { itemElem, ctx } = renderInventoryItem(clickedItem, {
      ctxOverrides: {
        currentlyHeldItem: heldItem
      }
    });

    fireEvent.click(itemElem);

    expect(ctx.swapClickedWithHeldItem).toHaveBeenCalledWith(clickedItem);
    expect(ctx.addClickedWithHeldItem).not.toHaveBeenCalled();
  });

  it("should still add all held count even when total count is greater than 64", () => {
    const heldItem = createItem({ slot: 2, id: "minecraft:stone", count: 40, snbt: "{foo:1b}" });
    const clickedItem = createItem({ slot: 12, id: "minecraft:stone", count: 40, snbt: "{foo:1b}" });
    const { itemElem, ctx } = renderInventoryItem(clickedItem, {
      ctxOverrides: {
        currentlyHeldItem: heldItem
      }
    });

    fireEvent.click(itemElem);

    expect(ctx.addClickedWithHeldItem).toHaveBeenCalledWith(clickedItem, 40);
  });

  it("should destroy held item when dropping to explorer with different item type", () => {
    const clickedItem = createItem({ slot: -1, id: "minecraft:diamond", count: 1 });
    const { itemElem, ctx } = renderInventoryItem(clickedItem, {
      ctxOverrides: {
        currentlyHeldItem: createItem({ slot: 2, id: "minecraft:stone", count: 3 })
      }
    });

    fireEvent.click(itemElem);

    expect(ctx.setCurrentlyHeldItem).toHaveBeenCalledWith(null);
    expect(ctx.swapClickedWithHeldItem).not.toHaveBeenCalled();
    expect(ctx.addClickedWithHeldItem).not.toHaveBeenCalled();
  });

  it("should pick up half and update clicked stack when right-clicking normal slot with empty hand", () => {
    const item = createItem({ slot: 4, id: "minecraft:stone", count: 9 });
    const { itemElem, ctx } = renderInventoryItem(item);

    fireEvent.contextMenu(itemElem);

    expect(ctx.setCurrentlyHeldItem).toHaveBeenCalledWith({ ...item, count: 5 });
    expect(ctx.halfClickedItem).toHaveBeenCalledWith(item);
  });

  it("should pick up 64 items from explorer on right-click with empty hand", () => {
    const item = createItem({ slot: -1, id: "minecraft:diamond", count: 1 });
    const { itemElem, ctx } = renderInventoryItem(item);

    fireEvent.contextMenu(itemElem);

    expect(ctx.setCurrentlyHeldItem).toHaveBeenCalledWith({ ...item, count: 64 });
  });

  it("should place one item into same clicked stack on right-click", () => {
    const heldItem = createItem({ slot: 2, id: "minecraft:stone", count: 6, snbt: "{foo:1b}" });
    const clickedItem = createItem({ slot: 4, id: "minecraft:stone", count: 12, snbt: "{foo:1b}" });
    const { itemElem, ctx } = renderInventoryItem(clickedItem, {
      ctxOverrides: {
        currentlyHeldItem: heldItem
      }
    });

    fireEvent.contextMenu(itemElem);

    expect(ctx.addClickedWithHeldItem).toHaveBeenCalledWith(clickedItem, 1);
  });

  it("should place one item per right click when clicking same item multiple times", () => {
    const heldItem = createItem({ slot: 2, id: "minecraft:stone", count: 6, snbt: "{foo:1b}" });
    const clickedItem = createItem({ slot: 4, id: "minecraft:stone", count: 12, snbt: "{foo:1b}" });
    const { itemElem, ctx } = renderInventoryItem(clickedItem, {
      ctxOverrides: {
        currentlyHeldItem: heldItem
      }
    });

    fireEvent.contextMenu(itemElem);
    fireEvent.contextMenu(itemElem);
    fireEvent.contextMenu(itemElem);

    expect(ctx.addClickedWithHeldItem).toHaveBeenCalledTimes(3);
    expect(ctx.addClickedWithHeldItem).toHaveBeenNthCalledWith(1, clickedItem, 1);
    expect(ctx.addClickedWithHeldItem).toHaveBeenNthCalledWith(2, clickedItem, 1);
    expect(ctx.addClickedWithHeldItem).toHaveBeenNthCalledWith(3, clickedItem, 1);
  });

  it("should place one held item into empty slot on right-click", () => {
    const heldItem = createItem({ slot: 2, id: "minecraft:diamond", count: 6, snbt: "{foo:1b}" });
    const clickedItem = createItem({ slot: 4, id: AIR, count: 0 });
    const { itemElem, ctx } = renderInventoryItem(clickedItem, {
      ctxOverrides: {
        currentlyHeldItem: heldItem
      }
    });

    fireEvent.contextMenu(itemElem);

    expect(ctx.addClickedWithHeldItem).toHaveBeenCalledWith(
      { ...clickedItem, id: heldItem.id, snbt: heldItem.snbt },
      1
    );
  });

  it("should clone 64 items when middle-clicking normal slot", () => {
    const item = createItem({ slot: 4, id: "minecraft:diamond", count: 8 });
    const { itemElem, ctx } = renderInventoryItem(item);

    fireMiddleClick(itemElem);

    expect(ctx.setCurrentlyHeldItem).toHaveBeenCalledWith({ ...item, count: 64 });
  });

  it("should increase held item count by one per left click in explorer when ids and nbt are same", () => {
    const clickedItem = createItem({ slot: -1, id: "minecraft:stone", count: 1, snbt: "{foo:1b}" });
    const heldItem = createItem({ slot: -1, id: "minecraft:stone", count: 7, snbt: "{foo:1b}" });
    const { itemElem, ctx } = renderInventoryItem(clickedItem, {
      ctxOverrides: {
        currentlyHeldItem: heldItem
      }
    });

    fireEvent.click(itemElem);
    fireEvent.click(itemElem);

    expect(ctx.setCurrentlyHeldItem).toHaveBeenCalledTimes(2);
    expect(ctx.setCurrentlyHeldItem).toHaveBeenNthCalledWith(1, { ...heldItem, count: 8 });
    expect(ctx.setCurrentlyHeldItem).toHaveBeenNthCalledWith(2, { ...heldItem, count: 8 });
  });

  it("should do nothing when item is held preview", () => {
    const item = createItem({ slot: 4, id: "minecraft:stone", count: 8 });
    const setCurrentlyHeldItem = vi.fn();
    const { itemElem, ctx } = renderInventoryItem(item, {
      held: true,
      ctxOverrides: {
        setCurrentlyHeldItem
      }
    });

    fireEvent.click(itemElem);
    fireEvent.contextMenu(itemElem);
    fireMiddleClick(itemElem);

    expect(setCurrentlyHeldItem).not.toHaveBeenCalled();
    expect(ctx.removeClickedItem).not.toHaveBeenCalled();
    expect(ctx.swapClickedWithHeldItem).not.toHaveBeenCalled();
    expect(ctx.addClickedWithHeldItem).not.toHaveBeenCalled();
    expect(ctx.halfClickedItem).not.toHaveBeenCalled();
  });

  it("should do nothing when nbt edit mode is enabled", () => {
    const item = createItem({ slot: 4, id: "minecraft:stone", count: 8 });
    const setCurrentlyHeldItem = vi.fn();
    const { itemElem, ctx } = renderInventoryItem(item, {
      ctxOverrides: {
        nbtEditMode: true,
        setCurrentlyHeldItem
      }
    });

    fireEvent.click(itemElem);
    fireEvent.contextMenu(itemElem);
    fireMiddleClick(itemElem);

    expect(setCurrentlyHeldItem).not.toHaveBeenCalled();
    expect(ctx.removeClickedItem).not.toHaveBeenCalled();
    expect(ctx.swapClickedWithHeldItem).not.toHaveBeenCalled();
    expect(ctx.addClickedWithHeldItem).not.toHaveBeenCalled();
    expect(ctx.halfClickedItem).not.toHaveBeenCalled();
  });
});
