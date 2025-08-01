import type { OnMount } from "@monaco-editor/react";
import type { ServerGamerules } from "./gamerules/gamerule";

export type APIResponse<T> = {
  code: number
  error: string
} & T;

export enum GameMode {
  ADVENTURE = "adventure",
  SURVIVAL = "survival",
  CREATIVE = "creative",
  SPECTATOR = "spectator"
}

export interface Save {
  name: string
  displayName: string
  path: string
  isCurrent: boolean
  defaultGameMode: GameMode
}

export interface Player {
  name: string
  uuid: string
  isOnline: boolean
  isOp: boolean
  isBanned: boolean
  gamemode?: GameMode
  banReason?: string
  isWhitelisted?: boolean
}

export type Whitelist = {
  name: string
  uuid: string
}[]

export type EditorRefType = Parameters<OnMount>[0];

/** `/api/info` */
export interface InfoResponse {
  favicon: string | null
  motd: string
  version: string
  port: number
  maxPlayerCount: number
  whitelist: boolean
  onlinePlayers: {
    name: string
    uuid: string
    gamemode: GameMode
    ping: number
  }[]
}

/** `/api/monitor` */
export interface MonitorResponse {
  mem: number
  cpu: number
  tps: number
}

/** `/api/control/properties` */
export interface ServerPropertiesResponse {
  properties: string
}

/** `/api/gamerules` */
export interface GamerulesResponse {
  gamerules: ServerGamerules
}

/** `/api/logs` */
export interface LogsResponse {
  logs: string[]
}
export interface LogResponse {
  log: string
}

/** `/api/saves` */
export interface SavesResponse {
  saves: Save[]
}

/** `/api/players` */
export interface PlayersResponse {
  maxPlayerCount: number
  whitelist: boolean
  players: Player[]
}

/** `/api/whitelist` */
export interface WhitelistResponse {
  whitelist: Whitelist
}
