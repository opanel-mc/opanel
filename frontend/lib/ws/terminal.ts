import { toast } from "sonner";
import { WebSocketClient } from ".";
import { $ } from "../i18n";
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

export interface ConsoleLog {
  time: number
  level: ConsoleLogLevel
  thread: string
  source: string
  line: string
  thrownMessage: string | null
  mcdr: boolean
  uuid?: string
}

export type TerminalMessageType = (
  /* server packet */
  "init"
  | "log"
  | "mcdr-log"
  /* client packet */
  | "command"
  /* common packet */
  | "autocomplete"
);

export class TerminalClient extends WebSocketClient<TerminalMessageType> {
  constructor() {
    super("/socket/terminal");
  }

  protected override onOpen() {
    console.log("Terminal connected.");
  }
  
  protected override onClose() {
    console.log("Terminal disconnected.");
  }

  protected override onError(err: Event) {
    console.log("Terminal connection failed. ", err);
    toast.error($("terminal.ws.error"));
  }
}
