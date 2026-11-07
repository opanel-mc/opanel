import type ServerMapPage from "./page";
import type { ReactNode, Ref } from "react";
import type { MapCanvasHandle } from "./map-canvas";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from "vitest";

afterEach(() => cleanup());

const { setCenter } = vi.hoisted(() => ({ setCenter: vi.fn() }));

vi.mock("next/dynamic", async () => {
  const { forwardRef, useImperativeHandle } = await import("react");
  return {
    default: () => forwardRef(function MockMap(
      { save, onLoad }: { save: string, onLoad: () => void },
      ref: Ref<MapCanvasHandle>
    ) {
      useImperativeHandle(ref, () => ({
        setCenter,
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
  CoordDialog: ({ onTeleport }: { onTeleport: (coord: { x: number, z: number }) => void }) => (
    <button onClick={() => onTeleport({ x: 70, z: 80 })}>Coordinate</button>
  ),
}));
vi.mock("@/lib/settings", () => ({
  getSettings: (key: string) => key === "map.render-settings"
    ? { biomeColoring: true, renderShadows: true, debugMode: false }
    : true,
  changeSettings: vi.fn(),
}));
vi.mock("@/lib/api", () => ({
  sendGetRequest: async () => ({
    saves: ["world", "other"].map((name) => ({ name, isRunning: true })),
  }),
  toastError: vi.fn(),
}));
vi.mock("./use-map-players", () => ({
  useMapPlayers: () => ({
    disconnected: false,
    players: [
      { uuid: "a", name: "Alpha", position: { save: "other", x: 10, y: 64, z: 10 } },
      { uuid: "b", name: "Beta", position: { save: "other", x: 90, y: 64, z: 90 } },
    ],
  }),
}));

let ServerMap: typeof ServerMapPage;

beforeAll(async () => {
  vi.stubEnv("VITE_OPANEL_MAP_PLAYERS", "true");
  ServerMap = (await import("./page")).default;
});
afterAll(() => vi.unstubAllEnvs());
beforeEach(() => setCenter.mockClear());

describe("map positioning while loading a world", () => {
  it.each(["Beta", "Origin", "Coordinate"])("keeps the latest %s selection after ready", async (action) => {
    render(<ServerMap />);
    await screen.findByRole("button", { name: "Ready world" });
    fireEvent.click(screen.getByRole("button", { name: /map.players/ }));
    fireEvent.click(screen.getByRole("button", { name: /Alpha/ }));
    await screen.findByRole("button", { name: "Ready other" });

    if(action === "Beta") {
      fireEvent.click(screen.getByRole("button", { name: /Beta/ }));
    } else {
      fireEvent.keyDown(screen.getByRole("dialog"), { key: "Escape" });
      fireEvent.click(screen.getByRole("button", { name: action === "Origin" ? /map.origin/ : "Coordinate" }));
    }

    const expected = action === "Beta" ? { x: 90, y: 64, z: 90, save: "other" }
      : action === "Origin" ? { x: 0, z: 0 } : { x: 70, z: 80 };
    expect(setCenter).toHaveBeenLastCalledWith(expected);
    const callsBeforeReady = setCenter.mock.calls.length;
    fireEvent.click(screen.getByRole("button", { name: "Ready other" }));
    expect(setCenter).toHaveBeenCalledTimes(callsBeforeReady);
    expect(setCenter).toHaveBeenLastCalledWith(expected);
  });
});
