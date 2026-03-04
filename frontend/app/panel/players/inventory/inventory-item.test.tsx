import type { ReactNode } from "react";
import type { ItemStack } from "@/lib/types";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { VersionContext } from "@/contexts/api-context";
import { InventoryContext } from "@/contexts/inventory-context";
import { createMockVersionContext } from "@/test/contexts-helper";
import { createItem, createMockInventoryContextValue } from "@/test/inventory-helper";
import { AIR, InventoryItem } from "./inventory-item";

vi.mock("./item-dialog", () => ({
  ItemDialog: ({ children }: { children: ReactNode }) => <>{children}</>
}));

const { MockComponentsResolver, mockResolverRef } = vi.hoisted(() => {
  class HoistedMockComponentsResolver {
    constructor(private readonly componentAmount: number) {}

    getComponentAmount() {
      return this.componentAmount;
    }
  }

  return {
    MockComponentsResolver: HoistedMockComponentsResolver,
    mockResolverRef: {
      current: null as any
    }
  };
});

vi.mock("@/lib/nbt", () => ({
  createResolver: vi.fn(() => mockResolverRef.current)
}));

vi.mock("@/lib/nbt/components-resolver", () => ({
  ComponentsResolver: MockComponentsResolver,
  itemModelToTextureId: vi.fn((model: string | null) => model)
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
  afterEach(() => {
    cleanup();
  });

  beforeEach(() => {
    mockResolverRef.current = {
      getItemModel: () => null,
      shouldGlint: () => false,
      isPotion: () => false,
      isTippedArrow: () => false,
      isDyedLeatherArmor: () => false,
      getPotionColor: () => null,
      getDyedColor: () => null,
      hasCustomName: () => false,
      hasEnchantments: () => false,
      getName: () => "Stone",
      getEnchantments: () => new Map(),
      getLore: () => [],
      isUnbreakable: () => false,
      getMapId: () => null
    };
  });

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

  it("should render glint overlay when resolver says it should glint", async () => {
    mockResolverRef.current = {
      ...mockResolverRef.current,
      shouldGlint: () => true
    };
    const item = createItem({ slot: 4, id: "minecraft:stone", count: 1, snbt: "{foo:1b}" });
    const { container } = renderInventoryItem(item);

    await waitFor(() => {
      expect(container.querySelector(".item-glint")).toBeInTheDocument();
    });
  });

  it("should render potion overlay when resolver marks item as potion", async () => {
    mockResolverRef.current = {
      ...mockResolverRef.current,
      isPotion: () => true,
      getPotionColor: () => [12, 34, 56]
    };
    const item = createItem({ slot: 4, id: "minecraft:potion", count: 1, snbt: "{foo:1b}" });
    const { container } = renderInventoryItem(item, {
      ctxOverrides: {
        textures: [
          { id: "minecraft:potion", readable: "Potion", texture: "/potion.png" }
        ] as any
      }
    });

    await waitFor(() => {
      const overlays = container.querySelectorAll(".color-overlay");
      expect(overlays.length).toBe(1);
      expect(overlays[0]).toHaveStyle("background-color: rgb(12,34,56)");
    });
  });

  it("should render tipped arrow overlay when resolver marks item as tipped arrow", async () => {
    mockResolverRef.current = {
      ...mockResolverRef.current,
      isTippedArrow: () => true,
      getPotionColor: () => [1, 2, 3]
    };
    const item = createItem({ slot: 4, id: "minecraft:tipped_arrow", count: 1, snbt: "{foo:1b}" });
    const { container } = renderInventoryItem(item, {
      ctxOverrides: {
        textures: [
          { id: "minecraft:tipped_arrow", readable: "Tipped Arrow", texture: "/tipped-arrow.png" }
        ] as any
      }
    });

    await waitFor(() => {
      const overlays = container.querySelectorAll(".color-overlay");
      expect(overlays.length).toBe(1);
      expect(overlays[0]).toHaveStyle("background-color: rgb(1,2,3)");
    });
  });

  it("should render leather armor overlay when resolver has dyed armor color", async () => {
    mockResolverRef.current = {
      ...mockResolverRef.current,
      isDyedLeatherArmor: () => true,
      getDyedColor: () => [45, 67, 89]
    };
    const item = createItem({ slot: 4, id: "minecraft:leather_helmet", count: 1, snbt: "{foo:1b}" });
    const { container } = renderInventoryItem(item, {
      ctxOverrides: {
        textures: [
          { id: "minecraft:leather_helmet", readable: "Leather Helmet", texture: "/leather-helmet.png" }
        ] as any
      }
    });

    await waitFor(() => {
      const overlays = container.querySelectorAll(".color-overlay");
      expect(overlays.length).toBe(1);
      expect(overlays[0]).toHaveStyle("background-color: rgb(45,67,89)");
    });
  });

  it("should render hover tag details from nbt resolver comprehensively", async () => {
    mockResolverRef.current = Object.assign(new MockComponentsResolver(7), {
      getItemModel: () => null,
      shouldGlint: () => true,
      isPotion: () => false,
      isTippedArrow: () => false,
      isDyedLeatherArmor: () => false,
      getPotionColor: () => null,
      getDyedColor: () => null,
      hasCustomName: () => true,
      hasEnchantments: () => true,
      getName: () => "My Custom Item",
      getEnchantments: () => new Map([["minecraft:sharpness", 5], ["minecraft:unbreaking", 3]]),
      getLore: () => ["First lore line", "Second lore line"],
      isUnbreakable: () => true,
      getMapId: () => 123
    });
    const item = createItem({ slot: 4, id: "minecraft:diamond_sword", count: 1, snbt: "{foo:1b}" });
    const { itemElem, container } = renderInventoryItem(item);

    fireEvent.mouseEnter(itemElem, { clientX: 10, clientY: 20 });

    await waitFor(() => {
      expect(screen.getByText("My Custom Item")).toBeInTheDocument();
    });

    expect(container.querySelector(".italic")).toBeInTheDocument();
    expect(container.querySelector(".cc-b")).toBeInTheDocument();
    expect(screen.getByText("First lore line")).toBeInTheDocument();
    expect(screen.getByText("Second lore line")).toBeInTheDocument();
    expect(screen.getByText("[item.unbreakable]")).toBeInTheDocument();
    expect(screen.getByText("[filled_map.id]")).toBeInTheDocument();
    expect(screen.getByText("minecraft:diamond_sword")).toBeInTheDocument();
    expect(screen.getByText("[players.inventory.item-tag.components](7)")).toBeInTheDocument();

    fireEvent.mouseLeave(itemElem);
    await waitFor(() => {
      const hoveredTag = container.querySelector(".fixed");
      expect(hoveredTag).not.toHaveClass("flex");
    });
  });
});
