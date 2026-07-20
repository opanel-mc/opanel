import type { InventoryType, ItemStack } from "@/lib/types";
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
import { toast } from "sonner";
import { InventoryContext } from "@/contexts/inventory-context";
import { cn } from "@/lib/utils";
import { minecraftAE } from "@/lib/fonts";
import { ItemDialog } from "./item-dialog";
import { $, $mc } from "@/lib/i18n";
import { VersionContext } from "@/contexts/api-context";
import { createResolver } from "@/lib/nbt";
import { ComponentsResolver, itemModelToTextureId } from "@/lib/nbt/components-resolver";

import GlintTexture from "@/assets/images/overlays/enchanted-glint.png";
import PotionOverlayTexture from "@/assets/images/overlays/potion-overlay.png";
import TippedArrowOverlayTexture from "@/assets/images/overlays/tipped-arrow-overlay.png";
import LeatherHelmetOverlayTexture from "@/assets/images/overlays/leather-helmet-overlay.png";
import LeatherChestplateOverlayTexture from "@/assets/images/overlays/leather-chestplate-overlay.png";
import LeatherLeggingsOverlayTexture from "@/assets/images/overlays/leather-leggings-overlay.png";
import LeatherBootsOverlayTexture from "@/assets/images/overlays/leather-boots-overlay.png";
import MissingTexture from "@/assets/images/missing-texture.png";
import "@/style/item-effect.css";

export const AIR = "minecraft:air";

function isFromExplorer(itemStack: ItemStack) {
  return itemStack.slot === -1;
}

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

export function InventoryItem({
  itemStack,
  inventoryType,
  held = false,
  className,
  ref
}: {
  itemStack: ItemStack
  inventoryType?: InventoryType
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
  const [resolvedNBT, setResolvedNBT] = useState<ItemNBTResolver | null>(null);
  const [hovered, setHovered] = useState(false);
  const textureIdFromItemModel = useMemo(
    () => itemModelToTextureId(resolvedNBT?.getItemModel() ?? null),
    [resolvedNBT]
  );

  const textureItem = useMemo(() => {
    const byModel = textureIdFromItemModel
      ? textures.find(({ id }) => id === textureIdFromItemModel)
      : null;
    return byModel ?? textures.find(({ id }) => id === itemStack.id);
  }, [textures, itemStack.id, textureIdFromItemModel]);
  const isModItem = textureItem === undefined && itemStack.id !== AIR;
  const hoveredItemTagRef = useRef<HTMLDivElement | null>(null);

  const handleLeftClick = () => {
    if(held || !ctx || nbtEditMode) return;
    if(isModItem) {
      toast.error($("players.inventory.interact-forbbiden"));
      return;
    }

    if(!currentlyHeldItem) { // pick up the item
      setCurrentlyHeldItem(itemStack);
      if(!isFromExplorer(itemStack) && inventoryType) removeClickedItem(inventoryType, itemStack);
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
      if(inventoryType) addClickedWithHeldItem(inventoryType, itemStack, currentlyHeldItem.count);
      return;
    }

    if(inventoryType) swapClickedWithHeldItem(inventoryType, itemStack);
  };

  const handleRightClick = (e: MouseEvent) => {
    e.preventDefault();
    if(held || !ctx || nbtEditMode) return;
    if(isModItem) {
      toast.error($("players.inventory.interact-forbbiden"));
      return;
    }

    if(!currentlyHeldItem && isFromExplorer(itemStack)) { // pick up 64 from explorer
      setCurrentlyHeldItem({ ...itemStack, count: 64 });
      return;
    }

    if(!currentlyHeldItem) { // pick up half of the item
      setCurrentlyHeldItem({ ...itemStack, count: Math.ceil(itemStack.count / 2) });
      if(inventoryType) halfClickedItem(inventoryType, itemStack);
      return;
    }

    if(isFromExplorer(itemStack)) { // just throw away the held item
      setCurrentlyHeldItem(null);
      return;
    }

    if(itemStack.id === currentlyHeldItem.id && itemStack.snbt === currentlyHeldItem.snbt) { // add one by one
      if(inventoryType) addClickedWithHeldItem(inventoryType, itemStack, 1);
      return;
    }

    if(itemStack.id === AIR) { // add one to empty slot
      if(inventoryType) {
        addClickedWithHeldItem(
          inventoryType,
          { ...itemStack, id: currentlyHeldItem.id, snbt: currentlyHeldItem.snbt },
          1
        );
      }
      return;
    }

    if(inventoryType) swapClickedWithHeldItem(inventoryType, itemStack);
  };

  const handleAuxClick = (e: MouseEvent) => {
    e.preventDefault();
    if(e.button !== 1) return;
    if(held || !ctx || nbtEditMode) return;
    if(isModItem) {
      toast.error($("players.inventory.interact-forbbiden"));
      return;
    }

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
      {itemStack.id !== AIR && (
        <img
          className="w-full z-0"
          src={textureItem?.texture || MissingTexture.src}
          alt={textureItem?.id || "missing-texture"}/>
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
              className="item-glint"
              style={{
                backgroundImage: `url(${GlintTexture.src})`,
                maskImage: `url(${textureItem ? textureItem.texture : ""})`,
                WebkitMaskImage: `url(${textureItem ? textureItem.texture : ""})`
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

      {/* Item Hovered Tag */}
      {(itemStack.id !== AIR && !held) && (
        <div
          className={cn(
            "fixed hidden whitespace-nowrap flex-col *:leading-5.5 z-20 cc-root text-white",
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
          {/* Enchantment List & Lore */}
          {(resolvedNBT && (resolvedNBT?.hasEnchantments() || resolvedNBT?.getLore().length > 0)) && (
            <div className="flex flex-col gap-0 mb-4 cc-7">
              {/* Enchantment List */}
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
              {/* Lore */}
              <div className="flex flex-col gap-0 cc-5 italic">
                {resolvedNBT.getLore().map((line, i) => (
                  <span key={i}>{line}</span>
                ))}
              </div>
            </div>
          )}
          {/* Unbreakable */}
          {resolvedNBT?.isUnbreakable() && (
            <span className="cc-9">{$("item.unbreakable")}</span>
          )}
          {/* Map ID */}
          {(resolvedNBT && resolvedNBT.getMapId() !== null) && (
            <span className="cc-7">
              {$("filled_map.id").replace("%s", resolvedNBT.getMapId()?.toString() ?? "")}
            </span>
          )}
          {/* Bee Amount */}
          {(resolvedNBT && resolvedNBT.getBeeAmount() !== null) && (
            <span className="cc-7">
              {$("container.beehive.bees").replace("%s", resolvedNBT.getBeeAmount()?.toString() ?? "0").replace("%s", "3")}
            </span>
          )}
          {/* Honey Level */}
          {(resolvedNBT && resolvedNBT.getHoneyLevel() !== null) && (
            <span className="cc-7">
              {$("container.beehive.honey").replace("%s", resolvedNBT.getHoneyLevel()?.toString() ?? "0").replace("%s", "5")}
            </span>
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

  if(isFromExplorer(itemStack) || itemStack.id === AIR) return itemComponent;

  return (
    <ItemDialog
      itemStack={itemStack}
      inventoryType={inventoryType}
      disabled={!nbtEditMode}
      asChild>
      {itemComponent}
    </ItemDialog>
  );
}
