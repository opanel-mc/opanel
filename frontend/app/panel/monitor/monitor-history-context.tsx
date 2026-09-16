"use client";

import type { AxiosError } from "axios";
import type { MonitorHistoryResponse } from "@/lib/types";
import type { PropsWithChildren } from "react";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState
} from "react";
import { sendGetRequest, toastError } from "@/lib/api";
import { $ } from "@/lib/i18n";
import {
  fillMonitorHistoryGaps,
  getMonitorHistoryRange,
  type MonitorHistoryChartData,
  type MonitorHistoryRange
} from "./history-data";

const MAX_RETENTION_DAYS = 3650;
const MAX_POINTS = 1000;
const RECENT_OVERVIEW_DAYS = 1;
const LIVE_WINDOW_MS = 200_000;

export type MonitorHistoryMode = "live" | "history";
export type MonitorHistoryLoadStatus = (
  "idle"
  | "loading"
  | "ready"
  | "empty"
  | "unavailable"
  | "error"
);

export interface MonitorHistorySelection {
  startIndex: number
  endIndex: number
}

interface MonitorHistoryContextValue {
  mode: MonitorHistoryMode
  overviewData: MonitorHistoryChartData[]
  detailData: MonitorHistoryChartData[]
  overviewStatus: MonitorHistoryLoadStatus
  detailStatus: MonitorHistoryLoadStatus
  selection: MonitorHistorySelection
  selectedRange: MonitorHistoryRange | null
  detailResolutionMs: number | null
  commitSelection: (selection: MonitorHistorySelection) => void
  returnToLive: () => void
  retryOverview: () => void
  retryDetail: () => void
}

const MonitorHistoryContext = createContext<MonitorHistoryContextValue | null>(null);

function normalizeSelection(
  selection: MonitorHistorySelection,
  dataLength: number
): MonitorHistorySelection {
  if(dataLength <= 0) return { startIndex: 0, endIndex: 0 };

  const startIndex = Math.max(0, Math.min(selection.startIndex, dataLength - 1));
  const endIndex = Math.max(startIndex, Math.min(selection.endIndex, dataLength - 1));
  return { startIndex, endIndex };
}

function getLiveSelection(
  data: MonitorHistoryChartData[],
  overviewTo: number
): MonitorHistorySelection {
  const endIndex = Math.max(0, data.length - 1);
  const liveFrom = overviewTo - LIVE_WINDOW_MS;
  const firstLiveIndex = data.findIndex(({ timestamp, durationMs }) => (
    timestamp + durationMs > liveFrom
  ));
  return {
    startIndex: firstLiveIndex < 0 ? endIndex : firstLiveIndex,
    endIndex
  };
}

function getErrorStatus(error: unknown): number | undefined {
  const axiosError = error as AxiosError;
  return axiosError.status ?? axiosError.response?.status;
}

function historyUrl(from: number, to: number): string {
  const params = new URLSearchParams({
    from: String(Math.floor(from)),
    to: String(Math.ceil(to)),
    maxPoints: String(MAX_POINTS)
  });
  return `/api/monitor/history?${params}`;
}

export function MonitorHistoryProvider({ children }: PropsWithChildren) {
  const [mode, setMode] = useState<MonitorHistoryMode>("live");
  const [overviewData, setOverviewData] = useState<MonitorHistoryChartData[]>([]);
  const [detailData, setDetailData] = useState<MonitorHistoryChartData[]>([]);
  const [overviewStatus, setOverviewStatus] = useState<MonitorHistoryLoadStatus>("loading");
  const [detailStatus, setDetailStatus] = useState<MonitorHistoryLoadStatus>("idle");
  const [selection, setSelection] = useState<MonitorHistorySelection>({
    startIndex: 0,
    endIndex: 0
  });
  const [selectedRange, setSelectedRange] = useState<MonitorHistoryRange | null>(null);
  const [detailResolutionMs, setDetailResolutionMs] = useState<number | null>(null);

  const modeRef = useRef<MonitorHistoryMode>("live");
  const overviewDataRef = useRef<MonitorHistoryChartData[]>([]);
  const overviewResponseRef = useRef<MonitorHistoryResponse | null>(null);
  const overviewRequestId = useRef(0);
  const detailRequestId = useRef(0);
  const initialLiveSelection = useRef<MonitorHistorySelection>({ startIndex: 0, endIndex: 0 });

  const changeMode = useCallback((nextMode: MonitorHistoryMode) => {
    modeRef.current = nextMode;
    setMode(nextMode);
  }, []);

  const handleError = useCallback((error: unknown): MonitorHistoryLoadStatus => {
    const status = getErrorStatus(error);
    if(status === 401) {
      toastError(error as AxiosError, $("dashboard.error"), [
        [401, $("common.error.401")]
      ]);
    }
    return status === 503 ? "unavailable" : "error";
  }, []);

  const refreshOverview = useCallback(async () => {
    const requestId = ++overviewRequestId.current;
    if(overviewDataRef.current.length === 0) setOverviewStatus("loading");

    const to = Date.now();
    const from = to - MAX_RETENTION_DAYS * 24 * 60 * 60 * 1000;
    try {
      const broadResponse = await sendGetRequest<MonitorHistoryResponse>(historyUrl(from, to));
      if(requestId !== overviewRequestId.current) return;

      let response = broadResponse;
      if(broadResponse.points.length > 0) {
        const firstTimestamp = Math.min(...broadResponse.points.map(({ timestamp }) => timestamp));
        const refinedFrom = Math.max(from, firstTimestamp);
        if(refinedFrom - from >= broadResponse.resolutionMs) {
          const refinedResponse = await sendGetRequest<MonitorHistoryResponse>(historyUrl(refinedFrom, to));
          if(requestId !== overviewRequestId.current) return;
          if(refinedResponse.points.length > 0) response = refinedResponse;
        }
      } else {
        const recentFrom = to - RECENT_OVERVIEW_DAYS * 24 * 60 * 60 * 1000;
        response = await sendGetRequest<MonitorHistoryResponse>(historyUrl(recentFrom, to));
        if(requestId !== overviewRequestId.current) return;
      }

      const data = fillMonitorHistoryGaps(response);
      overviewDataRef.current = data;
      overviewResponseRef.current = response;
      setOverviewData(data);
      setOverviewStatus(data.length === 0 ? "empty" : "ready");

      if(modeRef.current === "live") {
        const liveSelection = getLiveSelection(data, response.to);
        initialLiveSelection.current = liveSelection;
        setSelection(liveSelection);
      }
    } catch(error) {
      if(requestId !== overviewRequestId.current) return;
      setOverviewStatus(handleError(error));
    }
  }, [handleError]);

  const loadDetail = useCallback(async (range: MonitorHistoryRange) => {
    const requestId = ++detailRequestId.current;
    changeMode("history");
    setSelectedRange(range);
    setDetailData([]);
    setDetailResolutionMs(null);
    setDetailStatus("loading");

    try {
      const response = await sendGetRequest<MonitorHistoryResponse>(historyUrl(range.from, range.to));
      if(requestId !== detailRequestId.current) return;

      const data = fillMonitorHistoryGaps(response);
      setDetailData(data);
      setDetailResolutionMs(response.resolutionMs);
      setDetailStatus(data.length === 0 ? "empty" : "ready");
    } catch(error) {
      if(requestId !== detailRequestId.current) return;
      setDetailStatus(handleError(error));
    }
  }, [changeMode, handleError]);

  const commitRange = useCallback((nextSelection: MonitorHistorySelection) => {
    const response = overviewResponseRef.current;
    const data = overviewDataRef.current;
    if(!response || data.length === 0) return;

    const safeSelection = normalizeSelection(nextSelection, data.length);
    setSelection(safeSelection);
    const range = getMonitorHistoryRange(
      data,
      safeSelection.startIndex,
      safeSelection.endIndex,
      response.to
    );
    if(range) void loadDetail(range);
  }, [loadDetail]);

  const commitSelection = useCallback((nextSelection: MonitorHistorySelection) => {
    commitRange(nextSelection);
  }, [commitRange]);

  const returnToLive = useCallback(() => {
    detailRequestId.current++;
    changeMode("live");
    setDetailData([]);
    setDetailStatus("idle");
    setDetailResolutionMs(null);
    setSelectedRange(null);

    setSelection(initialLiveSelection.current);
  }, [changeMode]);

  const retryDetail = useCallback(() => {
    if(selectedRange) void loadDetail(selectedRange);
  }, [loadDetail, selectedRange]);

  const cancelPendingRequests = useCallback(() => {
    overviewRequestId.current++;
    detailRequestId.current++;
  }, []);

  useEffect(() => {
    void refreshOverview();
    return cancelPendingRequests;
  }, [cancelPendingRequests, refreshOverview]);

  const value = useMemo<MonitorHistoryContextValue>(() => ({
    mode,
    overviewData,
    detailData,
    overviewStatus,
    detailStatus,
    selection,
    selectedRange,
    detailResolutionMs,
    commitSelection,
    returnToLive,
    retryOverview: () => void refreshOverview(),
    retryDetail
  }), [
    mode,
    overviewData,
    detailData,
    overviewStatus,
    detailStatus,
    selection,
    selectedRange,
    detailResolutionMs,
    commitSelection,
    returnToLive,
    refreshOverview,
    retryDetail
  ]);

  return (
    <MonitorHistoryContext.Provider value={value}>
      {children}
    </MonitorHistoryContext.Provider>
  );
}

export function useMonitorHistory(): MonitorHistoryContextValue {
  const context = useContext(MonitorHistoryContext);
  if(!context) throw new Error("useMonitorHistory must be used within MonitorHistoryProvider");
  return context;
}
