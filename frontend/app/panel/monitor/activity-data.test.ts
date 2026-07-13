import type { ActivityData } from "@/lib/types";
import { afterEach, describe, expect, it, vi } from "vitest";
import { fillActivityData } from "./activity-data";

function activity(date: string, playerName?: string): ActivityData {
  return {
    date: `${date}T00:00:00`,
    players: playerName ? [{ name: playerName, uuid: playerName }] : []
  };
}

describe("fillActivityData", () => {
  afterEach(() => vi.useRealTimers());

  it("fills missing dates and pads the beginning to 30 entries", () => {
    const activities = [
      activity("2026-06-10", "Alex"),
      activity("2026-06-12", "Steve")
    ];

    const result = fillActivityData(activities);

    expect(result).toHaveLength(30);
    expect(result[0]).toEqual({ date: "2026-05-14T00:00:00", players: [] });
    expect(result.slice(-3)).toEqual([
      activities[0],
      activity("2026-06-11"),
      activities[1]
    ]);
    expect(activities).toHaveLength(2);
  });

  it("creates the latest 30 dated empty entries when no activity exists", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-07-13T12:00:00"));

    const result = fillActivityData([]);

    expect(result).toHaveLength(30);
    expect(result[0].date).toBe("2026-06-14T00:00:00");
    expect(result[29].date).toBe("2026-07-13T00:00:00");
    expect(result.every((item) => item.players.length === 0)).toBe(true);
  });

  it("does not remove dates when filling a gap produces more than 30 entries", () => {
    const result = fillActivityData([
      activity("2026-01-01"),
      activity("2026-02-01")
    ]);

    expect(result).toHaveLength(32);
    expect(result[0].date).toBe("2026-01-01T00:00:00");
    expect(result[31].date).toBe("2026-02-01T00:00:00");
  });
});
