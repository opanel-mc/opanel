import type { ItemStack } from "@/lib/types";
import type { ItemNBTResolver } from "@/lib/nbt/resolver";
import React from "react";

export interface InventoryTooltipData {
  itemStack: ItemStack
  resolvedNBT: ItemNBTResolver | null
}

export interface InventoryTooltipContextType {
  showTooltip: (
    owner: symbol,
    data: InventoryTooltipData,
    x: number,
    y: number
  ) => void
  moveTooltip: (owner: symbol, x: number, y: number) => void
  hideTooltip: (owner: symbol) => void
}

export const InventoryTooltipContext = React.createContext<InventoryTooltipContextType | null>(null);
