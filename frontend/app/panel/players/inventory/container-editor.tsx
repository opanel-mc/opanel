"use client";

import type { ItemStack } from "@/lib/types";
import type { ContainerSnapshot } from "@/lib/nbt/container";
import {
  useCallback,
  useEffect,
  useRef,
  type Dispatch,
  type SetStateAction
} from "react";
import { createPortal } from "react-dom";
import { $ } from "@/lib/i18n";
import { useInventoryRightDrag } from "@/hooks/use-inventory-right-drag";
import { cn } from "@/lib/utils";
import { InventoryItem } from "./inventory-item";
import { InventoryTrash } from "./inventory-trash";
import {
  resolveInventoryInteraction,
  type InventoryInteractionRequest
} from "./inventory-interaction";

export function ContainerEditor({
  container,
  heldItem,
  setHeldItem,
  onItemsChange
}: {
  container: ContainerSnapshot
  heldItem: ItemStack | null
  setHeldItem: Dispatch<SetStateAction<ItemStack | null>>
  onItemsChange: (items: ItemStack[]) => void
}) {
  const heldItemElemRef = useRef<HTMLDivElement | null>(null);
  const mousePositionRef = useRef({ x: 0, y: 0 });
  const heldItemRef = useRef(heldItem);
  const containerRef = useRef(container);
  const onItemsChangeRef = useRef(onItemsChange);
  const rightDragState = useInventoryRightDrag(heldItem !== null);
  heldItemRef.current = heldItem;
  containerRef.current = container;
  onItemsChangeRef.current = onItemsChange;

  const updateHeldItem = useCallback((item: ItemStack | null) => {
    heldItemRef.current = item;
    setHeldItem(item);
  }, [setHeldItem]);

  const handleInteract = useCallback((request: InventoryInteractionRequest) => {
    const result = resolveInventoryInteraction({
      ...request,
      heldItem: heldItemRef.current
    });
    updateHeldItem(result.nextHeldItem);
    if(!result.replacementItem) return;

    const items = [...containerRef.current.items];
    items[result.replacementItem.slot] = result.replacementItem;
    onItemsChangeRef.current(items);
  }, [updateHeldItem]);

  const positionHeldItem = useCallback(() => {
    if(!heldItemElemRef.current) return;

    const rect = heldItemElemRef.current.getBoundingClientRect();
    heldItemElemRef.current.style.top = `${mousePositionRef.current.y - rect.height / 2}px`;
    heldItemElemRef.current.style.left = `${mousePositionRef.current.x - rect.width / 2}px`;
  }, []);

  useEffect(() => {
    const handleMouseMove = (event: MouseEvent) => {
      mousePositionRef.current = { x: event.clientX, y: event.clientY };
      positionHeldItem();
    };
    document.addEventListener("mousemove", handleMouseMove);
    return () => document.removeEventListener("mousemove", handleMouseMove);
  }, [positionHeldItem]);

  useEffect(() => positionHeldItem(), [heldItem, positionHeldItem]);

  return (
    <>
      <section className="min-w-0 flex flex-col gap-2">
        <h3 className="text-sm font-semibold">
          {$("players.inventory.container-editor.title")}
        </h3>
        <div
          data-slot="container-inventory"
          data-container-size={container.size}
          className={cn(
            "w-[calc(9*48px+2px)] max-md:w-[calc(9*32px+2px)] max-w-full",
            "grid grid-cols-9 content-start border rounded-sm",
            "[&>*]:border-r [&>*]:border-b [&>*:nth-child(9n)]:border-r-0",
            container.size > 45
              ? "max-h-[calc(5*48px+2px)] max-md:max-h-[calc(5*32px+2px)] overflow-x-hidden overflow-y-auto o-scrollbar"
              : "overflow-hidden"
          )}>
          {container.items.map((item) => (
            <InventoryItem
              itemStack={item}
              interactionMode="container"
              onInteract={handleInteract}
              rightDragState={rightDragState}
              className="min-w-0 w-full max-md:h-[32px]"
              key={item.slot}/>
          ))}
        </div>
        <InventoryTrash
          canDelete={heldItem !== null}
          onDelete={() => updateHeldItem(null)}
          className="self-end max-md:h-[32px]"/>
      </section>
      {(heldItem && typeof document !== "undefined") && (
        createPortal(
          (
            <InventoryItem
              itemStack={heldItem}
              held
              interactionMode="container"
              className="fixed top-0 left-0 z-[90] bg-transparent!"
              ref={heldItemElemRef}/>
          ),
          document.body
        )
      )}
    </>
  );
}
