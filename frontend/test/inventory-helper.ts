import type { Item } from "minecraft-textures";
import type { InventoryTooltipContextType } from "@/contexts/inventory-tooltip-context";
import type { InventoryData, ItemStack, PlayerInventory } from "@/lib/types";
import { vi } from "vitest";

type Handler = (data: unknown) => void;

export function createItem(overrides?: Partial<ItemStack>): ItemStack {
  return {
    slot: 0,
    id: "minecraft:stone",
    count: 1,
    ...overrides
  };
}

export function createInventoryData(size: number, overrides?: Partial<InventoryData>): InventoryData {
  return {
    size,
    items: Array.from({ length: size }).map((_, slot) => createItem({ slot, id: "minecraft:air", count: 0 })),
    ...overrides
  };
}

export function createInventory(overrides?: Partial<PlayerInventory>): PlayerInventory {
  return {
    hash: "inventory-hash",
    main: createInventoryData(36),
    equipments: createInventoryData(5),
    enderChest: createInventoryData(27),
    ...overrides
  };
}

export function createMockInventoryTextures(): Item[] {
  return [
    { id: "minecraft:stone", readable: "Stone", texture: "/stone.png" },
    { id: "minecraft:diamond", readable: "Diamond", texture: "/diamond.png" },
    { id: "minecraft:air", readable: "Air", texture: "/air.png" }
  ] as Item[];
}

export function createMockInventoryTooltipContextValue(
  overrides?: Partial<InventoryTooltipContextType>
): InventoryTooltipContextType {
  return {
    showTooltip: vi.fn(),
    moveTooltip: vi.fn(),
    hideTooltip: vi.fn(),
    ...overrides
  };
}

export function createMockInventoryWsClient() {
  const handlers = new Map<string, Handler[]>();

  const subscribe = vi.fn((type: string, cb: Handler) => {
    const currentHandlers = handlers.get(type) ?? [];
    currentHandlers.push(cb);
    handlers.set(type, currentHandlers);
  });
  const send = vi.fn();
  const close = vi.fn();

  const emit = (type: string, data: unknown) => {
    const currentHandlers = handlers.get(type) ?? [];
    for(const handler of currentHandlers) {
      handler(data);
    }
  };

  return {
    client: {
      subscribe,
      send,
      close
    },
    emit,
    handlers
  };
}
