import userEvent from "@testing-library/user-event";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor
} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { InventoryContext } from "@/contexts/inventory-context";
import { InventoryType } from "@/lib/types";
import { createItem, createMockInventoryContextValue } from "@/test/inventory-helper";
import { parseContainerNBT } from "@/lib/nbt/container";
import { ItemDialog } from "./item-dialog";

vi.mock("@/lib/i18n", async () => {
  const actual = await vi.importActual("@/lib/i18n");
  return {
    ...actual,
    $: (id: string, ...args: unknown[]) => `[${id}]${args.length > 0 ? `(${args.join(",")})` : ""}`,
    localize: (id: string) => `[${id}]`,
    localizeRich: (id: string, ...args: unknown[]) => `[${id}]${args.length > 0 ? `(${args.join(",")})` : ""}`,
  };
});

vi.mock("next-themes", () => ({
  useTheme: () => ({ theme: "dark" })
}));

vi.mock("@/lib/nbt/snbt-format", async () => {
  const actual = await vi.importActual<Record<string, unknown>>("@/lib/nbt/snbt-format");
  const prettyFormatNBT = actual.prettyFormatNBT as (snbt: string) => string;
  return {
    ...actual,
    prettyFormatNBT: vi.fn(prettyFormatNBT)
  };
});

function renderItemDialog(options?: {
  snbt?: string,
  disabled?: boolean,
  id?: string
}) {
  const updateItemNBT = vi.fn();
  const ctx = createMockInventoryContextValue({ updateItemNBT });
  const itemStack = createItem({
    slot: 5,
    id: options?.id ?? "minecraft:stone",
    count: 1,
    snbt: options?.snbt
  });
  const elem = render(
    <InventoryContext.Provider value={ctx}>
      <ItemDialog
        itemStack={itemStack}
        inventoryType={InventoryType.MAIN}
        disabled={options?.disabled}
        asChild>
        <button>open dialog</button>
      </ItemDialog>
    </InventoryContext.Provider>
  );

  return { ...elem, updateItemNBT, itemStack, ctx };
}

function getContainerGrid(): HTMLElement {
  return document.querySelector("[data-slot='container-inventory']") as HTMLElement;
}

function getContainerSlot(slot: number): HTMLElement {
  return getContainerGrid().querySelector(`[data-slot-id='${slot}']`) as HTMLElement;
}

function getEditorContainer(itemId: string) {
  const parsed = parseContainerNBT(
    (screen.getByTestId("monaco-editor") as HTMLTextAreaElement).value,
    itemId
  );
  expect(parsed).not.toBeNull();
  if(!parsed) throw new Error("Expected valid container");
  return parsed;
}

describe("test item nbt editing dialog", () => {
  afterEach(() => cleanup());

  it("should open dialog and show editor", async () => {
    const user = userEvent.setup();

    renderItemDialog({ snbt: "{foo:1b}" });

    await user.click(screen.getByText("open dialog"));

    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByTestId("monaco-editor")).toBeInTheDocument();
  });

  it("should initialize editor value from formatted snbt", async () => {
    const user = userEvent.setup();

    renderItemDialog({ snbt: "{foo:1b}" });

    await user.click(screen.getByText("open dialog"));

    expect(screen.getByTestId("monaco-editor")).toHaveValue("{\n  foo: 1b\n}\n");
  });

  it("should not update editor value when item snbt changes while dialog is open", async () => {
    const user = userEvent.setup();
    const { rerender, ctx } = renderItemDialog({ snbt: "{foo:1b}" });

    await user.click(screen.getByText("open dialog"));
    expect(screen.getByTestId("monaco-editor")).toHaveValue("{\n  foo: 1b\n}\n");

    const newItemStack = createItem({
      slot: 5,
      id: "minecraft:stone",
      count: 1,
      snbt: "{bar:1b}"
    });
    rerender(
      <InventoryContext.Provider value={ctx}>
        <ItemDialog
          itemStack={newItemStack}
          inventoryType={InventoryType.MAIN}
          asChild>
          <button>open dialog</button>
        </ItemDialog>
      </InventoryContext.Provider>
    );

    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByTestId("monaco-editor")).toHaveValue("{\n  foo: 1b\n}\n");
  });

  it("should initialize editor value with empty object when snbt is missing", async () => {
    const user = userEvent.setup();

    renderItemDialog();

    await user.click(screen.getByText("open dialog"));

    expect(screen.getByTestId("monaco-editor")).toHaveValue("{}");
  });

  it("should call updateItemNBT when save button is clicked", async () => {
    const user = userEvent.setup();
    const { updateItemNBT, itemStack } = renderItemDialog({ snbt: "{foo:1b}" });

    await user.click(screen.getByText("open dialog"));
    fireEvent.change(screen.getByTestId("monaco-editor"), { target: { value: "{bar:1b}" } });

    await user.click(screen.getByRole("button", { name: /(\[dialog\.save\]|保存)/ }));

    expect(updateItemNBT).toHaveBeenCalledWith(InventoryType.MAIN, itemStack, "{bar:1b}");
  });

  it("should not call updateItemNBT when cancel button is clicked", async () => {
    const user = userEvent.setup();
    const { updateItemNBT } = renderItemDialog({ snbt: "{foo:1b}" });

    await user.click(screen.getByText("open dialog"));
    fireEvent.change(screen.getByTestId("monaco-editor"), { target: { value: "{bar:1b}" } });

    await user.click(screen.getByRole("button", { name: /(\[dialog\.cancel\]|取消)/ }));

    expect(updateItemNBT).not.toHaveBeenCalled();
  });

  it("should not open dialog when disabled", async () => {
    const user = userEvent.setup();

    renderItemDialog({ snbt: "{foo:1b}", disabled: true });

    await user.click(screen.getByText("open dialog"));

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("should reset editor value to formatted snbt after reopen", async () => {
    const user = userEvent.setup();

    renderItemDialog({ snbt: "{foo:1b}" });

    await user.click(screen.getByText("open dialog"));
    fireEvent.change(screen.getByTestId("monaco-editor"), { target: { value: "{bar:1b}" } });
    expect(screen.getByTestId("monaco-editor")).toHaveValue("{bar:1b}");

    await user.click(screen.getByRole("button", { name: /(\[dialog\.cancel\]|取消)/ }));
    await user.click(screen.getByText("open dialog"));

    expect(screen.getByTestId("monaco-editor")).toHaveValue("{\n  foo: 1b\n}\n");
  });

  it.each([
    [
      String.raw`{"minecraft:container":[{slot:4,item:{id:"minecraft:stone",count:2}}]}`,
      "components"
    ],
    [
      String.raw`{BlockEntityTag:{Items:[{Slot:4b,id:"minecraft:stone",Count:2b}]}}`,
      "tag"
    ]
  ])("should show 27 container slots for %s data", async (snbt, format) => {
    const user = userEvent.setup();
    renderItemDialog({ snbt, id: "minecraft:shulker_box" });

    await user.click(screen.getByText("open dialog"));

    await waitFor(() => expect(getContainerGrid()).toBeInTheDocument());
    expect(getContainerGrid()).toHaveAttribute("data-container-size", "27");
    expect(getContainerGrid().querySelectorAll("[data-slot='inventory-item']")).toHaveLength(27);
    expect(getContainerSlot(4)).toHaveAttribute("data-item-id", "minecraft:stone");
    expect(getEditorContainer("minecraft:shulker_box").format).toBe(format);
    expect(getContainerGrid()).toHaveClass("overflow-hidden");
    expect(getContainerGrid()).not.toHaveClass("overflow-y-auto", "overflow-x-auto");
  });

  it("should not show a container editor without container data", async () => {
    const user = userEvent.setup();
    renderItemDialog({ snbt: "{foo:1b}", id: "minecraft:shulker_box" });

    await user.click(screen.getByText("open dialog"));

    expect(document.querySelector("[data-slot='container-inventory']")).not.toBeInTheDocument();
  });

  it("should not show a container editor when existing container data is malformed", async () => {
    const user = userEvent.setup();
    renderItemDialog({
      snbt: String.raw`{"minecraft:container":{}}`,
      id: "minecraft:shulker_box"
    });

    await user.click(screen.getByText("open dialog"));

    expect(document.querySelector("[data-slot='container-inventory']")).not.toBeInTheDocument();
  });

  it("should synchronize valid SNBT changes and hide invalid container data", async () => {
    const user = userEvent.setup();
    renderItemDialog({
      snbt: String.raw`{"minecraft:container":[{slot:0,item:{id:"minecraft:stone"}}]}`,
      id: "minecraft:shulker_box"
    });
    await user.click(screen.getByText("open dialog"));
    await waitFor(() => expect(getContainerSlot(0)).toHaveAttribute("data-item-id", "minecraft:stone"));

    fireEvent.change(screen.getByTestId("monaco-editor"), {
      target: {
        value: String.raw`{"minecraft:container":[{slot:1,item:{id:"minecraft:diamond",count:2}}]}`
      }
    });
    await waitFor(() => expect(getContainerSlot(1)).toHaveAttribute("data-item-id", "minecraft:diamond"));
    expect(getContainerSlot(0)).toHaveAttribute("data-item-id", "minecraft:air");

    fireEvent.change(screen.getByTestId("monaco-editor"), { target: { value: "{" } });
    await waitFor(() => expect(
      document.querySelector("[data-slot='container-inventory']")
    ).not.toBeInTheDocument());

    fireEvent.change(screen.getByTestId("monaco-editor"), {
      target: { value: String.raw`{"minecraft:container":[]}` }
    });
    await waitFor(() => expect(getContainerGrid()).toBeInTheDocument());
    expect(getContainerSlot(1)).toHaveAttribute("data-item-id", "minecraft:air");
  });

  it("should move an item into an empty slot and save only the final SNBT", async () => {
    const user = userEvent.setup();
    const { updateItemNBT, itemStack } = renderItemDialog({
      snbt: String.raw`{"minecraft:container":[{slot:0,item:{id:"minecraft:stone",count:8}}]}`,
      id: "minecraft:shulker_box"
    });
    await user.click(screen.getByText("open dialog"));
    await waitFor(() => expect(getContainerSlot(0)).toHaveAttribute("data-item-id", "minecraft:stone"));

    await user.click(getContainerSlot(0));
    expect(screen.getByTestId("monaco-editor")).toHaveAttribute("readonly");
    expect(screen.getByRole("button", { name: /(\[dialog\.save\]|保存)/ })).toBeDisabled();
    expect(getEditorContainer("minecraft:shulker_box").items[0].id).toBe("minecraft:air");
    expect(screen.getAllByRole("dialog")).toHaveLength(1);
    const heldItem = document.querySelector("[data-held='true']") as HTMLElement;
    expect(heldItem).toBeInTheDocument();
    expect(screen.getByRole("dialog")).not.toContainElement(heldItem);
    vi.spyOn(heldItem, "getBoundingClientRect").mockReturnValue({
      width: 48,
      height: 48
    } as DOMRect);
    fireEvent.mouseMove(document, { clientX: 600, clientY: 500 });
    expect(heldItem).toHaveStyle({ left: "576px", top: "476px" });

    await user.click(getContainerSlot(2));
    expect(screen.getByTestId("monaco-editor")).not.toHaveAttribute("readonly");
    expect(screen.getByRole("button", { name: /(\[dialog\.save\]|保存)/ })).toBeEnabled();
    expect(getEditorContainer("minecraft:shulker_box").items[2]).toMatchObject({
      id: "minecraft:stone",
      count: 8
    });

    await user.click(screen.getByRole("button", { name: /(\[dialog\.save\]|保存)/ }));
    expect(updateItemNBT).toHaveBeenCalledTimes(1);
    expect(updateItemNBT).toHaveBeenCalledWith(
      InventoryType.MAIN,
      itemStack,
      expect.any(String)
    );
    const saved = parseContainerNBT(updateItemNBT.mock.calls[0][2], "minecraft:shulker_box");
    expect(saved).not.toBeNull();
    if(!saved) return;
    expect(saved.items[0].id).toBe("minecraft:air");
    expect(saved.items[2]).toMatchObject({ id: "minecraft:stone", count: 8 });
  });

  it("should split and merge stacks without cloning items", async () => {
    const user = userEvent.setup();
    renderItemDialog({
      snbt: String.raw`{"minecraft:container":[{slot:0,item:{id:"minecraft:stone",count:8}},{slot:1,item:{id:"minecraft:stone",count:3}}]}`,
      id: "minecraft:shulker_box"
    });
    await user.click(screen.getByText("open dialog"));
    await waitFor(() => expect(getContainerSlot(0)).toHaveAttribute("data-item-id", "minecraft:stone"));

    fireEvent.contextMenu(getContainerSlot(0));
    expect(getEditorContainer("minecraft:shulker_box").items[0].count).toBe(4);

    fireEvent.contextMenu(getContainerSlot(2));
    let container = getEditorContainer("minecraft:shulker_box");
    expect(container.items[2]).toMatchObject({ id: "minecraft:stone", count: 1 });
    expect(screen.getByRole("button", { name: /(\[dialog\.save\]|保存)/ })).toBeDisabled();

    await user.click(getContainerSlot(1));
    container = getEditorContainer("minecraft:shulker_box");
    expect(container.items[1].count).toBe(6);
    expect(screen.getByRole("button", { name: /(\[dialog\.save\]|保存)/ })).toBeEnabled();

    const beforeMiddleClick = (screen.getByTestId("monaco-editor") as HTMLTextAreaElement).value;
    fireEvent(
      getContainerSlot(1),
      new MouseEvent("auxclick", { bubbles: true, button: 1 })
    );
    expect(screen.getByTestId("monaco-editor")).toHaveValue(beforeMiddleClick);
    expect(screen.getByRole("button", { name: /(\[dialog\.save\]|保存)/ })).toBeEnabled();
  });

  it("should swap items and delete the explicitly held item", async () => {
    const user = userEvent.setup();
    renderItemDialog({
      snbt: String.raw`{"minecraft:container":[{slot:0,item:{id:"minecraft:stone"}},{slot:1,item:{id:"minecraft:diamond",count:2}}]}`,
      id: "minecraft:shulker_box"
    });
    await user.click(screen.getByText("open dialog"));
    await waitFor(() => expect(getContainerSlot(0)).toHaveAttribute("data-item-id", "minecraft:stone"));

    await user.click(getContainerSlot(0));
    await user.click(getContainerSlot(1));
    let container = getEditorContainer("minecraft:shulker_box");
    expect(container.items[1].id).toBe("minecraft:stone");
    const trash = document.querySelector("[data-slot='inventory-trash']") as HTMLElement;
    expect(trash.tagName).toBe("DIV");
    expect(trash).toHaveAttribute("data-slot", "inventory-trash");

    await user.click(trash);
    container = getEditorContainer("minecraft:shulker_box");
    expect(container.items[0].id).toBe("minecraft:air");
    expect(container.items[1].id).toBe("minecraft:stone");
    expect(container.items.some(({ id }) => id === "minecraft:diamond")).toBe(false);
    expect(screen.getByRole("button", { name: /(\[dialog\.save\]|保存)/ })).toBeEnabled();
  });

  it("should discard container edits when cancelled and reopened", async () => {
    const user = userEvent.setup();
    renderItemDialog({
      snbt: String.raw`{"minecraft:container":[{slot:0,item:{id:"minecraft:stone"}}]}`,
      id: "minecraft:shulker_box"
    });
    await user.click(screen.getByText("open dialog"));
    await waitFor(() => expect(getContainerSlot(0)).toHaveAttribute("data-item-id", "minecraft:stone"));

    await user.click(getContainerSlot(0));
    await user.click(getContainerSlot(3));
    expect(getEditorContainer("minecraft:shulker_box").items[3].id).toBe("minecraft:stone");

    await user.click(screen.getByRole("button", { name: /(\[dialog\.cancel\]|取消)/ }));
    await user.click(screen.getByText("open dialog"));
    await waitFor(() => expect(getContainerSlot(0)).toHaveAttribute("data-item-id", "minecraft:stone"));
    expect(getContainerSlot(3)).toHaveAttribute("data-item-id", "minecraft:air");
  });

  it("should expose a scrollable nine-column grid beyond five rows", async () => {
    const user = userEvent.setup();
    renderItemDialog({
      snbt: String.raw`{"minecraft:container":[{slot:53,item:{id:"mod:item"}}]}`,
      id: "mod:container"
    });
    await user.click(screen.getByText("open dialog"));

    await waitFor(() => expect(getContainerGrid()).toHaveAttribute("data-container-size", "54"));
    expect(getContainerGrid()).toHaveClass("grid-cols-9", "overflow-y-auto");
    expect(getContainerGrid().querySelectorAll("[data-slot='inventory-item']")).toHaveLength(54);
    expect(getContainerSlot(53)).toHaveAttribute("data-item-id", "mod:item");
  });
});
