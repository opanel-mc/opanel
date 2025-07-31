import type { LogResponse } from "@/lib/types";
import download from "downloadjs";
import { toast } from "sonner";
import { sendDeleteRequest, sendGetRequest, toastError } from "@/lib/api";

export async function downloadLog(name: string) {
  const fileName = name.endsWith(".log.gz") ? name.replace(".log.gz", ".log") : name;
  try {
    const res = await sendGetRequest<LogResponse>(`/api/logs/${name}`);
    download(res.log, fileName, "text/plain");
  } catch (e: any) {
    toastError(e, `无法下载日志 ${fileName}`, [
      [401, "未登录"],
      [404, "找不到该日志"]
    ]);
  }
}

export async function deleteLog(name: string) {
  try {
    await sendDeleteRequest(`/api/logs/${name}`);
    toast.success("删除成功");
  } catch (e: any) {
    toastError(e, "无法删除日志", [
      [400, "请求参数错误"],
      [401, "未登录"],
      [404, "找不到该日志"]
    ]);
  }
}
