import download from "downloadjs";
import { toast } from "sonner";
import { sendDeleteRequest, sendGetRequest, toastError } from "@/lib/api";
import { $ } from "@/lib/i18n";

export async function downloadLog(name: string) {
  const fileName = name.endsWith(".log.gz") ? name.replace(".log.gz", ".log") : name;
  try {
    const res = await sendGetRequest<string>(`/api/logs/${name}`);
    download(res, fileName, "text/plain");
  } catch (e: any) {
    toastError(e, $("logs.action.download.error", fileName), [
      [401, $("common.error.401")],
      [404, $("logs.action.download.error.404")]
    ]);
  }
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
