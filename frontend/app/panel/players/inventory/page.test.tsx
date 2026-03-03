import type { ReactNode } from "react";
import { act, cleanup, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { toast } from "sonner";
import { VersionContext } from "@/contexts/api-context";
import { emitter } from "@/lib/emitter";
import { getTextures } from "@/lib/texture";
import { createMockVersionContext } from "@/test/contexts-helper";
import { createInventory, createMockInventoryWsClient } from "@/test/inventory-helper";
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
  InventoryContent: ({ inventory }: { inventory: { hash: string } }) => (
    <div data-testid="inventory-content">{inventory.hash}</div>
  )
}));

vi.mock("./item-explorer", () => ({
  ItemExplorer: () => <div data-testid="item-explorer"/>
}));

vi.mock("./inventory-item", () => ({
  AIR: "minecraft:air",
  InventoryItem: () => <div data-testid="held-item"/>
}));

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
      mockClient?.emit("init", createInventory({ hash: "init-hash" }));
    });

    expect(await screen.findByTestId("inventory-content")).toHaveTextContent("init-hash");
  });

  it("should update inventory content when receiving update packet", async () => {
    renderPage();

    await waitFor(() => {
      expect(mockClient?.client.subscribe).toHaveBeenCalled();
    });

    act(() => {
      mockClient?.emit("init", createInventory({ hash: "init-hash" }));
    });
    expect(await screen.findByTestId("inventory-content")).toHaveTextContent("init-hash");

    act(() => {
      mockClient?.emit("update", createInventory({ hash: "update-hash" }));
    });

    await waitFor(() => {
      expect(screen.getByTestId("inventory-content")).toHaveTextContent("update-hash");
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
  });

  it("should show toast and redirect when receiving error code 404", async () => {
    renderPage();

    await waitFor(() => {
      expect(mockClient?.client.subscribe).toHaveBeenCalled();
    });

    act(() => {
      mockClient?.emit("error", 404);
    });

    expect(toast.error).toHaveBeenCalledWith("[players.inventory.ws.error.404]");
    expect(mockPush).toHaveBeenCalledWith("/panel/players");
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
