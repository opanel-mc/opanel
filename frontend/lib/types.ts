import { Player } from "@/app/panel/players/columns";
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
