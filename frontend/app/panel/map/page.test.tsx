import type { ReactNode, Ref } from "react";
import type { MapCanvasHandle } from "./map-canvas";
import type { PlayerMoveData } from "@/lib/ws/players";
import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import * as globalInfo from "@/lib/global";
import ServerMap from "./page";

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

const { capturedPlayers, useMapPlayersMock } = vi.hoisted(() => ({
  capturedPlayers: vi.fn(),
  useMapPlayersMock: vi.fn(),
}));

vi.mock("next/dynamic", async () => {
  const { forwardRef, useImperativeHandle } = await import("react");
  return {
    default: () => forwardRef(function MockMap(
      { save, players, onLoad }: { save: string, players?: PlayerMoveData[], onLoad: () => void },
      ref: Ref<MapCanvasHandle>
    ) {
      capturedPlayers(save, players);
      useImperativeHandle(ref, () => ({
        setCenter: vi.fn(),
        getCenter: () => ({ x: 0, z: 0 }),
        zoomIn: vi.fn(),
        zoomOut: vi.fn(),
        refresh: vi.fn(),
      }));
      return <button onClick={onLoad}>Ready {save}</button>;
    }),
  };
});

vi.mock("@/hooks/use-map-tiles", () => ({ DEFAULT_ZOOM: 2 }));
vi.mock("../sub-page", () => ({
  SubPage: ({ children }: { children: ReactNode }) => <div>{children}</div>,
}));
vi.mock("./coord-dialog", () => ({
  CoordDialog: () => null,
}));
vi.mock("@/lib/settings", () => ({
  getSettings: () => ({
    biomeColoring: true,
    renderShadows: true,
    showPlayers: true,
    debugMode: false,
  }),
  changeSettings: vi.fn(),
}));
vi.mock("@/lib/api", () => ({
  sendGetRequest: async () => ({
    saves: ["world", "other"].map((name) => ({ name, isRunning: true })),
  }),
  toastError: vi.fn(),
}));
vi.mock("./use-map-players", () => ({
  useMapPlayers: useMapPlayersMock,
}));

beforeEach(() => {
  capturedPlayers.mockClear();
  useMapPlayersMock.mockReset();
  useMapPlayersMock.mockImplementation((enabled: boolean) => enabled ? [
    { uuid: "a", name: "Alpha", position: { save: "world", x: 10, y: 64, z: 10 } },
    { uuid: "b", name: "Beta", position: { save: "other", x: 90, y: 64, z: 90 } },
  ] : []);
  vi.spyOn(globalInfo, "serverType", "get").mockReturnValue("Paper");
});

describe("map players display", () => {
  it("passes current save players to MapCanvas without rendering a player list", async () => {
    render(<ServerMap />);
    await screen.findByRole("button", { name: "Ready world" });

    expect(useMapPlayersMock).toHaveBeenCalledWith(true);
    expect(capturedPlayers).toHaveBeenLastCalledWith("world", [
      { uuid: "a", name: "Alpha", position: { save: "world", x: 10, y: 64, z: 10 } },
    ]);
    expect(screen.queryByRole("button", { name: /map\.players/ })).not.toBeInTheDocument();
  });

  it("disables map players on Pumpkin", async () => {
    vi.spyOn(globalInfo, "serverType", "get").mockReturnValue("Pumpkin");

    render(<ServerMap />);
    await screen.findByRole("button", { name: "Ready world" });

    expect(useMapPlayersMock).toHaveBeenCalledWith(false);
    expect(capturedPlayers).toHaveBeenLastCalledWith("world", undefined);
  });
});
