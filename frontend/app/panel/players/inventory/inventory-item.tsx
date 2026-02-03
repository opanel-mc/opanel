import type { ItemStack } from "@/lib/types";
import { type MouseEvent, type RefObject, useContext, useMemo } from "react";
import { InventoryContext } from "@/contexts/inventory-context";
import { cn } from "@/lib/utils";
import { minecraftAE } from "@/lib/fonts";

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
    currentlyHeldItem,
    setCurrentlyHeldItem,
    swapClickedWithHeldItem,
    addClickedWithHeldItem,
    removeClickedItem,
    halfClickedItem
  } = ctx;
  const isFromExplorer = itemStack.slot === -1;
  const textureItem = useMemo(() => (
    ctx.textures.find(({ id }) => id === itemStack.id)
  ), [ctx.textures, itemStack.id]);

  const handleLeftClick = () => {
    if(held || !ctx) return;

    if(!currentlyHeldItem) { // pick up the item
      setCurrentlyHeldItem(itemStack);
      !isFromExplorer && removeClickedItem(itemStack);
      return;
    }

    if(itemStack.id === currentlyHeldItem.id) {
      addClickedWithHeldItem(itemStack);
      return;
    }

    if(isFromExplorer) { // just throw away the held item
      setCurrentlyHeldItem(null);
      return;
    }

    swapClickedWithHeldItem(itemStack);
  };

  const handleRightClick = (e: MouseEvent) => {
    e.preventDefault();
    if(held || !ctx) return;

    if(!currentlyHeldItem && isFromExplorer) { // pick up 64 from explorer
      setCurrentlyHeldItem({ ...itemStack, count: 64 });
      return;
    }

    if(!currentlyHeldItem) { // pick up half of the item
      setCurrentlyHeldItem({ ...itemStack, count: Math.ceil(itemStack.count / 2) });
      halfClickedItem(itemStack);
      return;
    }
  };

  if(!ctx) return <></>;

  return (
    <div
      data-slot="inventory-item"
      data-slot-id={itemStack.slot}
      className={cn(
        "relative h-[48px] max-md:h-[36px] aspect-square p-1 hover:bg-muted select-none",
        held && "pointer-events-none",
        className
      )}
      title={textureItem ? textureItem.readable : ""}
      onClick={() => handleLeftClick()}
      onContextMenu={(e) => handleRightClick(e)}
      ref={ref}>
      {textureItem && (
        <img
          className="image-pixelated w-full"
          src={textureItem.texture}
          alt={textureItem.id}/>
      )}
      {itemStack.count > 1 && (
        <span className={cn("absolute -bottom-1 right-0 text-2xl max-md:text-lg text-white text-shadow-[3px_3px_0_#373737] select-none", minecraftAE.className)}>
          {itemStack.count}
        </span>
      )}
    </div>
  );
}
