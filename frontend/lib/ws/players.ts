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
  /* client packet */
  | "fetch"
);

export class PlayersClient extends WebSocketClient<PlayersMessageType> {
  constructor() {
    super("/socket/players");
  }

  protected override onOpen() {
    console.log("Player list connected.");
  }
  
  protected override onClose() {
    console.log("Player list disconnected.");
  }

  protected override onError(err: Event) {
    console.log("Player list connection failed. ", err);
    toast.error($("players.ws.error"));
  }
}
