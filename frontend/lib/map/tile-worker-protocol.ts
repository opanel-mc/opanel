export interface RenderSettings {
  biomeColoring: boolean
  renderShadows: boolean
  showPlayers: boolean
  debugMode: boolean
}

export interface Viewport {
  generation: number
  /** Chunk-space, fractional */
  camera: { x: number, z: number }
  /** Pixels per block */
  zoom: number
  /** Canvas size in CSS pixels */
  viewportPx: { width: number, height: number }
  tileBounds: {
    xMin: number
    xMax: number
    zMin: number
    zMax: number
  }
}

export interface InitMessage {
  type: "init"
  canvas: OffscreenCanvas
  saveName: string
  wasmModule: ArrayBuffer
  settings?: RenderSettings
}

export interface SetSettingsMessage {
  type: "setSettings"
  settings: RenderSettings
}

export interface SetFpsReportingMessage {
  type: "setFpsReporting"
  enabled: boolean
}

/**
 * Pure render update. Sent during ongoing user interaction (drag, zoom in
 * progress). Worker re-renders cached tiles but does NOT fetch new tiles.
 */
export interface ViewportMessage {
  type: "viewport"
  viewport: Viewport
}

/**
 * Render + fetch. Sent at drag release, zoom step, and resize. Worker
 * re-renders cached tiles AND issues a fetch for any uncached tiles in the
 * given viewport bounds.
 */
export interface RequestTilesMessage {
  type: "requestTiles"
  viewport: Viewport
}

export interface RefreshMessage {
  type: "refresh"
  viewport: Viewport
}

export interface ChunksFlushMessage {
  type: "chunksFlush"
  flushedChunks: [number, number][]
}

export interface ReadyMessage {
  type: "ready"
}

export interface FpsMessage {
  type: "fps"
  value: number
}

export interface TilesLoadedMessage {
  type: "tilesLoaded"
  value: number
}

export type MainToWorker =
  | InitMessage
  | SetSettingsMessage
  | SetFpsReportingMessage
  | ViewportMessage
  | RequestTilesMessage
  | RefreshMessage
  | ChunksFlushMessage;

export type WorkerToMain =
  | ReadyMessage
  | FpsMessage
  | TilesLoadedMessage;
