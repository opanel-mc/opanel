import type { ItemStack } from "@/lib/types";
import type { ItemNBTResolver } from "@/lib/nbt/resolver";
import { type MouseEvent, type RefObject, useContext, useEffect, useMemo, useState } from "react";
import { InventoryContext } from "@/contexts/inventory-context";
import { cn } from "@/lib/utils";
import { minecraftAE } from "@/lib/fonts";
import { ItemSheet } from "./item-sheet";
import { $mc } from "@/lib/i18n";
import { VersionContext } from "@/contexts/api-context";
import { createResolver } from "@/lib/nbt";

import GlintTexture from "@/assets/images/enchanted-glint.png";
import PotionOverlayTexture from "@/assets/images/potion-overlay.png";
import "@/style/item-effect.css";

export const AIR = "minecraft:air";

const glintItems = [
  "minecraft:enchanted_book",
  "minecraft:experience_bottle",
  "minecraft:enchanted_golden_apple",
  "minecraft:end_crystal",
  "minecraft:nether_star",
  "minecraft:written_book",
  "minecraft:debug_stick"
];

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
  const versionCtx = useContext(VersionContext);
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
  const [resolvedNBT, setResolvedNBT] = useState<ItemNBTResolver | null>(null);

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

  useEffect(() => {
    if(!versionCtx) return;

    setResolvedNBT(createResolver(versionCtx.version, itemStack.nbt));
  }, [versionCtx, itemStack.nbt]);

  if(!ctx) return <></>;

  const itemComponent = (
    <div
      data-slot="inventory-item"
      data-slot-id={itemStack.slot}
      data-item-id={itemStack.id}
      className={cn(
        "relative h-[48px] max-md:h-[36px] aspect-square p-1 hover:bg-muted select-none image-pixelated z-10",
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
          className="w-full"
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
      {((resolvedNBT && resolvedNBT.shouldGlint()) || glintItems.includes(itemStack.id)) && (
        <div
          className="item-glint absolute inset-0 top-0 left-0 z-10"
          style={{
            backgroundImage: `url(${GlintTexture.src})`,
            maskImage: `url(${textureItem ? textureItem.texture : ""})`,
            WebkitMaskImage: `url(${textureItem ? textureItem.texture : ""})`
          }}/>
      )}
      {(resolvedNBT && resolvedNBT.isPotion()) && (
        <div
          className="item-potion-overlay absolute inset-0 top-0 left-0 z-10"
          style={{
            backgroundImage: `url(${PotionOverlayTexture.src})`,
            backgroundColor: `rgb(${resolvedNBT.getPotionColor()?.join(",")})`,
            maskImage: `url(${PotionOverlayTexture.src})`,
            WebkitMaskImage: `url(${PotionOverlayTexture.src})`
          }}/>
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
