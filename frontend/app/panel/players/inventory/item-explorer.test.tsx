import type { Item } from "minecraft-textures";
import { useState } from "react";
import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { InventoryTextureContext } from "@/contexts/inventory-texture-context";
import { ItemExplorer } from "./item-explorer";

const { inventoryItemRender } = vi.hoisted(() => ({
  inventoryItemRender: vi.fn()
}));

vi.mock("./inventory-item", () => ({
  AIR: "minecraft:air",
  InventoryItem: ({ itemStack }: { itemStack: { id: string } }) => {
    inventoryItemRender(itemStack.id);
    return <div>{itemStack.id}</div>;
  }
}));

const textures = [
  { id: "minecraft:stone", readable: "Stone", texture: "/stone.png" },
  { id: "minecraft:diamond", readable: "Diamond", texture: "/diamond.png" }
] as Item[];
const onInteract = vi.fn();

function ExplorerHarness() {
  const [heldVersion, setHeldVersion] = useState(0);

  return (
    <InventoryTextureContext.Provider value={textures}>
      <button onClick={() => setHeldVersion(version => version + 1)}>
        held {heldVersion}
      </button>
      <ItemExplorer onInteract={onInteract}/>
    </InventoryTextureContext.Provider>
  );
}

describe("item explorer render isolation", () => {
  afterEach(() => cleanup());

  beforeEach(() => {
    inventoryItemRender.mockClear();
  });

  it("does not regenerate or rerender items when unrelated held state changes", async () => {
    const user = userEvent.setup();
    render(<ExplorerHarness />);
    expect(inventoryItemRender).toHaveBeenCalledTimes(2);

    await user.click(screen.getByRole("button", { name: "held 0" }));

    expect(screen.getByRole("button", { name: "held 1" })).toBeInTheDocument();
    expect(inventoryItemRender).toHaveBeenCalledTimes(2);
  });
});
