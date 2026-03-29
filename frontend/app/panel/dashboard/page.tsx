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
import { getSettings } from "@/lib/settings";
import { $ } from "@/lib/i18n";
import { SystemCard } from "./system-card";

const requestMonitorInterval = getSettings("dashboard.monitor-interval");

export default function Dashboard() {
  const [info, setInfo] = useState<APIResponse<InfoResponse>>();
  const [monitorData, setMonitorData] = useState(
    new Array<MonitorResponse>(50).fill({ cpu: 0, memory: 0, tps: 20 })
  );

  const fetchServerInfo = async () => {
    try {
      const res = await sendGetRequest<InfoResponse>("/api/info");
      setInfo(res);
    } catch (e: any) {
      toastError(e, $("dashboard.error"), [
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    }
  };

  const requestMonitor = async () => {
    const res = await sendGetRequest<MonitorResponse>("/api/monitor");
    const currentData = await getCurrentState(setMonitorData);
    const newData = [...currentData];
    newData.shift();
    newData.push(res);
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
    <SubPage
      title={$("dashboard.title")}
      category={$("sidebar.server")}
      icon={<Gauge />}
      pageClassName="min-2xl:px-[5%]"
      className="flex-1 min-h-0 h-full max-xl-h:min-h-[600px] flex max-xl:flex-col gap-2">
      <InfoContext.Provider value={info}>
        <MonitorContext.Provider value={monitorData}>
          {/* Left side */}
          <div className="flex-2 flex flex-col gap-2">
            {/* Upper */}
            <InfoCard className="row-start-1 col-span-2"/>

            {/* Center */}
            <div className="flex-1 min-h-0 flex max-lg:flex-col gap-2 *:flex-1">
              <PlayersCard className="row-span-3"/>
              <MonitorCard className="row-span-3"/>
            </div>

            {/* Lower */}
            <div className="min-lg:h-36 flex max-lg:flex-col gap-2 *:flex-1">
              <TimeCard />
              <TPSCard />
            </div>
          </div>

          {/* Right side */}
          <div className="flex-1 min-w-0 flex flex-col gap-2">
            <SystemCard className=""/>
            <TerminalCard className="flex-1"/>
          </div>
        </MonitorContext.Provider>
      </InfoContext.Provider>
    </SubPage>
  );
}
