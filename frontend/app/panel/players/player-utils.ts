/* eslint-disable @typescript-eslint/no-unused-vars */
import { toast } from "sonner";
import { sendPostRequest } from "@/lib/api";
import { GameMode } from "@/lib/types";
import { gameModeToString } from "@/lib/utils";

export async function giveOp(uuid: string, doToast = true) {
  try {
    await sendPostRequest(`/api/players/op?uuid=${uuid}`);
    doToast && toast.success("成功给予该玩家OP权限");
  } catch (e) {
    toast.error("无法给予该玩家OP权限");
  }
}

export async function depriveOp(uuid: string, doToast = true) {
  try {
    await sendPostRequest(`/api/players/deop?uuid=${uuid}`);
    doToast && toast.success("成功解除该玩家OP权限");
  } catch (e) {
    toast.error("无法解除该玩家OP权限");
  }
}

export async function kick(uuid: string, reason?: string, doToast = true) {
  try {
    await sendPostRequest(`/api/players/kick?uuid=${uuid}&r=${reason}`);
    doToast && toast.success("已踢出该玩家");
  } catch (e) {
    toast.error("无法踢出该玩家");
  }
}

export async function ban(uuid: string, reason?: string, doToast = true) {
  try {
    await sendPostRequest(`/api/players/ban?uuid=${uuid}&r=${reason}`);
    doToast && toast.success("已封禁该玩家");
  } catch (e) {
    toast.error("无法封禁该玩家");
  }
}

export async function pardon(uuid: string, doToast = true) {
  try {
    await sendPostRequest(`/api/players/pardon?uuid=${uuid}`);
    doToast && toast.success("已解封该玩家");
  } catch (e) {
    toast.error("无法解封该玩家");
  }
}

export async function setGameMode(uuid: string, gamemode: GameMode, doToast = true) {
  try {
    await sendPostRequest(`/api/players/gamemode?uuid=${uuid}&gm=${gamemode}`);
    doToast && toast.success("已将该玩家的游戏模式设置为"+ gameModeToString(gamemode));
  } catch (e) {
    toast.error("无法设置该玩家的游戏模式");
  }
}
