import { getSettings } from "../settings";

export type ConsoleLogLevel = "INFO" | "WARN" | "ERROR";
export const defaultLogLevel: ConsoleLogLevel = getSettings("terminal.log-level");

export function getLogLevelId(level: ConsoleLogLevel) {
  switch(level) {
    case "INFO": return 1;
    case "WARN": return 2;
    case "ERROR": return 3;
  }
}
