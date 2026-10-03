"use client";

import type { APIResponse, InfoResponse } from "@/lib/types";
import type { PropsWithChildren } from "react";
import { useContext, useEffect, useState } from "react";
import { Activity } from "lucide-react";
import { $ } from "@/lib/i18n";
import { SubPage } from "../sub-page";
import { InfoContext, MonitorContext, VersionContext } from "@/contexts/api-context";
import { useMonitor } from "@/hooks/use-monitor";
import {
  ActivityMonitorBlock,
  CpuMonitorBlock,
  DiskIOMonitorBlock,
  JvmMemoryMonitorBlock,
  MemoryMonitorBlock,
  NetworkMonitorBlock,
  TpsMonitorBlock
} from "./monitor-block";
import { emitter } from "@/lib/emitter";
import { sendGetRequest, toastError } from "@/lib/api";
import { MonitorHistoryProvider } from "./monitor-history-context";
import { cn } from "@/lib/utils";
import { isPumpkin } from "@/lib/server-type";
import { serverType } from "@/lib/global";

function RealtimeMonitorProvider({ children }: PropsWithChildren) {
  const monitorDataList = useMonitor(200);
  return <MonitorContext.Provider value={monitorDataList}>{children}</MonitorContext.Provider>;
}

export default function Monitor() {
  const [info, setInfo] = useState<APIResponse<InfoResponse>>();
  const versionCtx = useContext(VersionContext);
  
  const fetchServerInfo = async () => {
    try {
      const res = await sendGetRequest<InfoResponse>("/api/info");
      setInfo(res);
    } catch (e: any) {
      toastError(e, $("dashboard.error"), [
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    } finally {
      emitter.emit("loading-done");
    }
  };

  useEffect(() => {
    fetchServerInfo();

    emitter.on("refresh-data", () => fetchServerInfo());
  }, []);

  if(!versionCtx) return <></>;

  return (
    <SubPage
      title={$("monitor.title")}
      category={$("sidebar.server")}
      icon={<Activity />}
      className="grid grid-cols-2 gap-5">
      <InfoContext.Provider value={info}>
        <RealtimeMonitorProvider>
          <MonitorHistoryProvider enabled={versionCtx?.monitorHistoryEnabled ?? false}>
            {!isPumpkin(serverType) && (
              <ActivityMonitorBlock className="col-span-2"/>
            )}
            <CpuMonitorBlock className="col-span-2"/>
            <MemoryMonitorBlock
              className={cn(
                "max-lg:col-span-2",
                isPumpkin(serverType) && "col-span-2"
              )}/>
            {!isPumpkin(serverType) && (
              <JvmMemoryMonitorBlock className="max-lg:col-span-2"/>
            )}
            <TpsMonitorBlock className="max-lg:col-span-2"/>
            <NetworkMonitorBlock className="max-lg:col-span-2"/>
            <DiskIOMonitorBlock className="col-span-2"/>
          </MonitorHistoryProvider>
        </RealtimeMonitorProvider>
      </InfoContext.Provider>
    </SubPage>
  );
}
