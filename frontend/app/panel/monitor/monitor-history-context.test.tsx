import type { APIResponse, MonitorHistoryResponse } from "@/lib/types";
import type { ReactNode } from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { sendGetRequest } from "@/lib/api";
import {
  MonitorHistoryProvider,
  useMonitorHistory
} from "./monitor-history-context";

vi.mock("@/lib/api", () => ({
  sendGetRequest: vi.fn(),
  toastError: vi.fn()
}));

function response(values: number[]): APIResponse<MonitorHistoryResponse> {
  return {
    code: 200,
    error: "",
    from: 0,
    to: values.length * 60_000,
    resolutionMs: 60_000,
    points: values.map((value, index) => ({
      timestamp: index * 60_000,
      durationMs: 60_000,
      sampleCount: 60,
      average: {
        cpu: value,
        memory: value,
        jvmMemory: value,
        tps: value,
        networkUpload: value,
        networkDownload: value,
        diskRead: value,
        diskWrite: value
      },
      minimum: {
        cpu: value,
        memory: value,
        jvmMemory: value,
        tps: value,
        networkUpload: value,
        networkDownload: value,
        diskRead: value,
        diskWrite: value
      },
      maximum: {
        cpu: value,
        memory: value,
        jvmMemory: value,
        tps: value,
        networkUpload: value,
        networkDownload: value,
        diskRead: value,
        diskWrite: value
      }
    }))
  };
}

function Harness({ children }: { children?: ReactNode }) {
  const history = useMonitorHistory();
  return (
    <div>
      <span data-testid="mode">{history.mode}</span>
      <span data-testid="enabled">{String(history.enabled)}</span>
      <span data-testid="overview-status">{history.overviewStatus}</span>
      <span data-testid="detail-status">{history.detailStatus}</span>
      <span data-testid="detail-cpu">{history.detailData[0]?.cpu ?? ""}</span>
      <span data-testid="selection">
        {history.selection.startIndex}:{history.selection.endIndex}
      </span>
      <button onClick={() => history.commitSelection({ startIndex: 0, endIndex: 1 })}>
        first range
      </button>
      <button onClick={() => history.commitSelection({ startIndex: 1, endIndex: 2 })}>
        second range
      </button>
      <button onClick={history.returnToLive}>live</button>
      {children}
    </div>
  );
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((promiseResolve) => {
    resolve = promiseResolve;
  });
  return { promise, resolve };
}

describe("MonitorHistoryProvider", () => {
  beforeEach(() => vi.clearAllMocks());
  afterEach(() => {
    cleanup();
  });

  it("does not load history while the feature is disabled", async () => {
    const { rerender } = render(
      <MonitorHistoryProvider enabled={false}>
        <Harness />
      </MonitorHistoryProvider>
    );

    expect(screen.getByTestId("enabled")).toHaveTextContent("false");
    expect(screen.getByTestId("overview-status")).toHaveTextContent("idle");
    expect(sendGetRequest).not.toHaveBeenCalled();

    vi.mocked(sendGetRequest).mockResolvedValueOnce(response([10, 20, 30]));
    rerender(
      <MonitorHistoryProvider enabled>
        <Harness />
      </MonitorHistoryProvider>
    );

    await waitFor(() => expect(screen.getByTestId("overview-status")).toHaveTextContent("ready"));
    expect(sendGetRequest).toHaveBeenCalledTimes(1);
  });

  it("loads one full overview while keeping the default live mode", async () => {
    vi.mocked(sendGetRequest).mockResolvedValueOnce(response([10, 20, 30]));

    render(
      <MonitorHistoryProvider>
        <Harness />
      </MonitorHistoryProvider>
    );

    await waitFor(() => expect(screen.getByTestId("overview-status")).toHaveTextContent("ready"));
    expect(screen.getByTestId("mode")).toHaveTextContent("live");
    expect(sendGetRequest).toHaveBeenCalledTimes(1);

    const url = new URL(vi.mocked(sendGetRequest).mock.calls[0][0], "http://localhost");
    expect(url.searchParams.get("maxPoints")).toBe("1000");
    expect(Number(url.searchParams.get("to")) - Number(url.searchParams.get("from")))
      .toBe(3650 * 24 * 60 * 60 * 1000);
  });

  it("refetches from the discovered first bucket so the full overview keeps its shape", async () => {
    const now = Date.now();
    const broad = response([10, 20]);
    broad.to = now;
    broad.resolutionMs = 4 * 24 * 60 * 60 * 1000;
    broad.points[0].timestamp = now - 2 * 24 * 60 * 60 * 1000;
    broad.points[0].durationMs = broad.resolutionMs;
    broad.points[1].timestamp = now - 24 * 60 * 60 * 1000;
    broad.points[1].durationMs = broad.resolutionMs;

    const refined = response([30, 40, 50]);
    refined.to = now;
    refined.points.forEach((point, index) => {
      point.timestamp = now - (3 - index) * 60_000;
    });

    vi.mocked(sendGetRequest)
      .mockResolvedValueOnce(broad)
      .mockResolvedValueOnce(refined);

    render(
      <MonitorHistoryProvider>
        <Harness />
      </MonitorHistoryProvider>
    );

    await waitFor(() => expect(screen.getByTestId("overview-status")).toHaveTextContent("ready"));
    expect(sendGetRequest).toHaveBeenCalledTimes(2);
    const refinedUrl = new URL(vi.mocked(sendGetRequest).mock.calls[1][0], "http://localhost");
    expect(refinedUrl.searchParams.get("from")).toBe(String(broad.points[0].timestamp));
  });

  it("keeps realtime monitoring available when no history exists", async () => {
    vi.mocked(sendGetRequest)
      .mockResolvedValueOnce(response([]))
      .mockResolvedValueOnce(response([]));

    render(
      <MonitorHistoryProvider>
        <Harness />
      </MonitorHistoryProvider>
    );

    await waitFor(() => expect(screen.getByTestId("overview-status")).toHaveTextContent("empty"));
    expect(screen.getByTestId("mode")).toHaveTextContent("live");
  });

  it("places the frozen live selection at the real end of the overview", async () => {
    vi.mocked(sendGetRequest)
      .mockResolvedValueOnce(response([
        10, 20, 30, 40, 50, 60, 70, 80, 90, 100
      ]))
      .mockResolvedValueOnce(response([10, 20]));

    render(
      <MonitorHistoryProvider>
        <Harness />
      </MonitorHistoryProvider>
    );

    await waitFor(() => expect(screen.getByTestId("overview-status")).toHaveTextContent("ready"));
    expect(screen.getByTestId("selection")).toHaveTextContent("6:9");

    fireEvent.click(screen.getByRole("button", { name: "first range" }));
    await waitFor(() => expect(screen.getByTestId("mode")).toHaveTextContent("history"));
    fireEvent.click(screen.getByRole("button", { name: "live" }));
    expect(screen.getByTestId("selection")).toHaveTextContent("6:9");
    expect(sendGetRequest).toHaveBeenCalledTimes(2);
  });

  it("reports a 503 as unavailable without leaving realtime mode", async () => {
    vi.mocked(sendGetRequest).mockRejectedValueOnce({ status: 503 });

    render(
      <MonitorHistoryProvider>
        <Harness />
      </MonitorHistoryProvider>
    );

    await waitFor(() => expect(screen.getByTestId("overview-status")).toHaveTextContent("unavailable"));
    expect(screen.getByTestId("mode")).toHaveTextContent("live");
  });

  it("shares a committed detail query and returns explicitly to live mode", async () => {
    vi.mocked(sendGetRequest)
      .mockResolvedValueOnce(response([10, 20, 30]))
      .mockResolvedValueOnce(response([40, 50]));

    render(
      <MonitorHistoryProvider>
        <Harness />
        <Harness />
      </MonitorHistoryProvider>
    );
    await waitFor(() => expect(screen.getAllByTestId("overview-status")[0]).toHaveTextContent("ready"));

    fireEvent.click(screen.getAllByRole("button", { name: "first range" })[0]);
    await waitFor(() => expect(screen.getAllByTestId("detail-status")[0]).toHaveTextContent("ready"));

    expect(sendGetRequest).toHaveBeenCalledTimes(2);
    expect(screen.getAllByTestId("mode")[0]).toHaveTextContent("history");

    fireEvent.click(screen.getAllByRole("button", { name: "live" })[1]);
    expect(screen.getAllByTestId("mode")[0]).toHaveTextContent("live");
    expect(screen.getAllByTestId("mode")[1]).toHaveTextContent("live");
  });

  it("ignores a stale detail response after a newer range has completed", async () => {
    const first = deferred<APIResponse<MonitorHistoryResponse>>();
    const second = deferred<APIResponse<MonitorHistoryResponse>>();
    vi.mocked(sendGetRequest)
      .mockResolvedValueOnce(response([10, 20, 30]))
      .mockImplementationOnce(() => first.promise as never)
      .mockImplementationOnce(() => second.promise as never);

    render(
      <MonitorHistoryProvider>
        <Harness />
      </MonitorHistoryProvider>
    );
    await waitFor(() => expect(screen.getByTestId("overview-status")).toHaveTextContent("ready"));

    fireEvent.click(screen.getByRole("button", { name: "first range" }));
    fireEvent.click(screen.getByRole("button", { name: "second range" }));
    second.resolve(response([80]));
    await waitFor(() => expect(screen.getByTestId("detail-cpu")).toHaveTextContent("80"));

    first.resolve(response([5]));
    await Promise.resolve();
    expect(screen.getByTestId("detail-cpu")).toHaveTextContent("80");
  });
});
