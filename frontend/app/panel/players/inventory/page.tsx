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
import { InventoryItem } from "./inventory-item";
import { InventoryClient } from "@/lib/ws/inventory";
import { useWebSocket } from "@/hooks/use-websocket";
import { emitter } from "@/lib/emitter";

const AIR = "minecraft:air";

export default function Inventory() {
  const searchParams = useSearchParams();
  const uuid = searchParams.get("uuid");
  const { push } = useRouter();
  const versionCtx = useContext(VersionContext);
  const [textures, setTextures] = useState<Item[] | null>(null);
  const [inventory, setInventory] = useState<PlayerInventory | null>(null);
  const [currentlyHeldItem, setCurrentlyHeldItem] = useState<ItemStack | null>(null);
  const heldItemElemRef = useRef<HTMLDivElement | null>(null);
  const mousePositionRef = useRef<{ x: number; y: number }>({ x: 0, y: 0 });
  const client = useWebSocket(InventoryClient, uuid ?? "");

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

  const swapClickedWithHeldItem = (clickedItem: ItemStack) => {
    setCurrentlyHeldItem(clickedItem.id === AIR ? null : clickedItem);
    client?.send("update", { ...currentlyHeldItem, slot: clickedItem.slot });
  };

  const addClickedWithHeldItem = (clickedItem: ItemStack) => {
    setCurrentlyHeldItem(null);
    client?.send("update", { ...clickedItem, count: clickedItem.count + currentlyHeldItem!.count });
  };

  const removeClickedItem = ({ slot }: ItemStack) => {
    client?.send("update", { id: AIR, count: 0, slot, nbt: null });
  };

  const halfClickedItem = (clickedItem: ItemStack) => {
    client?.send("update", { ...clickedItem, count: Math.floor(clickedItem.count / 2) });
  };

  // Get textures by mc version
  useEffect(() => {
    if(!versionCtx) return;

    getTextures(versionCtx.version).then(setTextures);
  }, [versionCtx]);

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
      title="玩家"
      subTitle="背包编辑"
      description="查看和编辑玩家的背包内容"
      category="服务器"
      icon={<Backpack />}
      pageClassName="min-xl:px-64!"
      className="min-h-0 h-full flex gap-4 max-lg:flex-col max-lg:items-center">
      <InventoryContext.Provider value={{
        textures,
        currentlyHeldItem,
        setCurrentlyHeldItem,
        swapClickedWithHeldItem,
        addClickedWithHeldItem,
        removeClickedItem,
        halfClickedItem
      }}>
        {inventory && <InventoryContent inventory={inventory}/>}
        <ItemExplorer className="flex-1 w-full"/>
        {currentlyHeldItem && (
          <InventoryItem
            itemStack={currentlyHeldItem}
            held
            className="fixed bg-transparent!"
            ref={heldItemElemRef}/>
        )}
      </InventoryContext.Provider>
    </SubPage>
  );
}
