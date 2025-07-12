"use-client";

import type { MonitorResponse } from "@/lib/types";
import { useEffect, useState } from "react";
import { Area, AreaChart, CartesianGrid } from "recharts";
import { ChartLine } from "lucide-react";
import { FunctionalCard } from "@/components/functional-card";
import { type ChartConfig, ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { cn, getCurrentState } from "@/lib/utils";
import { sendGetRequest } from "@/lib/api";

const chartConfig = {
  memory: {
    label: "内存占用"
  },
  cpu: {
    label: "CPU占用"
  }
} satisfies ChartConfig;

const requestInterval = 2000;

export function MonitorCard({
  className,
}: Readonly<{
  className?: string
}>) {
  const [data, setData] = useState(new Array(50).fill({ memory: 0, cpu: 0 }));

  const requestMonitor = async () => {
    const currentData = await getCurrentState(setData);
    const newData = [...currentData];
    const { mem, cpu } = await sendGetRequest<MonitorResponse>("/api/monitor");
    newData.shift();
    newData.push({ memory: mem, cpu });
    setData(newData);
  };

  useEffect(() => {
    const timer = setInterval(() => {
      requestMonitor();
    }, requestInterval);

    return () => clearInterval(timer);
  }, []);

  return (
    <FunctionalCard
      icon={ChartLine}
      title="资源监控"
      className={cn(className, "justify-between")}
      innerClassName="!overflow-hidden">
      <ChartContainer config={chartConfig} className="w-full max-h-96">
        <AreaChart
          accessibilityLayer
          data={data}
          margin={{ left: 0, right: 0, bottom: 80 }}>
          <CartesianGrid vertical={false} stroke="var(--border)"/>
          <Area
            dataKey="memory"
            type="monotone"
            fill="url(#fillMemory)"
            stroke="var(--color-chart-2)"
            strokeWidth="2"
            isAnimationActive={false}/>
          <Area
            dataKey="cpu"
            type="monotone"
            fill="url(#fillCpu)"
            stroke="var(--color-foreground)"
            strokeWidth="2"
            isAnimationActive={false}/>
          <ChartTooltip
            cursor={false}
            content={<ChartTooltipContent hideLabel indicator="line"/>}/>
          <defs>
            <linearGradient id="fillMemory" x1="0" y1="0" x2="0" y2="1">
              <stop
                offset="10%"
                stopColor="var(--color-chart-2)"/>
              <stop
                offset="90%"
                stopColor="var(--color-card)"/>
            </linearGradient>
          </defs>
          <defs>
            <linearGradient id="fillCpu" x1="0" y1="0" x2="0" y2="1">
              <stop
                offset="10%"
                stopColor="var(--color-foreground)"/>
              <stop
                offset="90%"
                stopColor="var(--color-card)"/>
            </linearGradient>
          </defs>
        </AreaChart>
      </ChartContainer>
    </FunctionalCard>
  );
}
