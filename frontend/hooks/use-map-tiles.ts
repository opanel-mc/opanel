import type { MainToWorker, Viewport } from "@/lib/map/tile-worker-protocol";
import { useCallback, useEffect, useRef } from "react";
import { useLatestRef } from "./use-latest-ref";

const TILE_BLOCKS = 16;
export const DEFAULT_ZOOM = 2;
export const MIN_ZOOM = 1.75;
export const MAX_ZOOM = 10;
const VIEWPORT_QUERY_KEY = "v";
const COORD_LIMIT = 2_000_000;

export interface UseMapTilesOptions {
  postWorkerMessage: (msg: MainToWorker) => void;
}

type ViewportUpdateKind = "viewport" | "requestTiles";

function parseViewportFromUrl(): { x: number, z: number, zoom: number } {
  const fallback = { x: 0, z: 0, zoom: DEFAULT_ZOOM };
  if(typeof window === "undefined") return fallback;
  
  const raw = new URLSearchParams(window.location.search).get(VIEWPORT_QUERY_KEY);
  if(!raw) return fallback;

  const [coords, zoomStr] = raw.split("@");
  if(!coords || !zoomStr) return fallback;

  const [xStr, zStr] = coords.split(",");
  if(!xStr || !zStr) return fallback;

  let x = parseFloat(xStr);
  let z = parseFloat(zStr);
  let zoom = parseFloat(zoomStr);

  if(
    !Number.isFinite(x) || !Number.isFinite(z)
    || Math.abs(x) > COORD_LIMIT || Math.abs(z) > COORD_LIMIT
  ) {
    x = 0;
    z = 0;
  }

  zoom = (
    !Number.isFinite(zoom)
    ? DEFAULT_ZOOM
    : Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom))
  );

  return { x, z, zoom };
}

function serializeViewportToUrl(camera: { x: number, z: number }, zoom: number): void {
  if(typeof window === "undefined") return;

  const params = new URLSearchParams(window.location.search);
  params.delete(VIEWPORT_QUERY_KEY);

  const others = params.toString();
  const v = `${camera.x.toFixed(2)},${camera.z.toFixed(2)}@${zoom.toFixed(3)}`;
  const newSearch = others ? `?${others}&${VIEWPORT_QUERY_KEY}=${v}` : `?${VIEWPORT_QUERY_KEY}=${v}`;
  const newUrl = `${window.location.pathname}${newSearch}${window.location.hash}`;
  window.history.replaceState(window.history.state, "", newUrl);
}

/**
 * Pure state container + rAF-coalesced dispatcher for map viewport updates.
 * Owns the canonical `Viewport` ref but never re-renders the React tree —
 * the consumer mutates the ref directly during pointer events and calls
 * `postViewport()` (render only) or `postRequestTiles()` (render + fetch).
 */
export function useMapTiles({ postWorkerMessage }: UseMapTilesOptions) {
  const initial = parseViewportFromUrl();
  const viewportRef = useRef<Viewport>({
    generation: 0,
    camera: { x: initial.x, z: initial.z },
    zoom: initial.zoom,
    viewportPx: { width: 0, height: 0 },
    tileBounds: { xMin: 0, xMax: 0, zMin: 0, zMax: 0 },
  });
  const rafRef = useRef<number | null>(null);
  // The pending message kind for the next rAF. `requestTiles` is the stronger
  // commitment (it implies a render too), so it always wins coalescing.
  const pendingKindRef = useRef<ViewportUpdateKind | null>(null);
  const postWorkerMessageRef = useLatestRef(postWorkerMessage);

  const schedulePost = useCallback((kind: ViewportUpdateKind) => {
    if(pendingKindRef.current !== "requestTiles") {
      pendingKindRef.current = kind;
    }
    if(rafRef.current !== null) return;

    rafRef.current = requestAnimationFrame(() => {
      rafRef.current = null;
      const k = pendingKindRef.current;
      pendingKindRef.current = null;
      if(!k) return;

      const { width, height } = viewportRef.current.viewportPx;
      if(width === 0 || height === 0) return;

      const viewport = viewportRef.current;
      const tilePx = viewport.zoom * TILE_BLOCKS;
      const halfW = (viewport.viewportPx.width / 2) / tilePx;
      const halfH = (viewport.viewportPx.height / 2) / tilePx;
      viewport.tileBounds.xMin = Math.floor(viewport.camera.x - halfW);
      viewport.tileBounds.xMax = Math.ceil(viewport.camera.x + halfW);
      viewport.tileBounds.zMin = Math.floor(viewport.camera.z - halfH);
      viewport.tileBounds.zMax = Math.ceil(viewport.camera.z + halfH);
      viewport.generation += 1;

      postWorkerMessageRef.current(
        k === "viewport"
        ? { type: "viewport", viewport: viewportRef.current }
        : { type: "requestTiles", viewport: viewportRef.current }
      );

      if(k === "requestTiles") {
        serializeViewportToUrl(viewport.camera, viewport.zoom);
      }
    });
  }, [postWorkerMessageRef]);

  const postViewport = useCallback(() => schedulePost("viewport"), [schedulePost]);
  const postRequestTiles = useCallback(() => schedulePost("requestTiles"), [schedulePost]);

  const setViewportSize = useCallback((width: number, height: number) => {
    viewportRef.current.viewportPx = { width, height };
    postRequestTiles();
  }, [postRequestTiles]);

  // eslint-disable-next-line arrow-body-style
  useEffect(() => {
    return () => {
      if(rafRef.current !== null) cancelAnimationFrame(rafRef.current);
    };
  }, []);

  return { viewportRef, postViewport, postRequestTiles, setViewportSize };
}
