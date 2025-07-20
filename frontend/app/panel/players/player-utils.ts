/* eslint-disable @typescript-eslint/no-unused-vars */
import { toast } from "sonner";
import { sendPostRequest } from "@/lib/api";

export async function giveOp(uuid: string) {
  try {
    await sendPostRequest(`/api/players/op?uuid=${uuid}`);
    toast.success("成功给予该玩家OP权限");
  } catch (e) {
    toast.error("无法给予该玩家OP权限");
  }
}

export async function depriveOp(uuid: string) {
  try {
    await sendPostRequest(`/api/players/deop?uuid=${uuid}`);
    toast.success("成功解除该玩家OP权限");
  } catch (e) {
    toast.error("无法解除该玩家OP权限");
  }
}

export async function kick(uuid: string, reason?: string) {
  try {
    await sendPostRequest(`/api/players/kick?uuid=${uuid}&r=${reason}`);
    toast.success("已踢出该玩家");
  } catch (e) {
    toast.error("无法踢出该玩家");
  }
}

export async function ban(uuid: string, reason?: string) {
  try {
    await sendPostRequest(`/api/players/ban?uuid=${uuid}&r=${reason}`);
    toast.success("已封禁该玩家");
  } catch (e) {
    toast.error("无法封禁该玩家");
  }
}

export async function pardon(uuid: string) {
  try {
    await sendPostRequest(`/api/players/pardon?uuid=${uuid}`);
    toast.success("已解封该玩家");
  } catch (e) {
    toast.error("无法解封该玩家");
  }
}
