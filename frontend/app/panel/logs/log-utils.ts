import { toast } from "sonner";
import { apiUrl, sendDeleteRequest, toastError } from "@/lib/api";
import { $ } from "@/lib/i18n";

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
