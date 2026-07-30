import type { Item } from "minecraft-textures";
import React from "react";

export const InventoryTextureContext = React.createContext<Item[] | null>(null);
