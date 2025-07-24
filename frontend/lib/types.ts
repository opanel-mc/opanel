import type { OnMount } from "@monaco-editor/react";
import type { ServerGamerules } from "./gamerules/gamerule";
import type { Player } from "@/app/panel/players/columns";

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

export type EditorRefType = Parameters<OnMount>[0];

// /api/info
export interface InfoResponse {
  favicon: string | null
  motd: string
  version: string
  port: number
  maxPlayerCount: number
  onlinePlayers: {
    name: string
    uuid: string
    gamemode: GameMode
    ping: number
  }[]
}

// /api/monitor
export interface MonitorResponse {
  mem: number
  cpu: number
  tps: number
}

// /api/control/properties
export interface ServerPropertiesResponse {
  properties: string
}

// /api/gamerules
export interface GamerulesResponse {
  gamerules: ServerGamerules
}

// /api/logs
export interface LogsResponse {
  logs: string[]
}
export interface LogResponse {
  log: string
}

// /api/players
export interface PlayersResponse {
  maxPlayerCount: number
  players: Player[]
}
