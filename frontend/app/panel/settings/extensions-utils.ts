import { apiUrl, sendDeleteRequest, sendPostRequest, toastError } from "@/lib/api";
import { emitter } from "@/lib/emitter";
import { $ } from "@/lib/i18n";

export function downloadExtension(fileName: string) {
  window.open(`${apiUrl}/api/extensions/${encodeURIComponent(fileName)}`, "_blank");
}

export async function toggleExtension(fileName: string, enabled: boolean) {
  try {
    await sendPostRequest(`/api/extensions/${encodeURIComponent(fileName)}?enabled=${enabled ? "1" : "0"}`);
    emitter.emit("refresh-data");
  } catch (e: any) {
    toastError(e, enabled
      ? $("settings.extensions.action.toggle.enable.error", fileName)
      : $("settings.extensions.action.toggle.disable.error", fileName), [
      [400, $("common.error.400")],
      [401, $("common.error.401")],
      [404, $("settings.extensions.action.toggle.error.404", fileName)],
      [500, $("common.error.500")]
    ]);
  }
}

export async function deleteExtension(fileName: string) {
  try {
    await sendDeleteRequest(`/api/extensions/${encodeURIComponent(fileName)}`);
    emitter.emit("refresh-data");
  } catch (e: any) {
    toastError(e, $("settings.extensions.action.delete.error", fileName), [
      [400, $("common.error.400")],
      [401, $("common.error.401")],
      [404, $("settings.extensions.action.delete.error.404", fileName)],
      [500, $("common.error.500")]
    ]);
  }
}
