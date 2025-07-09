import type { APIResponse } from "@/lib/types";
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
  players: {
    name: string
    uuid: string
    isOp: boolean
  }[]
}
export const InfoContext = getAPIContext<InfoResponse>();
