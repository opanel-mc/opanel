"use client";

import type { APIResponse, InfoResponse } from "@/lib/types";
import { useEffect, useState } from "react";
import { Activity } from "lucide-react";
import { $ } from "@/lib/i18n";
import { SubPage } from "../sub-page";
import { InfoContext, MonitorContext } from "@/contexts/api-context";
import { useMonitor } from "@/hooks/use-monitor";
import { CpuMonitorBlock, JvmMemoryMonitorBlock, MemoryMonitorBlock, NetworkMonitorBlock, TpsMonitorBlock } from "./monitor-block";
import { emitter } from "@/lib/emitter";
import { sendGetRequest, toastError } from "@/lib/api";

export default function Monitor() {
  const [info, setInfo] = useState<APIResponse<InfoResponse>>();
  const monitorDataList = useMonitor(200);
  
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

  return (
    <SubPage
      title={$("monitor.title")}
      category={$("sidebar.server")}
      icon={<Activity />}
      className="grid grid-cols-2 gap-5">
      <InfoContext.Provider value={info}>
        <MonitorContext.Provider value={monitorDataList}>
          <CpuMonitorBlock className="col-span-2"/>
          <MemoryMonitorBlock className="max-lg:col-span-2"/>
          <JvmMemoryMonitorBlock className="max-lg:col-span-2"/>
          <TpsMonitorBlock className="max-lg:col-span-2"/>
          <NetworkMonitorBlock className="max-lg:col-span-2"/>
        </MonitorContext.Provider>
      </InfoContext.Provider>
    </SubPage>
  );
}
