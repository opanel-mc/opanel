import { toast } from "sonner";
import { checkAuth, wsUrl } from "../api";

type MessageType<M extends string> = M | "connect" | "error";
interface Packet<M extends string, D> {
  type: MessageType<M>
  data: D
}

export abstract class WebSocketClient<M extends string> {
  private socket: WebSocket | null = null;

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

    const url = new URL(route, wsUrl);
    this.socket = new WebSocket(url);
    this.init();
  }

  private init() {
    if(!this.socket) return;

    this.subscribe("connect", () => {
      this.onOpen();
    });

    this.subscribe("error", (err) => {
      this.onError(err);
    });

    this.socket.addEventListener("error", (err) => {
      this.onError(err);
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
    if(this.socket) {
      this.socket.close();
      this.socket = null;
    }
  }
}
