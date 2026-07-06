import { useContext, type PropsWithChildren } from "react";
import { Area, AreaChart, CartesianGrid, YAxis } from "recharts";
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent
} from "@/components/ui/chart";
import { cn, formatDataSize } from "@/lib/utils";
import { $ } from "@/lib/i18n";
import { InfoContext, MonitorContext } from "@/contexts/api-context";

const YAXIS_TICKS = [0, 25, 50, 75, 100];

export function MonitorBlock({
  title,
  description,
  additionalInfo,
  children,
  className,
  innerClassName
}: PropsWithChildren<{
  title: string
  description?: string
  additionalInfo?: string
  className?: string
  innerClassName?: string
}>) {
  return (
    <section className={cn("flex flex-col gap-3", className)}>
      <div className="px-2 flex items-end gap-3">
        <div className="flex flex-col gap-0.5">
          <h2 className="text-lg font-semibold">{title}</h2>
          {description && (
            <span className="text-xs text-muted-foreground">{description}</span>
          )}
        </div>
        {additionalInfo && (
          <span className="ml-auto text-sm text-muted-foreground text-right">{additionalInfo}</span>
        )}
      </div>
      <div className={cn("border rounded-md bg-background", innerClassName)}>
        {children}
      </div>
    </section>
  );
}

export function CpuMonitorBlock({ className }: {
  className?: string
}) {
  const info = useContext(InfoContext);
  const monitorDataList = useContext(MonitorContext);

  return (
    <MonitorBlock
      title="CPU"
      description={$("monitor.cpu.description")}
      additionalInfo={info?.system.cpuName}
      className={className}>
      <ChartContainer
        config={{
          cpu: {
            label: $("monitor.chart.cpu")
          }
        }}
        className="w-full max-h-52">
        <AreaChart
          accessibilityLayer
          data={monitorDataList}
          margin={{ top: 10, left: 0, right: 0, bottom: 10 }}>
          <CartesianGrid vertical={false} stroke="var(--border)"/>
          <Area
            dataKey="cpu"
            type="monotone"
            fill="url(#fillCpu)"
            stroke="var(--color-foreground)"
            strokeWidth="2"
            isAnimationActive={false}/>
          <YAxis hide domain={[0, 100]} ticks={YAXIS_TICKS}/>
          <ChartTooltip
            cursor={false}
            content={<ChartTooltipContent hideLabel indicator="line" valueFormatter={(value) => `${value}%`}/>}/>
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
    </MonitorBlock>
  );
}

export function MemoryMonitorBlock({ className }: {
  className?: string
}) {
  const info = useContext(InfoContext);
  const monitorDataList = useContext(MonitorContext);

  return (
    <MonitorBlock
      title={$("monitor.memory.title")}
      description={$("monitor.memory.description")}
      additionalInfo={formatDataSize(info?.system.memory ?? 0)}
      className={className}>
      <ChartContainer
        config={{
          memory: {
            label: $("monitor.chart.memory")
          }
        }}
        className="w-full max-h-44">
        <AreaChart
          accessibilityLayer
          data={monitorDataList}
          margin={{ top: 10, left: 0, right: 0, bottom: 10 }}>
          <CartesianGrid vertical={false} stroke="var(--border)"/>
          <Area
            dataKey="memory"
            type="monotone"
            fill="url(#fillMemory)"
            stroke="var(--color-chart-2)"
            strokeWidth="2"
            isAnimationActive={false}/>
          <YAxis hide domain={[0, 100]} ticks={YAXIS_TICKS}/>
          <ChartTooltip
            cursor={false}
            content={<ChartTooltipContent hideLabel indicator="line" valueFormatter={(value) => `${value}%`}/>}/>
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
        </AreaChart>
      </ChartContainer>
    </MonitorBlock>
  );
}

export function JvmMemoryMonitorBlock({ className }: {
  className?: string
}) {
  const info = useContext(InfoContext);
  const monitorDataList = useContext(MonitorContext);

  return (
    <MonitorBlock
      title={$("monitor.jvm-memory.title")}
      description={$("monitor.jvm-memory.description")}
      additionalInfo={info ? `Java ${info.system.java}` : undefined}
      className={className}>
      <ChartContainer
        config={{
          jvmMemory: {
            label: $("monitor.chart.jvm-memory")
          }
        }}
        className="w-full max-h-44">
        <AreaChart
          accessibilityLayer
          data={monitorDataList}
          margin={{ top: 10, left: 0, right: 0, bottom: 10 }}>
          <CartesianGrid vertical={false} stroke="var(--border)"/>
          <Area
            dataKey="jvmMemory"
            type="monotone"
            fill="url(#fillJvmMemory)"
            stroke="var(--color-chart-4)"
            strokeWidth="2"
            isAnimationActive={false}/>
          <YAxis hide domain={[0, 100]} ticks={YAXIS_TICKS}/>
          <ChartTooltip
            cursor={false}
            content={<ChartTooltipContent hideLabel indicator="line" valueFormatter={(value) => `${value}%`}/>}/>
          <defs>
            <linearGradient id="fillJvmMemory" x1="0" y1="0" x2="0" y2="1">
              <stop
                offset="10%"
                stopColor="var(--color-chart-4)"/>
              <stop
                offset="90%"
                stopColor="var(--color-card)"/>
            </linearGradient>
          </defs>
        </AreaChart>
      </ChartContainer>
    </MonitorBlock>
  );
}

export function TpsMonitorBlock({ className }: {
  className?: string
}) {
  const monitorDataList = useContext(MonitorContext);

  return (
    <MonitorBlock
      title="TPS"
      description={$("monitor.tps.description")}
      className={className}>
      <ChartContainer
        config={{
          tps: {
            label: "TPS"
          }
        }}
        className="w-full max-h-24">
        <AreaChart
          accessibilityLayer
          data={monitorDataList}
          margin={{ top: 10, left: 0, right: 0, bottom: 10 }}>
          <CartesianGrid vertical={false} stroke="var(--border)"/>
          <Area
            dataKey="tps"
            type="monotone"
            fill="url(#fillTps)"
            stroke="var(--color-chart-3)"
            strokeWidth="2"
            isAnimationActive={false}/>
          <YAxis hide domain={[0, 20]} ticks={[0, 10, 20]}/>
          <ChartTooltip
            cursor={false}
            content={<ChartTooltipContent hideLabel indicator="line"/>}/>
          <defs>
            <linearGradient id="fillTps" x1="0" y1="0" x2="0" y2="1">
              <stop
                offset="10%"
                stopColor="var(--color-chart-3)"/>
              <stop
                offset="90%"
                stopColor="var(--color-card)"/>
            </linearGradient>
          </defs>
        </AreaChart>
      </ChartContainer>
    </MonitorBlock>
  );
}
