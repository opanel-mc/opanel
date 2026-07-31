import type { ReactNode } from "react";
import type { ItemStack, PlayerInventory } from "@/lib/types";
import type { InventoryInteractionRequest } from "./inventory-interaction";
import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { toast } from "sonner";
import { InventoryType } from "@/lib/types";
import { VersionContext } from "@/contexts/api-context";
import { emitter } from "@/lib/emitter";
import { getTextures } from "@/lib/texture";
import { createMockVersionContext } from "@/test/contexts-helper";
import {
  createInventory,
  createInventoryData,
  createItem,
  createMockInventoryWsClient
} from "@/test/inventory-helper";
import InventoryPage from "./page";

const mockPush = vi.fn();
const mockSearchParamsGet = vi.fn<(key: string) => string | null>();
let mockClient: ReturnType<typeof createMockInventoryWsClient> | null = null;

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush }),
  useSearchParams: () => ({
    get: mockSearchParamsGet
  })
}));

vi.mock("@/hooks/use-websocket", () => ({
  useWebSocket: () => mockClient?.client ?? null
}));

vi.mock("@/lib/texture", () => ({
  getTextures: vi.fn()
}));

vi.mock("../../sub-page", () => ({
  SubPage: ({ children }: { children: ReactNode }) => <div data-testid="sub-page">{children}</div>
}));

vi.mock("./inventory-content", () => ({
  InventoryContent: ({
    inventory,
    heldItem,
    onInteract,
    onUpdateItemNBT
  }: {
    inventory: PlayerInventory
    heldItem: ItemStack | null
    onInteract: (request: InventoryInteractionRequest) => void
    onUpdateItemNBT: (
      inventoryType: InventoryType,
      item: ItemStack,
      snbt: string
    ) => void
  }) => (
    <div>
      <div data-testid="inventory-content">{inventory.main.size}</div>
      <div data-testid="slot-0">
        {inventory.main.items[0]?.id}:{inventory.main.items[0]?.count}
      </div>
      <div data-testid="slot-0-snbt">
        {inventory.main.items[0]?.snbt ?? "none"}
      </div>
      <div data-testid="slot-1">
        {inventory.main.items[1]?.id}:{inventory.main.items[1]?.count}
      </div>
      <div data-testid="held-state">{heldItem ? `${heldItem.id}:${heldItem.count}` : "empty"}</div>
      <button onClick={() => onInteract({
        button: "left",
        source: "inventory",
        clickedItem: inventory.main.items[0],
        maxStackSize: 64,
        inventoryType: InventoryType.MAIN
      })}>
        interact slot 0
      </button>
      <button onClick={() => onInteract({
        button: "left",
        source: "inventory",
        clickedItem: inventory.main.items[1],
        maxStackSize: 64,
        inventoryType: InventoryType.MAIN
      })}>
        interact slot 1
      </button>
      <button onClick={() => onUpdateItemNBT(
        InventoryType.MAIN,
        inventory.main.items[0],
        "{updated:1b}"
      )}>
        update nbt
      </button>
    </div>
  )
}));

vi.mock("./item-explorer", () => ({
  ItemExplorer: () => <div data-testid="item-explorer"/>
}));

vi.mock("./inventory-item", () => ({
  InventoryItem: () => <div data-testid="held-item"/>
}));

function createInventoryWithMainItems(items: ItemStack[]) {
  const main = createInventoryData(36);
  for(const item of items) {
    main.items[item.slot] = item;
  }
  return createInventory({ main });
}

function renderPage() {
  const versionCtx = createMockVersionContext();
  return {
    ...render(
      <VersionContext.Provider value={versionCtx}>
        <InventoryPage />
      </VersionContext.Provider>
    ),
    versionCtx
  };
}

describe("test inventory page", () => {
  afterEach(() => cleanup());

  beforeEach(() => {
    vi.clearAllMocks();
    emitter.removeAllListeners("refresh-data");
    mockSearchParamsGet.mockImplementation((key) => key === "uuid" ? "test-uuid" : null);
    mockClient = createMockInventoryWsClient();
    (getTextures as any).mockResolvedValue([
      { id: "minecraft:stone", readable: "Stone", texture: "/stone.png" }
    ]);
  });

  it("should redirect to players page when uuid is missing", () => {
    mockSearchParamsGet.mockReturnValue(null);

    renderPage();

    expect(mockPush).toHaveBeenCalledWith("/panel/players");
    expect(screen.queryByTestId("sub-page")).not.toBeInTheDocument();
  });

  it("should fetch textures by server version", async () => {
    const { versionCtx } = renderPage();

    await waitFor(() => {
      expect(getTextures).toHaveBeenCalledWith(versionCtx.version);
    });

    expect(screen.getByTestId("sub-page")).toBeInTheDocument();
    expect(screen.getByTestId("item-explorer")).toBeInTheDocument();
  });

  it("should render nothing while textures are loading", async () => {
    (getTextures as any).mockImplementation(() => new Promise(() => {}));

    renderPage();

    await waitFor(() => {
      expect(getTextures).toHaveBeenCalled();
    });
    expect(screen.queryByTestId("sub-page")).not.toBeInTheDocument();
  });

  it("should render inventory content when receiving init packet", async () => {
    renderPage();

    await waitFor(() => {
      expect(mockClient?.client.subscribe).toHaveBeenCalled();
    });

    act(() => {
      mockClient?.emit("init", createInventory({ main: createInventoryData(101) }));
    });

    expect(await screen.findByTestId("inventory-content")).toHaveTextContent("101");
  });

  it("should update inventory content when receiving update packet", async () => {
    renderPage();

    await waitFor(() => {
      expect(mockClient?.client.subscribe).toHaveBeenCalled();
    });

    act(() => {
      mockClient?.emit("init", createInventory({ main: createInventoryData(101) }));
    });
    expect(await screen.findByTestId("inventory-content")).toHaveTextContent("101");

    act(() => {
      mockClient?.emit("update", createInventory({ main: createInventoryData(102) }));
    });

    await waitFor(() => {
      expect(screen.getByTestId("inventory-content")).toHaveTextContent("102");
    });
  });

  it("should optimistically update a slot before the server reply", async () => {
    renderPage();
    const inventory = createInventoryWithMainItems([
      createItem({ slot: 0, count: 8 })
    ]);

    await waitFor(() => expect(mockClient?.client.subscribe).toHaveBeenCalled());
    act(() => mockClient?.emit("init", inventory));
    expect(await screen.findByTestId("slot-0")).toHaveTextContent("minecraft:stone:8");

    fireEvent.click(screen.getByRole("button", { name: "interact slot 0" }));

    expect(screen.getByTestId("slot-0")).toHaveTextContent("minecraft:air:0");
    expect(screen.getByTestId("held-state")).toHaveTextContent("minecraft:stone:8");
    expect(mockClient?.client.send).toHaveBeenCalledWith("update", {
      inventoryType: InventoryType.MAIN,
      item: { slot: 0, id: "minecraft:air", count: 0 }
    });
  });

  it("should replace optimistic state with the complete server snapshot", async () => {
    renderPage();
    const inventory = createInventoryWithMainItems([
      createItem({ slot: 0, count: 8 })
    ]);

    await waitFor(() => expect(mockClient?.client.subscribe).toHaveBeenCalled());
    act(() => mockClient?.emit("init", inventory));
    fireEvent.click(await screen.findByRole("button", { name: "interact slot 0" }));
    expect(screen.getByTestId("slot-0")).toHaveTextContent("minecraft:air:0");

    act(() => mockClient?.emit("update", createInventoryWithMainItems([
      createItem({
        slot: 0,
        id: "minecraft:diamond",
        count: 2,
        snbt: "{server:1b}"
      })
    ])));

    expect(screen.getByTestId("slot-0")).toHaveTextContent("minecraft:diamond:2");
    expect(screen.getByTestId("slot-0-snbt")).toHaveTextContent("{server:1b}");
  });

  it("should use the latest held item during consecutive interactions", async () => {
    renderPage();
    const inventory = createInventoryWithMainItems([
      createItem({ slot: 0, count: 8 })
    ]);

    await waitFor(() => expect(mockClient?.client.subscribe).toHaveBeenCalled());
    act(() => mockClient?.emit("init", inventory));

    fireEvent.click(await screen.findByRole("button", { name: "interact slot 0" }));
    fireEvent.click(screen.getByRole("button", { name: "interact slot 1" }));

    expect(screen.getByTestId("slot-0")).toHaveTextContent("minecraft:air:0");
    expect(screen.getByTestId("slot-1")).toHaveTextContent("minecraft:stone:8");
    expect(screen.getByTestId("held-state")).toHaveTextContent("empty");
    expect(mockClient?.client.send).toHaveBeenNthCalledWith(1, "update", {
      inventoryType: InventoryType.MAIN,
      item: { slot: 0, id: "minecraft:air", count: 0 }
    });
    expect(mockClient?.client.send).toHaveBeenNthCalledWith(2, "update", {
      inventoryType: InventoryType.MAIN,
      item: createItem({ slot: 1, count: 8 })
    });
  });

  it("should optimistically update NBT and send only one update", async () => {
    renderPage();
    const item = createItem({ slot: 0, count: 1, snbt: "{old:1b}" });

    await waitFor(() => expect(mockClient?.client.subscribe).toHaveBeenCalled());
    act(() => mockClient?.emit("init", createInventoryWithMainItems([item])));
    fireEvent.click(await screen.findByRole("button", { name: "update nbt" }));

    expect(screen.getByTestId("slot-0-snbt")).toHaveTextContent("{updated:1b}");
    expect(mockClient?.client.send).toHaveBeenCalledTimes(1);
    expect(mockClient?.client.send).toHaveBeenCalledWith("update", {
      inventoryType: InventoryType.MAIN,
      item: { ...item, snbt: "{updated:1b}" }
    });
  });

  it("should show toast when receiving error code 400", async () => {
    renderPage();

    await waitFor(() => {
      expect(mockClient?.client.subscribe).toHaveBeenCalled();
    });

    act(() => {
      mockClient?.emit("error", 400);
    });

    expect(toast.error).toHaveBeenCalledWith("[players.inventory.ws.error.400]");
    expect(mockClient?.client.send).toHaveBeenCalledWith("fetch", null);
  });

  it("should show toast and redirect when receiving error code 404", async () => {
    renderPage();

    await waitFor(() => {
      expect(mockClient?.client.subscribe).toHaveBeenCalled();
    });
    act(() => mockClient?.emit("init", createInventoryWithMainItems([
      createItem({ slot: 0, count: 2 })
    ])));
    fireEvent.click(await screen.findByRole("button", { name: "interact slot 0" }));
    expect(screen.getByTestId("held-state")).toHaveTextContent("minecraft:stone:2");

    act(() => {
      mockClient?.emit("error", 404);
    });

    expect(toast.error).toHaveBeenCalledWith("[players.inventory.ws.error.404]");
    expect(mockPush).toHaveBeenCalledWith("/panel/players");
    expect(screen.getByTestId("held-state")).toHaveTextContent("empty");
  });

  it("should send fetch on refresh-data and clean listener on unmount", async () => {
    const elem = renderPage();

    await waitFor(() => {
      expect(mockClient?.client.subscribe).toHaveBeenCalled();
    });

    act(() => {
      emitter.emit("refresh-data");
    });
    expect(mockClient?.client.send).toHaveBeenCalledWith("fetch", null);

    (mockClient?.client.send as any).mockClear();
    elem.unmount();

    act(() => {
      emitter.emit("refresh-data");
    });
    expect(mockClient?.client.send).not.toHaveBeenCalled();
  });
});
