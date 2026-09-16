"use client";

import type { ChartConfig } from "@/components/ui/chart";
import type { MonitorData } from "@/lib/types";
import type { ReactNode } from "react";
import { memo, useContext, useEffect, useMemo, useState } from "react";
import { Area, AreaChart } from "recharts";
import { Radio, RefreshCw } from "lucide-react";
import { ChartContainer } from "@/components/ui/chart";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Slider } from "@/components/ui/slider";
import { Spinner } from "@/components/ui/spinner";
import { MonitorContext } from "@/contexts/api-context";
import { $ } from "@/lib/i18n";
import { cn } from "@/lib/utils";
import {
  formatMonitorHistoryRange,
  type MonitorHistoryChartData,
  type MonitorHistoryRange,
  type MonitorMetric
} from "./history-data";
import {
  type MonitorHistoryLoadStatus,
  useMonitorHistory
} from "./monitor-history-context";

export type MonitorChartData = MonitorData | MonitorHistoryChartData;

export interface MonitorNavigatorSeries {
  dataKey: MonitorMetric
  label: string
  color: string
}

const MonitorHistoryOverviewPlot = memo(function MonitorHistoryOverviewPlot({
  config,
  data,
  series
}: {
  config: ChartConfig
  data: MonitorHistoryChartData[]
  series: MonitorNavigatorSeries[]
}) {
  return (
    <ChartContainer
      config={config}
      className="pointer-events-none absolute inset-0 h-full w-full">
      <AreaChart
        accessibilityLayer
        data={data}
        margin={{ top: 3, right: 5, bottom: 3, left: 5 }}>
        {series.map(({ dataKey, color }) => (
          <Area
            key={dataKey}
            dataKey={dataKey}
            type="linear"
            fill={color}
            fillOpacity={0.08}
            stroke={color}
            strokeWidth={1}
            connectNulls={false}
            isAnimationActive={false}/>
        ))}
      </AreaChart>
    </ChartContainer>
  );
});

function StatusMessage({
  status,
  retry,
  compact = false
}: {
  status: MonitorHistoryLoadStatus
  retry: () => void
  compact?: boolean
}) {
  if(status === "idle" || status === "loading") {
    return compact
      ? <Skeleton className="h-12 w-full rounded-sm"/>
      : (
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Spinner />{$("monitor.history.loading")}
        </div>
      );
  }

  const message = (
    status === "empty"
    ? $("monitor.history.empty")
    : status === "unavailable"
    ? $("monitor.history.unavailable")
    : $("monitor.history.error")
  );

  return (
    <div className={cn(
      "flex items-center justify-center gap-2 text-xs text-muted-foreground",
      compact ? "h-12 rounded-sm border border-dashed bg-muted/20" : "text-sm"
    )}>
      <span>{message}</span>
      {status !== "empty" && (
        <Button
          variant="ghost"
          size="icon-xs"
          title={$("monitor.history.retry")}
          aria-label={$("monitor.history.retry")}
          onClick={retry}>
          <RefreshCw />
        </Button>
      )}
    </div>
  );
}

const MonitorHistoryNavigator = memo(function MonitorHistoryNavigator({
  series
}: {
  series: MonitorNavigatorSeries[]
}) {
  const {
    mode,
    overviewData,
    overviewStatus,
    selection,
    selectedRange,
    commitSelection,
    returnToLive,
    retryOverview
  } = useMonitorHistory();

  const config = useMemo<ChartConfig>(() => Object.fromEntries(
    series.map(({ dataKey, label, color }) => [dataKey, { label, color }])
  ), [series]);
  const [draftSelection, setDraftSelection] = useState(selection);

  useEffect(() => {
    setDraftSelection(selection);
  }, [selection]);

  const rangeLabel = (
    mode === "live"
    ? $("monitor.history.live-range")
    : selectedRange
    ? formatMonitorHistoryRange(selectedRange)
    : $("monitor.history.range")
  );

  return (
    <div className="border-t bg-muted/10 px-3 py-2">
      <div className="mb-2 flex min-w-0 flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
        <span className="min-w-0 flex-1 truncate" title={rangeLabel}>{rangeLabel}</span>
        <Button
          variant={mode === "live" ? "secondary" : "ghost"}
          size="xs"
          className="shrink-0 cursor-pointer"
          onClick={returnToLive}>
          <Radio className={cn(mode === "live" && "text-emerald-600 dark:text-emerald-400")}/>
          {$("monitor.history.live")}
        </Button>
      </div>

      {overviewStatus === "ready" ? (
        <div className="relative h-12 overflow-hidden rounded-sm border bg-muted/20">
          <MonitorHistoryOverviewPlot config={config} data={overviewData} series={series}/>
          <Slider
            aria-label={$("monitor.history.navigator")}
            min={0}
            max={Math.max(0, overviewData.length - 1)}
            step={1}
            minStepsBetweenThumbs={0}
            value={[draftSelection.startIndex, draftSelection.endIndex]}
            onValueChange={([startIndex, endIndex]) => {
              setDraftSelection({ startIndex, endIndex });
            }}
            onValueCommit={([startIndex, endIndex]) => {
              const nextSelection = { startIndex, endIndex };
              setDraftSelection(nextSelection);
              commitSelection(nextSelection);
            }}
            className={cn(
              "absolute inset-0 z-10 h-12 px-[3px]",
              "[&_[data-slot=slider-track]]:h-full",
              "[&_[data-slot=slider-track]]:rounded-sm",
              "[&_[data-slot=slider-track]]:bg-transparent",
              "[&_[data-slot=slider-range]]:border-x",
              "[&_[data-slot=slider-range]]:border-foreground/40",
              "[&_[data-slot=slider-range]]:bg-foreground/15",
              "[&_[data-slot=slider-thumb]]:h-6",
              "[&_[data-slot=slider-thumb]]:w-1.5",
              "[&_[data-slot=slider-thumb]]:rounded-full",
              "[&_[data-slot=slider-thumb]]:border-background",
              "[&_[data-slot=slider-thumb]]:bg-foreground",
              "[&_[data-slot=slider-thumb]]:ring-0!",
              "[&_[data-slot=slider-thumb]]:hover:cursor-ew-resize",
            )}/>
        </div>
      ) : (
        <StatusMessage status={overviewStatus} retry={retryOverview} compact/>
      )}
    </div>
  );
});

export function MonitorHistoryChart({
  series,
  children
}: {
  series: MonitorNavigatorSeries[]
  children: (
    data: MonitorChartData[],
    isHistory: boolean,
    selectedRange: MonitorHistoryRange | null
  ) => ReactNode
}) {
  const liveData = useContext(MonitorContext);
  const {
    enabled,
    mode,
    detailData,
    detailStatus,
    selectedRange,
    retryDetail
  } = useMonitorHistory();
  const isHistory = mode === "history";
  const data = isHistory ? detailData : liveData;

  return (
    <>
      <div className="relative">
        {children(data, isHistory, selectedRange)}
        {isHistory && detailStatus !== "ready" && (
          <div className="absolute inset-0 flex items-center justify-center bg-background/80 backdrop-blur-[1px]">
            <StatusMessage status={detailStatus} retry={retryDetail}/>
          </div>
        )}
      </div>
      {enabled && <MonitorHistoryNavigator series={series}/>}
    </>
  );
}
