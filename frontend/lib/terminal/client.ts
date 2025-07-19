import type { ConsoleLogLevel } from "./log-levels";
import { toast } from "sonner";
import { wsUrl } from "../api";

export interface ConsoleLog {
  time: number
  level: ConsoleLogLevel
  thread: string
  source: string
  line: string
}

export interface TerminalPacket {
  type: (
    /* server packet */
    "init"
    | "log"
    | "error"
    /* client packet */
    | "command"
    /* common packet */
    | "autocomplete"
  )
  data: any
}

export class WebSocketClient {
  private socket: WebSocket | null = null;

  constructor() {
    this.socket = new WebSocket(wsUrl + "/terminal");
    this.init();
  }

  private init() {
    this.socket?.addEventListener("open", () => {
      console.log("Terminal connected.");
    });
    this.socket?.addEventListener("error", (err) => {
      console.log("Terminal connection failed. ", err);
    });
    this.socket?.addEventListener("close", () => {
      console.log("Terminal disconnected.");
    });
  }

  public onOpen(cb: () => void) {
    if(!this.socket) throw new Error("WebSocket not initialized.");

    this.socket?.addEventListener("open", () => cb());
  }

  public onMessage(cb: (type: "init" | "log" | "autocomplete", data: any) => void) {
    if(!this.socket) throw new Error("WebSocket not initialized.");

    this.socket.addEventListener("message", (e) => {
      const { type, data } = JSON.parse(e.data) satisfies TerminalPacket;
      const log = data as ConsoleLog;
      console.log("[Terminal] "+ log.line);

      if(type === "init" && !(data instanceof Array)) {
        throw new Error("Received an incorrect initial packet.");
      }
      if(type === "error") {
        toast.error("Packet Error", { description: data });
        throw new Error("Packet error: "+ data);
      }

      cb(type, log);
    });
  }

  public send(msg: TerminalPacket) {
    if(!this.socket) throw new Error("WebSocket not initialized.");
    this.socket.send(JSON.stringify(msg));
  }

  public close() {
    if(!this.socket) return;
    this.socket.close();
    this.socket = null;
  }
}
