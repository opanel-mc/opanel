import { io, type Socket } from "socket.io-client";

export const wsUrl = (
  process.env.NODE_ENV === "development"
  ? `ws://localhost:3000`
  : `ws://${window.location.host}`
);

export class WebSocketClient {
  private socket: Socket | null = null;

  constructor() {
    this.socket = io(wsUrl);
    this.init();
  }

  private init() {
    this.socket?.on("connect", () => {
      console.log("Terminal connected. ID:"+ this.socket?.id);
    });
    this.socket?.on("connect_error", (err) => {
      if(!this.socket?.active) {
        console.log("Terminal connection failed. Error:"+ err.message);
      }
    });
    this.socket?.on("disconnect", () => {
      console.log("Terminal disconnected.");
    });
  }

  public onMessage(cb: (msg: string) => void) {
    if(!this.socket) throw new Error("WebSocket not initialized.");
    this.socket.on("server-message", (msg) => {
      console.log("[Terminal] "+ msg);
      cb(msg);
    });
  }

  public send(msg: string) {
    if(!this.socket) throw new Error("WebSocket not initialized.");
    this.socket.emit("client-message", msg);
  }

  public close() {
    if(!this.socket) return;
    this.socket.close();
    this.socket = null;
  }
}
