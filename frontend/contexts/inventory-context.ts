import type { Item } from "minecraft-textures";
import type { InventoryType, ItemStack, SetState } from "@/lib/types";
import React from "react";

interface InventoryContextType {
  textures: Item[]
  currentlyHeldItem: ItemStack | null
  setCurrentlyHeldItem: SetState<ItemStack | null>
  nbtEditMode: boolean
  setNbtEditMode: SetState<boolean>
  swapClickedWithHeldItem: (inventoryType: InventoryType, clickedItem: ItemStack) => void
  addClickedWithHeldItem: (inventoryType: InventoryType, clickedItem: ItemStack, count: number) => void
  removeClickedItem: (inventoryType: InventoryType, clickedItem: ItemStack) => void
  halfClickedItem: (inventoryType: InventoryType, clickedItem: ItemStack) => void
  updateItemNBT: (inventoryType: InventoryType, item: ItemStack, snbt: string) => void
}

export const InventoryContext = React.createContext<InventoryContextType>(undefined!);
