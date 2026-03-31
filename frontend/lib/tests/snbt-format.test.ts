import { describe, expect, it } from "vitest";
import { prettyFormatNBT } from "../nbt/snbt-format";

describe("test prettyFormatNBT", () => {
  it("should keep string state correct after escaped quotes", () => {
    const snbt = "{text:\"\\\"\",\"minecraft: enchantments\":{\"minecraft: sharpness\":20}}";
    expect(prettyFormatNBT(snbt)).toBe("{\n  text: \"\\\"\",\n  \"minecraft: enchantments\": {\n    \"minecraft: sharpness\": 20\n  }\n}\n");
  });

  it("should not add spaces to colons inside quoted keys", () => {
    const snbt = "{'minecraft: lore':['line1','line2']}";
    expect(prettyFormatNBT(snbt)).toBe("{\n  'minecraft: lore': [\n    'line1',\n    'line2'\n  ]\n}\n");
  });
});
