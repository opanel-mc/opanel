import type { LogResponse } from "@/lib/types";
import download from "downloadjs";
import { toast } from "sonner";
import { sendDeleteRequest, sendGetRequest } from "@/lib/api";

export async function downloadLog(name: string) {
  const fileName = name.endsWith(".log.gz") ? name.replace(".log.gz", ".log") : name;
  try {
    const res = await sendGetRequest<LogResponse>(`/api/logs/${name}`);
    download(res.log, fileName, "text/plain");
  } catch (e: any) {
    toast.error(`无法下载日志 ${fileName}`, { description: e.message });
  }
}

export async function deleteLog(name: string) {
  try {
    await sendDeleteRequest(`/api/logs/${name}`);
    toast.success("删除成功");
  } catch (e: any) {
    toast.error("删除失败", { description: e.message });
  }
}
