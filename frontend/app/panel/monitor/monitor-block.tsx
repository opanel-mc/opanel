import type { ActivityData, ActivityResponse, MonitorData } from "@/lib/types";
import {
  type ReactNode,
  type PropsWithChildren,
  useContext,
  useState,
  useEffect,
  memo
} from "react";
import { Area, AreaChart, Bar, BarChart, CartesianGrid, XAxis, YAxis } from "recharts";
import { ArrowUpDown, MoveDown, MoveUp } from "lucide-react";
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent
} from "@/components/ui/chart";
import { Badge } from "@/components/ui/badge";
import { cn, formatDataSize } from "@/lib/utils";
import { $ } from "@/lib/i18n";
import { InfoContext, MonitorContext } from "@/contexts/api-context";
import { googleSansCode } from "@/lib/fonts";
import { sendGetRequest, toastError } from "@/lib/api";
import { fillActivityData } from "./activity-data";
import {
  formatMonitorHistoryAxisTick,
  formatMonitorHistoryTooltip,
  getMonitorHistoryAverage,
  type MonitorMetric
} from "./history-data";
import {
  MonitorHistoryChart,
  type MonitorNavigatorSeries
} from "./monitor-history-chart";
import { useMonitorHistory } from "./monitor-history-context";

const YAXIS_TICKS = [0, 25, 50, 75, 100];
const ACTIVITY_DATE_LABEL_INTERVAL = 4;
const CPU_NAVIGATOR_SERIES = [{
  dataKey: "cpu",
  label: $("monitor.chart.cpu"),
  color: "var(--color-foreground)"
}] satisfies MonitorNavigatorSeries[];
const MEMORY_NAVIGATOR_SERIES = [{
  dataKey: "memory",
  label: $("monitor.chart.memory"),
  color: "var(--color-chart-2)"
}] satisfies MonitorNavigatorSeries[];
const JVM_MEMORY_NAVIGATOR_SERIES = [{
  dataKey: "jvmMemory",
  label: $("monitor.chart.jvm-memory"),
  color: "var(--color-chart-4)"
}] satisfies MonitorNavigatorSeries[];
const TPS_NAVIGATOR_SERIES = [{
  dataKey: "tps",
  label: "TPS",
  color: "var(--color-chart-3)"
}] satisfies MonitorNavigatorSeries[];
const NETWORK_NAVIGATOR_SERIES = [
  {
    dataKey: "networkUpload",
    label: $("monitor.chart.network-upload"),
    color: "var(--color-chart-5)"
  },
  {
    dataKey: "networkDownload",
    label: $("monitor.chart.network-download"),
    color: "var(--color-chart-2)"
  }
] satisfies MonitorNavigatorSeries[];
const DISK_NAVIGATOR_SERIES = [
  {
    dataKey: "diskRead",
    label: $("monitor.chart.disk-read"),
    color: "var(--color-chart-5)"
  },
  {
    dataKey: "diskWrite",
    label: $("monitor.chart.disk-write"),
    color: "var(--color-chart-2)"
  }
] satisfies MonitorNavigatorSeries[];

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
  additionalInfo?: string | ReactNode
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
          <div className="ml-auto text-sm text-muted-foreground text-right">{additionalInfo}</div>
        )}
      </div>
      <div className={cn("border rounded-md bg-background", innerClassName)}>
        {children}
      </div>
    </section>
  );
}

function MonitorHistoryAverage<T extends MonitorMetric>({ metrics, children }: {
  metrics: T[]
  children: (averages: Record<T, number>) => ReactNode
}) {
  const { mode, detailData, detailStatus } = useMonitorHistory();
  if(mode !== "history" || detailStatus !== "ready") return null;

  const averages = {} as Record<T, number>;
  for(const metric of metrics) {
    const value = getMonitorHistoryAverage(detailData, metric);
    if(value === null) return null;
    averages[metric] = value;
  }

  return children(averages);
}

type ActivityChartData = {
  dateLabel: string
  fullDateLabel: string
  playerCount: number
}

function formatActivityDate(dateText: string | null, options: Intl.DateTimeFormatOptions): string {
  if(!dateText) return "";
  
  const date = new Date(dateText);
  if(isNaN(date.getTime())) return "";

  return new Intl.DateTimeFormat(undefined, options).format(date);
}

export const ActivityMonitorBlock = memo(({ className }: {
  className?: string
}) => {
  const info = useContext(InfoContext);
  const [activities, setActivities] = useState<ActivityData[]>([]);

  const fetchActivity = async () => {
    try {
      const { activities } = await sendGetRequest<ActivityResponse>("/api/monitor/activity");
      setActivities(activities);
    } catch (e: any) {
      toastError(e, $("dashboard.error"), [
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    }
  };

  useEffect(() => {
    fetchActivity();
  }, []);

  const activityChartData: ActivityChartData[] = fillActivityData(activities)
    .map((activity) => ({
      dateLabel: formatActivityDate(activity.date, { month: "short", day: "numeric" }),
      fullDateLabel: formatActivityDate(activity.date, { year: "numeric", month: "short", day: "numeric" }),
      playerCount: activity.players.length
    }));

  return (
    <MonitorBlock
      title={$("monitor.activity.title")}
      description={$("monitor.activity.description")}
      className={className}>
      <ChartContainer
        config={{
          playerCount: {
            label: $("monitor.chart.activity"),
            color: "var(--color-highlight-primary)"
          }
        }}
        className="w-full h-56">
        <BarChart
          accessibilityLayer
          data={activityChartData}
          margin={{ top: 10, left: 0, right: 12, bottom: 10 }}>
          <CartesianGrid vertical={false} stroke="var(--border)"/>
          <XAxis
            dataKey="dateLabel"
            interval={0}
            tickLine={false}
            axisLine={false}
            tickMargin={8}
            tickFormatter={(value, index) => (
              (activityChartData.length - 1 - index) % ACTIVITY_DATE_LABEL_INTERVAL === 0
                ? value
                : ""
            )}/>
          <YAxis hide domain={[0, info ? info.maxPlayerCount : "auto"]}/>
          <ChartTooltip
            cursor={false}
            content={(
              <ChartTooltipContent
                labelFormatter={(_, payload) => payload?.[0]?.payload?.fullDateLabel ?? ""}
                indicator="line"
                valueFormatter={(value) => value}/>
            )}/>
          <Bar
            dataKey="playerCount"
            fill="var(--color-playerCount)"
            radius={[6, 6, 0, 0]}
            isAnimationActive={false}/>
        </BarChart>
      </ChartContainer>
    </MonitorBlock>
  );
});

export function CpuMonitorBlock({ className }: {
  className?: string
}) {
  const info = useContext(InfoContext);

  return (
    <MonitorBlock
      title="CPU"
      description={$("monitor.cpu.description")}
      additionalInfo={(
        <div className="flex flex-col gap-1">
          <MonitorHistoryAverage metrics={["cpu"]}>
            {({ cpu }) => (
              <span className={cn("text-xs", googleSansCode.className)}>
                <span className="mr-3">{$("monitor.history.average")}</span>
                {`${cpu.toFixed(2)}%`}
              </span>
            )}
          </MonitorHistoryAverage>
          {info?.system.cpuName && <span>{info.system.cpuName}</span>}
        </div>
      )}
      className={className}>
      <MonitorHistoryChart series={CPU_NAVIGATOR_SERIES}>
        {(data, isHistory, range) => (
          <ChartContainer
            config={{
              cpu: {
                label: $("monitor.chart.cpu")
              }
            }}
            className="w-full h-56">
            <AreaChart
              accessibilityLayer
              data={data}
              margin={{ top: 10, left: 0, right: 8, bottom: 4 }}>
              <CartesianGrid vertical={false} stroke="var(--border)"/>
              <Area
                dataKey="cpu"
                type="monotone"
                fill="url(#fillCpu)"
                stroke="var(--color-foreground)"
                strokeWidth="2"
                connectNulls={false}
                isAnimationActive={false}/>
              <YAxis hide domain={[0, 100]} ticks={YAXIS_TICKS}/>
              {isHistory && (
                <XAxis
                  dataKey="timestamp"
                  type="number"
                  scale="time"
                  allowDataOverflow
                  domain={range ? [range.from, range.to] : ["dataMin", "dataMax"]}
                  tickLine={false}
                  axisLine={false}
                  tickMargin={8}
                  minTickGap={32}
                  tickFormatter={(value) => formatMonitorHistoryAxisTick(value, range ? range.to - range.from : 0)}/>
              )}
              <ChartTooltip
                cursor={false}
                content={(
                  <ChartTooltipContent
                    hideLabel={!isHistory}
                    labelFormatter={(_, payload) => formatMonitorHistoryTooltip(payload)}
                    indicator="line"
                    valueFormatter={(value) => `${value}%`}/>
                )}/>
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
        )}
      </MonitorHistoryChart>
    </MonitorBlock>
  );
}

export function MemoryMonitorBlock({ className }: {
  className?: string
}) {
  const info = useContext(InfoContext);

  return (
    <MonitorBlock
      title={$("monitor.memory.title")}
      description={$("monitor.memory.description")}
      additionalInfo={formatDataSize(info?.system.memory ?? 0)}
      className={className}>
      <MonitorHistoryChart series={MEMORY_NAVIGATOR_SERIES}>
        {(data, isHistory, range) => (
          <ChartContainer
            config={{
              memory: {
                label: $("monitor.chart.memory")
              }
            }}
            className="w-full h-52">
            <AreaChart
              accessibilityLayer
              data={data}
              margin={{ top: 10, left: 0, right: 8, bottom: 4 }}>
              <CartesianGrid vertical={false} stroke="var(--border)"/>
              <Area
                dataKey="memory"
                type="monotone"
                fill="url(#fillMemory)"
                stroke="var(--color-chart-2)"
                strokeWidth="2"
                connectNulls={false}
                isAnimationActive={false}/>
              <YAxis hide domain={[0, 100]} ticks={YAXIS_TICKS}/>
              {isHistory && (
                <XAxis
                  dataKey="timestamp"
                  type="number"
                  scale="time"
                  allowDataOverflow
                  domain={range ? [range.from, range.to] : ["dataMin", "dataMax"]}
                  tickLine={false}
                  axisLine={false}
                  tickMargin={8}
                  minTickGap={32}
                  tickFormatter={(value) => formatMonitorHistoryAxisTick(value, range ? range.to - range.from : 0)}/>
              )}
              <ChartTooltip
                cursor={false}
                content={(
                  <ChartTooltipContent
                    hideLabel={!isHistory}
                    labelFormatter={(_, payload) => formatMonitorHistoryTooltip(payload)}
                    indicator="line"
                    valueFormatter={(value) => `${value}%`}/>
                )}/>
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
        )}
      </MonitorHistoryChart>
    </MonitorBlock>
  );
}

export function JvmMemoryMonitorBlock({ className }: {
  className?: string
}) {
  const info = useContext(InfoContext);

  return (
    <MonitorBlock
      title={$("monitor.jvm-memory.title")}
      description={$("monitor.jvm-memory.description")}
      additionalInfo={
        info
        ? (
          <>
            {formatDataSize(info.system.jvmMemory)}
            <Badge variant="outline" className="ml-2">{`Java ${info.system.java}`}</Badge>
          </>
        )
        : undefined
      }
      className={className}>
      <MonitorHistoryChart series={JVM_MEMORY_NAVIGATOR_SERIES}>
        {(data, isHistory, range) => (
          <ChartContainer
            config={{
              jvmMemory: {
                label: $("monitor.chart.jvm-memory")
              }
            }}
            className="w-full h-52">
            <AreaChart
              accessibilityLayer
              data={data}
              margin={{ top: 10, left: 0, right: 8, bottom: 4 }}>
              <CartesianGrid vertical={false} stroke="var(--border)"/>
              <Area
                dataKey="jvmMemory"
                type="monotone"
                fill="url(#fillJvmMemory)"
                stroke="var(--color-chart-4)"
                strokeWidth="2"
                connectNulls={false}
                isAnimationActive={false}/>
              <YAxis hide domain={[0, 100]} ticks={YAXIS_TICKS}/>
              {isHistory && (
                <XAxis
                  dataKey="timestamp"
                  type="number"
                  scale="time"
                  allowDataOverflow
                  domain={range ? [range.from, range.to] : ["dataMin", "dataMax"]}
                  tickLine={false}
                  axisLine={false}
                  tickMargin={8}
                  minTickGap={32}
                  tickFormatter={(value) => formatMonitorHistoryAxisTick(value, range ? range.to - range.from : 0)}/>
              )}
              <ChartTooltip
                cursor={false}
                content={(
                  <ChartTooltipContent
                    hideLabel={!isHistory}
                    labelFormatter={(_, payload) => formatMonitorHistoryTooltip(payload)}
                    indicator="line"
                    valueFormatter={(value) => `${value}%`}/>
                )}/>
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
        )}
      </MonitorHistoryChart>
    </MonitorBlock>
  );
}

function TpsMonitorStatus() {
  const monitorDataList = useContext(MonitorContext);
  const latestTps = (
    monitorDataList.length > 0
    ? monitorDataList[monitorDataList.length - 1].tps
    : 20
  );

  const getTpsStatus = (tps: number) => {
    if(tps >= 18) {
      return {
        label: $("monitor.tps.status.smooth"),
        className: "border-emerald-600/30 bg-emerald-500/10 text-emerald-700 dark:border-emerald-500/40 dark:bg-emerald-500/15 dark:text-emerald-300",
        dotClassName: "bg-emerald-600 dark:bg-emerald-400"
      };
    }

    if(tps >= 16) {
      return {
        label: $("monitor.tps.status.fluctuating"),
        className: "border-amber-600/30 bg-amber-500/10 text-amber-700 dark:border-amber-500/40 dark:bg-amber-500/15 dark:text-amber-300",
        dotClassName: "bg-amber-600 dark:bg-amber-400"
      };
    }

    return {
      label: $("monitor.tps.status.lagging"),
      className: "border-red-600/30 bg-red-500/10 text-red-700 dark:border-red-500/40 dark:bg-red-500/15 dark:text-red-300",
      dotClassName: "bg-red-600 dark:bg-red-400"
    };
  }
  const tpsStatus = getTpsStatus(latestTps);

  return (
    <Badge
      variant="outline"
      title={`${latestTps.toFixed(1)} TPS`}
      className={cn("cursor-help gap-1.5 px-2 py-0.5", tpsStatus.className)}>
      <div className={cn("w-2 h-2 rounded-full", tpsStatus.dotClassName)}/>
      {tpsStatus.label}
    </Badge>
  );
}

export function TpsMonitorBlock({ className }: {
  className?: string
}) {
  const { mode } = useMonitorHistory();

  return (
    <MonitorBlock
      title="TPS"
      description={$("monitor.tps.description")}
      additionalInfo={mode === "history" ? (
        <MonitorHistoryAverage metrics={["tps"]}>
          {({ tps }) => (
            <span className={cn("text-xs", googleSansCode.className)}>
              <span className="mr-3">{$("monitor.history.average")}</span>
              {`${tps.toFixed(1)} TPS`}
            </span>
          )}
        </MonitorHistoryAverage>
      ) : <TpsMonitorStatus />}
      className={className}>
      <MonitorHistoryChart series={TPS_NAVIGATOR_SERIES}>
        {(data, isHistory, range) => (
          <ChartContainer
            config={{
              tps: {
                label: "TPS"
              }
            }}
            className="w-full h-52">
            <AreaChart
              accessibilityLayer
              data={data}
              margin={{ top: 10, left: 0, right: 8, bottom: 4 }}>
              <CartesianGrid vertical={false} stroke="var(--border)"/>
              <Area
                dataKey="tps"
                type="monotone"
                fill="url(#fillTps)"
                stroke="var(--color-chart-3)"
                strokeWidth="2"
                connectNulls={false}
                isAnimationActive={false}/>
              <YAxis hide domain={[0, 20]} ticks={[0, 10, 20]}/>
              {isHistory && (
                <XAxis
                  dataKey="timestamp"
                  type="number"
                  scale="time"
                  allowDataOverflow
                  domain={range ? [range.from, range.to] : ["dataMin", "dataMax"]}
                  tickLine={false}
                  axisLine={false}
                  tickMargin={8}
                  minTickGap={32}
                  tickFormatter={(value) => formatMonitorHistoryAxisTick(value, range ? range.to - range.from : 0)}/>
              )}
              <ChartTooltip
                cursor={false}
                content={(
                  <ChartTooltipContent
                    hideLabel={!isHistory}
                    labelFormatter={(_, payload) => formatMonitorHistoryTooltip(payload)}
                    indicator="line"/>
                )}/>
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
        )}
      </MonitorHistoryChart>
    </MonitorBlock>
  );
}

function NetworkMonitorValues({ data, children }: PropsWithChildren<{
  data: Pick<MonitorData, "networkUpload" | "networkDownload"> | null
}>) {
  return (
    <span className={cn("text-xs flex items-center [&>svg]:size-3", googleSansCode.className)}>
      {children}
      <MoveUp />
      {`${data ? formatDataSize(data.networkUpload) : "0 KB"}/s`}
      <MoveDown className="ml-2"/>
      {`${data ? formatDataSize(data.networkDownload) : "0KB"}/s`}
      <ArrowUpDown className="ml-2 mr-1"/>
      {`${
        data
        ? formatDataSize((data.networkUpload + data.networkDownload) / 2)
        : "0KB"
      }/s`}
    </span>
  );
}

function NetworkMonitorStatus() {
  const monitorDataList = useContext(MonitorContext);
  const latestData = (
    monitorDataList.length > 0
    ? monitorDataList[monitorDataList.length - 1]
    : null
  );

  return <NetworkMonitorValues data={latestData}/>;
}

export function NetworkMonitorBlock({ className }: {
  className?: string
}) {
  const { mode } = useMonitorHistory();

  return (
    <MonitorBlock
      title={$("monitor.network.title")}
      description={$("monitor.network.description")}
      additionalInfo={mode === "history" ? (
        <MonitorHistoryAverage metrics={["networkUpload", "networkDownload"]}>
          {(averages) => (
            <NetworkMonitorValues data={averages}>
              <span className="mr-3">{$("monitor.history.average")}</span>
            </NetworkMonitorValues>
          )}
        </MonitorHistoryAverage>
      ) : <NetworkMonitorStatus />}
      className={className}>
      <MonitorHistoryChart series={NETWORK_NAVIGATOR_SERIES}>
        {(data, isHistory, range) => (
          <ChartContainer
            config={{
              networkUpload: {
                label: $("monitor.chart.network-upload")
              },
              networkDownload: {
                label: $("monitor.chart.network-download")
              }
            }}
            className="w-full h-52">
            <AreaChart
              accessibilityLayer
              data={data}
              margin={{ top: 10, left: 0, right: 8, bottom: 4 }}>
              <CartesianGrid vertical={false} stroke="var(--border)"/>
              <Area
                dataKey="networkUpload"
                type="monotone"
                fill="transparent"
                stroke="var(--color-chart-5)"
                strokeWidth="2"
                connectNulls={false}
                isAnimationActive={false}/>
              <Area
                dataKey="networkDownload"
                type="monotone"
                fill="transparent"
                stroke="var(--color-chart-2)"
                strokeWidth="2"
                connectNulls={false}
                isAnimationActive={false}/>
              <YAxis hide domain={["auto", "auto"]}/>
              {isHistory && (
                <XAxis
                  dataKey="timestamp"
                  type="number"
                  scale="time"
                  allowDataOverflow
                  domain={range ? [range.from, range.to] : ["dataMin", "dataMax"]}
                  tickLine={false}
                  axisLine={false}
                  tickMargin={8}
                  minTickGap={32}
                  tickFormatter={(value) => formatMonitorHistoryAxisTick(value, range ? range.to - range.from : 0)}/>
              )}
              <ChartTooltip
                cursor={false}
                content={(
                  <ChartTooltipContent
                    hideLabel={!isHistory}
                    labelFormatter={(_, payload) => formatMonitorHistoryTooltip(payload)}
                    indicator="line"
                    valueFormatter={(value) => `${formatDataSize(parseInt(value.replaceAll(",", "")))}/s`}/>
                )}/>
            </AreaChart>
          </ChartContainer>
        )}
      </MonitorHistoryChart>
    </MonitorBlock>
  );
}

function DiskIOMonitorValues({ data, children }: PropsWithChildren<{
  data: Pick<MonitorData, "diskRead" | "diskWrite"> | null
}>) {
  return (
    <span className={cn("text-xs flex items-center [&>svg]:size-3", googleSansCode.className)}>
      {children}
      <span className="mr-2">Read</span>
      {`${data ? formatDataSize(data.diskRead) : "0 KB"}/s`}
      <span className="ml-3 mr-2">Write</span>
      {`${data ? formatDataSize(data.diskWrite) : "0KB"}/s`}
    </span>
  );
}

function DiskIOMonitorStatus() {
  const monitorDataList = useContext(MonitorContext);
  const latestData = (
    monitorDataList.length > 0
    ? monitorDataList[monitorDataList.length - 1]
    : null
  );

  return <DiskIOMonitorValues data={latestData}/>;
}

export function DiskIOMonitorBlock({ className }: {
  className?: string
}) {
  const { mode } = useMonitorHistory();

  return (
    <MonitorBlock
      title={$("monitor.disk-io.title")}
      description={$("monitor.disk-io.description")}
      additionalInfo={mode === "history" ? (
        <MonitorHistoryAverage metrics={["diskRead", "diskWrite"]}>
          {(averages) => (
            <DiskIOMonitorValues data={averages}>
              <span className="mr-3">{$("monitor.history.average")}</span>
            </DiskIOMonitorValues>
          )}
        </MonitorHistoryAverage>
      ) : <DiskIOMonitorStatus />}
      className={className}>
      <MonitorHistoryChart series={DISK_NAVIGATOR_SERIES}>
        {(data, isHistory, range) => (
          <ChartContainer
            config={{
              diskRead: {
                label: $("monitor.chart.disk-read")
              },
              diskWrite: {
                label: $("monitor.chart.disk-write")
              }
            }}
            className="w-full h-52">
            <AreaChart
              accessibilityLayer
              data={data}
              margin={{ top: 10, left: 0, right: 8, bottom: 4 }}>
              <CartesianGrid vertical={false} stroke="var(--border)"/>
              <Area
                dataKey="diskRead"
                type="monotone"
                fill="transparent"
                stroke="var(--color-chart-5)"
                strokeWidth="2"
                connectNulls={false}
                isAnimationActive={false}/>
              <Area
                dataKey="diskWrite"
                type="monotone"
                fill="transparent"
                stroke="var(--color-chart-2)"
                strokeWidth="2"
                connectNulls={false}
                isAnimationActive={false}/>
              <YAxis hide domain={["auto", "auto"]}/>
              {isHistory && (
                <XAxis
                  dataKey="timestamp"
                  type="number"
                  scale="time"
                  allowDataOverflow
                  domain={range ? [range.from, range.to] : ["dataMin", "dataMax"]}
                  tickLine={false}
                  axisLine={false}
                  tickMargin={8}
                  minTickGap={32}
                  tickFormatter={(value) => formatMonitorHistoryAxisTick(value, range ? range.to - range.from : 0)}/>
              )}
              <ChartTooltip
                cursor={false}
                content={(
                  <ChartTooltipContent
                    hideLabel={!isHistory}
                    labelFormatter={(_, payload) => formatMonitorHistoryTooltip(payload)}
                    indicator="line"
                    valueFormatter={(value) => `${formatDataSize(parseInt(value.replaceAll(",", "")))}/s`}/>
                )}/>
            </AreaChart>
          </ChartContainer>
        )}
      </MonitorHistoryChart>
    </MonitorBlock>
  );
}
