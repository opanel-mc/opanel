import { describe, expect, it } from "vitest";
import { mapProjection } from "./projection";

describe("mapProjection", () => {
  it("uses the canvas pixel dimensions and rounds the origin at fractional zoom", () => {
    expect(mapProjection({
      camera: { x: -2.25, z: -1.5 },
      zoom: 2.25,
      viewportPx: { width: 801.75, height: 603.25 },
    })).toEqual({
      width: 801,
      height: 603,
      zoom: 2.25,
      tilePx: 36,
      originX: 482,
      originY: 356,
    });
  });
});
