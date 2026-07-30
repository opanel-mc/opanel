import type { InventoryType, ItemStack } from "@/lib/types";
import {
  type MouseEvent,
  type RefObject,
  useContext,
  useEffect,
  useMemo,
  useRef,
  memo
} from "react";
import { toast } from "sonner";
import { InventoryTextureContext } from "@/contexts/inventory-texture-context";
import { InventoryTooltipContext } from "@/contexts/inventory-tooltip-context";
import { cn } from "@/lib/utils";
import { minecraftAE } from "@/lib/fonts";
import { ItemDialog } from "./item-dialog";
import { $ } from "@/lib/i18n";
import { VersionContext } from "@/contexts/api-context";
import { createResolver } from "@/lib/nbt";
import { itemModelToTextureId } from "@/lib/nbt/components-resolver";
import { DEFAULT_MAX_STACK_SIZE } from "@/lib/nbt/resolver";
import {
  AIR,
  type InventoryInteractionButton,
  type InventoryInteractionRequest,
  type InventoryInteractionSource
} from "./inventory-interaction";

import GlintTexture from "@/assets/images/overlays/enchanted-glint.png";
import PotionOverlayTexture from "@/assets/images/overlays/potion-overlay.png";
import TippedArrowOverlayTexture from "@/assets/images/overlays/tipped-arrow-overlay.png";
import LeatherHelmetOverlayTexture from "@/assets/images/overlays/leather-helmet-overlay.png";
import LeatherChestplateOverlayTexture from "@/assets/images/overlays/leather-chestplate-overlay.png";
import LeatherLeggingsOverlayTexture from "@/assets/images/overlays/leather-leggings-overlay.png";
import LeatherBootsOverlayTexture from "@/assets/images/overlays/leather-boots-overlay.png";
import MissingTexture from "@/assets/images/inventory/missing-texture.png";
import "@/style/item-effect.css";

export { AIR } from "./inventory-interaction";

function getLeatherOverlay(id: string): string | null {
  switch(id) {
    case "minecraft:leather_helmet":
      return LeatherHelmetOverlayTexture.src;
    case "minecraft:leather_chestplate":
      return LeatherChestplateOverlayTexture.src;
    case "minecraft:leather_leggings":
      return LeatherLeggingsOverlayTexture.src;
    case "minecraft:leather_boots":
      return LeatherBootsOverlayTexture.src;
    default:
      return null;
  }
}

export interface InventoryItemProps {
  itemStack: ItemStack
  inventoryType?: InventoryType
  placeholderIcon?: string
  held?: boolean
  interactionMode?: "inventory" | "container"
  nbtEditMode?: boolean
  onInteract?: (request: InventoryInteractionRequest) => void
  onUpdateItemNBT?: (inventoryType: InventoryType, item: ItemStack, snbt: string) => void
  className?: string
  ref?: RefObject<HTMLDivElement | null>
}

const noopInteract = () => undefined;

export const InventoryItem = memo(({
  itemStack,
  inventoryType,
  placeholderIcon,
  held = false,
  interactionMode = "inventory",
  nbtEditMode = false,
  onInteract = noopInteract,
  onUpdateItemNBT,
  className,
  ref
}: InventoryItemProps) => {
  const versionCtx = useContext(VersionContext);
  const textures = useContext(InventoryTextureContext);
  const tooltipCtx = useContext(InventoryTooltipContext);
  const resolvedNBT = useMemo(() => {
    if(!versionCtx || !itemStack.snbt) return null;
    return createResolver(versionCtx.version, itemStack.id, itemStack.snbt);
  }, [versionCtx, itemStack.id, itemStack.snbt]);
  const maxStackSize = resolvedNBT?.getMaxStackSize() ?? DEFAULT_MAX_STACK_SIZE;
  const textureIdFromItemModel = useMemo(
    () => itemModelToTextureId(resolvedNBT?.getItemModel() ?? null),
    [resolvedNBT]
  );

  const itemTexture = useMemo(() => {
    if(!textures) return undefined;

    const byModel = (
      textureIdFromItemModel
      ? textures.find(({ id }) => id === textureIdFromItemModel)
      : null
    );
    return byModel ?? textures.find(({ id }) => id === itemStack.id);
  }, [itemStack.id, textureIdFromItemModel, textures]);

  const isModItem = itemTexture === undefined && itemStack.id !== AIR;
  const isContainerMode = interactionMode === "container";
  const tooltipOwnerRef = useRef<symbol>(
    Symbol("inventory-item-tooltip-owner")
  );
  const source: InventoryInteractionSource = isContainerMode
    ? "container"
    : itemStack.slot === -1
      ? "explorer"
      : "inventory";

  const interact = (button: InventoryInteractionButton) => {
    if(held || nbtEditMode) return;
    if(isModItem && !isContainerMode) {
      toast.error($("players.inventory.interact-forbbiden"));
      return;
    }
    
    onInteract({
      button,
      source,
      clickedItem: itemStack,
      maxStackSize,
      inventoryType
    });
  };

  const handleRightClick = (e: MouseEvent) => {
    e.preventDefault();
    interact("right");
  };

  const handleAuxClick = (e: MouseEvent) => {
    e.preventDefault();
    if(e.button !== 1) return;
    interact("middle");
  };

  useEffect(() => () => {
    tooltipCtx?.hideTooltip(tooltipOwnerRef.current);
  }, [
    itemStack.count,
    itemStack.id,
    itemStack.slot,
    itemStack.snbt,
    tooltipCtx
  ]);

  if(!textures || !tooltipCtx) return <></>;

  const itemComponent = (
    <div
      data-slot="inventory-item"
      data-slot-id={itemStack.slot}
      data-item-id={itemStack.id}
      data-held={held ? "true" : undefined}
      className={cn(
        "relative h-[48px] max-md:h-[36px] aspect-square p-1 hover:bg-muted select-none image-pixelated",
        held && "pointer-events-none",
        ((nbtEditMode && itemStack.slot !== -1) || isContainerMode) && "cursor-pointer",
        className
      )}
      onClick={() => interact("left")}
      onContextMenu={(e) => handleRightClick(e)}
      onAuxClick={(e) => handleAuxClick(e)}
      onMouseEnter={(e) => {
        if(itemStack.id === AIR || held) return;
        tooltipCtx.showTooltip(
          tooltipOwnerRef.current,
          { itemStack, resolvedNBT },
          e.clientX,
          e.clientY
        );
      }}
      onMouseMove={(e) => {
        if(itemStack.id === AIR || held) return;
        tooltipCtx.moveTooltip(tooltipOwnerRef.current, e.clientX, e.clientY);
      }}
      onMouseLeave={() => tooltipCtx.hideTooltip(tooltipOwnerRef.current)}
      ref={ref}>
      {itemStack.id !== AIR && (
        <img
          className="w-full z-0"
          src={itemTexture?.texture || MissingTexture.src}
          alt={itemTexture?.id || "missing-texture"}/>
      )}
      {(itemStack.id === AIR && placeholderIcon) && (
        <img
          className="w-full z-0"
          src={placeholderIcon}
          alt="placeholder-icon"/>
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

      {(itemTexture && resolvedNBT) && (
        <>
          {/* Enchanted Glint Effect */}
          {resolvedNBT.shouldGlint() && (
            <div
              className="item-glint"
              style={{
                backgroundImage: `url(${GlintTexture.src})`,
                maskImage: `url(${itemTexture ? itemTexture.texture : ""})`,
                WebkitMaskImage: `url(${itemTexture ? itemTexture.texture : ""})`
              }}/>
          )}
          {/* Potion Color Overlay */}
          {resolvedNBT.isPotion() && (
            <div
              className="color-overlay"
              style={{
                backgroundImage: `url(${PotionOverlayTexture.src})`,
                backgroundColor: `rgb(${resolvedNBT.getPotionColor()?.join(",")})`,
                maskImage: `url(${PotionOverlayTexture.src})`,
                WebkitMaskImage: `url(${PotionOverlayTexture.src})`
              }}/>
          )}
          {/* Tipped Arrow Color Overlay */}
          {resolvedNBT.isTippedArrow() && (
            <div
              className="color-overlay"
              style={{
                backgroundImage: `url(${TippedArrowOverlayTexture.src})`,
                backgroundColor: `rgb(${resolvedNBT.getPotionColor()?.join(",")})`,
                maskImage: `url(${TippedArrowOverlayTexture.src})`,
                WebkitMaskImage: `url(${TippedArrowOverlayTexture.src})`
              }}/>
          )}
          {/* Leather Armor Color Overlay */}
          {resolvedNBT.isDyedLeatherArmor() && (
            <div
              className="color-overlay"
              style={{
                backgroundImage: `url(${getLeatherOverlay(itemStack.id)})`,
                backgroundColor: `rgb(${resolvedNBT.getDyedColor()?.join(",")})`,
                maskImage: `url(${getLeatherOverlay(itemStack.id)})`,
                WebkitMaskImage: `url(${getLeatherOverlay(itemStack.id)})`
              }}/>
          )}
        </>
      )}

    </div>
  );

  if(isContainerMode || itemStack.slot === -1 || itemStack.id === AIR) return itemComponent;

  return (
    <ItemDialog
      itemStack={itemStack}
      inventoryType={inventoryType}
      disabled={!nbtEditMode}
      onUpdateItemNBT={onUpdateItemNBT}
      asChild>
      {itemComponent}
    </ItemDialog>
  );
}, (prev, next) => (
  prev.itemStack.slot === next.itemStack.slot
  && prev.itemStack.id === next.itemStack.id
  && prev.itemStack.count === next.itemStack.count
  && prev.itemStack.snbt === next.itemStack.snbt
  && prev.inventoryType === next.inventoryType
  && prev.placeholderIcon === next.placeholderIcon
  && prev.held === next.held
  && prev.interactionMode === next.interactionMode
  && prev.nbtEditMode === next.nbtEditMode
  && prev.onInteract === next.onInteract
  && prev.onUpdateItemNBT === next.onUpdateItemNBT
  && prev.className === next.className
  && prev.ref === next.ref
));
