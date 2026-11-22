import type { Player } from "../types";
import { toast } from "sonner";
import { WebSocketClient } from ".";
import { $ } from "../i18n";

export type PlayerMoveData = Pick<Player, "uuid" | "name" | "position">;

export type PlayersMessageType = (
  /* server packet */
  "init"
  | "join"
  | "leave"
  | "move"
  | "gamemode-change"
  | "ping"
  /* client packet */
  | "fetch"
  | "fetch-ping"
);

export interface PlayersClientOptions {
  onDisconnected?: () => void
  onlineOnly?: boolean
  positionOnly?: boolean
}

export class PlayersClient extends WebSocketClient<PlayersMessageType> {
  private readonly onDisconnected?: () => void;

  constructor(options?: PlayersClientOptions | (() => void)) {
    const {
      onDisconnected,
      onlineOnly = false,
      positionOnly = false
    } = typeof options === "function" ? { onDisconnected: options } : (options ?? {});

    const params = new URLSearchParams();
    if(onlineOnly) params.set("onlineOnly", "true");
    if(positionOnly) params.set("positionOnly", "true");
    const query = params.toString();

    super(query ? `/socket/players?${query}` : "/socket/players");
    this.onDisconnected = onDisconnected;
  }

  protected override onOpen() {
    console.log("Player list connected.");
  }
  
  protected override onClose() {
    console.log("Player list disconnected.");
    this.onDisconnected?.();
  }

  protected override onError(err: Event) {
    console.log("Player list connection failed. ", err);
    toast.error($("players.ws.error"));
  }
}
