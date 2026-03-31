import { describe, expect, it } from "vitest";
import { prettyFormatNBT } from "../nbt/snbt-format";

describe("prettyFormatNBT", () => {
  it("should keep string state correct after escaped quotes", () => {
    const snbt = String.raw`{text:"\"","minecraft:enchantments":{"minecraft:sharpness":20}}`;
    expect(prettyFormatNBT(snbt)).toBe(`{
  text: "\\\"",
  "minecraft:enchantments": {
    "minecraft:sharpness": 20
  }
}
`);
  });

  it("should not add spaces to colons inside quoted keys", () => {
    const snbt = "{'minecraft:lore':['line1','line2']}";
    expect(prettyFormatNBT(snbt)).toBe(`{
  'minecraft:lore': [
    'line1',
    'line2'
  ]
}
`);
  });
});
