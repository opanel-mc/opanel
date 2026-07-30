import type { ReactNode } from "react";
import type { Item } from "minecraft-textures";
import type { InventoryInteractionRequest } from "./inventory-interaction";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { toast } from "sonner";
import { InventoryType, type ItemStack } from "@/lib/types";
import { VersionContext } from "@/contexts/api-context";
import { InventoryTextureContext } from "@/contexts/inventory-texture-context";
import { InventoryTooltipContext } from "@/contexts/inventory-tooltip-context";
import { createMockVersionContext } from "@/test/contexts-helper";
import {
  createItem,
  createMockInventoryTooltipContextValue
} from "@/test/inventory-helper";
import { createResolver } from "@/lib/nbt";
import { AIR, InventoryItem } from "./inventory-item";
import { InventoryTooltipProvider } from "./inventory-item-tooltip";

vi.mock("./item-dialog", () => ({
  ItemDialog: ({ children }: { children: ReactNode }) => <>{children}</>
}));

const { MockComponentsResolver, resolverFactoryRef } = vi.hoisted(() => {
  class HoistedMockComponentsResolver {
    constructor(private readonly componentAmount = 0) {}

    getComponentAmount() {
      return this.componentAmount;
    }
  }

  return {
    MockComponentsResolver: HoistedMockComponentsResolver,
    resolverFactoryRef: {
      current: (_id: string) => null as any
    }
  };
});

vi.mock("@/lib/nbt", () => ({
  createResolver: vi.fn((_version: string, id: string) => resolverFactoryRef.current(id))
}));

vi.mock("@/lib/nbt/components-resolver", () => ({
  ComponentsResolver: MockComponentsResolver,
  itemModelToTextureId: vi.fn((model: string | null) => model)
}));

const textures = [
  { id: "minecraft:stone", readable: "Stone", texture: "/stone.png" },
  { id: "minecraft:diamond", readable: "Diamond", texture: "/diamond.png" },
  { id: "minecraft:air", readable: "Air", texture: "/air.png" }
] as Item[];

function createResolverMock(id: string) {
  return {
    getItemModel: () => null,
    shouldGlint: () => false,
    isPotion: () => false,
    isTippedArrow: () => false,
    isDyedLeatherArmor: () => false,
    getPotionColor: () => null,
    getDyedColor: () => null,
    hasCustomName: () => false,
    hasEnchantments: () => false,
    getName: () => id,
    getEnchantments: () => new Map(),
    getLore: () => [],
    isUnbreakable: () => false,
    getMapId: () => null,
    getBeeAmount: () => null,
    getHoneyLevel: () => null,
    getMaxStackSize: () => 64
  };
}

function renderInventoryItem(itemStack: ItemStack, options?: {
  held?: boolean
  nbtEditMode?: boolean
  inventoryType?: InventoryType
  placeholderIcon?: string
  textures?: Item[]
  onInteract?: (request: InventoryInteractionRequest) => void
}) {
  const ctx = createMockInventoryTooltipContextValue();
  const itemTextures = options?.textures ?? textures;
  const onInteract = options?.onInteract
    ?? vi.fn<(request: InventoryInteractionRequest) => void>();
  const elem = render(
    <VersionContext.Provider value={createMockVersionContext()}>
      <InventoryTextureContext.Provider value={itemTextures}>
        <InventoryTooltipContext.Provider value={ctx}>
          <InventoryItem
            itemStack={itemStack}
            inventoryType={options?.inventoryType ?? InventoryType.MAIN}
            placeholderIcon={options?.placeholderIcon}
            held={options?.held}
            nbtEditMode={options?.nbtEditMode}
            onInteract={onInteract}/>
        </InventoryTooltipContext.Provider>
      </InventoryTextureContext.Provider>
    </VersionContext.Provider>
  );
  const itemElem = elem.container.querySelector("[data-slot='inventory-item']") as HTMLElement;
  expect(itemElem).toBeInTheDocument();

  return { ...elem, itemElem, ctx, onInteract };
}

function fireMiddleClick(elem: HTMLElement) {
  fireEvent(elem, new MouseEvent("auxclick", {
    bubbles: true,
    cancelable: true,
    button: 1
  }));
}

describe("test inventory item", () => {
  afterEach(() => cleanup());

  beforeEach(() => {
    resolverFactoryRef.current = createResolverMock;
    vi.mocked(createResolver).mockClear();
    vi.mocked(toast.error).mockClear();
  });

  it("renders a placeholder only for an empty slot", () => {
    const placeholderIcon = "/empty-armor-slot-helmet.png";
    const { container } = renderInventoryItem(
      createItem({ id: AIR, count: 0 }),
      { placeholderIcon }
    );

    expect(container.querySelector(`img[src="${placeholderIcon}"]`)).toBeInTheDocument();
    expect(container.querySelector("img[src='/stone.png']")).not.toBeInTheDocument();
  });

  it("forwards left, right, and middle interaction requests", () => {
    const item = createItem({ slot: 10, count: 8, snbt: "{}" });
    const { itemElem, onInteract } = renderInventoryItem(item);

    fireEvent.click(itemElem);
    fireEvent.contextMenu(itemElem);
    fireMiddleClick(itemElem);

    expect(onInteract).toHaveBeenNthCalledWith(1, {
      button: "left",
      source: "inventory",
      clickedItem: item,
      maxStackSize: 64,
      inventoryType: InventoryType.MAIN
    });
    expect(onInteract).toHaveBeenNthCalledWith(2, expect.objectContaining({ button: "right" }));
    expect(onInteract).toHaveBeenNthCalledWith(3, expect.objectContaining({ button: "middle" }));
  });

  it("marks slot -1 as an explorer interaction", () => {
    const item = createItem({ slot: -1 });
    const { itemElem, onInteract } = renderInventoryItem(item);

    fireEvent.click(itemElem);

    expect(onInteract).toHaveBeenCalledWith(expect.objectContaining({
      source: "explorer",
      clickedItem: item
    }));
  });

  it("does not interact while held or in NBT edit mode", () => {
    const held = renderInventoryItem(createItem(), { held: true });
    fireEvent.click(held.itemElem);
    expect(held.onInteract).not.toHaveBeenCalled();
    cleanup();

    const editing = renderInventoryItem(createItem(), { nbtEditMode: true });
    fireEvent.click(editing.itemElem);
    expect(editing.onInteract).not.toHaveBeenCalled();
  });

  it("blocks mod items in the player inventory but permits container drafts", () => {
    const item = createItem({ id: "mod:custom_item" });
    const blocked = renderInventoryItem(item);
    fireEvent.click(blocked.itemElem);

    expect(blocked.onInteract).not.toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith("[players.inventory.interact-forbbiden]");
    cleanup();

    const ctx = createMockInventoryTooltipContextValue();
    const onInteract = vi.fn();
    const container = render(
      <VersionContext.Provider value={createMockVersionContext()}>
        <InventoryTextureContext.Provider value={textures}>
          <InventoryTooltipContext.Provider value={ctx}>
            <InventoryItem
              itemStack={item}
              interactionMode="container"
              onInteract={onInteract}/>
          </InventoryTooltipContext.Provider>
        </InventoryTextureContext.Provider>
      </VersionContext.Provider>
    );
    fireEvent.click(
      container.container.querySelector("[data-slot='inventory-item']") as HTMLElement
    );

    expect(onInteract).toHaveBeenCalledWith(expect.objectContaining({
      source: "container",
      clickedItem: item
    }));
  });

  it("passes the resolver stack limit to the interaction", () => {
    resolverFactoryRef.current = (id) => ({
      ...createResolverMock(id),
      getMaxStackSize: () => 16
    });
    const item = createItem({ snbt: "{}" });
    const { itemElem, onInteract } = renderInventoryItem(item);

    fireEvent.click(itemElem);

    expect(onInteract).toHaveBeenCalledWith(expect.objectContaining({
      maxStackSize: 16
    }));
  });

  it("uses an item-model texture override when the resolver provides one", () => {
    resolverFactoryRef.current = (id) => ({
      ...createResolverMock(id),
      getItemModel: () => "minecraft:diamond"
    });
    const { container } = renderInventoryItem(createItem({ snbt: "{}" }));

    expect(container.querySelector("img")).toHaveAttribute("src", "/diamond.png");
  });

  it.each([
    {
      id: "minecraft:potion",
      texture: "/potion.png",
      resolver: { isPotion: () => true, getPotionColor: () => [12, 34, 56] }
    },
    {
      id: "minecraft:tipped_arrow",
      texture: "/tipped-arrow.png",
      resolver: { isTippedArrow: () => true, getPotionColor: () => [1, 2, 3] }
    },
    {
      id: "minecraft:leather_helmet",
      texture: "/leather-helmet.png",
      resolver: { isDyedLeatherArmor: () => true, getDyedColor: () => [45, 67, 89] }
    }
  ])("renders the color overlay for $id", ({ id, texture, resolver }) => {
    resolverFactoryRef.current = () => ({
      ...createResolverMock(id),
      ...resolver
    });
    const { container } = renderInventoryItem(
      createItem({ id, snbt: "{}" }),
      {
        textures: [
          { id, readable: id, texture }
        ] as Item[]
      }
    );

    expect(container.querySelector(".color-overlay")).toBeInTheDocument();
  });

  it("renders an enchanted glint overlay", () => {
    resolverFactoryRef.current = (id) => ({
      ...createResolverMock(id),
      shouldGlint: () => true
    });
    const { container } = renderInventoryItem(createItem({ snbt: "{}" }));

    expect(container.querySelector(".item-glint")).toBeInTheDocument();
  });

  it("uses the shared tooltip controller", () => {
    const item = createItem({ snbt: "{}" });
    const { itemElem, ctx } = renderInventoryItem(item);

    fireEvent.mouseEnter(itemElem, { clientX: 20, clientY: 30 });
    fireEvent.mouseMove(itemElem, { clientX: 21, clientY: 31 });
    fireEvent.mouseLeave(itemElem);

    const owner = vi.mocked(ctx.showTooltip).mock.calls[0][0];
    expect(typeof owner).toBe("symbol");
    expect(ctx.showTooltip).toHaveBeenCalledWith(
      owner,
      expect.objectContaining({ itemStack: item }),
      20,
      30
    );
    expect(ctx.moveTooltip).toHaveBeenCalledWith(owner, 21, 31);
    expect(ctx.hideTooltip).toHaveBeenCalledWith(owner);
  });

  it("keeps a single tooltip portal and switches its content", () => {
    render(
      <VersionContext.Provider value={createMockVersionContext()}>
        <InventoryTextureContext.Provider value={textures}>
          <InventoryTooltipProvider>
            <InventoryItem itemStack={createItem({ slot: 0, snbt: "{}" })}/>
            <InventoryItem itemStack={createItem({
              slot: 1,
              id: "minecraft:diamond",
              snbt: "{}"
            })}/>
          </InventoryTooltipProvider>
        </InventoryTextureContext.Provider>
      </VersionContext.Provider>
    );
    const items = screen.getAllByRole("img").map(image => image.parentElement as HTMLElement);

    fireEvent.mouseEnter(items[0], { clientX: 20, clientY: 30 });
    expect(document.querySelectorAll("[data-slot='inventory-item-hover']")).toHaveLength(1);
    expect(document.querySelector("[data-slot='inventory-item-hover']")).toHaveTextContent(
      "minecraft:stone"
    );

    fireEvent.mouseEnter(items[1], { clientX: 40, clientY: 50 });
    expect(document.querySelectorAll("[data-slot='inventory-item-hover']")).toHaveLength(1);
    expect(document.querySelector("[data-slot='inventory-item-hover']")).toHaveTextContent(
      "minecraft:diamond"
    );
  });

  it("preserves detailed NBT tooltip content in the shared portal", () => {
    resolverFactoryRef.current = () => Object.assign(
      new MockComponentsResolver(7),
      createResolverMock("minecraft:diamond_sword"),
      {
        hasCustomName: () => true,
        hasEnchantments: () => true,
        getName: () => "My Custom Item",
        getEnchantments: () => new Map([["minecraft:sharpness", 5]]),
        getLore: () => ["First lore line", "Second lore line"],
        isUnbreakable: () => true,
        getMapId: () => 123,
        getBeeAmount: () => 2,
        getHoneyLevel: () => 4
      }
    );
    render(
      <VersionContext.Provider value={createMockVersionContext()}>
        <InventoryTextureContext.Provider value={[
          {
            id: "minecraft:diamond_sword",
            readable: "Diamond Sword",
            texture: "/diamond-sword.png"
          }
        ] as Item[]}>
          <InventoryTooltipProvider>
            <InventoryItem itemStack={createItem({
              id: "minecraft:diamond_sword",
              snbt: "{}"
            })}/>
          </InventoryTooltipProvider>
        </InventoryTextureContext.Provider>
      </VersionContext.Provider>
    );
    const item = screen.getByRole("img").parentElement as HTMLElement;

    fireEvent.mouseEnter(item, { clientX: 10, clientY: 10 });

    const tooltip = document.querySelector(
      "[data-slot='inventory-item-hover']"
    ) as HTMLElement;
    expect(tooltip).toBeInTheDocument();
    expect(tooltip).toHaveTextContent("My Custom Item");
    expect(tooltip).toHaveTextContent("First lore line");
    expect(tooltip).toHaveTextContent("Second lore line");
    expect(tooltip).toHaveTextContent("minecraft:diamond_sword");
    expect(tooltip).toHaveTextContent("[players.inventory.item-tag.components](7)");
    expect(tooltip.querySelector(".italic")).toBeInTheDocument();
    expect(tooltip.querySelector(".cc-b")).toBeInTheDocument();

    fireEvent.mouseLeave(item);
    expect(document.querySelector("[data-slot='inventory-item-hover']")).not.toBeInTheDocument();
  });

  it("does not rerender when a semantically identical item object is passed", () => {
    const ctx = createMockInventoryTooltipContextValue();
    const onInteract = vi.fn();
    const versionCtx = createMockVersionContext();
    const item = createItem({ snbt: "{}" });
    const elem = render(
      <VersionContext.Provider value={versionCtx}>
        <InventoryTextureContext.Provider value={textures}>
          <InventoryTooltipContext.Provider value={ctx}>
            <InventoryItem itemStack={item} onInteract={onInteract}/>
          </InventoryTooltipContext.Provider>
        </InventoryTextureContext.Provider>
      </VersionContext.Provider>
    );
    expect(createResolver).toHaveBeenCalledTimes(1);

    elem.rerender(
      <VersionContext.Provider value={versionCtx}>
        <InventoryTextureContext.Provider value={textures}>
          <InventoryTooltipContext.Provider value={ctx}>
            <InventoryItem itemStack={{ ...item }} onInteract={onInteract}/>
          </InventoryTooltipContext.Provider>
        </InventoryTextureContext.Provider>
      </VersionContext.Provider>
    );

    expect(createResolver).toHaveBeenCalledTimes(1);
  });
});
