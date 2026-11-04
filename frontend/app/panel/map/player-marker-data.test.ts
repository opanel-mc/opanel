import { expect, it } from "vitest";
import { playersInWorld, playerScreenPosition } from "./player-marker-data";
it("filters other worlds and invalid coordinates", () => {
  const player = { uuid: "id", name: "Alex", world: "world", x: 1, y: 64, z: 2 };
  expect(playersInWorld([player, { ...player, world: "world_nether" }, { ...player, x: NaN }], "world")).toEqual([player]);
});
it("projects block coordinates using the terrain camera's chunk coordinates", () => {
  expect(playerScreenPosition({ x: -16, z: 32 }, { camera: { x: -1, z: 2 }, zoom: 4, viewportPx: { width: 200, height: 100 } })).toEqual({ left: 100, top: 50 });
});
