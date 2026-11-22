import type {
  ChunksFlushMessage,
  InitMessage,
  MainToWorker,
  RefreshMessage,
  RenderSettings,
  RequestTilesMessage,
  SetFpsReportingMessage,
  SetSettingsMessage,
  Viewport,
  ViewportMessage,
  WorkerToMain,
} from "./tile-worker-protocol";
import {
  initSync,
  init_panic_hook,
  render_tile_bundle_rgba,
} from "@/wasm-lib/pkg/wasm_lib.js";
import { fetchAvailableTiles, fetchTiles, fetchTilesInRange } from "./tile-fetch";
import { mapProjection, TILE_BLOCKS } from "./projection";

const MACRO_BLOCKS_PER_TILE = 16;
const MACRO_PX = MACRO_BLOCKS_PER_TILE * TILE_BLOCKS;
const BASE_COLOR = "#222";
const FPS_WINDOW_MS = 1000;
const FPS_REPORT_INTERVAL_MS = 200;

function getTileKey(save: string, x: number, z: number): string {
  return `${save}/${x}.${z}`;
}

function getTileBundleKey(save: string, xMin: number, zMin: number, xMax: number, zMax: number): string {
  return `${save}:${xMin},${zMin},${xMax},${zMax}`;
}

class TileWorker {
  private canvas: OffscreenCanvas | null = null;
  private ctx: OffscreenCanvasRenderingContext2D | null = null;
  private saveName = "";
  private currentViewport: Viewport | null = null;
  private availableTiles: Set<string> = new Set();
  private settings!: RenderSettings;

  private readonly tileCache = new Map<string, ImageBitmap>();
  private readonly macroCanvases = new Map<string, OffscreenCanvas>();
  private readonly inflight = new Set<string>();
  private readonly inflightBundles = new Set<string>();

  private readonly frameTimes: number[] = [];
  private fpsReportTimer: NodeJS.Timeout | null = null;

  constructor() {
    self.onmessage = async ({ data }: MessageEvent<MainToWorker>) => {
      switch(data.type) {
        case "init":
          return this.handleInit(data);
        case "setSettings":
          return this.handleSetSettings(data);
        case "setFpsReporting":
          return this.handleSetFpsReporting(data);
        case "viewport":
          return this.handleViewport(data);
        case "requestTiles":
          return this.handleRequestTiles(data);
        case "refresh":
          return this.handleRefresh(data);
        case "chunksFlush":
          return this.handleChunksFlush(data);
      }
    };
  }

  private static inBounds(viewport: Viewport, x: number, z: number): boolean {
    return (
      x >= viewport.tileBounds.xMin && x <= viewport.tileBounds.xMax &&
      z >= viewport.tileBounds.zMin && z <= viewport.tileBounds.zMax
    );
  }

  private async handleInit(msg: InitMessage) {
    this.canvas = msg.canvas;
    this.ctx = this.canvas.getContext("2d");
    this.saveName = msg.saveName;
    if(msg.settings) this.settings = msg.settings;
    if(this.ctx) this.ctx.imageSmoothingEnabled = false;
    initSync({ module: new Uint8Array(msg.wasmModule) });
    init_panic_hook();

    await this.reloadAvailableTiles();
    self.postMessage({ type: "ready" } satisfies WorkerToMain);
  }

  private handleSetSettings({ settings }: SetSettingsMessage) {
    this.settings = settings;

    for(const bitmap of this.tileCache.values()) {
      bitmap.close();
    }
    this.tileCache.clear();
    this.macroCanvases.clear();
    self.postMessage({ type: "tilesLoaded", value: this.tileCache.size } satisfies WorkerToMain);

    if(this.currentViewport) {
      this.render(this.currentViewport);
      this.loadTilesInBounds(this.currentViewport);
    }
  }

  private handleSetFpsReporting({ enabled }: SetFpsReportingMessage) {
    if(enabled && !this.fpsReportTimer) {
      this.fpsReportTimer = setInterval(() => this.reportFps(), FPS_REPORT_INTERVAL_MS);
    } else if(!enabled && this.fpsReportTimer) {
      clearInterval(this.fpsReportTimer);
      this.fpsReportTimer = null;
      this.frameTimes.length = 0;
    }
  }

  private handleViewport({ viewport }: ViewportMessage) {
    this.currentViewport = viewport;
    this.render(viewport);
  }

  private handleRequestTiles({ viewport }: RequestTilesMessage) {
    this.currentViewport = viewport;
    this.render(viewport);
    this.loadTilesInBounds(viewport);
  }

  private handleRefresh({ viewport }: RefreshMessage) {
    this.tileCache.clear();
    this.macroCanvases.clear();
    this.inflight.clear();
    this.inflightBundles.clear();

    this.currentViewport = viewport;
    this.render(viewport);
    this.loadTilesInBounds(viewport);
  }

  private handleChunksFlush({ flushedChunks }: ChunksFlushMessage) {
    if(!this.currentViewport) return;

    const chunksToReload = flushedChunks.filter(([chunkX, chunkZ]) => (
      TileWorker.inBounds(this.currentViewport!, chunkX, chunkZ)
    ));

    this.forceLoadTiles(chunksToReload);
  }

  private async reloadAvailableTiles() {
    const tiles = await fetchAvailableTiles(this.saveName);
    this.availableTiles = new Set(tiles.map(([x, z]) => getTileKey(this.saveName, x, z)));

    if(this.currentViewport) {
      this.render(this.currentViewport);
    }
  }

  /** Decide what to fetch, dedupe inflight, kick off the bundle request. */
  private async loadTilesInBounds(viewport: Viewport) {
    const { xMin, xMax, zMin, zMax } = viewport.tileBounds;

    const pendingKeys: string[] = [];
    for(let z = zMin; z <= zMax; z++) {
      for(let x = xMin; x <= xMax; x++) {
        const key = getTileKey(this.saveName, x, z);
        if(!this.availableTiles.has(key)) continue;
        if(this.tileCache.has(key) || this.inflight.has(key)) continue;

        pendingKeys.push(key);
      }
    }
    if(pendingKeys.length === 0) return;
    
    for(const key of pendingKeys) {
      this.inflight.add(key);
    }

    const bKey = getTileBundleKey(this.saveName, xMin, zMin, xMax, zMax);
    if(this.inflightBundles.has(bKey)) return;
    this.inflightBundles.add(bKey);

    try {
      const bytes = await fetchTilesInRange(this.saveName, xMin, zMin, xMax, zMax);
      if(!bytes) return;

      await this.cacheTileBundle(bytes);
    } catch {
      //
    } finally {
      for(const key of pendingKeys) {
        this.inflight.delete(key);
      }
      this.inflightBundles.delete(bKey);
    }
  }

  private async forceLoadTiles(tileCoords: [number, number][]) {
    const pendingKeys: string[] = [];
    const coords: [number, number][] = [];
    for(const [x, z] of tileCoords) {
      const key = getTileKey(this.saveName, x, z);
      if(this.inflight.has(key)) continue;

      pendingKeys.push(key);
      coords.push([x, z]);
    }
    if(pendingKeys.length === 0) return;

    for(const key of pendingKeys) {
      this.inflight.add(key);
    }

    try {
      const bytes = await fetchTiles(this.saveName, coords);
      if(!bytes) return;

      await this.cacheTileBundle(bytes, false);
    } catch {
      //
    } finally {
      for(const key of pendingKeys) {
        this.inflight.delete(key);
      }
    }
  }

  private async cacheTileBundle(bytes: ArrayBuffer, useCache = true) {
    const bundle = render_tile_bundle_rgba(
      new Uint8Array(bytes),
      this.settings.biomeColoring,
      this.settings.renderShadows,
    );
    const count = bundle.len();
    for(let i = 0; i < count; i++) {
      const x = bundle.x_at(i);
      const z = bundle.z_at(i);
      const key = getTileKey(this.saveName, x, z);
      if(useCache && this.tileCache.has(key)) continue;

      const rgba = bundle.rgba_at(i);
      const clamped = new Uint8ClampedArray(rgba);
      const imageData = new ImageData(clamped, TILE_BLOCKS, TILE_BLOCKS);
      const bitmap = await createImageBitmap(imageData);

      this.tileCache.set(key, bitmap);
      this.stampTileIntoMacro(x, z, bitmap);
      self.postMessage({ type: "tilesLoaded", value: this.tileCache.size } satisfies WorkerToMain);
      if(this.currentViewport && TileWorker.inBounds(this.currentViewport, x, z)) {
        this.render(this.currentViewport);
      }
    }
  }

  private render(viewport: Viewport) {
    if(!this.canvas || !this.ctx) return;

    this.frameTimes.push(performance.now());

    const { width, height, tilePx, originX, originY } = mapProjection(viewport);
    let resized = false;
    if(this.canvas.width !== width) {
      this.canvas.width = width;
      resized = true;
    }
    if(this.canvas.height !== height) {
      this.canvas.height = height;
      resized = true;
    }
    if(resized) this.ctx.imageSmoothingEnabled = false;

    this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

    const macroBlockPx = MACRO_BLOCKS_PER_TILE * tilePx;
    const mxMin = Math.floor(viewport.tileBounds.xMin / MACRO_BLOCKS_PER_TILE);
    const mxMax = Math.floor(viewport.tileBounds.xMax / MACRO_BLOCKS_PER_TILE);
    const mzMin = Math.floor(viewport.tileBounds.zMin / MACRO_BLOCKS_PER_TILE);
    const mzMax = Math.floor(viewport.tileBounds.zMax / MACRO_BLOCKS_PER_TILE);

    for(let mz = mzMin; mz <= mzMax; mz++) {
      for(let mx = mxMin; mx <= mxMax; mx++) {
        const macro = this.macroCanvases.get(getTileKey(this.saveName, mx, mz));
        if(!macro) continue;
        // Round both edges so adjacent macros share an exact pixel boundary;
        // raw fractional positions leave 1px seams at non-integer zoom levels.
        const x0 = Math.round(originX + mx * macroBlockPx);
        const y0 = Math.round(originY + mz * macroBlockPx);
        const x1 = Math.round(originX + (mx + 1) * macroBlockPx);
        const y1 = Math.round(originY + (mz + 1) * macroBlockPx);
        this.ctx.drawImage(macro, x0, y0, x1 - x0, y1 - y0);
      }
    }
  }

  private getOrCreateMacro(mx: number, mz: number): OffscreenCanvas {
    const key = getTileKey(this.saveName, mx, mz);
    let macro = this.macroCanvases.get(key);
    if(!macro) {
      macro = new OffscreenCanvas(MACRO_PX, MACRO_PX);
      const mctx = macro.getContext("2d");
      if(mctx) mctx.imageSmoothingEnabled = false;
      this.macroCanvases.set(key, macro);
    }
    return macro;
  }

  private stampTileIntoMacro(x: number, z: number, bitmap: ImageBitmap) {
    const mx = Math.floor(x / MACRO_BLOCKS_PER_TILE);
    const mz = Math.floor(z / MACRO_BLOCKS_PER_TILE);
    const localX = x - mx * MACRO_BLOCKS_PER_TILE;
    const localZ = z - mz * MACRO_BLOCKS_PER_TILE;
    const macro = this.getOrCreateMacro(mx, mz);
    const mctx = macro.getContext("2d");
    if(!mctx) return;

    mctx.fillStyle = BASE_COLOR;
    mctx.fillRect(localX * TILE_BLOCKS, localZ * TILE_BLOCKS, TILE_BLOCKS, TILE_BLOCKS);
    mctx.drawImage(bitmap, localX * TILE_BLOCKS, localZ * TILE_BLOCKS);
  }

  private reportFps() {
    const cutoff = performance.now() - FPS_WINDOW_MS;
    while(this.frameTimes.length > 0 && this.frameTimes[0] < cutoff) {
      this.frameTimes.shift();
    }
    self.postMessage({ type: "fps", value: this.frameTimes.length } satisfies WorkerToMain);
  }
}

new TileWorker();
