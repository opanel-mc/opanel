import type { GameMode } from "@/lib/types";
import { toast } from "sonner";
import { sendDeleteRequest, sendPostRequest, toastError } from "@/lib/api";
import { gameModeToString, stringToBase64 } from "@/lib/utils";
import { $ } from "@/lib/i18n";

export async function giveOp(uuid: string, doToast = true) {
  try {
    await sendPostRequest(`/api/players/op?uuid=${uuid}`);
    doToast && toast.success($("players.action.op.success"));
  } catch (e: any) {
    toastError(e, $("players.action.op.error"), [
      [400, $("common.error.400")],
      [401, $("common.error.401")],
      [404, $("players.action.op.error.404")]
    ]);
  }
}

export async function depriveOp(uuid: string, doToast = true) {
  try {
    await sendPostRequest(`/api/players/deop?uuid=${uuid}`);
    doToast && toast.success($("players.action.deop.success"));
  } catch (e: any) {
    toastError(e, $("players.action.deop.error"), [
      [400, $("common.error.400")],
      [401, $("common.error.401")],
      [404, $("players.action.deop.error.404")]
    ]);
  }
}

export async function kick(uuid: string, reason?: string, doToast = true) {
  try {
    await sendPostRequest(`/api/players/kick?uuid=${uuid}&r=${reason ? stringToBase64(reason) : ""}`);
    doToast && toast.success($("players.action.kick.success"));
  } catch (e: any) {
    toastError(e, $("players.action.kick.error"), [
      [400, $("common.error.400")],
      [401, $("common.error.401")],
      [403, $("players.action.kick.error.403")],
      [404, $("players.action.kick.error.404")]
    ]);
  }
}

export async function ban(uuid: string, reason?: string, doToast = true) {
  try {
    await sendPostRequest(`/api/players/ban?uuid=${uuid}&r=${reason ? stringToBase64(reason) : ""}`);
    doToast && toast.success($("players.action.ban.success"));
  } catch (e: any) {
    toastError(e, $("players.action.ban.error"), [
      [400, $("common.error.400")],
      [401, $("common.error.401")],
      [404, $("players.action.ban.error.404")]
    ]);
  }
}

export async function pardon(uuid: string, doToast = true) {
  try {
    await sendPostRequest(`/api/players/pardon?uuid=${uuid}`);
    doToast && toast.success($("players.action.pardon.success"));
  } catch (e: any) {
    toastError(e, $("players.action.pardon.error"), [
      [400, $("common.error.400")],
      [401, $("common.error.401")],
      [404, $("players.action.pardon.error.404")]
    ]);
  }
}

export async function setGameMode(uuid: string, gamemode: GameMode, doToast = true) {
  try {
    await sendPostRequest(`/api/players/gamemode?uuid=${uuid}&gm=${gamemode}`);
    doToast && toast.success($("players.action.set-gamemode.success", gameModeToString(gamemode)));
  } catch (e: any) {
    toastError(e, $("players.action.set-gamemode.error"), [
      [400, $("common.error.400")],
      [401, $("common.error.401")],
      [404, $("players.action.set-gamemode.error.404")]
    ]);
  }
}

export async function removePlayerData(uuid: string, doToast = true) {
  try {
    await sendDeleteRequest(`/api/players?uuid=${uuid}`);
    doToast && toast.success($("players.action.remove.success"));
  } catch (e: any) {
    toastError(e, $("players.action.remove.error"), [
      [400, $("common.error.400")],
      [401, $("common.error.401")],
      [404, $("players.action.remove.error.404")],
      [500, $("common.error.500")]
    ]);
  }
}

export async function setWhitelistEnabled(enabled: boolean, doToast = true) {
  try {
    await sendPostRequest(`/api/whitelist/${enabled ? "enable" : "disable"}`);
    doToast && toast.success(
      enabled ? $("players.enable-whitelist.success") : $("players.disable-whitelist.success")
    );
  } catch (e: any) {
    toastError(e, enabled ? $("players.enable-whitelist.error") : $("players.disable-whitelist.error"), [
      [401, "未登录"],
      [500, "服务器内部错误"]
    ]);
  }
}

export async function addToWhitelist(name: string, uuid: string, doToast = true) {
  try {
    await sendPostRequest(`/api/whitelist/add?name=${name}&uuid=${uuid}`);
    doToast && toast.success($("players.action.add-to-whitelist.success"));
  } catch (e: any) {
    toastError(e, $("players.action.add-to-whitelist.error"), [
      [400, $("common.error.400")],
      [401, $("common.error.401")],
      [500, $("common.error.500")]
    ]);
  }
}

export async function removeFromWhitelist(name: string, uuid: string, doToast = true) {
  try {
    await sendPostRequest(`/api/whitelist/remove?name=${name}&uuid=${uuid}`);
    doToast && toast.success($("players.action.remove-from-whitelist.success"));
  } catch (e: any) {
    toastError(e, $("players.action.remove-from-whitelist.error"), [
      [400, $("common.error.400")],
      [401, $("common.error.401")],
      [500, $("common.error.500")]
    ]);
  }
}
