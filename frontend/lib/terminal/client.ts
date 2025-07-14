import { wsUrl } from "../api";

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

  public onMessage(cb: (type: "init" | "log", data: any) => void) {
    if(!this.socket) throw new Error("WebSocket not initialized.");

    this.socket.addEventListener("message", (e) => {
      console.log("[Terminal] "+ e.data);
      const { type, data } = JSON.parse(e.data);
      if(type === "init" && !(data instanceof Array)) {
        throw new Error("Received an incorrect initial packet.");
      }

      cb(type, data);
    });
  }

  public send(msg: string) {
    if(!this.socket) throw new Error("WebSocket not initialized.");
    this.socket.send(msg);
  }

  public close() {
    if(!this.socket) return;
    this.socket.close();
    this.socket = null;
  }
}
