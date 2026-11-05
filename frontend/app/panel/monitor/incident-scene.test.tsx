import { afterEach, expect, it, vi } from "vitest";
import { cleanup, render, screen } from "@testing-library/react";
import { IncidentScene } from "./incident-scene";
vi.mock("@/lib/i18n", () => ({ $: (key: string) => key }));
vi.mock("../map/density-names", () => ({ densityEntityName: (key: string) => key, densityBlockName: (key: string) => key }));
afterEach(() => cleanup());
it("preserves evidence status, player coordinates and map destination", () => {
  render(<IncidentScene scene={{ requestedAt: 1000, completedAt: 2000, candidates: 2,
    players: [{ uuid: "id", name: "Alex", world: "world_nether", x: -20, y: 64, z: 50 }],
    chunks: [{ world: "world_nether", x: -2, z: 3, sampledAt: 1500, status: "truncated", entities: { ITEM: 1024 }, blockEntities: { HOPPER: 4 } }] }}/>);
  expect(screen.getByText("monitor.scene.partial")).toBeInTheDocument();
  expect(screen.getByText("monitor.scene.truncated")).toBeInTheDocument();
  expect(screen.getByRole("link")).toHaveAttribute("href", "/panel/map?world=world_nether&x=-24&z=56");
  expect(screen.getByText("ITEM: 1024")).toBeInTheDocument();
  expect(screen.getByText("Alex: -20 64 50")).toBeInTheDocument();
});
it("distinguishes missing evidence from zero counts", () => {
  render(<IncidentScene/>);
  expect(screen.getByText("monitor.scene.missing")).toBeInTheDocument();
  expect(screen.queryByRole("link")).toBeNull();
});
it("shows an unavailable status instead of zero for unsampled chunks", () => {
  render(<IncidentScene scene={{ requestedAt: 1000, completedAt: 2000, candidates: 1, players: [],
    chunks: [{ world: "world", x: 0, z: 0, sampledAt: 1500, status: "unloaded", entities: {}, blockEntities: {} }] }}/>);
  const summary = screen.getByText(/world · \(0, 0\)/);
  expect(summary).toHaveTextContent("monitor.scene.unloaded");
  expect(summary).not.toHaveTextContent("map.density.entities: 0");
});
