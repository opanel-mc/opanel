import type { InventoryType, ItemStack } from "../types";
import { WebSocketClient } from ".";

export interface InventoryMovePayload {
  items: ItemStack[]
}

export interface InventoryUpdatePayload {
  inventoryType: InventoryType
  item: ItemStack
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

  protected override onOpen() {
    console.log("Inventory connected.");
  }
  
  protected override onClose() {
    console.log("Inventory disconnected.");
  }

  protected override onError(err: Event) {
    console.log("Inventory connection failed. ", err);
  }
}
