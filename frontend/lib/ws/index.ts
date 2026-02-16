import { toast } from "sonner";
import { checkAuth, wsUrl } from "../api";
import { $ } from "../i18n";

type MessageType<M extends string> = M | "connect" | "ping" | "pong" | "error";
interface Packet<M extends string, D> {
  type: MessageType<M>
  data: D
}

const heartbeatInterval = 20000; // 20s

export abstract class WebSocketClient<M extends string> {
  private socket: WebSocket | null = null;

  private heartbeatTimer: NodeJS.Timeout | null = null;

  constructor(route: string) {
    checkAuth().then((res) => {
      if(!res) {
        if(this.socket !== null) {
          this.socket.close();
          this.socket = null;
        }
        window.location.href = "/login";
      }
    });

    const url = new URL(wsUrl);
    url.pathname = route;
    this.socket = new WebSocket(url);
    this.init();
  }

  private init() {
    if(!this.socket) return;

    this.subscribe("connect", () => {
      this.onOpen();

      // Start heartbeat
      this.send("ping", null);
    });

    // Heartbeat to keep connection alive
    this.subscribe("pong", () => {
      this.heartbeatTimer = setTimeout(() => {
        this.send("ping", null);
      }, heartbeatInterval);
    });

    this.subscribe("error", (err) => {
      this.onError(err);
    });

    this.socket.addEventListener("error", (err) => {
      this.onError(err);
      toast.error($("terminal.ws.error"));
    });

    this.socket.addEventListener("close", () => {
      this.onClose();
    });
  }

  public subscribe<D>(type: MessageType<M>, cb: (data: D) => void) {
    if(!this.socket) {
      toast.error("WebSocket not initialized.");
      return;
    }
    this.socket.addEventListener("message", (e) => {
      const packet: Packet<M, D> = JSON.parse(e.data);
      if(packet.type === type) {
        cb(packet.data);
      }
    });
  }

  protected abstract onOpen(): void;
  protected abstract onClose(): void;
  protected abstract onError(err: any): void;

  public send<D>(type: MessageType<M>, data: D) {
    if(!this.socket) {
      toast.error("WebSocket not initialized.");
      return;
    }
    this.socket.send(JSON.stringify({ type, data }));
  }

  public close() {
    if(this.heartbeatTimer) clearTimeout(this.heartbeatTimer);
    if(this.socket) {
      this.socket.close();
      this.socket = null;
    }
  }
}
