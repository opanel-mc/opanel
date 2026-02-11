"use client";

import type { PlayerInventory } from "@/lib/types";
import { useContext } from "react";
import { cn } from "@/lib/utils";
import { InventoryItem } from "./inventory-item";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { InventoryContext } from "@/contexts/inventory-context";
import { $ } from "@/lib/i18n";

export function InventoryContent({
  inventory,
  className
}: {
  inventory: PlayerInventory,
  className?: string
}) {
  const ctx = useContext(InventoryContext);
  const { currentlyHeldItem, nbtEditMode, setNbtEditMode } = ctx;

  return (
    <div className="w-fit">
      <div className="flex justify-end items-center gap-2 mb-4">
        <Label>{$("players.inventory.nbt-mode.label")}</Label>
        <Switch
          disabled={currentlyHeldItem !== null}
          checked={nbtEditMode}
          onCheckedChange={setNbtEditMode}/>
      </div>

      <div className={cn(
        "w-[calc(9*48px)] max-md:w-[calc(9*36px)]",
        "h-fit flex flex-col gap-2 [&_section]:border [&_section]:rounded-sm [&_section]:bg-background dark:[&_section]:bg-transparent [&_section]:grid [&_section]:grid-cols-9 [&_section]:overflow-hidden",
        "[&_*]:data-[slot=inventory-item]:border-muted [&_*]:data-[slot=inventory-item]:border-r [&_*]:data-[slot=inventory-item]:border-b [&_*]:data-[slot=inventory-item]:nth-[9n]:border-r-0",
        className
      )}>
        <section className="flex-3 grid-rows-3 [&_*]:data-[slot=inventory-item]:nth-[n+19]:border-b-0">
          {inventory?.items.slice(9, 36).map((item, i) => (
            <InventoryItem itemStack={item} key={i}/>
          ))}
        </section>
        <section className="flex-1 grid-rows-1 [&_*]:data-[slot=inventory-item]:border-b-0">
          {inventory?.items.slice(0, 9).map((item, i) => (
            <InventoryItem itemStack={item} key={i}/>
          ))}
        </section>
        {/** @todo Ender chest */}
        {/* <section className="mt-4 flex-3 grid-rows-3 [&_*]:data-[slot=inventory-item]:nth-[n+19]:border-b-0">
          {Array.from({ length: 27 }).map((_, i) => (
            <InventoryItem itemStack={{ }} key={i}/>
          ))}
        </section> */}
      </div>
    </div>
  );
}
