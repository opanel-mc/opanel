import type { ItemStack } from "@/lib/types";
import type { ItemNBTResolver } from "@/lib/nbt/resolver";
import {
  type MouseEvent,
  type RefObject,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState
} from "react";
import { InventoryContext } from "@/contexts/inventory-context";
import { cn } from "@/lib/utils";
import { minecraftAE } from "@/lib/fonts";
import { ItemDialog } from "./item-dialog";
import { $, $mc } from "@/lib/i18n";
import { VersionContext } from "@/contexts/api-context";
import { createResolver } from "@/lib/nbt";

import GlintTexture from "@/assets/images/enchanted-glint.png";
import PotionOverlayTexture from "@/assets/images/potion-overlay.png";
import "@/style/item-effect.css";
import { ComponentsResolver } from "@/lib/nbt/components-resolver";

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
  const [hovered, setHovered] = useState(false);
  const hoveredItemTagRef = useRef<HTMLDivElement | null>(null);

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
      && itemStack.snbt === currentlyHeldItem.snbt
    ) { // add one to held item from explorer
      setCurrentlyHeldItem({ ...currentlyHeldItem, count: currentlyHeldItem.count + 1 });
      return;
    }

    if(isFromExplorer(itemStack)) { // just throw away the held item
      setCurrentlyHeldItem(null);
      return;
    }

    if(itemStack.id === currentlyHeldItem.id && itemStack.snbt === currentlyHeldItem.snbt) {
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

    if(itemStack.id === currentlyHeldItem.id && itemStack.snbt === currentlyHeldItem.snbt) { // add one by one
      addClickedWithHeldItem(itemStack, 1);
      return;
    }

    if(itemStack.id === AIR) { // add one to empty slot
      addClickedWithHeldItem({ ...itemStack, id: currentlyHeldItem.id, snbt: currentlyHeldItem.snbt }, 1);
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

  const setHoveredTagPosition = (x: number, y: number) => {
    if(!hoveredItemTagRef.current) return;

    hoveredItemTagRef.current.style.left = `${x + 15}px`;
    hoveredItemTagRef.current.style.top = `${y - 20}px`;
  };

  const handleMouseEnter = (e: MouseEvent) => {
    if(held) return;

    setHovered(true);
    setHoveredTagPosition(e.clientX, e.clientY);
  };

  const handleMouseMove = (e: MouseEvent) => {
    if(held) return;

    setHoveredTagPosition(e.clientX, e.clientY);
  };

  const handleMouseLeave = () => {
    if(held) return;

    setHovered(false);
  };

  useEffect(() => {
    if(!versionCtx) return;

    setResolvedNBT(
      itemStack.snbt
      ? createResolver(versionCtx.version, itemStack.id, itemStack.snbt)
      : null
    );
  }, [versionCtx, itemStack]);

  if(!ctx) return <></>;

  const itemComponent = (
    <div
      data-slot="inventory-item"
      data-slot-id={itemStack.slot}
      data-item-id={itemStack.id}
      className={cn(
        "relative h-[48px] max-md:h-[36px] aspect-square p-1 hover:bg-muted select-none image-pixelated",
        held && "pointer-events-none",
        (nbtEditMode && !isFromExplorer(itemStack)) && "cursor-pointer",
        className
      )}
      onClick={() => handleLeftClick()}
      onContextMenu={(e) => handleRightClick(e)}
      onAuxClick={(e) => handleAuxClick(e)}
      onMouseEnter={(e) => handleMouseEnter(e)}
      onMouseMove={(e) => handleMouseMove(e)}
      onMouseLeave={() => handleMouseLeave()}
      ref={ref}>
      {textureItem && (
        <img
          className="w-full z-0"
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

      {(textureItem && resolvedNBT) && (
        <>
          {/* Enchanted Glint Effect */}
          {resolvedNBT.shouldGlint() && (
            <div
              className="item-glint absolute inset-0 top-0 left-0 z-10"
              style={{
                backgroundImage: `url(${GlintTexture.src})`,
                maskImage: `url(${textureItem ? textureItem.texture : ""})`,
                WebkitMaskImage: `url(${textureItem ? textureItem.texture : ""})`
              }}/>
          )}
          {/* Potion Color Overlay */}
          {resolvedNBT.isPotion() && (
            <div
              className="item-potion-overlay absolute inset-0 top-0 left-0 z-10"
              style={{
                backgroundImage: `url(${PotionOverlayTexture.src})`,
                backgroundColor: `rgb(${resolvedNBT.getPotionColor()?.join(",")})`,
                maskImage: `url(${PotionOverlayTexture.src})`,
                WebkitMaskImage: `url(${PotionOverlayTexture.src})`
              }}/>
          )}
        </>
      )}

      {/* Item Hovered Tag */}
      {(itemStack.id !== AIR && !held) && (
        <div
          className={cn(
            "fixed hidden whitespace-nowrap flex-col *:leading-5.5 z-20 cc-root",
            "bg-[rgba(0,0,0,.95)] outline-2 -outline-offset-4 outline-[rgb(41,5,96)] rounded-sm py-1 px-2",
            hovered && "flex",
            minecraftAE.className
          )}
          ref={hoveredItemTagRef}>
          {/* Name / Custom Name */}
          <span className={cn(
            resolvedNBT?.hasCustomName() && "italic",
            resolvedNBT?.hasEnchantments() && "cc-b"
          )}>
            {resolvedNBT?.getName() ?? $mc(itemStack.id)}
          </span>
          {/* Enchantment List */}
          {resolvedNBT?.hasEnchantments() && (
            <div className="flex flex-col gap-0 mb-4 cc-7">
              {Array.from(resolvedNBT.getEnchantments()).map(([id, level], i) => (
                <span key={i}>
                  {$(`enchantment.minecraft.${id.replace("minecraft:", "")}` as any) +" "}
                  {
                    level >= 1 && level <= 10
                    ? $(`enchantment.level.${level}` as any)
                    : level
                  }
                </span>
              ))}
            </div>
          )}
          {/* Unbreakable */}
          {resolvedNBT?.isUnbreakable() && (
            <span className="cc-9">{$("item.unbreakable")}</span>
          )}
          {/* Item ID */}
          <span className="cc-7">{itemStack.id}</span>
          {/* Component Amount (>=1.20.5) */}
          {resolvedNBT instanceof ComponentsResolver && (
            <span className="cc-7">
              {$("players.inventory.item-tag.components", resolvedNBT.getComponentAmount())}
            </span>
          )}
        </div>
      )}
    </div>
  );

  if(isFromExplorer(itemStack)) return itemComponent;

  return (
    <ItemDialog
      itemStack={itemStack}
      disabled={!nbtEditMode}
      asChild>
      {itemComponent}
    </ItemDialog>
  );
}
