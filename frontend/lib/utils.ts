import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"
import { GameMode } from "./types";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
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

export function getRandomArrayItem<T>(arr: T[]): T {
  if(arr.length === 0) throw new Error("Array is empty.");
  return arr[getRandom(0, arr.length - 1)];
}

export function getCurrentState<T>(setState: React.Dispatch<React.SetStateAction<T>>): Promise<T> {
  return new Promise((resolve) => {
    setState((currentState) => {
      resolve(currentState);
      return currentState;
    });
  });
}

/**
 * @example
 * ```ts
 * getInputtedArgumentStr("do hello world", 5); // "he"
 * getInputtedArgumentStr("do hello world", 13); // "worl"
 * ```
 */
export function getInputtedArgumentStr(str: string, cursor: number): string {
  if(cursor > str.length) throw new Error("Cursor position is out of the length of the string.");

  const trimmed = str.substring(0, cursor);
  const arr = trimmed.split(" ");
  return arr[arr.length - 1];
}

/**
 * @example
 * ```ts
 * getCurrentArgumentNumber("do hello world", 5); // 2
 * getCurrentArgumentNumber("do hello world", 13); // 3
 * ```
 */
export function getCurrentArgumentNumber(str: string, cursor: number): number {
  if(cursor > str.length) throw new Error("Cursor position is out of the length of the string.");

  const trimmed = str.substring(0, cursor);
  const arr = trimmed.split(" ");
  return arr.length;
}

export function objectToMap<V>(obj: { [key: string]: V }): Map<string, V> {
  const map = new Map<string, V>();
  for(const key in obj) {
    map.set(key, obj[key]);
  }
  return map;
}

export function formatDataSize(bytes: number): string {
  const kb = bytes / 1024;
  const mb = kb / 1024;
  const gb = mb / 1024;
  const tb = gb / 1024;
  const pb = tb / 1024;

  if(tb >= 1024) return `${pb.toFixed(2)} PB`;
  if(gb >= 1024) return `${tb.toFixed(2)} TB`;
  if(mb >= 1024) return `${gb.toFixed(2)} GB`;
  if(kb >= 1024) return `${mb.toFixed(2)} MB`;
  return `${kb.toFixed(2)} KB`;
}

export function stringToBase64(str: string): string {
  const bytes = new TextEncoder().encode(str);
  const binStr = String.fromCharCode(...bytes);
  return btoa(binStr);
}

export function base64ToString(base64: string): string {
  const binStr = atob(base64);
  const bytes = Uint8Array.from(binStr, (char) => char.charCodeAt(0));
  return new TextDecoder().decode(bytes);
}
