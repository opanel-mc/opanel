import type { MonitorData } from "@/lib/types";
import { useEffect, useState } from "react";
import { MonitorClient } from "@/lib/ws/monitor";

function createInitialMonitorData(limit: number): MonitorData[] {
  return Array.from({ length: limit }, () => ({
    cpu: 0,
    memory: 0,
    jvmMemory: 0,
    tps: 20,
    networkUpload: 0,
    networkDownload: 0
  }));
}

export function useMonitor(limit: number): MonitorData[] {
  const safeLimit = Math.max(0, Math.floor(limit));
  const [monitorData, setMonitorData] = useState<MonitorData[]>(
    () => createInitialMonitorData(safeLimit)
  );

  useEffect(() => {
    setMonitorData(createInitialMonitorData(safeLimit));

    const client = new MonitorClient(safeLimit);

    client.subscribe("init", (data: MonitorData[]) => {
      setMonitorData(data);
    });

    client.subscribe("update", (data: MonitorData) => {
      setMonitorData((prev) => {
        const newData = [...prev];
        newData.shift();
        newData.push(data);
        return newData;
      });
    });

    return () => client.close();
  }, [safeLimit]);

  return monitorData;
}
