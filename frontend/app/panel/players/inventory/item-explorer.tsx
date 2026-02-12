"use client";

import { useContext, useState } from "react";
import { Search } from "lucide-react";
import { InventoryContext } from "@/contexts/inventory-context";
import { cn } from "@/lib/utils";
import { AIR, InventoryItem } from "./inventory-item";
import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput
} from "@/components/ui/input-group";
import { $, $mc } from "@/lib/i18n";

export function ItemExplorer({ className }: { className?: string }) {
  const ctx = useContext(InventoryContext);
  const [searchValue, setSearchValue] = useState("");

  if(!ctx) return <></>;

  return (
    <div className={cn("min-h-0 flex flex-col gap-2", className)}>
      <div className="flex-1 border rounded-sm bg-background dark:bg-transparent flex flex-wrap content-start overflow-y-auto o-scrollbar">
        {
          ctx.textures
            .filter(({ id, readable }) => (
              id.toLowerCase() !== AIR
              && (
                id.includes(searchValue)
                || readable.toLowerCase().includes(searchValue.toLowerCase())
                || $mc(id).toLowerCase().includes(searchValue.toLowerCase())
              )
            ))
            .map((item, i) => (
              <InventoryItem
                itemStack={{ id: item.id, count: 1, slot: -1 }}
                key={i}/>
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
