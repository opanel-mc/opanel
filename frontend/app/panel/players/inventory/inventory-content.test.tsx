import { cleanup, render, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { InventoryContext } from "@/contexts/inventory-context";
import { createInventory, createItem, createMockInventoryContextValue } from "@/test/inventory-helper";
import { InventoryContent } from "./inventory-content";

vi.mock("./inventory-item", () => ({
  AIR: "minecraft:air",
  InventoryItem: (props: any) => {
    return (
      <div
        data-testid={`inventory-item-${props.itemStack.container ?? "main"}-${props.itemStack.slot}`}
        data-readonly={props.readonly ? "true" : "false"}/>
    );
  }
}));

function renderInventoryContent(inventory: ReturnType<typeof createInventory>) {
  const ctx = createMockInventoryContextValue();
  return render(
    <InventoryContext.Provider value={ctx}>
      <InventoryContent inventory={inventory}/>
    </InventoryContext.Provider>
  );
}

describe("test inventory content", () => {
  afterEach(() => cleanup());

  it("should render equipment and ender chest sections when ender read is enabled", () => {
    const inventory = createInventory({
      items: Array.from({ length: 41 }).map((_, slot) => createItem({ slot, id: "minecraft:air", count: 0 })),
      enderSize: 27,
      enderItems: Array.from({ length: 27 }).map((_, slot) => createItem({ slot, id: "minecraft:air", count: 0, container: "ender" })),
      capabilities: {
        readEnderChest: true,
        writeEnderChest: true
      }
    });

    renderInventoryContent(inventory);

    expect(screen.getByText("[players.inventory.equipment.title]")).toBeInTheDocument();
    expect(screen.getByText("[players.inventory.ender-chest.title]")).toBeInTheDocument();
    expect(screen.getByTestId("inventory-item-main-39")).toBeInTheDocument();
    expect(screen.getByTestId("inventory-item-main-40")).toBeInTheDocument();
    expect(screen.getByTestId("inventory-item-ender-0")).toBeInTheDocument();
    expect(screen.getByTestId("inventory-item-ender-26")).toBeInTheDocument();
  });

  it("should place the equipment row above the main grid with armor on the left and offhand on the right", () => {
    const inventory = createInventory({
      items: Array.from({ length: 41 }).map((_, slot) => createItem({ slot, id: "minecraft:air", count: 0 })),
      capabilities: {
        readEnderChest: false,
        writeEnderChest: false
      }
    });

    renderInventoryContent(inventory);

    const equipmentRow = screen.getByTestId("equipment-row");
    const mainGrid = screen.getByTestId("main-grid");
    const armorRow = screen.getByTestId("armor-row");
    const offhandSlot = screen.getByTestId("offhand-slot");

    expect(equipmentRow.compareDocumentPosition(mainGrid) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(
      within(armorRow)
        .getAllByTestId(/inventory-item-main-/)
        .map((elem) => elem.getAttribute("data-testid"))
    ).toEqual([
      "inventory-item-main-39",
      "inventory-item-main-38",
      "inventory-item-main-37",
      "inventory-item-main-36"
    ]);
    expect(within(offhandSlot).getByTestId("inventory-item-main-40")).toBeInTheDocument();
  });

  it("should hide ender chest section when ender read is disabled", () => {
    const inventory = createInventory({
      items: Array.from({ length: 41 }).map((_, slot) => createItem({ slot, id: "minecraft:air", count: 0 })),
      enderSize: 27,
      enderItems: Array.from({ length: 27 }).map((_, slot) => createItem({ slot, id: "minecraft:air", count: 0, container: "ender" })),
      capabilities: {
        readEnderChest: false,
        writeEnderChest: false
      }
    });

    renderInventoryContent(inventory);

    expect(screen.queryByText("[players.inventory.ender-chest.title]")).not.toBeInTheDocument();
  });

  it("should render ender slots as readonly when ender write is disabled", () => {
    const inventory = createInventory({
      items: Array.from({ length: 41 }).map((_, slot) => createItem({ slot, id: "minecraft:air", count: 0 })),
      enderSize: 27,
      enderItems: Array.from({ length: 27 }).map((_, slot) => createItem({ slot, id: "minecraft:air", count: 0, container: "ender" })),
      capabilities: {
        readEnderChest: true,
        writeEnderChest: false
      }
    });

    renderInventoryContent(inventory);

    expect(screen.getByText("[players.inventory.ender-chest.readonly]")).toBeInTheDocument();
    expect(screen.getByTestId("inventory-item-ender-0")).toHaveAttribute("data-readonly", "true");
    expect(screen.getByTestId("inventory-item-main-0")).toHaveAttribute("data-readonly", "false");
  });

  it("should allow editing ender slots when ender write is enabled", () => {
    const inventory = createInventory({
      items: Array.from({ length: 41 }).map((_, slot) => createItem({ slot, id: "minecraft:air", count: 0 })),
      enderSize: 27,
      enderItems: Array.from({ length: 27 }).map((_, slot) => createItem({ slot, id: "minecraft:air", count: 0, container: "ender" })),
      capabilities: {
        readEnderChest: true,
        writeEnderChest: true
      }
    });

    renderInventoryContent(inventory);

    expect(screen.queryByText("[players.inventory.ender-chest.readonly]")).not.toBeInTheDocument();
    expect(screen.getByTestId("inventory-item-ender-0")).toHaveAttribute("data-readonly", "false");
  });
});
