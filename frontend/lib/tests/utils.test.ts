import { describe, expect, it } from "vitest";
import { NbtBool, NbtList, NbtObject, NbtString } from "../snbt";
import { base64ToString, formatDataSize, gameModeToString, getCurrentArgumentIndex, getInputtedArgumentStr, isNumeric, stringToBase64, textComponentToString } from "../utils";
import { GameMode } from "../types";

describe("test gameModeToString", () => {
  it("should return the correct string for each game mode", () => {
    expect(gameModeToString(GameMode.SURVIVAL)).toBe("[common.gamemode.survival]");
    expect(gameModeToString(GameMode.CREATIVE)).toBe("[common.gamemode.creative]");
    expect(gameModeToString(GameMode.ADVENTURE)).toBe("[common.gamemode.adventure]");
    expect(gameModeToString(GameMode.SPECTATOR)).toBe("[common.gamemode.spectator]");
  });
});

describe("test getInputtedArgumentStr", () => {
  it("should return the inputted argument string based on cursor position", () => {
    expect(getInputtedArgumentStr("do hello world", 5)).toBe("he");
    expect(getInputtedArgumentStr("do hello world", 13)).toBe("worl");
  });

  it("should return empty string when the cursor is at 0", () => {
    expect(getInputtedArgumentStr("do hello world", 0)).toBe("");
  });

  it("should throw error if the given cursor position is out of the string length", () => {
    expect(() => getInputtedArgumentStr("do hello world", -1)).toThrowError("Cursor position is out of the length of the string.");
    expect(() => getInputtedArgumentStr("do hello world", 15)).toThrowError("Cursor position is out of the length of the string.");
  });
});

describe("test getCurrentArgumentIndex", () => {
  it("should return the current argument index based on cursor position", () => {
    expect(getCurrentArgumentIndex("do hello world", 5)).toBe(2);
    expect(getCurrentArgumentIndex("do hello world", 13)).toBe(3);
  });

  it("should return 1 when the cursor is at 0", () => {
    expect(getCurrentArgumentIndex("do hello world", 0)).toBe(1);
  });

  it("should throw error if the given cursor position is out of the string length", () => {
    expect(() => getCurrentArgumentIndex("do hello world", -1)).toThrowError("Cursor position is out of the length of the string.");
    expect(() => getCurrentArgumentIndex("do hello world", 15)).toThrowError("Cursor position is out of the length of the string.");
  });
});

describe("test formatDataSize", () => {
  it("should format data size correctly", () => {
    expect(formatDataSize(1024)).toBe("1.00 KB");
    expect(formatDataSize(1048576)).toBe("1.00 MB");
    expect(formatDataSize(1073741824)).toBe("1.00 GB");
    expect(formatDataSize(1099511627776)).toBe("1.00 TB");
    expect(formatDataSize(1125899906842624)).toBe("1.00 PB");
  });

  it("should format data size less than 1 KB correctly", () => {
    expect(formatDataSize(256)).toBe("0.25 KB");
    expect(formatDataSize(512)).toBe("0.50 KB");
  });

  it("should round data size to 2 decimal places", () => {
    expect(formatDataSize(1536)).toBe("1.50 KB");
    expect(formatDataSize(1572864)).toBe("1.50 MB");
  });
});

describe("test stringToBase64 and base64ToString", () => {
  it("should convert string to base64 and back correctly", () => {
    const str = "Hello, World!";
    const base64 = stringToBase64(str);
    expect(base64ToString(base64)).toBe(str);
  });

  it("should handle empty string correctly", () => {
    const str = "";
    const base64 = stringToBase64(str);
    expect(base64ToString(base64)).toBe(str);
  });

  it("should handle unicode characters correctly", () => {
    const str = "你好，世界！";
    const base64 = stringToBase64(str);
    expect(base64ToString(base64)).toBe(str);
  });
});

describe("test isNumeric", () => {
  it("should return true for numeric strings", () => {
    expect(isNumeric("123")).toBe(true);
    expect(isNumeric("-123")).toBe(true);
    expect(isNumeric("3.14")).toBe(true);
    expect(isNumeric("-3.14")).toBe(true);
  });

  it("should return true for Infinity", () => {
    expect(isNumeric("Infinity")).toBe(true);
    expect(isNumeric("-Infinity")).toBe(true);
  });

  it("should return false for non-numeric strings", () => {
    expect(isNumeric("abc")).toBe(false);
    expect(isNumeric("123abc")).toBe(false);
    expect(isNumeric(" ")).toBe(false);
  });

  it("should return false for empty string and NaN", () => {
    expect(isNumeric("")).toBe(false);
    expect(isNumeric("NaN")).toBe(false);
  });
});

describe("test textComponentToString", () => {
  it("should directly return the string value if the inputted nbt is NbtString", () => {
    const str = "Hello, World!";
    expect(textComponentToString(new NbtString(str))).toBe(str);
  });

  it("should only return the text value of the inputted nbt if the component is NbtObject (text component)", () => {
    const str = "Hello, World!";
    const nbt = new NbtObject({
      text: new NbtString(str),
      extra: new NbtList([
        new NbtObject({
          text: new NbtString(" This is extra text."),
        }),
      ]),
      bold: new NbtBool(true),
    });
    expect(textComponentToString(nbt)).toBe(str);
  });

  it("should return null if the inputted nbt is not a text component", () => {
    const nbt1 = new NbtObject({
      extra: new NbtList([
        new NbtObject({
          text: new NbtString(" This is extra text."),
        }),
      ]),
      bold: new NbtBool(true),
    });
    expect(textComponentToString(nbt1)).toBeNull();

    const nbt2 = new NbtList([]);
    expect(textComponentToString(nbt2 as any)).toBeNull();
  });
});
