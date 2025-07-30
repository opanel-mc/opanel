import type { GameMode } from "@/lib/types";
import { toast } from "sonner";
import { sendPostRequest } from "@/lib/api";
import { gameModeToString } from "@/lib/utils";

export async function giveOp(uuid: string, doToast = true) {
  try {
    await sendPostRequest(`/api/players/op?uuid=${uuid}`);
    doToast && toast.success("成功给予该玩家OP权限");
  } catch (e: any) {
    toast.error("无法给予该玩家OP权限", { description: e.message });
  }
}

export async function depriveOp(uuid: string, doToast = true) {
  try {
    await sendPostRequest(`/api/players/deop?uuid=${uuid}`);
    doToast && toast.success("成功解除该玩家OP权限");
  } catch (e: any) {
    toast.error("无法解除该玩家OP权限", { description: e.message });
  }
}

export async function kick(uuid: string, reason?: string, doToast = true) {
  try {
    await sendPostRequest(`/api/players/kick?uuid=${uuid}&r=${reason}`);
    doToast && toast.success("已踢出该玩家");
  } catch (e: any) {
    toast.error("无法踢出该玩家", { description: e.message });
  }
}

export async function ban(uuid: string, reason?: string, doToast = true) {
  try {
    await sendPostRequest(`/api/players/ban?uuid=${uuid}&r=${reason}`);
    doToast && toast.success("已封禁该玩家");
  } catch (e: any) {
    toast.error("无法封禁该玩家", { description: e.message });
  }
}

export async function pardon(uuid: string, doToast = true) {
  try {
    await sendPostRequest(`/api/players/pardon?uuid=${uuid}`);
    doToast && toast.success("已解封该玩家");
  } catch (e: any) {
    toast.error("无法解封该玩家", { description: e.message });
  }
}

export async function setGameMode(uuid: string, gamemode: GameMode, doToast = true) {
  try {
    await sendPostRequest(`/api/players/gamemode?uuid=${uuid}&gm=${gamemode}`);
    doToast && toast.success("已将该玩家的游戏模式设置为"+ gameModeToString(gamemode));
  } catch (e: any) {
    toast.error("无法设置该玩家的游戏模式", { description: e.message });
  }
}
