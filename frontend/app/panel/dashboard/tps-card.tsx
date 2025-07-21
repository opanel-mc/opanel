"use-client";

import { useContext } from "react";
import { Area, AreaChart, CartesianGrid, YAxis } from "recharts";
import { Server } from "lucide-react";
import { FunctionalCard } from "@/components/functional-card";
import { type ChartConfig, ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { cn } from "@/lib/utils";
import { MonitorContext } from "@/contexts/api-context";

const chartConfig = {
  tps: {
    label: "TPS"
  }
} satisfies ChartConfig;

export function TPSCard({
  className,
}: Readonly<{
  className?: string
}>) {
  const data = useContext(MonitorContext);

  return (
    <FunctionalCard
      icon={Server}
      title="TPS监控"
      className={cn(className, "justify-between")}
      innerClassName="!overflow-hidden">
      <ChartContainer config={chartConfig} className="w-full max-h-20">
        <AreaChart
          accessibilityLayer
          data={data}
          margin={{ left: 0, right: 0, bottom: 20 }}>
          <CartesianGrid vertical={false} stroke="var(--border)"/>
          <Area
            dataKey="tps"
            type="monotone"
            fill="transparent"
            stroke="var(--color-chart-3)"
            strokeWidth="2"
            isAnimationActive={false}/>
          <YAxis hide domain={[0, 20]}/>
          <ChartTooltip
            cursor={false}
            content={<ChartTooltipContent hideLabel indicator="line"/>}/>
        </AreaChart>
      </ChartContainer>
    </FunctionalCard>
  );
}
