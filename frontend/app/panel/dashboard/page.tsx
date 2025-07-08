"use client";

import { Gauge } from "lucide-react";
import { SubPage } from "../sub-page";
import { Card } from "@/components/ui/card";
import { InfoCard } from "./info-card";
import { RecommendedCard } from "./recommended-card";

export default function Dashboard() {
  return (
    <SubPage title="仪表盘" icon={<Gauge />} className="h-[500px] grid grid-rows-3 grid-cols-3 gap-4 pb-20 [&>*]:p-4">
      <InfoCard className="row-start-1"/>
      <Card className="row-span-2 row-start-2"></Card>
      <RecommendedCard className="row-start-1"/>
      <Card className="row-span-2 row-start-2"></Card>
      <Card className="row-span-3"></Card>
    </SubPage>
  );
}
