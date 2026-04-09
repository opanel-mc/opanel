import { describe, expect, it } from "vitest";
import { sortLogs } from "./log-utils";

describe("sortLogs", () => {
  it("should put latest.log and debug.log at the front in order", () => {
    const result = sortLogs([
      "2026-04-08-1.log.gz",
      "debug.log",
      "2026-04-09-2.log.gz",
      "latest.log"
    ]);

    expect(result.map((item) => item.name)).toEqual([
      "latest.log",
      "debug.log",
      "2026-04-09-2.log.gz",
      "2026-04-08-1.log.gz"
    ]);
  });

  it("should sort archived logs from newest to oldest", () => {
    const result = sortLogs([
      "2026-04-07-2.log.gz",
      "2026-04-08-1.log.gz",
      "2026-04-07-3.log.gz",
      "2026-04-09-1.log.gz"
    ]);

    expect(result.map((item) => item.name)).toEqual([
      "2026-04-09-1.log.gz",
      "2026-04-08-1.log.gz",
      "2026-04-07-3.log.gz",
      "2026-04-07-2.log.gz"
    ]);
  });
});
