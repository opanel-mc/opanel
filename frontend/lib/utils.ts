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
