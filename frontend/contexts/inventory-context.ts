import type { Item } from "minecraft-textures";
import type { ItemStack, SetState } from "@/lib/types";
import React from "react";

interface InventoryContextType {
  textures: Item[]
  currentlyHeldItem: ItemStack | null
  setCurrentlyHeldItem: SetState<ItemStack | null>
  swapClickedWithHeldItem: (clickedItem: ItemStack) => void
  addClickedWithHeldItem: (clickedItem: ItemStack) => void
  removeClickedItem: (clickedItem: ItemStack) => void
  halfClickedItem: (clickedItem: ItemStack) => void
}

export const InventoryContext = React.createContext<InventoryContextType>(undefined!);
