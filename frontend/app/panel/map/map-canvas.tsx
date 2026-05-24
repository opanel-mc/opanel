"use client";

import type { MainToWorker, RenderSettings, WorkerToMain } from "@/lib/map/tile-worker-protocol";
import {
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useRef,
  type PointerEvent,
  type WheelEvent,
} from "react";
import { MAX_ZOOM, MIN_ZOOM, useMapTiles } from "@/hooks/use-map-tiles";
import { useLatestRef } from "@/hooks/use-latest-ref";
import { useWebSocket } from "@/hooks/use-websocket";
import { type ChunksFlushedPayload, MapClient } from "@/lib/ws/map";

const TILE_BLOCKS = 16;

export interface MapCanvasHandle {
  zoomIn: () => void;
  zoomOut: () => void;
}

interface MapCanvasProps {
  save: string
  settings: RenderSettings
  onCoordChange?: (coord: { x: number, z: number } | null) => void
  onFpsChange?: (fps: number) => void
  onTilesLoadedChange?: (count: number) => void
  onZoomChange?: (zoom: number) => void
  onResize?: (width: number, height: number) => void
  onLoad?: () => void
}

const MapCanvas = forwardRef<MapCanvasHandle, MapCanvasProps>(function MapCanvas({
  save,
  settings,
  onCoordChange,
  onFpsChange,
  onTilesLoadedChange,
  onZoomChange,
  onResize,
  onLoad
}, ref) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const workerRef = useRef<Worker | null>(null);
  const dragRef = useRef({ active: false, lastX: 0, lastY: 0 });
  const settingsRef = useRef(settings);
  const onFpsChangeRef = useLatestRef(onFpsChange);
  const onTilesLoadedChangeRef = useLatestRef(onTilesLoadedChange);
  const onZoomChangeRef = useLatestRef(onZoomChange);
  const onResizeRef = useLatestRef(onResize);
  const onLoadRef = useLatestRef(onLoad);
  const saveRef = useLatestRef(save);
  const client = useWebSocket(MapClient);

  const { viewportRef, postViewport, postRequestTiles, setViewportSize } = useMapTiles({
    postWorkerMessage: (msg) => workerRef.current?.postMessage(msg),
  });

  const handlePointerDown = (e: PointerEvent<HTMLDivElement>) => {
    dragRef.current.active = true;
    dragRef.current.lastX = e.clientX;
    dragRef.current.lastY = e.clientY;
    e.currentTarget.setPointerCapture(e.pointerId);
  };

  const reportCoord = (e: PointerEvent<HTMLDivElement>) => {
    if(!onCoordChange) return;

    const rect = e.currentTarget.getBoundingClientRect();
    const px = e.clientX - rect.left - rect.width / 2;
    const py = e.clientY - rect.top - rect.height / 2;
    const blockX = viewportRef.current.camera.x * TILE_BLOCKS + px / viewportRef.current.zoom;
    const blockZ = viewportRef.current.camera.z * TILE_BLOCKS + py / viewportRef.current.zoom;
    onCoordChange({ x: blockX, z: blockZ });
  };

  const handlePointerMove = (e: PointerEvent<HTMLDivElement>) => {
    if(dragRef.current.active) {
      const dx = e.clientX - dragRef.current.lastX;
      const dy = e.clientY - dragRef.current.lastY;
      dragRef.current.lastX = e.clientX;
      dragRef.current.lastY = e.clientY;
      const tilePx = viewportRef.current.zoom * TILE_BLOCKS;
      viewportRef.current.camera.x -= dx / tilePx;
      viewportRef.current.camera.z -= dy / tilePx;
      postViewport();
    }

    reportCoord(e);
  };

  const handlePointerLeave = () => {
    onCoordChange?.(null);
  };

  const handlePointerEnd = (e: PointerEvent<HTMLDivElement>) => {
    const wasDragging = dragRef.current.active;
    dragRef.current.active = false;
    if(e.currentTarget.hasPointerCapture(e.pointerId)) {
      e.currentTarget.releasePointerCapture(e.pointerId);
    }
    if(wasDragging) postRequestTiles();
  };

  const applyZoom = (factor: number, anchorPx: number, anchorPy: number) => {
    const oldZoom = viewportRef.current.zoom;
    const newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, oldZoom * factor));
    if(newZoom === oldZoom) return;

    // Keep the world point under the anchor stationary on screen.
    const oldTilePx = oldZoom * TILE_BLOCKS;
    const newTilePx = newZoom * TILE_BLOCKS;
    viewportRef.current.camera.x += anchorPx / oldTilePx - anchorPx / newTilePx;
    viewportRef.current.camera.z += anchorPy / oldTilePx - anchorPy / newTilePx;
    viewportRef.current.zoom = newZoom;
    postRequestTiles();

    onZoomChange?.(newZoom);
  };

  const handleWheel = (e: WheelEvent<HTMLDivElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const px = e.clientX - rect.left - rect.width / 2;
    const py = e.clientY - rect.top - rect.height / 2;
    applyZoom(e.deltaY > 0 ? 0.9 : 1.1, px, py);
  };

  // `transferControlToOffscreen` can only be called once per canvas element.
  // Initialize the worker exactly once per mount. Save changes remount the
  // whole component via `key={save}` in the parent, which gives us a fresh
  // canvas and a fresh worker. Settings changes go through a postMessage.
  const initWorker = useCallback(async () => {
    // Skip until we have a save: the page mounts with an empty `save` and only
    // fills it in after fetching the world list. Initializing here would spin
    // up a throwaway worker and consume the canvas's one-shot transfer.
    if(workerRef.current || !canvasRef.current || !save) return;

    const wasmUrl = new URL("../../../wasm-lib/pkg/wasm_lib_bg.wasm", import.meta.url);
    const wasmResp = await fetch(wasmUrl);
    const wasmBuffer = await wasmResp.arrayBuffer();
    // return early if one of workerRef and canvasRef is changed while awaiting
    if(workerRef.current || !canvasRef.current) return;

    const offscreen = canvasRef.current.transferControlToOffscreen();

    const worker = new Worker(
      new URL("../../../lib/map/tile-worker.ts", import.meta.url),
      { type: "module" },
    );
    workerRef.current = worker;

    worker.onmessage = (e: MessageEvent<WorkerToMain>) => {
      switch(e.data.type) {
        case "ready":
          // First tiles fetch
          postRequestTiles();
          onLoadRef.current?.();
          return;
        case "fps":
          onFpsChangeRef.current?.(e.data.value);
          return;
        case "tilesLoaded":
          onTilesLoadedChangeRef.current?.(e.data.value);
          return;
      }
    };

    worker.postMessage({
      type: "init",
      canvas: offscreen,
      saveName: save,
      wasmModule: wasmBuffer,
      settings: settingsRef.current,
    }, [offscreen, wasmBuffer]);

    if(settingsRef.current?.debugMode) {
      worker.postMessage({ type: "setFpsReporting", enabled: true } satisfies MainToWorker);
    }
  }, [save, onFpsChangeRef, onTilesLoadedChangeRef, onLoadRef, postRequestTiles]);

  useImperativeHandle(ref, () => ({
    zoomIn: () => applyZoom(1.1, 0, 0),
    zoomOut: () => applyZoom(0.9, 0, 0),
  }));

  useEffect(() => {
    initWorker();

    return () => {
      workerRef.current?.terminate();
      workerRef.current = null;
    };
  }, [initWorker]);

  useEffect(() => {
    onZoomChangeRef.current?.(viewportRef.current.zoom);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if(!client) return;

    client.subscribe("chunks-flush", ({ saveName, flushedChunks }: ChunksFlushedPayload) => {
      if(saveName !== saveRef.current) return;

      workerRef.current?.postMessage({ type: "chunksFlush", flushedChunks } satisfies MainToWorker);
    });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [client]);

  useEffect(() => {
    if(!workerRef.current) return;
    workerRef.current.postMessage({ type: "setSettings", settings } satisfies MainToWorker);
  }, [settings]);

  useEffect(() => {
    if(!workerRef.current) return;
    workerRef.current.postMessage({ type: "setFpsReporting", enabled: settings.debugMode } satisfies MainToWorker);
  }, [settings.debugMode]);

  // Resize observer keeps the worker's OffscreenCanvas in sync with the
  // container's CSS box.
  useEffect(() => {
    const containerElem = containerRef.current;
    if(!containerElem) return;

    const resizeObserver = new ResizeObserver((entries) => {
      const rect = entries[0]?.contentRect;
      if(!rect) return;

      setViewportSize(rect.width, rect.height);
      onResizeRef.current?.(rect.width, rect.height);
    });
    resizeObserver.observe(containerElem);

    return () => resizeObserver.disconnect();
  }, [setViewportSize, onResizeRef]);

  return (
    <div
      ref={containerRef}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerEnd}
      onPointerCancel={handlePointerEnd}
      onPointerLeave={handlePointerLeave}
      onWheel={handleWheel}
      className="w-full h-full overflow-hidden touch-none cursor-grab active:cursor-grabbing select-none">
      <canvas ref={canvasRef} className="block w-full h-full image-pixelated"/>
    </div>
  );
});

export default MapCanvas;
