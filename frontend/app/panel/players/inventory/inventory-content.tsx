"use client";

import type { PlayerInventory } from "@/lib/types";
import { useContext } from "react";
import { cn } from "@/lib/utils";
import { AIR, InventoryItem } from "./inventory-item";
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

  const mainItemMap = new Map(
    (inventory.items ?? [])
      .map((item) => [item.slot, item] as const)
  );
  const getMainItem = (slot: number) => {
    const item = mainItemMap.get(slot);
    if(item) return { ...item, container: "main" as const };
    return { slot, id: AIR, count: 0, container: "main" as const };
  };

  const mainSlots = Array.from({ length: 27 }, (_, i) => i + 9);
  const hotbarSlots = Array.from({ length: 9 }, (_, i) => i);
  const equipmentSlots = [
    { slot: 39, label: $("players.inventory.equipment.helmet") },
    { slot: 38, label: $("players.inventory.equipment.chestplate") },
    { slot: 37, label: $("players.inventory.equipment.leggings") },
    { slot: 36, label: $("players.inventory.equipment.boots") },
    { slot: 40, label: $("players.inventory.equipment.offhand") }
  ];
  const armorSlots = equipmentSlots.slice(0, 4);
  const offhandSlot = equipmentSlots[4];

  const enderSize = inventory.enderSize ?? 27;
  const canReadEnderChest = inventory.capabilities?.readEnderChest ?? false;
  const canWriteEnderChest = inventory.capabilities?.writeEnderChest ?? false;
  const enderItemMap = new Map(
    (inventory.enderItems ?? [])
      .map((item) => [item.slot, item] as const)
  );
  const getEnderItem = (slot: number) => {
    const item = enderItemMap.get(slot);
    if(item) return { ...item, container: "ender" as const };
    return { slot, id: AIR, count: 0, container: "ender" as const };
  };

  return (
    <div className="w-fit flex flex-col gap-4">
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
        <div
          className="border rounded-sm bg-background dark:bg-transparent p-2 flex flex-col gap-2"
          data-testid="equipment-section">
          <div className="flex items-center justify-between gap-3 max-md:flex-wrap">
            <h3 className="text-sm font-medium">{$("players.inventory.equipment.title")}</h3>
            {offhandSlot && (
              <span className="text-xs text-muted-foreground max-md:hidden">{offhandSlot.label}</span>
            )}
          </div>
          <div
            className="flex items-start justify-between gap-4 max-md:flex-wrap"
            data-testid="equipment-row"
            data-layout="equipment-row">
            <div className="flex flex-wrap gap-2" data-testid="armor-row">
              {armorSlots.map(({ slot, label }) => (
                <div data-testid={`equipment-slot-${slot}`} key={slot} title={label}>
                  <InventoryItem itemStack={getMainItem(slot)}/>
                </div>
              ))}
            </div>
            {offhandSlot && (
              <div
                className="flex items-center gap-2 max-md:w-full max-md:justify-between"
                data-testid="offhand-slot"
                data-slot-group="offhand">
                <span className="text-xs text-muted-foreground md:hidden">{offhandSlot.label}</span>
                <div data-testid={`equipment-slot-${offhandSlot.slot}`} title={offhandSlot.label}>
                  <InventoryItem itemStack={getMainItem(offhandSlot.slot)}/>
                </div>
              </div>
            )}
          </div>
        </div>

        <section
          className="flex-3 grid-rows-3 [&_*]:data-[slot=inventory-item]:nth-[n+19]:border-b-0"
          data-testid="main-grid"
          data-layout="main-grid">
          {mainSlots.map((slot) => (
            <InventoryItem itemStack={getMainItem(slot)} key={slot}/>
          ))}
        </section>
        <section
          className="flex-1 grid-rows-1 [&_*]:data-[slot=inventory-item]:border-b-0"
          data-testid="hotbar-grid">
          {hotbarSlots.map((slot) => (
            <InventoryItem itemStack={getMainItem(slot)} key={slot}/>
          ))}
        </section>
      </div>

      {canReadEnderChest && (
        <div>
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-sm font-medium">{$("players.inventory.ender-chest.title")}</h3>
            {!canWriteEnderChest && (
              <span className="text-xs text-muted-foreground">{$("players.inventory.ender-chest.readonly")}</span>
            )}
          </div>

          <div className={cn(
            "w-[calc(9*48px)] max-md:w-[calc(9*36px)]",
            "h-fit border rounded-sm bg-background dark:bg-transparent grid grid-cols-9 overflow-hidden",
            "[&_*]:data-[slot=inventory-item]:border-muted [&_*]:data-[slot=inventory-item]:border-r [&_*]:data-[slot=inventory-item]:border-b [&_*]:data-[slot=inventory-item]:nth-[9n]:border-r-0 [&_*]:data-[slot=inventory-item]:nth-[n+19]:border-b-0"
          )}>
            {Array.from({ length: enderSize }, (_, i) => (
              <InventoryItem
                itemStack={getEnderItem(i)}
                readonly={!canWriteEnderChest}
                key={`ender-${i}`}/>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
