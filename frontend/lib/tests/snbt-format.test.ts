import { describe, expect, it } from "vitest";
import { minifyNBT, prettyFormatNBT } from "../nbt/snbt-format";

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

describe("minifyNBT", () => {
  it("should remove formatting whitespace outside strings", () => {
    const snbt = `{
  text: "hello",
  value: 42
}`;
    expect(minifyNBT(snbt)).toBe(`{text:"hello",value:42}`);
  });

  it("should preserve whitespace inside double-quoted strings", () => {
    expect(minifyNBT(`{text: "hello world"}`)).toBe(`{text:"hello world"}`);
  });

  it("should preserve whitespace inside single-quoted strings", () => {
    expect(minifyNBT(`{text: 'hello world'}`)).toBe(`{text:'hello world'}`);
  });

  it("should preserve whitespace after escaped quote inside string", () => {
    expect(minifyNBT(String.raw`{text: "say \"hi there\""}`)).toBe(String.raw`{text:"say \"hi there\""}`);
  });

  it("should handle strings with no whitespace correctly", () => {
    expect(minifyNBT(`{a:1,b:2}`)).toBe(`{a:1,b:2}`);
  });
});
