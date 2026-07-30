import { describe, expect, it } from "vitest";
import {
  getContainerSize,
  parseContainerNBT,
  serializeContainerNBT
} from "../nbt/container";

describe("container NBT", () => {
  it("parses modern container components with sparse slots and optional fields", () => {
    const parsed = parseContainerNBT(
      String.raw`{"minecraft:container":[{slot:7,item:{id:"minecraft:diamond_pickaxe",components:{"minecraft:unbreakable":{}}}},{slot:20,item:{id:"mod:machine",count:3}}]}`,
      "minecraft:shulker_box"
    );

    expect(parsed).not.toBeNull();
    if(!parsed) return;
    expect(parsed.format).toBe("components");
    expect(parsed.size).toBe(27);
    expect(parsed.items[7]).toEqual({
      slot: 7,
      id: "minecraft:diamond_pickaxe",
      count: 1,
      snbt: "{'minecraft:unbreakable':{}}"
    });
    expect(parsed.items[20]).toEqual({
      slot: 20,
      id: "mod:machine",
      count: 3,
      snbt: undefined
    });
    expect(parsed.items[0]).toEqual({
      slot: 0,
      id: "minecraft:air",
      count: 0
    });
  });

  it("parses legacy BlockEntityTag.Items", () => {
    const parsed = parseContainerNBT(
      String.raw`{BlockEntityTag:{Items:[{Slot:2b,id:"minecraft:stone",Count:12b,tag:{Damage:1}}]},display:{Name:"Box"}}`,
      "minecraft:shulker_box"
    );

    expect(parsed).not.toBeNull();
    if(!parsed) return;
    expect(parsed.format).toBe("tag");
    expect(parsed.size).toBe(27);
    expect(parsed.items[2]).toEqual({
      slot: 2,
      id: "minecraft:stone",
      count: 12,
      snbt: "{Damage:1}"
    });
  });

  it("uses the last item when slots are duplicated", () => {
    const parsed = parseContainerNBT(
      String.raw`{"minecraft:container":[{slot:0,item:{id:"minecraft:stone"}},{slot:0,item:{id:"minecraft:diamond",count:2}}]}`,
      "mod:container"
    );

    expect(parsed).not.toBeNull();
    if(!parsed) return;
    expect(parsed.items[0].id).toBe("minecraft:diamond");
    expect(parsed.items[0].count).toBe(2);
  });

  it.each([
    ["minecraft:chest", 27],
    ["minecraft:trapped_chest", 27],
    ["minecraft:barrel", 27],
    ["minecraft:ender_chest", 27],
    ["minecraft:blue_shulker_box", 27],
    ["minecraft:dispenser", 9],
    ["minecraft:dropper", 9],
    ["minecraft:crafter", 9],
    ["minecraft:hopper", 5],
    ["minecraft:brewing_stand", 5],
    ["minecraft:furnace", 3],
    ["minecraft:blast_furnace", 3],
    ["minecraft:smoker", 3],
    ["minecraft:campfire", 4],
    ["minecraft:soul_campfire", 4],
    ["minecraft:chiseled_bookshelf", 6],
    ["minecraft:decorated_pot", 1]
  ])("uses the vanilla capacity for %s", (itemId, expectedSize) => {
    expect(getContainerSize(itemId, -1)).toBe(expectedSize);
  });

  it("rounds unknown containers to rows and expands known containers for existing data", () => {
    expect(getContainerSize("mod:container", -1)).toBe(9);
    expect(getContainerSize("mod:container", 10)).toBe(18);
    expect(getContainerSize("minecraft:hopper", 20)).toBe(21);
    expect(getContainerSize("mod:container", 255)).toBe(256);
  });

  it.each([
    String.raw`{"minecraft:container":{}}`,
    String.raw`{"minecraft:container":[{slot:-1,item:{id:"minecraft:stone"}}]}`,
    String.raw`{"minecraft:container":[{slot:256,item:{id:"minecraft:stone"}}]}`,
    String.raw`{"minecraft:container":[{slot:0,item:{count:1}}]}`,
    String.raw`{"minecraft:container":[{slot:0,item:{id:"minecraft:stone",components:1}}]}`,
    String.raw`{BlockEntityTag:{Items:[{Slot:0b,id:"minecraft:stone",Count:0b}]}}`,
    "{"
  ])("rejects malformed container SNBT: %s", (snbt) => {
    expect(parseContainerNBT(snbt, "minecraft:chest")).toBeNull();
  });

  it("rejects modern container lists longer than 256 entries", () => {
    const entries = Array.from(
      { length: 257 },
      () => String.raw`{slot:0,item:{id:"minecraft:stone"}}`
    ).join(",");
    expect(parseContainerNBT(
      `{"minecraft:container":[${entries}]}`,
      "mod:container"
    )).toBeNull();
  });

  it("treats missing container data as invalid", () => {
    expect(parseContainerNBT("{foo:1b}", "minecraft:chest")).toBeNull();
    expect(parseContainerNBT(
      "{BlockEntityTag:{CustomName:'Box'}}",
      "minecraft:chest"
    )).toBeNull();
  });

  it("serializes modern contents while preserving other root data", () => {
    const snbt = String.raw`{foo:1b,"minecraft:container":[{slot:0,item:{id:"minecraft:stone"}}]}`;
    const parsed = parseContainerNBT(snbt, "minecraft:chest");
    expect(parsed).not.toBeNull();
    if(!parsed) return;

    const items = [...parsed.items];
    items[0] = { slot: 0, id: "minecraft:air", count: 0 };
    items[8] = {
      slot: 8,
      id: "minecraft:diamond",
      count: 2,
      snbt: String.raw`{"minecraft:custom_name":"Gem"}`
    };
    const serialized = serializeContainerNBT(snbt, "components", items);
    const reparsed = parseContainerNBT(serialized, "minecraft:chest");

    expect(serialized).toContain("foo: 1b");
    expect(serialized).toContain("'minecraft:container'");
    expect(serialized).toContain("'minecraft:custom_name'");
    expect(reparsed).not.toBeNull();
    if(!reparsed) return;
    expect(reparsed.items[0].id).toBe("minecraft:air");
    expect(reparsed.items[8]).toEqual({
      slot: 8,
      id: "minecraft:diamond",
      count: 2,
      snbt: "{'minecraft:custom_name':'Gem'}"
    });
  });

  it("serializes legacy contents with byte slot and count fields", () => {
    const snbt = String.raw`{display:{Name:"Box"},BlockEntityTag:{Items:[]}}`;
    const items = [
      {
        slot: 4,
        id: "minecraft:stone",
        count: 7,
        snbt: "{Damage:2}"
      }
    ];
    const serialized = serializeContainerNBT(snbt, "tag", items);
    const reparsed = parseContainerNBT(serialized, "minecraft:shulker_box");

    expect(serialized).toContain("Slot: 4b");
    expect(serialized).toContain("Count: 7b");
    expect(serialized).toContain("display:");
    expect(reparsed).not.toBeNull();
    if(!reparsed) return;
    expect(reparsed.items[4]).toEqual({
      slot: 4,
      id: "minecraft:stone",
      count: 7,
      snbt: "{Damage:2}"
    });
  });
});
