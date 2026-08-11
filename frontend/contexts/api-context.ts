import type { APIResponse, ExtensionPage, MonitorData, InfoResponse, VersionResponse } from "@/lib/types";
import React from "react";

function getAPIContext<R>() {
  const ctx = React.createContext<APIResponse<R> | undefined>(undefined);
  ctx.displayName = "APIContext";
  return ctx;
}

export const VersionContext = getAPIContext<VersionResponse>();
export const InfoContext = getAPIContext<InfoResponse>();

export const ExtensionsContext = React.createContext<ExtensionPage[]>([]);
ExtensionsContext.displayName = "ExtensionsContext";

export const MonitorContext = React.createContext<MonitorData[]>(undefined!);
MonitorContext.displayName = "APIContext";
