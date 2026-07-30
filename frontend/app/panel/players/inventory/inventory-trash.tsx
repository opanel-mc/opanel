"use client";

import { type KeyboardEvent, useContext } from "react";
import { Trash2 } from "lucide-react";
import { InventoryContext } from "@/contexts/inventory-context";
import { cn } from "@/lib/utils";

export function InventoryTrash({ className }: { className?: string }) {
  const ctx = useContext(InventoryContext);

  if(!ctx) return <></>;

  const canDelete = ctx.currentlyHeldItem !== null;

  const deleteHeldItem = () => {
    if(!canDelete) return;

    ctx.setCurrentlyHeldItem(null);
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    if(e.key !== "Enter" && e.key !== " ") return;

    e.preventDefault();
    deleteHeldItem();
  };

  return (
    <div
      data-slot="inventory-trash"
      role="button"
      className={cn(
        "h-10 max-md:h-8 aspect-square p-1 select-none",
        "flex items-center justify-center border rounded-sm",
        "bg-red-100/70 border-red-200 text-red-600",
        "dark:bg-red-950/60 dark:border-red-950 dark:text-red-400",
        canDelete && "hover:bg-red-200 dark:hover:bg-red-950",
        className
      )}
      onClick={deleteHeldItem}
      onKeyDown={handleKeyDown}>
      <Trash2 className="size-4 max-md:size-3"/>
    </div>
  );
}
