import userEvent from "@testing-library/user-event";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { InventoryContext } from "@/contexts/inventory-context";
import { createItem, createMockInventoryContextValue } from "@/test/inventory-helper";
import { mockMonacoEditor } from "@/test/test-helper";
import { ItemDialog } from "./item-dialog";

mockMonacoEditor();

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

vi.mock("@/lib/nbt/snbt-format", () => ({
  prettyFormatNBT: vi.fn((snbt: string) => `formatted:${snbt}`)
}));

function renderItemDialog(options?: {
  snbt?: string,
  disabled?: boolean
}) {
  const updateItemNBT = vi.fn();
  const ctx = createMockInventoryContextValue({ updateItemNBT });
  const itemStack = createItem({
    slot: 5,
    id: "minecraft:stone",
    count: 1,
    snbt: options?.snbt
  });
  const elem = render(
    <InventoryContext.Provider value={ctx}>
      <ItemDialog
        itemStack={itemStack}
        disabled={options?.disabled}
        asChild>
        <button>open dialog</button>
      </ItemDialog>
    </InventoryContext.Provider>
  );

  return { ...elem, updateItemNBT, itemStack, ctx };
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

    expect(screen.getByTestId("monaco-editor")).toHaveValue("formatted:{foo:1b}");
  });

  it("should not update editor value when item snbt changes while dialog is open", async () => {
    const user = userEvent.setup();
    const { rerender, ctx } = renderItemDialog({ snbt: "{foo:1b}" });

    await user.click(screen.getByText("open dialog"));
    expect(screen.getByTestId("monaco-editor")).toHaveValue("formatted:{foo:1b}");

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
          asChild>
          <button>open dialog</button>
        </ItemDialog>
      </InventoryContext.Provider>
    );

    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByTestId("monaco-editor")).toHaveValue("formatted:{foo:1b}");
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

    expect(updateItemNBT).toHaveBeenCalledWith(itemStack, "{bar:1b}");
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

    expect(screen.getByTestId("monaco-editor")).toHaveValue("formatted:{foo:1b}");
  });
});
