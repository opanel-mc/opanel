import type { ItemStack } from "../types";
import {
  NbtList,
  NbtNumber,
  NbtObject,
  NbtString,
  type NbtValue,
  parseNbtString
} from "@/lib/snbt";
import { prettyFormatNBT } from "./snbt-format";

export type ContainerFormat = "components" | "tag";

export interface ContainerSnapshot {
  format: ContainerFormat
  items: ItemStack[]
  size: number
}

const MAX_CONTAINER_SIZE = 256;
const UNKNOWN_CONTAINER_MIN_SIZE = 9;

const VANILLA_CONTAINER_SIZES: Record<string, number> = {
  "minecraft:chest": 27,
  "minecraft:trapped_chest": 27,
  "minecraft:barrel": 27,
  "minecraft:ender_chest": 27,
  "minecraft:dispenser": 9,
  "minecraft:dropper": 9,
  "minecraft:crafter": 9,
  "minecraft:hopper": 5,
  "minecraft:brewing_stand": 5,
  "minecraft:furnace": 3,
  "minecraft:blast_furnace": 3,
  "minecraft:smoker": 3,
  "minecraft:campfire": 4,
  "minecraft:soul_campfire": 4,
  "minecraft:chiseled_bookshelf": 6,
  "minecraft:decorated_pot": 1
};

function quoteString(value: string): string {
  return `'${value
    .replace(/\\/g, "\\\\")
    .replace(/'/g, "\\'")
    .replace(/\n/g, "\\n")
    .replace(/\r/g, "\\r")
    .replace(/\t/g, "\\t")}'`;
}

function stringifyKey(key: string): string {
  return /^[A-Za-z0-9._+-]+$/.test(key) ? key : quoteString(key);
}

export function stringifyNBT(value: NbtValue): string {
  if(value instanceof NbtObject) {
    return `{${Object.entries(value.children)
      .map(([key, child]) => `${stringifyKey(key)}:${stringifyNBT(child)}`)
      .join(",")}}`;
  }
  if(value instanceof NbtList) {
    return `[${value.children.map(stringifyNBT).join(",")}]`;
  }
  return value.text();
}

function getInteger(value: NbtValue | undefined): number | null {
  if(
    !(value instanceof NbtNumber)
    || !Number.isInteger(value.value)
  ) return null;
  return value.value;
}

function parseObject(value: string | undefined): NbtObject | null {
  if(!value) return null;
  try {
    const parsed = parseNbtString(value);
    return parsed instanceof NbtObject ? parsed : null;
  } catch {
    return null;
  }
}

function itemDataToSNBT(value: NbtValue | undefined): string | undefined {
  if(!(value instanceof NbtObject) || value.isEmpty()) return undefined;
  return stringifyNBT(value);
}

function getVanillaContainerSize(itemId: string): number | null {
  if(
    itemId === "minecraft:shulker_box"
    || (itemId.startsWith("minecraft:") && itemId.endsWith("_shulker_box"))
  ) {
    return 27;
  }
  return VANILLA_CONTAINER_SIZES[itemId] ?? null;
}

export function getContainerSize(itemId: string, highestSlot: number): number {
  const requiredSize = Math.min(MAX_CONTAINER_SIZE, highestSlot + 1);
  const vanillaSize = getVanillaContainerSize(itemId);
  if(vanillaSize !== null) return Math.max(vanillaSize, requiredSize);

  const minimumSize = Math.max(UNKNOWN_CONTAINER_MIN_SIZE, requiredSize);
  return Math.min(MAX_CONTAINER_SIZE, Math.ceil(minimumSize / 9) * 9);
}

function createEmptyItems(size: number): ItemStack[] {
  return Array.from({ length: size }, (_, slot) => ({
    slot,
    id: "minecraft:air",
    count: 0
  }));
}

function parseModernItems(container: NbtList): ItemStack[] | null {
  if(container.children.length > MAX_CONTAINER_SIZE) return null;

  const items: ItemStack[] = [];
  for(const entry of container.children) {
    if(!(entry instanceof NbtObject)) return null;

    const slot = getInteger(entry.get("slot"));
    const item = entry.get("item");
    if(slot === null || slot < 0 || slot >= MAX_CONTAINER_SIZE || !(item instanceof NbtObject)) {
      return null;
    }

    const id = item.get("id");
    const countTag = item.get("count");
    const count = countTag === undefined ? 1 : getInteger(countTag);
    const components = item.get("components");
    if(
      !(id instanceof NbtString)
      || count === null
      || count <= 0
      || (components !== undefined && !(components instanceof NbtObject))
    ) {
      return null;
    }

    items.push({
      slot,
      id: id.value,
      count,
      snbt: itemDataToSNBT(components)
    });
  }
  return items;
}

function parseLegacyItems(container: NbtList): ItemStack[] | null {
  if(container.children.length > MAX_CONTAINER_SIZE) return null;

  const items: ItemStack[] = [];
  for(const item of container.children) {
    if(!(item instanceof NbtObject)) return null;

    const slot = getInteger(item.get("Slot"));
    const id = item.get("id");
    const count = getInteger(item.get("Count"));
    const tag = item.get("tag");
    if(
      slot === null
      || slot < 0
      || slot >= MAX_CONTAINER_SIZE
      || !(id instanceof NbtString)
      || count === null
      || count <= 0
      || (tag !== undefined && !(tag instanceof NbtObject))
    ) {
      return null;
    }

    items.push({
      slot,
      id: id.value,
      count,
      snbt: itemDataToSNBT(tag)
    });
  }
  return items;
}

function normalizeItems(itemId: string, parsedItems: ItemStack[]): { items: ItemStack[], size: number } {
  const highestSlot = parsedItems.reduce((highest, item) => Math.max(highest, item.slot), -1);
  const size = getContainerSize(itemId, highestSlot);
  const items = createEmptyItems(size);
  for(const item of parsedItems) {
    if(item.slot < size) items[item.slot] = item;
  }
  return { items, size };
}

export function parseContainerNBT(snbt: string, itemId: string): ContainerSnapshot | null {
  const root = parseObject(snbt);
  if(!root) return null;

  const modernContainer = root.get("minecraft:container");
  if(modernContainer !== undefined) {
    if(!(modernContainer instanceof NbtList)) return null;
    const parsedItems = parseModernItems(modernContainer);
    if(!parsedItems) return null;
    return {
      format: "components",
      ...normalizeItems(itemId, parsedItems)
    };
  }

  const blockEntityTag = root.get("BlockEntityTag");
  if(blockEntityTag instanceof NbtObject) {
    const legacyContainer = blockEntityTag.get("Items");
    if(legacyContainer !== undefined) {
      if(!(legacyContainer instanceof NbtList)) return null;
      const parsedItems = parseLegacyItems(legacyContainer);
      if(!parsedItems) return null;
      return {
        format: "tag",
        ...normalizeItems(itemId, parsedItems)
      };
    }
  }

  return null;
}

function parseItemSNBT(snbt: string | undefined): NbtObject | null {
  if(!snbt) return null;
  const parsed = parseObject(snbt);
  if(!parsed) throw new Error("Invalid item SNBT");
  return parsed;
}

function createModernContainerItem(item: ItemStack): NbtObject {
  const itemTag = new NbtObject({
    id: new NbtString(item.id),
    count: new NbtNumber(item.count)
  });
  const components = parseItemSNBT(item.snbt);
  if(components && !components.isEmpty()) itemTag.addChild("components", components);

  return new NbtObject({
    slot: new NbtNumber(item.slot),
    item: itemTag
  });
}

function createLegacyContainerItem(item: ItemStack): NbtObject {
  const itemTag = new NbtObject({
    Slot: new NbtNumber(item.slot, "b"),
    id: new NbtString(item.id),
    Count: new NbtNumber(item.count, "b")
  });
  const tag = parseItemSNBT(item.snbt);
  if(tag && !tag.isEmpty()) itemTag.addChild("tag", tag);
  return itemTag;
}

export function serializeContainerNBT(
  snbt: string,
  format: ContainerFormat,
  items: ItemStack[]
): string {
  const root = parseObject(snbt);
  if(!root) throw new Error("Invalid root SNBT");

  const containerItems = items
    .filter((item) => (
      item.id !== "minecraft:air"
      && item.count > 0
      && item.slot >= 0
      && item.slot < MAX_CONTAINER_SIZE
    ))
    .sort((a, b) => a.slot - b.slot);

  if(format === "components") {
    if(!(root.get("minecraft:container") instanceof NbtList)) {
      throw new Error("Missing modern container component");
    }
    root.set(
      "minecraft:container",
      new NbtList(containerItems.map(createModernContainerItem))
    );
  } else {
    const blockEntityTag = root.get("BlockEntityTag");
    if(!(blockEntityTag instanceof NbtObject) || !(blockEntityTag.get("Items") instanceof NbtList)) {
      throw new Error("Missing legacy container tag");
    }
    blockEntityTag.set(
      "Items",
      new NbtList(containerItems.map(createLegacyContainerItem))
    );
  }

  return prettyFormatNBT(stringifyNBT(root));
}
