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

  public onMessage(cb: (msg: string) => void) {
    if(!this.socket) throw new Error("WebSocket not initialized.");

    this.socket.addEventListener("message", (e) => {
      console.log("[Terminal] "+ e.data);
      cb(e.data);
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
