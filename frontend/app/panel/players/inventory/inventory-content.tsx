"use client";

import { useContext } from "react";
import { InventoryType, type PlayerInventory } from "@/lib/types";
import { cn } from "@/lib/utils";
import { InventoryItem } from "./inventory-item";
import { InventoryTrash } from "./inventory-trash";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { InventoryContext } from "@/contexts/inventory-context";
import { $ } from "@/lib/i18n";
import EmptyArmorSlotHelmet from "@/assets/images/inventory/empty-armor-slot-helmet.png";
import EmptyArmorSlotChestplate from "@/assets/images/inventory/empty-armor-slot-chestplate.png";
import EmptyArmorSlotLeggings from "@/assets/images/inventory/empty-armor-slot-leggings.png";
import EmptyArmorSlotBoots from "@/assets/images/inventory/empty-armor-slot-boots.png";
import EmptyArmorSlotShield from "@/assets/images/inventory/empty-armor-slot-shield.png";

const armorPlaceholderIcons = [
  EmptyArmorSlotHelmet.src,
  EmptyArmorSlotChestplate.src,
  EmptyArmorSlotLeggings.src,
  EmptyArmorSlotBoots.src,
];

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
        {/** Equipments */}
        <div className="flex justify-between gap-2">
          <section className="w-[calc(4*48px)] flex!">
            {inventory.equipments.items.slice(0, 4).map((item, i) => (
              <InventoryItem
                itemStack={item}
                inventoryType={InventoryType.EQUIPMENTS}
                placeholderIcon={armorPlaceholderIcons[i]}
                key={i}/>
            ))}
          </section>
          <section className="w-12 flex!">
            <InventoryItem
              itemStack={inventory.equipments.items[inventory.equipments.items.length - 1]}
              inventoryType={InventoryType.EQUIPMENTS}
              placeholderIcon={EmptyArmorSlotShield.src}/>
          </section>
        </div>

        {/** Main */}
        <section className="flex-3 grid-rows-3 [&_*]:data-[slot=inventory-item]:nth-[n+19]:border-b-0">
          {inventory.main.items.slice(9, 36).map((item, i) => (
            <InventoryItem
              itemStack={item}
              inventoryType={InventoryType.MAIN}
              key={i}/>
          ))}
        </section>

        {/** Main (Hotbar) */}
        <section className="flex-1 grid-rows-1 [&_*]:data-[slot=inventory-item]:border-b-0">
          {inventory.main.items.slice(0, 9).map((item, i) => (
            <InventoryItem
              itemStack={item}
              inventoryType={InventoryType.MAIN}
              key={i}/>
          ))}
        </section>

        {/** Ender Chest */}
        <h2 className="mt-3 text-sm font-semibold">
          {$("players.inventory.ender-chest.title")}
        </h2>
        <section className="flex-3 grid-rows-3 [&_*]:data-[slot=inventory-item]:nth-[n+19]:border-b-0">
          {inventory.enderChest.items.map((item, i) => (
            <InventoryItem
              itemStack={item}
              inventoryType={InventoryType.ENDER_CHEST}
              key={i}/>
          ))}
        </section>
        <InventoryTrash className="self-end"/>
      </div>
    </div>
  );
}
