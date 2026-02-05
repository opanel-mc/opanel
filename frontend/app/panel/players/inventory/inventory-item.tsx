import type { ItemStack } from "@/lib/types";
import { type MouseEvent, type RefObject, useContext, useMemo } from "react";
import { InventoryContext } from "@/contexts/inventory-context";
import { cn } from "@/lib/utils";
import { minecraftAE } from "@/lib/fonts";
import { ItemSheet } from "./item-sheet";
import { $mc } from "@/lib/i18n";

export const AIR = "minecraft:air";

function isFromExplorer(itemStack: ItemStack) {
  return itemStack.slot === -1;
}

export function InventoryItem({
  itemStack,
  held = false,
  className,
  ref
}: {
  itemStack: ItemStack
  held?: boolean
  className?: string
  ref?: RefObject<HTMLDivElement | null>
}) {
  const ctx = useContext(InventoryContext);
  const {
    textures,
    currentlyHeldItem,
    setCurrentlyHeldItem,
    nbtEditMode,
    swapClickedWithHeldItem,
    addClickedWithHeldItem,
    removeClickedItem,
    halfClickedItem
  } = ctx;
  const textureItem = useMemo(() => (
    textures.find(({ id }) => id === itemStack.id)
  ), [textures, itemStack.id]);

  const handleLeftClick = () => {
    if(held || !ctx || nbtEditMode) return;

    if(!currentlyHeldItem) { // pick up the item
      setCurrentlyHeldItem(itemStack);
      !isFromExplorer(itemStack) && removeClickedItem(itemStack);
      return;
    }

    if(
      isFromExplorer(itemStack)
      && isFromExplorer(currentlyHeldItem)
      && itemStack.id === currentlyHeldItem.id
    ) { // add one to held item from explorer
      setCurrentlyHeldItem({ ...currentlyHeldItem, count: currentlyHeldItem.count + 1 });
      return;
    }

    if(isFromExplorer(itemStack)) { // just throw away the held item
      setCurrentlyHeldItem(null);
      return;
    }

    if(itemStack.id === currentlyHeldItem.id) {
      addClickedWithHeldItem(itemStack, currentlyHeldItem.count);
      return;
    }

    swapClickedWithHeldItem(itemStack);
  };

  const handleRightClick = (e: MouseEvent) => {
    e.preventDefault();
    if(held || !ctx || nbtEditMode) return;

    if(!currentlyHeldItem && isFromExplorer(itemStack)) { // pick up 64 from explorer
      setCurrentlyHeldItem({ ...itemStack, count: 64 });
      return;
    }

    if(!currentlyHeldItem) { // pick up half of the item
      setCurrentlyHeldItem({ ...itemStack, count: Math.ceil(itemStack.count / 2) });
      halfClickedItem(itemStack);
      return;
    }

    if(isFromExplorer(itemStack)) { // just throw away the held item
      setCurrentlyHeldItem(null);
      return;
    }

    if(itemStack.id === currentlyHeldItem.id) { // add one by one
      addClickedWithHeldItem(itemStack, 1);
      return;
    }

    if(itemStack.id === AIR) { // add one to empty slot
      addClickedWithHeldItem({ ...itemStack, id: currentlyHeldItem.id }, 1);
      return;
    }

    swapClickedWithHeldItem(itemStack);
  };

  const handleAuxClick = (e: MouseEvent) => {
    e.preventDefault();
    if(e.button !== 1) return;
    if(held || !ctx || nbtEditMode) return;

    if(!isFromExplorer(itemStack)) {
      setCurrentlyHeldItem({ ...itemStack, count: 64 });
      return;
    }
  };

  if(!ctx) return <></>;

  const itemComponent = (
    <div
      data-slot="inventory-item"
      data-slot-id={itemStack.slot}
      data-item-id={itemStack.id}
      className={cn(
        "relative h-[48px] max-md:h-[36px] aspect-square p-1 hover:bg-muted select-none z-10",
        held && "pointer-events-none",
        (nbtEditMode && !isFromExplorer(itemStack)) && "cursor-pointer",
        className
      )}
      title={$mc(itemStack.id)}
      onClick={() => handleLeftClick()}
      onContextMenu={(e) => handleRightClick(e)}
      onAuxClick={(e) => handleAuxClick(e)}
      ref={ref}>
      {textureItem && (
        <img
          className="image-pixelated w-full"
          src={textureItem.texture}
          alt={textureItem.id}/>
      )}
      {itemStack.count > 1 && (
        <span className={cn(
          "absolute -bottom-1 right-0 text-2xl max-md:text-lg text-white select-none",
          "text-shadow-[-1px_-1px_0_#000,1px_-1px_0_#000,-1px_1px_0_#000,1px_1px_0_#000] dark:text-shadow-[3px_3px_0_#373737]",
          minecraftAE.className
        )}>
          {itemStack.count}
        </span>
      )}
    </div>
  );

  if(isFromExplorer(itemStack)) return itemComponent;

  return (
    <ItemSheet
      itemStack={itemStack}
      disabled={!nbtEditMode}
      asChild>
      {itemComponent}
    </ItemSheet>
  );
}
