import type { GameMode } from "@/lib/types";
import { toast } from "sonner";
import { sendPostRequest, toastError } from "@/lib/api";
import { gameModeToString } from "@/lib/utils";

export async function giveOp(uuid: string, doToast = true) {
  try {
    await sendPostRequest(`/api/players/op?uuid=${uuid}`);
    doToast && toast.success("成功给予该玩家OP权限");
  } catch (e: any) {
    toastError(e, "无法给予该玩家OP权限", [
      [400, "请求参数错误"],
      [401, "未登录"]
    ]);
  }
}

export async function depriveOp(uuid: string, doToast = true) {
  try {
    await sendPostRequest(`/api/players/deop?uuid=${uuid}`);
    doToast && toast.success("成功解除该玩家OP权限");
  } catch (e: any) {
    toastError(e, "无法解除该玩家OP权限", [
      [400, "请求参数错误"],
      [401, "未登录"]
    ]);
  }
}

export async function kick(uuid: string, reason?: string, doToast = true) {
  try {
    await sendPostRequest(`/api/players/kick?uuid=${uuid}&r=${reason}`);
    doToast && toast.success("已踢出该玩家");
  } catch (e: any) {
    toastError(e, "无法踢出该玩家", [
      [400, "请求参数错误"],
      [401, "未登录"],
      [403, "该玩家不在线"]
    ]);
  }
}

export async function ban(uuid: string, reason?: string, doToast = true) {
  try {
    await sendPostRequest(`/api/players/ban?uuid=${uuid}&r=${reason}`);
    doToast && toast.success("已封禁该玩家");
  } catch (e: any) {
    toastError(e, "无法封禁该玩家", [
      [400, "请求参数错误"],
      [401, "未登录"]
    ]);
  }
}

export async function pardon(uuid: string, doToast = true) {
  try {
    await sendPostRequest(`/api/players/pardon?uuid=${uuid}`);
    doToast && toast.success("已解封该玩家");
  } catch (e: any) {
    toastError(e, "无法解封该玩家", [
      [400, "请求参数错误"],
      [401, "未登录"]
    ]);
  }
}

export async function setGameMode(uuid: string, gamemode: GameMode, doToast = true) {
  try {
    await sendPostRequest(`/api/players/gamemode?uuid=${uuid}&gm=${gamemode}`);
    doToast && toast.success("已将该玩家的游戏模式设置为"+ gameModeToString(gamemode));
  } catch (e: any) {
    toastError(e, "无法设置该玩家的游戏模式", [
      [400, "请求参数错误"],
      [401, "未登录"]
    ]);
  }
}

export async function addToWhitelist(name: string, uuid: string, doToast = true) {
  try {
    await sendPostRequest(`/api/whitelist/add?name=${name}&uuid=${uuid}`);
    doToast && toast.success("已将该玩家加入白名单");
  } catch (e: any) {
    toastError(e, "无法将该玩家加入白名单", [
      [400, "请求参数错误"],
      [401, "未登录"],
      [500, "服务器内部错误"]
    ]);
  }
}

export async function removeFromWhitelist(name: string, uuid: string, doToast = true) {
  try {
    await sendPostRequest(`/api/whitelist/remove?name=${name}&uuid=${uuid}`);
    doToast && toast.success("已将该玩家移出白名单");
  } catch (e: any) {
    toastError(e, "无法将该玩家移出白名单", [
      [400, "请求参数错误"],
      [401, "未登录"],
      [500, "服务器内部错误"]
    ]);
  }
}
