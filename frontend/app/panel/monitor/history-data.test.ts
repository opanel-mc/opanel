import type { MonitorData, MonitorHistoryPoint, MonitorHistoryResponse } from "@/lib/types";
import { describe, expect, it } from "vitest";
import {
  fillMonitorHistoryGaps,
  formatMonitorHistoryAxisTick,
  formatMonitorHistoryResolution,
  getMonitorHistoryRange
} from "./history-data";

function metrics(value: number): MonitorData {
  return {
    cpu: value,
    memory: value + 1,
    jvmMemory: value + 2,
    tps: value + 3,
    networkUpload: value + 4,
    networkDownload: value + 5,
    diskRead: value + 6,
    diskWrite: value + 7
  };
}

function point(timestamp: number, average: number): MonitorHistoryPoint {
  return {
    timestamp,
    durationMs: 60_000,
    sampleCount: 60,
    average: metrics(average),
    minimum: metrics(average - 10),
    maximum: metrics(average + 10)
  };
}

function response(points: MonitorHistoryPoint[]): MonitorHistoryResponse {
  return {
    from: 60_000,
    to: 360_000,
    resolutionMs: 60_000,
    points
  };
}

describe("monitor history data", () => {
  it("uses averages and inserts null slots for missing buckets", () => {
    const result = fillMonitorHistoryGaps(response([
      point(60_000, 40),
      point(180_000, 70)
    ]));

    expect(result).toHaveLength(3);
    expect(result[0]).toMatchObject({ timestamp: 60_000, cpu: 40, memory: 41 });
    expect(result[0]).not.toHaveProperty("minimum");
    expect(result[0]).not.toHaveProperty("maximum");
    expect(result[1]).toMatchObject({
      timestamp: 120_000,
      sampleCount: 0,
      cpu: null,
      diskWrite: null
    });
    expect(result[2].cpu).toBe(70);
  });

  it("sorts points before constructing the complete time grid", () => {
    const result = fillMonitorHistoryGaps(response([
      point(180_000, 70),
      point(60_000, 40)
    ]));

    expect(result.map(({ timestamp }) => timestamp)).toEqual([60_000, 120_000, 180_000]);
  });

  it("converts navigator indexes to a clamped half-open range", () => {
    const data = fillMonitorHistoryGaps(response([
      point(60_000, 40),
      point(180_000, 70)
    ]));

    expect(getMonitorHistoryRange(data, 1, 2, 225_000)).toEqual({
      from: 120_000,
      to: 225_000
    });
    expect(getMonitorHistoryRange(data, -10, 99, 360_000)).toEqual({
      from: 60_000,
      to: 240_000
    });
  });

  it("formats resolutions and adapts axis labels to the selected span", () => {
    expect(formatMonitorHistoryResolution(60_000)).toBe("1m");
    expect(formatMonitorHistoryResolution(15 * 60_000)).toBe("15m");
    expect(formatMonitorHistoryResolution(3_600_000)).toBe("1h");
    expect(formatMonitorHistoryAxisTick(Date.now(), 60 * 60_000)).not.toBe("");
  });
});
