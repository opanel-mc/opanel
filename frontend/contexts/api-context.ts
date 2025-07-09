import type { APIResponse, GameMode } from "@/lib/types";
import React from "react";

function getAPIContext<R>() {
  const ctx = React.createContext<APIResponse<R> | undefined>(undefined);
  ctx.displayName = "APIContext";
  return ctx;
}

// /api/info
export interface InfoResponse {
  favicon: string | null
  motd: string
  ip: string
  port: number
  onlinePlayers: {
    name: string
    uuid: string
    isOp: boolean
    gameMode: GameMode
  }[]
}
export const InfoContext = getAPIContext<InfoResponse>();
