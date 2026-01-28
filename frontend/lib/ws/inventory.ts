import { toast } from "sonner";
import { WebSocketClient } from ".";

export type InventoryMessageType = (
  /* server packet */
  "init"
  | "update"
  | "player-join"
  | "player-leave"
  /* client packet */
  | "fetch"
);

export interface InventoryItem {
  type: string;
  amount: number;
  displayName?: string;
  lore?: string[];
  durability?: number;
  maxDurability?: number;
  enchantments?: Record<string, number>;
  customModelData?: number;
  unbreakable?: boolean;
  nbt?: string;
}

export interface InventoryData {
  hotbar: (InventoryItem | null)[];
  inventory: (InventoryItem | null)[][];
  armor: (InventoryItem | null)[];
  offhand: (InventoryItem | null)[];
}

export interface PlayerInventory {
  uuid: string;
  name?: string;
  inventory?: InventoryData;
}

export class InventoryClient extends WebSocketClient<InventoryMessageType> {
  constructor() {
    super("/socket/inventory");
  }

  override onOpen() {
    console.log("Inventory WebSocket connected.");
  }
  
  override onClose() {
    console.log("Inventory WebSocket disconnected.");
  }

  override onError(err: Event) {
    console.log("Inventory WebSocket connection failed. ", err);
    toast.error("Failed to connect to inventory WebSocket.");
  }
}
