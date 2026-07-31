import userEvent from "@testing-library/user-event";
import { cleanup, render } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { InventoryTrash } from "./inventory-trash";

function renderInventoryTrash(held = true) {
  const onDelete = vi.fn();
  const elem = render(<InventoryTrash canDelete={held} onDelete={onDelete}/>);

  return {
    onDelete,
    trash: elem.container.querySelector("[data-slot='inventory-trash']") as HTMLElement
  };
}

describe("inventory trash", () => {
  afterEach(() => cleanup());

  it("deletes the currently held item when clicked", async () => {
    const user = userEvent.setup();
    const { onDelete, trash } = renderInventoryTrash();

    expect(trash.tagName).toBe("DIV");
    expect(trash).toHaveAttribute("data-slot", "inventory-trash");

    await user.click(trash);

    expect(onDelete).toHaveBeenCalledOnce();
  });

  it("does nothing without a currently held item", async () => {
    const user = userEvent.setup();
    const { onDelete, trash } = renderInventoryTrash(false);

    await user.click(trash);

    expect(onDelete).not.toHaveBeenCalled();
  });
});
