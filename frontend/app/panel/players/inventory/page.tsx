"use client";

import type { Item } from "minecraft-textures";
import type { ItemStack, PlayerInventory } from "@/lib/types";
import { useCallback, useContext, useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Backpack } from "lucide-react";
import { SubPage } from "../../sub-page";
import { InventoryContext } from "@/contexts/inventory-context";
import { InventoryContent } from "./inventory-content";
import { ItemExplorer } from "./item-explorer";
import { VersionContext } from "@/contexts/api-context";
import { getTextures } from "@/lib/texture";
import { AIR, InventoryItem } from "./inventory-item";
import { InventoryClient } from "@/lib/ws/inventory";
import { useWebSocket } from "@/hooks/use-websocket";
import { emitter } from "@/lib/emitter";
import { $ } from "@/lib/i18n";

export default function Inventory() {
  const searchParams = useSearchParams();
  const uuid = searchParams.get("uuid");
  const { push } = useRouter();
  const versionCtx = useContext(VersionContext);
  const [textures, setTextures] = useState<Item[] | null>(null);
  const [inventory, setInventory] = useState<PlayerInventory | null>(null);
  const [currentlyHeldItem, setCurrentlyHeldItem] = useState<ItemStack | null>(null);
  const [nbtEditMode, setNbtEditMode] = useState(false);
  const heldItemElemRef = useRef<HTMLDivElement | null>(null);
  const mousePositionRef = useRef<{ x: number; y: number }>({ x: 0, y: 0 });
  const client = useWebSocket(InventoryClient, uuid ?? "");

  // Get textures by mc version
  useEffect(() => {
    if(!versionCtx) return;

    getTextures(versionCtx.version).then(setTextures);
  }, [versionCtx]);

  const positionHeldItemCountainer = () => {
    if(!heldItemElemRef.current) return;

    const heldItemElem = heldItemElemRef.current;
    const rect = heldItemElem.getBoundingClientRect();
    heldItemElem.style.top = `${mousePositionRef.current.y - rect.height / 2}px`;
    heldItemElem.style.left = `${mousePositionRef.current.x - rect.width / 2}px`;
  };

  const handleMouseMove = useCallback((e: MouseEvent) => {
    mousePositionRef.current = { x: e.clientX, y: e.clientY };
    positionHeldItemCountainer();
  }, []);

  const minusHeldItemCount = (count: number) => {
    if(!currentlyHeldItem) return;

    const newCount = currentlyHeldItem.count - count;
    if(newCount <= 0) {
      setCurrentlyHeldItem(null);
      return;
    }
    setCurrentlyHeldItem({ ...currentlyHeldItem, count: newCount });
  };

  const swapClickedWithHeldItem = (clickedItem: ItemStack) => {
    setCurrentlyHeldItem(clickedItem.id === AIR ? null : clickedItem);
    client?.send("update", { ...currentlyHeldItem, slot: clickedItem.slot });
  };

  const addClickedWithHeldItem = (clickedItem: ItemStack, count: number) => {
    minusHeldItemCount(count);
    client?.send("update", { ...clickedItem, count: clickedItem.count + count });
  };

  const removeClickedItem = ({ slot }: ItemStack) => {
    client?.send("update", { id: AIR, count: 0, slot, snbt: null });
  };

  const halfClickedItem = (clickedItem: ItemStack) => {
    client?.send("update", { ...clickedItem, count: Math.floor(clickedItem.count / 2) });
  };

  const updateItemNBT = (item: ItemStack, snbt: string) => {
    client?.send("update", { ...item, snbt });
  };

  useEffect(() => {
    if(!client) return;

    client.subscribe("init", (data: PlayerInventory) => {
      setInventory(data);
    });

    client.subscribe("update", (data: PlayerInventory) => {
      setInventory(data);
    });

    emitter.on("refresh-data", () => client.send("fetch", null));

    return () => {
      emitter.removeAllListeners("refresh-data");
    };
  }, [client, uuid]);

  // Update held item position as soon as it is picked up
  useEffect(() => {
    positionHeldItemCountainer();
  }, [currentlyHeldItem]);

  useEffect(() => {
    document.addEventListener("mousemove", handleMouseMove);
    return () => {
      document.removeEventListener("mousemove", handleMouseMove);
    };
  }, [handleMouseMove]);

  if(!uuid) {
    push("/panel/players");
    return <></>;
  }

  if(!versionCtx || !textures) return <></>;

  return (
    <SubPage
      title={$("players.title")}
      subTitle={$("players.inventory.title")}
      description={$("players.inventory.description")}
      category={$("sidebar.server")}
      icon={<Backpack />}
      pageClassName="min-xl:px-64!"
      className="min-h-0 h-full flex gap-4 max-lg:flex-col max-lg:items-center">
      <InventoryContext.Provider value={{
        textures,
        currentlyHeldItem,
        setCurrentlyHeldItem,
        nbtEditMode,
        setNbtEditMode,
        swapClickedWithHeldItem,
        addClickedWithHeldItem,
        removeClickedItem,
        halfClickedItem,
        updateItemNBT
      }}>
        {inventory && <InventoryContent inventory={inventory}/>}
        <ItemExplorer className="flex-1 w-full"/>
        {currentlyHeldItem && (
          <InventoryItem
            itemStack={currentlyHeldItem}
            held
            className="fixed top-0 left-0 bg-transparent!"
            ref={heldItemElemRef}/>
        )}
      </InventoryContext.Provider>
    </SubPage>
  );
}
