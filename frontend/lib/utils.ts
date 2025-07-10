import axios from "axios";
import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"
import { getCookie } from "cookies-next/client";
import { apiUrl } from "./global";
import { type APIResponse, GameMode } from "./types";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export async function sendGetRequest<R>(api: string): Promise<APIResponse<R>> {
  return (await axios.request({
    method: "get",
    url: apiUrl + api,
    headers: { "X-Credential-Token": getCookie("token") }
  })).data as APIResponse<R>;
}

export async function sendPostRequest<R, T = any>(api: string, body?: T): Promise<APIResponse<R>> {
  const data = body ? JSON.stringify(body) : "";
  
  return (await axios.request({
    method: "post",
    maxBodyLength: Infinity,
    url: apiUrl + api,
    headers: { "Content-Type": "text/plain" },
    data
  })).data as APIResponse<R>;
}

export function gameModeToString(gameMode: GameMode) {
  switch(gameMode) {
    case GameMode.ADVENTURE:
      return "冒险";
    case GameMode.SURVIVAL:
      return "生存";
    case GameMode.CREATIVE:
      return "创造";
    case GameMode.SPECTATOR:
      return "旁观";
    default:
      return "未知";
  }
}

export function getRandom(min: number, max: number): number {
    return Math.floor(Math.random() * (max - min + 1) + min);
}

export function getRandomRanged(ranges: [number, number][]): number {
  if(ranges.length === 0) throw new Error("Ranges array is empty.");
  const candidates: number[] = [];
  for(const range of ranges) {
    candidates.push(getRandom(range[0], range[1]));
  }
  return getRandomArrayItem(candidates);
}

export function getRandomArrayItem<T>(arr: T[]): T {
  if(arr.length === 0) throw new Error("Array is empty.");
  return arr[getRandom(0, arr.length - 1)];
}

export function unicodeHexToString(hex: string): string {
  return String.fromCodePoint(parseInt(hex, 16));
}
