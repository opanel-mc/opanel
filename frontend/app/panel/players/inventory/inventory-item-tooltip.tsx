"use client";

import type { PropsWithChildren } from "react";
import {
  useCallback,
  useLayoutEffect,
  useMemo,
  useRef,
  useState
} from "react";
import { createPortal } from "react-dom";
import {
  InventoryTooltipContext,
  type InventoryTooltipData,
} from "@/contexts/inventory-tooltip-context";
import { ComponentsResolver } from "@/lib/nbt/components-resolver";
import { minecraftAE } from "@/lib/fonts";
import { cn } from "@/lib/utils";
import { $, $mc } from "@/lib/i18n";

interface TooltipState extends InventoryTooltipData {
  owner: symbol
}

export function InventoryTooltipProvider({
  children
}: PropsWithChildren) {
  const [tooltip, setTooltip] = useState<TooltipState | null>(null);
  const tooltipRef = useRef<HTMLDivElement | null>(null);
  const positionRef = useRef({ x: 0, y: 0 });
  const ownerRef = useRef<symbol | null>(null);

  const positionTooltip = useCallback((x: number, y: number) => {
    const elem = tooltipRef.current;
    if(!elem) return;

    const gap = 15;
    const viewportMargin = 8;
    const rect = elem.getBoundingClientRect();
    let left = x + gap;
    let top = y - 20;

    if(left + rect.width > window.innerWidth - viewportMargin) {
      left = x - rect.width - gap;
    }
    left = Math.max(
      viewportMargin,
      Math.min(left, window.innerWidth - rect.width - viewportMargin)
    );
    top = Math.max(
      viewportMargin,
      Math.min(top, window.innerHeight - rect.height - viewportMargin)
    );

    elem.style.left = `${left}px`;
    elem.style.top = `${top}px`;
  }, []);

  const showTooltip = useCallback((
    owner: symbol,
    data: InventoryTooltipData,
    x: number,
    y: number
  ) => {
    ownerRef.current = owner;
    positionRef.current = { x, y };
    setTooltip({ owner, ...data });
  }, []);

  const moveTooltip = useCallback((owner: symbol, x: number, y: number) => {
    if(ownerRef.current !== owner) return;
    positionRef.current = { x, y };
    positionTooltip(x, y);
  }, [positionTooltip]);

  const hideTooltip = useCallback((owner: symbol) => {
    if(ownerRef.current !== owner) return;
    ownerRef.current = null;
    setTooltip(null);
  }, []);

  useLayoutEffect(() => {
    if(!tooltip) return;
    positionTooltip(positionRef.current.x, positionRef.current.y);
  }, [positionTooltip, tooltip]);

  const context = useMemo(() => ({
    showTooltip,
    moveTooltip,
    hideTooltip
  }), [hideTooltip, moveTooltip, showTooltip]);

  return (
    <InventoryTooltipContext.Provider value={context}>
      {children}
      {(tooltip && typeof document !== "undefined") && createPortal(
        (
          <InventoryItemTooltip
            data={tooltip}
            ref={tooltipRef}/>
        ),
        document.body
      )}
    </InventoryTooltipContext.Provider>
  );
}

function InventoryItemTooltip({
  data,
  ref
}: {
  data: InventoryTooltipData
  ref: React.RefObject<HTMLDivElement | null>
}) {
  const { itemStack, resolvedNBT } = data;

  return (
    <div
      data-slot="inventory-item-hover"
      className={cn(
        "fixed flex w-max max-w-[calc(100vw-1rem)] whitespace-normal pointer-events-none flex-col *:leading-5.5 z-[100] cc-root text-white",
        "bg-[rgba(0,0,0,.95)] outline-2 -outline-offset-4 outline-[rgb(41,5,96)] rounded-sm py-1 px-2",
        minecraftAE.className
      )}
      ref={ref}>
      <span className={cn(
        resolvedNBT?.hasCustomName() && "italic",
        resolvedNBT?.hasEnchantments() && "cc-b"
      )}>
        {resolvedNBT?.getName() ?? $mc(itemStack.id)}
      </span>
      {(resolvedNBT && (resolvedNBT.hasEnchantments() || resolvedNBT.getLore().length > 0)) && (
        <div className="flex flex-col gap-0 mb-4 cc-7">
          {Array.from(resolvedNBT.getEnchantments()).map(([id, level]) => (
            <span key={id}>
              {$(`enchantment.minecraft.${id.replace("minecraft:", "")}` as any) +" "}
              {
                level >= 1 && level <= 10
                ? $(`enchantment.level.${level}` as any)
                : level
              }
            </span>
          ))}
          <div className="flex flex-col gap-0 cc-5 italic">
            {resolvedNBT.getLore().map((line, i) => (
              <span key={i}>{line}</span>
            ))}
          </div>
        </div>
      )}
      {resolvedNBT?.isUnbreakable() && (
        <span className="cc-9">{$("item.unbreakable")}</span>
      )}
      {(resolvedNBT && resolvedNBT.getMapId() !== null) && (
        <span className="cc-7">
          {$("filled_map.id").replace("%s", resolvedNBT.getMapId()?.toString() ?? "")}
        </span>
      )}
      {(resolvedNBT && resolvedNBT.getBeeAmount() !== null) && (
        <span className="cc-7">
          {$("container.beehive.bees").replace("%s", resolvedNBT.getBeeAmount()?.toString() ?? "0").replace("%s", "3")}
        </span>
      )}
      {(resolvedNBT && resolvedNBT.getHoneyLevel() !== null) && (
        <span className="cc-7">
          {$("container.beehive.honey").replace("%s", resolvedNBT.getHoneyLevel()?.toString() ?? "0").replace("%s", "5")}
        </span>
      )}
      <span className="cc-7">{itemStack.id}</span>
      {resolvedNBT instanceof ComponentsResolver && (
        <span className="cc-7">
          {$("players.inventory.item-tag.components", resolvedNBT.getComponentAmount())}
        </span>
      )}
    </div>
  );
}
