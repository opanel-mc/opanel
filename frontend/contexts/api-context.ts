import type { APIResponse, InfoResponse } from "@/lib/types";
import React from "react";

function getAPIContext<R>() {
  const ctx = React.createContext<APIResponse<R> | undefined>(undefined);
  ctx.displayName = "APIContext";
  return ctx;
}

export const InfoContext = getAPIContext<InfoResponse>();

export const MonitorContext = React.createContext<{ memory: number, cpu: number, tps: number }[]>(undefined!);
MonitorContext.displayName = "APIContext";
