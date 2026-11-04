import type { MapViewport } from "./map-viewport";
export type MapPlayer = { uuid: string; name: string; world: string; x: number; y: number; z: number };
export function playersInWorld(players: MapPlayer[], world: string): MapPlayer[] {
  return players.filter(player => player.world === world && player.name && player.uuid
    && [player.x, player.y, player.z].every(Number.isFinite));
}
export function playerScreenPosition(player: Pick<MapPlayer, "x" | "z">, view: MapViewport) {
  return {
    left: (player.x - view.camera.x * 16) * view.zoom + view.viewportPx.width / 2,
    top: (player.z - view.camera.z * 16) * view.zoom + view.viewportPx.height / 2,
  };
}
