import { Area, AreaChart, CartesianGrid } from "recharts";
import { ChartLine } from "lucide-react";
import { FunctionalCard } from "@/components/functional-card";
import { type ChartConfig, ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { cn } from "@/lib/utils";

const chartConfig = {
  memory: {
    label: "内存占用"
  },
  cpu: {
    label: "CPU占用"
  }
} satisfies ChartConfig;

const testData: any[] = [
  // { time: 0, memory: 50, cpu: 30 },
  // { time: 1, memory: 53, cpu: 60 },
  // { time: 2, memory: 49, cpu: 53 },
  // { time: 3, memory: 32, cpu: 93 },
  // { time: 4, memory: 52, cpu: 23 },
  // { time: 5, memory: 59, cpu: 68 },
  // { time: 6, memory: 59, cpu: 15 },
  // { time: 7, memory: 1, cpu: 17 },
  // { time: 8, memory: 43, cpu: 33 },
  // { time: 9, memory: 59, cpu: 30 },
  // { time: 10, memory: 60, cpu: 21 },
  // { time: 11, memory: 72, cpu: 50 },
];

export function MonitorCard({
  className,
}: Readonly<{
  className?: string
}>) {
  return (
    <FunctionalCard
      icon={ChartLine}
      title="资源监控"
      className={cn(className, "justify-between")}
      innerClassName="!overflow-hidden">
      <ChartContainer config={chartConfig} className="w-full max-h-96">
        <AreaChart
          accessibilityLayer
          data={testData}
          margin={{ left: 0, right: 0, bottom: 80 }}>
          <CartesianGrid vertical={false}/>
          <Area
            dataKey="memory"
            type="natural"
            fill="url(#fillMemory)"
            stroke="var(--color-chart-2)"
            strokeWidth="3"/>
          <Area
            dataKey="cpu"
            type="natural"
            fill="url(#fillCpu)"
            stroke="var(--color-foreground)"
            strokeWidth="3"/>
          <ChartTooltip
            cursor={false}
            content={<ChartTooltipContent hideLabel/>}/>
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
