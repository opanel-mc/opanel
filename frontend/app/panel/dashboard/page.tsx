"use client";

import type { APIResponse, InfoResponse } from "@/lib/types";
import { useEffect, useState } from "react";
import { Gauge } from "lucide-react";
import { toast } from "sonner";
import { SubPage } from "../sub-page";
import { InfoContext } from "@/contexts/api-context";
import { sendGetRequest } from "@/lib/api";
import { InfoCard } from "./info-card";
import { PlayersCard } from "./players-card";
import { RecommendedCard } from "./recommended-card";
import { MonitorCard } from "./monitor-card";
import { TerminalCard } from "./terminal-card";

export default function Dashboard() {
  const [info, setInfo] = useState<APIResponse<InfoResponse>>();

  const fetchServerInfo = async () => {
    try {
      const res = await sendGetRequest<InfoResponse>("/api/info");
      setInfo(res);
    } catch (e) {
      toast.error("无法连接到服务器", { description: `错误：${e}` });
    }
  };

  useEffect(() => {
    fetchServerInfo();
  }, []);

  return (
    <SubPage title="仪表盘" icon={<Gauge />} className="h-[500px] grid grid-rows-3 grid-cols-3 gap-3 pb-20 [&>*]:p-4">
      <InfoContext.Provider value={info}>
        <InfoCard className="row-start-1"/>
        <PlayersCard className="row-span-2 row-start-2"/>
        <RecommendedCard className="row-start-1"/>
        <MonitorCard className="row-span-2 row-start-2"/>
        <TerminalCard className="row-span-3 rounded-md"></TerminalCard>
      </InfoContext.Provider>
    </SubPage>
  );
}
