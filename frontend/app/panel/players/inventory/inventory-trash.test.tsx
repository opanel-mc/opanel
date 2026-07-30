import userEvent from "@testing-library/user-event";
import { cleanup, render } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { InventoryContext } from "@/contexts/inventory-context";
import { createItem, createMockInventoryContextValue } from "@/test/inventory-helper";
import { InventoryTrash } from "./inventory-trash";

function renderInventoryTrash(held = true) {
  const setCurrentlyHeldItem = vi.fn();
  const context = createMockInventoryContextValue({
    currentlyHeldItem: held ? createItem() : null,
    setCurrentlyHeldItem
  });

  const elem = render(
    <InventoryContext.Provider value={context}>
      <InventoryTrash/>
    </InventoryContext.Provider>
  );

  return {
    setCurrentlyHeldItem,
    trash: elem.container.querySelector("[data-slot='inventory-trash']") as HTMLElement
  };
}

describe("inventory trash", () => {
  afterEach(() => cleanup());

  it("deletes the currently held item when clicked", async () => {
    const user = userEvent.setup();
    const { setCurrentlyHeldItem, trash } = renderInventoryTrash();

    expect(trash.tagName).toBe("DIV");
    expect(trash).toHaveAttribute("data-slot", "inventory-trash");

    await user.click(trash);

    expect(setCurrentlyHeldItem).toHaveBeenCalledOnce();
    expect(setCurrentlyHeldItem).toHaveBeenCalledWith(null);
  });

  it("does nothing without a currently held item", async () => {
    const user = userEvent.setup();
    const { setCurrentlyHeldItem, trash } = renderInventoryTrash(false);

    await user.click(trash);

    expect(setCurrentlyHeldItem).not.toHaveBeenCalled();
  });
});
