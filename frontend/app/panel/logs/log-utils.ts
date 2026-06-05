import { toast } from "sonner";
import { apiUrl, sendDeleteRequest, toastError } from "@/lib/api";
import { $ } from "@/lib/i18n";

type LogType = "gzip" | "log";

type LogData = {
  name: string;
  type: LogType;
};

export const LOG_ARCHIVE_PATTERN = /^(\d{4})-(\d{2})-(\d{2})-(\d+)\.log\.gz$/;

export function sortLogs(logs: string[]): LogData[] {
  return logs
    .map((name) => ({
      name,
      type: (name.endsWith(".log.gz") ? "gzip" : "log") as LogType
    }))
    .sort((a, b) => {
      const orderA = (
        a.name === "latest.log"
        ? 0
        : a.name === "debug.log"
          ? 1
          : 2
      );
      const orderB = (
        b.name === "latest.log"
        ? 0
        : b.name === "debug.log"
          ? 1
          : 2
      );
      if(orderA !== orderB) return orderA - orderB;
      if(orderA < 2) return 0;

      const archiveA = LOG_ARCHIVE_PATTERN.exec(a.name);
      const archiveB = LOG_ARCHIVE_PATTERN.exec(b.name);
      if(archiveA && archiveB) {
        const dateA = Number(`${archiveA[1]}${archiveA[2]}${archiveA[3]}`);
        const dateB = Number(`${archiveB[1]}${archiveB[2]}${archiveB[3]}`);
        if(dateA !== dateB) return dateB - dateA;
        return Number(archiveB[4]) - Number(archiveA[4]);
      }
      if(archiveA) return -1;
      if(archiveB) return 1;
      return a.name.localeCompare(b.name);
    });
}

export async function downloadLog(name: string) {
  window.open(`${apiUrl}/api/logs/${name}/download`, "_blank");
}

export async function deleteLog(name: string) {
  try {
    await sendDeleteRequest(`/api/logs/${name}`);
    toast.success($("logs.action.delete.success"));
  } catch (e: any) {
    toastError(e, $("logs.action.delete.error"), [
      [400, $("common.error.400")],
      [401, $("common.error.401")],
      [403, $("logs.action.delete.error.403")],
      [404, $("logs.action.delete.error.404")]
    ]);
  }
}
