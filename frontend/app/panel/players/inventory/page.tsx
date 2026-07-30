"use client";

import type { Item } from "minecraft-textures";
import type { InventoryType, ItemStack, PlayerInventory } from "@/lib/types";
import { useCallback, useContext, useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";
import { Backpack } from "lucide-react";
import { SubPage } from "../../sub-page";
import { InventoryContent } from "./inventory-content";
import { ItemExplorer } from "./item-explorer";
import { VersionContext } from "@/contexts/api-context";
import { getTextures } from "@/lib/texture";
import { InventoryItem } from "./inventory-item";
import { InventoryTooltipProvider } from "./inventory-item-tooltip";
import { InventoryTextureContext } from "@/contexts/inventory-texture-context";
import {
  replaceInventoryItem,
  resolveInventoryInteraction,
  type InventoryInteractionRequest
} from "./inventory-interaction";
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
  const currentlyHeldItemRef = useRef<ItemStack | null>(null);
  const heldItemElemRef = useRef<HTMLDivElement | null>(null);
  const mousePositionRef = useRef<{ x: number; y: number }>({ x: 0, y: 0 });
  const client = useWebSocket(InventoryClient, uuid ?? "");

  useEffect(() => {
    if(!versionCtx) return;

    getTextures(versionCtx.version).then(setTextures);
  }, [versionCtx]);

  const positionHeldItemContainer = useCallback(() => {
    if(!heldItemElemRef.current) return;

    const heldItemElem = heldItemElemRef.current;
    const rect = heldItemElem.getBoundingClientRect();
    heldItemElem.style.top = `${mousePositionRef.current.y - rect.height / 2}px`;
    heldItemElem.style.left = `${mousePositionRef.current.x - rect.width / 2}px`;
  }, []);

  const handleMouseMove = useCallback((event: MouseEvent) => {
    mousePositionRef.current = { x: event.clientX, y: event.clientY };
    positionHeldItemContainer();
  }, [positionHeldItemContainer]);

  const updateHeldItem = useCallback((item: ItemStack | null) => {
    currentlyHeldItemRef.current = item;
    setCurrentlyHeldItem(item);
  }, []);

  const updateItem = useCallback((inventoryType: InventoryType, item: ItemStack) => {
    if(!client) return false;

    setInventory(currentInventory => currentInventory
      ? replaceInventoryItem(currentInventory, inventoryType, item)
      : currentInventory);
    client.send("update", { inventoryType, item });
    return true;
  }, [client]);

  const updateItemNBT = useCallback((
    inventoryType: InventoryType,
    item: ItemStack,
    snbt: string
  ) => {
    updateItem(inventoryType, { ...item, snbt });
  }, [updateItem]);

  const handleInteract = useCallback((request: InventoryInteractionRequest) => {
    const result = resolveInventoryInteraction({
      button: request.button,
      source: request.source,
      clickedItem: request.clickedItem,
      heldItem: currentlyHeldItemRef.current,
      maxStackSize: request.maxStackSize
    });

    if(request.inventoryType && result.replacementItem) {
      if(!updateItem(request.inventoryType, result.replacementItem)) return;
    }
    updateHeldItem(result.nextHeldItem);
  }, [updateHeldItem, updateItem]);

  const deleteHeldItem = useCallback(() => updateHeldItem(null), [updateHeldItem]);

  useEffect(() => {
    if(!client) return;

    client.subscribe("init", (data: PlayerInventory) => {
      setInventory(data);
      emitter.emit("loading-done");
    });

    client.subscribe("update", (data: PlayerInventory) => {
      setInventory(data);
    });

    client.subscribe("error", (err: number) => {
      switch(err) {
        case 400:
          toast.error($("players.inventory.ws.error.400"));
          client.send("fetch", null);
          break;
        case 404:
          updateHeldItem(null);
          toast.error($("players.inventory.ws.error.404"));
          push("/panel/players");
          break;
      }
    });

    emitter.on("refresh-data", () => client.send("fetch", null));

    return () => {
      emitter.removeAllListeners("refresh-data");
    };
  }, [client, push, updateHeldItem]);

  useEffect(() => {
    positionHeldItemContainer();
  }, [currentlyHeldItem, positionHeldItemContainer]);

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
      category={$("sidebar.management")}
      icon={<Backpack />}
      pageClassName="min-h-0 min-xl:px-64!"
      className="min-h-0 h-full flex gap-4 max-lg:flex-col max-lg:items-center">
      <InventoryTextureContext.Provider value={textures}>
        <InventoryTooltipProvider>
          {inventory && (
            <InventoryContent
              inventory={inventory}
              heldItem={currentlyHeldItem}
              nbtEditMode={nbtEditMode}
              setNbtEditMode={setNbtEditMode}
              onInteract={handleInteract}
              onUpdateItemNBT={updateItemNBT}
              onDeleteHeldItem={deleteHeldItem}/>
          )}
          <ItemExplorer
            onInteract={handleInteract}
            className="flex-1 w-full"/>
          {currentlyHeldItem && (
            <InventoryItem
              itemStack={currentlyHeldItem}
              held
              className="fixed top-0 left-0 bg-transparent!"
              ref={heldItemElemRef}/>
          )}
        </InventoryTooltipProvider>
      </InventoryTextureContext.Provider>
    </SubPage>
  );
}
