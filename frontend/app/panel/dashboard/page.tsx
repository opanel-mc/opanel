"use client";

import type { APIResponse, InfoResponse, MonitorResponse } from "@/lib/types";
import { useEffect, useState } from "react";
import { Gauge } from "lucide-react";
import { InfoContext, MonitorContext } from "@/contexts/api-context";
import { sendGetRequest, toastError } from "@/lib/api";
import { getCurrentState } from "@/lib/utils";
import { InfoCard } from "./info-card";
import { TimeCard } from "./time-card";
import { PlayersCard } from "./players-card";
import { MonitorCard } from "./monitor-card";
import { TerminalCard } from "./terminal-card";
import { TPSCard } from "./tps-card";
import { SubPage } from "../sub-page";
import { emitter } from "@/lib/emitter";

const requestMonitorInterval = 2000;

export default function Dashboard() {
  const [info, setInfo] = useState<APIResponse<InfoResponse>>();
  const [monitorData, setMonitorData] = useState(new Array(50).fill({ memory: 0, cpu: 0, tps: 20 }));

  const fetchServerInfo = async () => {
    try {
      const res = await sendGetRequest<InfoResponse>("/api/info");
      setInfo(res);
    } catch (e: any) {
      toastError(e, "无法连接到服务器", [
        [401, "未登录"],
        [500, "服务器内部错误"]
      ]);
    }
  };

  const requestMonitor = async () => {
    const currentData = await getCurrentState(setMonitorData);
    const newData = [...currentData];
    const { mem, cpu, tps } = await sendGetRequest<MonitorResponse>("/api/monitor");
    newData.shift();
    newData.push({ memory: mem, cpu, tps });
    setMonitorData(newData);
  };

  useEffect(() => {
    fetchServerInfo();

    emitter.on("refresh-data", () => fetchServerInfo());
  }, []);

  useEffect(() => {
    const timer = setInterval(() => {
      requestMonitor();
    }, requestMonitorInterval);

    return () => clearInterval(timer);
  }, []);

  return (
    <SubPage title="仪表盘" icon={<Gauge />} className="h-[650px] max-xl:h-fit grid grid-rows-4 grid-cols-3 max-xl:flex flex-col gap-3 pb-20 [&>*]:p-4">
      <InfoContext.Provider value={info}>
        <MonitorContext.Provider value={monitorData}>
          <InfoCard className="row-start-1 col-span-2"/>
          <TimeCard className="row-start-4"/>
          <PlayersCard className="row-span-2 row-start-2"/>
          <MonitorCard className="row-span-2 row-start-2"/>
          <TPSCard className="row-start-4"/>
          <TerminalCard className="row-span-4 max-sm:hidden"></TerminalCard>
        </MonitorContext.Provider>
      </InfoContext.Provider>
    </SubPage>
  );
}
