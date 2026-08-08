import type { Editor, OnMount } from "@monaco-editor/react";
import type { ServerGamerules } from "./gamerules";

export type ArrayItem<A> = A extends (infer T)[] ? T : never;
export type SetState<T> = React.Dispatch<React.SetStateAction<T>>;

export type APIResponse<T> = {
  code: number
  error: string
} & T;

export type ServerType = "Paper" | "Fabric" | "Forge" | "NeoForge" | "Folia" | "Leaves";

export enum GameMode {
  ADVENTURE = "adventure",
  SURVIVAL = "survival",
  CREATIVE = "creative",
  SPECTATOR = "spectator"
}

export enum Difficulty {
  PEACEFUL = "peaceful",
  EASY = "easy",
  NORMAL = "normal",
  HARD = "hard"
}

export enum Dimension {
  OVERWORLD = "overworld",
  NETHER = "nether",
  THE_END = "the_end"
}

export interface MonitorData {
  cpu: number
  memory: number
  jvmMemory: number
  tps: number
  networkUpload: number
  networkDownload: number
}

export interface Save {
  name: string
  displayName: string // base64
  path: string
  size: number
  isRunning: boolean
  isCurrent: boolean
  defaultGameMode: GameMode
  difficulty: Difficulty
  isDifficultyLocked: boolean
  isHardcore: boolean
  datapacks: Record<string, boolean>
}

export interface Player {
  name: string
  uuid: string
  isOnline: boolean
  isOp: boolean
  isBanned: boolean
  gamemode: GameMode
  position: {
    x: number
    y: number
    z: number
  }
  banReason?: string // base64
  isWhitelisted?: boolean
  ping?: number
  ip?: string
  joinTime?: number
}

/** Bot player may not have a name */
export interface UnnamedPlayer extends Player {
  name: never
}

export interface ItemStack {
  /**
   * - `slot !== -1`: Normal item stack
   * - `slot === -1`: Item stack from item explorer
   */
  slot: number
  id: string
  count: number
  snbt?: string
}

export enum InventoryType {
  MAIN = "main",
  EQUIPMENTS = "equipments",
  ENDER_CHEST = "enderChest"
}

export interface InventoryData {
  size: number
  items: ItemStack[]
}

export interface PlayerInventory {
  hash: string
  main: InventoryData
  equipments: InventoryData
  enderChest: InventoryData
}

export type Whitelist = {
  name: string
  uuid: string
}[]

export interface Plugin {
  fileName: string // base64
  name: string
  version?: string
  description?: string // base64
  authors: string[]
  website?: string
  icon?: string
  size: number
  enabled: boolean
  loaded: boolean
  source?: string
}

export interface PluginUpdate {
  fileName: string // base64
  name: string
  currentVersion: string
  latestVersion: string
  downloadUrl: string
  projectUrl: string
  source?: string
  projectId?: string
  requiresBinding?: boolean
  requiresRestart?: boolean
  channel?: string
  digestAlgorithm?: string
  digestValue?: string
}

export interface ScheduledTask {
  id: string
  name: string // base64
  cron: string
  commands: string[]
  enabled: boolean
}

export interface ActivityData {
  date: string | null // yyyy-MM-dd'T'HH:mm:ss
  players: { name: string; uuid: string }[]
}

export type EditorRefType = Parameters<OnMount>[0];
export type EditorOptionsType = React.ComponentProps<typeof Editor>["options"];

export enum AvatarProvider {
  MINOTAR = "https://minotar.net/avatar/",
  MINEATAR = "https://api.mineatar.io/face/",
  MCHEADS = "https://api.mcheads.org/head/"
}

export enum SkinProvider {
  MINOTAR = "https://minotar.net/skin/",
  MINEATAR = "https://api.mineatar.io/skin/",
  MCHEADS = "https://api.mcheads.org/skin/"
}

// export enum CapeProvider {
//   /** @see https://github.com/crafatar/crafatar/issues/329#issuecomment-3559253664 */
//   CRAFATAR = "https://avatars.cloudhaven.gg/capes/"
// }

export interface CommandShortcut {
  name: string
  command: string
}

/** `/api/version` */
export interface VersionResponse {
  serverType: ServerType
  version: string
  map: boolean
  mcdr: boolean
  codeOfConduct: boolean
}

/** `/api/info` */
export interface InfoResponse {
  favicon: string | null
  motd: string // base64
  port: number
  maxPlayerCount: number
  whitelist: boolean
  uptime: number
  ingameTime: {
    current: number
    doDaylightCycle: boolean
    paused: boolean
    mspt: number
  }
  system: {
    os: string
    arch: string
    cpuName: string
    cpuCore: number
    cpuThread: number
    memory: number
    jvmMemory: number
    gpus: string[]
    java: string
  }
}

/** `/api/control/properties` */
export interface ServerPropertiesResponse {
  properties: string // base64
}

/** `/api/control/code-of-conduct` */
export interface CodeOfConductResponse {
  codeOfConducts: Record<string, string> // base64
}

/** `/api/control/paper-config` */
export interface PaperServerConfigResponse {
  bukkit: string // base64
  spigot: string // base64
  paper: string // base64
  leaves?: string // base64
}

/** `/api/gamerules` */
export interface GamerulesResponse {
  gamerules: ServerGamerules
}

/** `/api/logs` */
export interface LogsResponse {
  logs: string[]
}

/** `/api/saves` */
export interface SavesResponse {
  saves: Save[]
}

/** `/api/saves/{saveName}` */
export interface DownloadSaveResponse {
  download: string
}

/** `/api/players` */
export interface PlayersResponse {
  maxPlayerCount: number
  whitelist: boolean
}

/** `/api/whitelist` */
export interface WhitelistResponse {
  whitelist: Whitelist
}

/** `/api/banned-ips` */
export interface BannedIpsResponse {
  bannedIps: string[]
}

/** `/api/plugins` */
export interface PluginsResponse {
  plugins: Plugin[]
  folderPath: string
}

/** `/api/plugins/check-updates` */
export interface PluginUpdatesResponse {
  updates: PluginUpdate[]
}

export interface PluginUpdateBinding {
  fileName: string // base64
  source: string
  projectId?: string | null
  owner?: string | null
  repo?: string | null
  assetPattern?: string | null
  channels: string[]
}

export interface PluginUpdateBindingsResponse {
  bindings: PluginUpdateBinding[]
}

export interface PluginUpdateStatusResponse {
  autoCheckPluginUpdates: boolean
  autoApplyPluginUpdates: boolean
  pluginUpdateRestartStrategy: string
  pluginUpdateCheckInterval: number
  modrinthApiSource: string
  lastCheckedAt: number
  pendingUpdateCount: number
}

/** Body for `POST /api/plugins/update-settings` (all fields optional). */
export interface PluginUpdateSettings {
  autoCheckPluginUpdates?: boolean
  autoApplyPluginUpdates?: boolean
  pluginUpdateRestartStrategy?: string
  pluginUpdateCheckInterval?: number
  modrinthApiSource?: string
}

/** `/api/plugins/marketplace/status` */
export interface MarketplaceStatusResponse {
  source: string
  serverType: ServerType
  mcVersion: string | null
  loaderCategories: string[]
}

export interface MarketplaceProjectHit {
  id: string
  slug: string
  title: string // base64
  author: string // base64
  summary: string // base64
  iconUrl: string | null
  downloads: number
  follows: number
  projectUrl: string
  projectType: string
  categories: string[]
  updatedAt: string
}

/** `/api/plugins/marketplace/search` */
export interface MarketplaceSearchResponse {
  hits: MarketplaceProjectHit[]
  totalHits: number
  offset: number
  limit: number
  applied: {
    serverType: string
    mcVersion: string | null
    source: string
    compatibleOnly: boolean
  }
}

export interface MarketplaceDependency {
  projectId: string
  dependencyType: string
  versionId?: string | null
  projectTitle?: string | null // base64
}

export interface MarketplaceVersion {
  id: string
  name: string // base64
  versionNumber: string // base64
  channel: string
  datePublished: string
  gameVersions: string[]
  loaders: string[]
  downloads: number
  fileName: string
  fileSize: number
  downloadUrl: string
  sha1: string | null
  compatible: boolean
  dependencies: MarketplaceDependency[]
}

/** `/api/plugins/marketplace/project/{projectId}` */
export interface MarketplaceProjectResponse {
  project: {
    id: string
    slug: string
    title: string // base64
    author: string // base64
    description: string // base64
    iconUrl: string | null
    projectUrl: string
    sourceUrl?: string | null
    projectType: string
    updatedAt: string
    downloads: number
    follows: number
    categories: string[]
    versionIds: string[]
  }
  versions: MarketplaceVersion[]
  versionsFilteredByGame: boolean
  compatibility: {
    serverType: string
    mcVersion: string | null
    loaders: string[]
  }
}

export interface MarketplaceSelectedFile {
  projectId: string
  projectTitle: string // base64
  versionId: string
  versionNumber: string // base64
  fileName: string
  size: number
  url: string
  sha1: string | null
}

/** `/api/plugins/marketplace/project/{projectId}/install-preview` */
export interface MarketplaceInstallPreviewResponse {
  target: MarketplaceSelectedFile
  missingDependencies: MarketplaceSelectedFile[]
  alreadyInstalled: MarketplaceDependency[]
  unresolvedDependencies: MarketplaceDependency[]
  conflicts: { fileName: string }[]
}

/** Body entry for `POST /api/plugins/marketplace/install`. */
export interface MarketplaceInstallEntry {
  projectId: string
  versionId: string
}

/** `POST /api/plugins/marketplace/install` */
export interface MarketplaceInstallResponse {
  installed: {
    projectId: string
    projectTitle: string
    versionNumber: string
    fileName: string
  }[]
  requiresRestart: boolean
}

/** `/api/tasks` */
export interface TasksResponse {
  tasks: ScheduledTask[]
}

/** `/api/tasks/{id}` */
export interface CreateTaskResponse {
  taskId: string
}

/** `/api/auth/oidc/config` */
export interface OidcConfigResponse {
  enabled: boolean
  displayName?: string
  discoveryUrl?: string
  clientId?: string
}

/** `/api/monitor/activity` */
export interface ActivityResponse {
  activities: ActivityData[]
}

/** `https://api.github.com/repos/opanel-mc/opanel/releases` */
export type GithubReleaseResponse = {
  id: number
  tag_name: string
  name: string
  prerelease: boolean
  published_at: string
  body: string
}[]
