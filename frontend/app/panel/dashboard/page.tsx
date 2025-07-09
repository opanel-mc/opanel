"use client";

import type { APIResponse } from "@/lib/types";
import { useEffect, useState } from "react";
import { Gauge } from "lucide-react";
import { SubPage } from "../sub-page";
import { Card } from "@/components/ui/card";
import { type InfoResponse, InfoContext } from "@/contexts/api-context";
import { sendGetRequest } from "@/lib/utils";
import { InfoCard } from "./info-card";
import { PlayersCard } from "./players-card";
import { RecommendedCard } from "./recommended-card";

export default function Dashboard() {
  const [info, setInfo] = useState<APIResponse<InfoResponse>>();

  const fetchServerInfo = async () => {
    const res = await sendGetRequest<InfoResponse>("/api/info");
    setInfo(res);
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
        <Card className="row-span-2 row-start-2 rounded-md"></Card>
        <Card className="row-span-3 rounded-md"></Card>
      </InfoContext.Provider>
    </SubPage>
  );
}
