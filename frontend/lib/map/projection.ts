import type { Viewport } from "./tile-worker-protocol";

export const TILE_BLOCKS = 16;

export function mapProjection({ camera, zoom, viewportPx }: Pick<Viewport, "camera" | "zoom" | "viewportPx">) {
  const width = Math.trunc(viewportPx.width);
  const height = Math.trunc(viewportPx.height);
  const tilePx = zoom * TILE_BLOCKS;
  const originX = Math.round(width / 2 - camera.x * tilePx);
  const originY = Math.round(height / 2 - camera.z * tilePx);
  return { width, height, zoom, tilePx, originX, originY };
}
