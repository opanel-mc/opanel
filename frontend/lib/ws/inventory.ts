import type { ItemStack } from "../types";
import { toast } from "sonner";
import { WebSocketClient } from ".";
import { $ } from "../i18n";

export interface InventoryMovePayload {
  items: ItemStack[]
}

export type InventoryMessageType = (
  /* server packet */
  "init"
  /* client packet */
  | "fetch"
  /* common packet */
  | "update"
);

export class InventoryClient extends WebSocketClient<InventoryMessageType> {
  constructor(uuid: string) {
    super(`/socket/inventory/${uuid}`);
  }

  override onOpen() {
    console.log("Inventory connected.");
  }
  
  override onClose() {
    console.log("Inventory disconnected.");
  }

  override onError(err: Event) {
    console.log("Inventory connection failed. ", err);
    toast.error($("players.inventory.ws.error"));
  }
}
