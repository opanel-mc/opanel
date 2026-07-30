"use client";

import type { ItemStack } from "@/lib/types";
import type { ContainerSnapshot } from "@/lib/nbt/container";
import {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  type Dispatch,
  type SetStateAction
} from "react";
import { createPortal } from "react-dom";
import { InventoryType } from "@/lib/types";
import { InventoryContext } from "@/contexts/inventory-context";
import { $ } from "@/lib/i18n";
import { cn } from "@/lib/utils";
import { AIR, InventoryItem } from "./inventory-item";
import { InventoryTrash } from "./inventory-trash";

function emptyItem(slot: number): ItemStack {
  return { slot, id: AIR, count: 0 };
}

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
  const parentContext = useContext(InventoryContext);
  const heldItemElemRef = useRef<HTMLDivElement | null>(null);
  const mousePositionRef = useRef({ x: 0, y: 0 });

  const replaceItem = useCallback((item: ItemStack) => {
    const items = [...container.items];
    items[item.slot] = item.id === AIR || item.count <= 0
      ? emptyItem(item.slot)
      : item;
    onItemsChange(items);
  }, [container.items, onItemsChange]);

  const swapClickedWithHeldItem = useCallback((
    _inventoryType: InventoryType,
    clickedItem: ItemStack
  ) => {
    if(!heldItem) return;
    setHeldItem(clickedItem.id === AIR ? null : clickedItem);
    replaceItem({ ...heldItem, slot: clickedItem.slot });
  }, [heldItem, replaceItem, setHeldItem]);

  const addClickedWithHeldItem = useCallback((
    _inventoryType: InventoryType,
    clickedItem: ItemStack,
    count: number
  ) => {
    if(!heldItem) return;

    const heldCount = heldItem.count - count;
    setHeldItem(heldCount > 0 ? { ...heldItem, count: heldCount } : null);
    replaceItem({ ...clickedItem, count: clickedItem.count + count });
  }, [heldItem, replaceItem, setHeldItem]);

  const removeClickedItem = useCallback((
    _inventoryType: InventoryType,
    clickedItem: ItemStack
  ) => {
    replaceItem(emptyItem(clickedItem.slot));
  }, [replaceItem]);

  const halfClickedItem = useCallback((
    _inventoryType: InventoryType,
    clickedItem: ItemStack
  ) => {
    const count = Math.floor(clickedItem.count / 2);
    replaceItem(count > 0
      ? { ...clickedItem, count }
      : emptyItem(clickedItem.slot));
  }, [replaceItem]);

  const localCtx = useMemo(() => ({
    textures: parentContext.textures,
    currentlyHeldItem: heldItem,
    setCurrentlyHeldItem: setHeldItem,
    nbtEditMode: false,
    setNbtEditMode: () => undefined,
    swapClickedWithHeldItem,
    addClickedWithHeldItem,
    removeClickedItem,
    halfClickedItem,
    updateItemNBT: () => undefined
  }), [
    addClickedWithHeldItem,
    halfClickedItem,
    heldItem,
    parentContext.textures,
    removeClickedItem,
    setHeldItem,
    swapClickedWithHeldItem
  ]);

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

  if(!parentContext) return <></>;

  return (
    <InventoryContext.Provider value={localCtx}>
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
              inventoryType={InventoryType.MAIN}
              interactionMode="container"
              className="min-w-0 w-full max-md:h-[32px]"
              key={item.slot}/>
          ))}
        </div>
        <InventoryTrash className="self-end max-md:h-[32px]"/>
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
    </InventoryContext.Provider>
  );
}
