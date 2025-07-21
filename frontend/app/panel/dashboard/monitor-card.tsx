"use-client";

import { useContext } from "react";
import { Area, AreaChart, CartesianGrid, YAxis } from "recharts";
import { ChartLine } from "lucide-react";
import { FunctionalCard } from "@/components/functional-card";
import { type ChartConfig, ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { cn } from "@/lib/utils";
import { MonitorContext } from "@/contexts/api-context";

const chartConfig = {
  memory: {
    label: "内存占用"
  },
  cpu: {
    label: "CPU占用"
  }
} satisfies ChartConfig;

export function MonitorCard({
  className,
}: Readonly<{
  className?: string
}>) {
  const data = useContext(MonitorContext);

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
          <YAxis hide domain={[0, 100]}/>
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
