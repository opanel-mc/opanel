"use client";

import type { InventoryInteractionRequest } from "./inventory-interaction";
import { memo, useContext, useMemo, useState } from "react";
import { Search } from "lucide-react";
import { InventoryTextureContext } from "@/contexts/inventory-texture-context";
import { cn } from "@/lib/utils";
import { AIR, InventoryItem } from "./inventory-item";
import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput
} from "@/components/ui/input-group";
import { $, $mc } from "@/lib/i18n";

function ItemExplorerComponent({
  onInteract,
  className
}: {
  onInteract: (request: InventoryInteractionRequest) => void
  className?: string
}) {
  const textures = useContext(InventoryTextureContext);
  const [searchValue, setSearchValue] = useState("");
  const items = useMemo(() => (
    (textures ?? [])
      .filter(({ id, readable }) => (
        id.toLowerCase() !== AIR
        && (
          id.includes(searchValue)
          || readable.toLowerCase().includes(searchValue.toLowerCase())
          || $mc(id).toLowerCase().includes(searchValue.toLowerCase())
        )
      ))
      .map(({ id }) => ({ id, count: 1, slot: -1 }))
  ), [searchValue, textures]);

  if(!textures) return <></>;

  return (
    <div className={cn("min-h-0 flex flex-col gap-2", className)}>
      <div className="flex-1 border rounded-sm bg-background dark:bg-transparent flex flex-wrap content-start overflow-y-auto o-scrollbar">
        {
          items.map((item) => (
            <InventoryItem
              itemStack={item}
              onInteract={onInteract}
              key={item.id}/>
          ))
        }
      </div>
      <InputGroup>
        <InputGroupAddon>
          <Search />
        </InputGroupAddon>
        <InputGroupInput
          value={searchValue}
          placeholder={$("players.inventory.explorer.search.placeholder")}
          autoFocus
          autoComplete="off"
          onChange={(e) => setSearchValue(e.target.value)}/>
      </InputGroup>
    </div>
  );
}

export const ItemExplorer = memo(ItemExplorerComponent);
