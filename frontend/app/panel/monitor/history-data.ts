import type { MonitorData, MonitorHistoryPoint, MonitorHistoryResponse } from "@/lib/types";

export type MonitorMetric = keyof MonitorData;

export type MonitorHistoryChartData = {
  timestamp: number
  durationMs: number
  sampleCount: number
} & Record<MonitorMetric, number | null>;

export interface MonitorHistoryRange {
  from: number
  to: number
}

const EMPTY_METRICS: Record<MonitorMetric, null> = {
  cpu: null,
  memory: null,
  jvmMemory: null,
  tps: null,
  networkUpload: null,
  networkDownload: null,
  diskRead: null,
  diskWrite: null
};

function pointToChartData(point: MonitorHistoryPoint): MonitorHistoryChartData {
  return {
    timestamp: point.timestamp,
    durationMs: point.durationMs,
    sampleCount: point.sampleCount,
    ...point.average
  };
}

export function fillMonitorHistoryGaps(response: MonitorHistoryResponse): MonitorHistoryChartData[] {
  if(response.points.length === 0 || response.resolutionMs <= 0) return [];

  const points = [...response.points].sort((a, b) => a.timestamp - b.timestamp);
  const pointByTimestamp = new Map(points.map((point) => [point.timestamp, point]));
  const firstTimestamp = points[0].timestamp;
  const lastTimestamp = points[points.length - 1].timestamp;
  const result: MonitorHistoryChartData[] = [];

  for(
    let timestamp = firstTimestamp;
    timestamp <= lastTimestamp;
    timestamp += response.resolutionMs
  ) {
    const point = pointByTimestamp.get(timestamp);
    result.push(
      point
      ? pointToChartData(point)
      : {
        timestamp,
        durationMs: response.resolutionMs,
        sampleCount: 0,
        ...EMPTY_METRICS
      }
    );
  }

  return result;
}

export function getMonitorHistoryRange(
  data: MonitorHistoryChartData[],
  startIndex: number,
  endIndex: number,
  maximumTo: number
): MonitorHistoryRange | null {
  if(data.length === 0) return null;

  const safeStartIndex = Math.max(0, Math.min(startIndex, data.length - 1));
  const safeEndIndex = Math.max(safeStartIndex, Math.min(endIndex, data.length - 1));
  const start = data[safeStartIndex];
  const end = data[safeEndIndex];
  const to = Math.min(maximumTo, end.timestamp + end.durationMs);

  if(start.timestamp >= to) return null;
  return { from: start.timestamp, to };
}

export function formatMonitorHistoryRange(range: MonitorHistoryRange): string {
  const formatter = new Intl.DateTimeFormat(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  });
  return `${formatter.format(range.from)} – ${formatter.format(range.to)}`;
}

export function formatMonitorHistoryResolution(resolutionMs: number): string {
  const seconds = Math.round(resolutionMs / 1000);
  if(seconds < 60) return `${seconds}s`;
  if(seconds < 3600) return `${Math.round(seconds / 60)}m`;
  if(seconds < 86400) return `${Math.round(seconds / 3600)}h`;
  return `${Math.round(seconds / 86400)}d`;
}

export function formatMonitorHistoryAxisTick(timestamp: number, span: number): string {
  const options: Intl.DateTimeFormatOptions = (
    span <= 24 * 60 * 60 * 1000
    ? { hour: "2-digit", minute: "2-digit" }
    : span <= 180 * 24 * 60 * 60 * 1000
    ? { month: "short", day: "numeric" }
    : { year: "numeric", month: "short" }
  );
  return new Intl.DateTimeFormat(undefined, options).format(timestamp);
}

export function formatMonitorHistoryTooltip(
  payload: readonly { payload?: unknown }[] | undefined
): string {
  const value = payload?.[0]?.payload as Partial<MonitorHistoryChartData> | undefined;
  if(typeof value?.timestamp !== "number" || typeof value.durationMs !== "number") return "";

  const formatter = new Intl.DateTimeFormat(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: value.durationMs < 60_000 ? "2-digit" : undefined
  });
  return `${formatter.format(value.timestamp)} – ${formatter.format(value.timestamp + value.durationMs)}`;
}
