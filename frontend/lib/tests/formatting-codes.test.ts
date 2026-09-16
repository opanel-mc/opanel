import { describe, expect, it } from "vitest";
import AnsiConverter from "ansi-to-html";
import { parseTextToANSI, parseTextToHTML } from "../formatting-codes/text";

const colorCases = [
  ["0", "30"],
  ["1", "34"],
  ["2", "32"],
  ["3", "36"],
  ["4", "31"],
  ["5", "35"],
  ["6", "33"],
  ["7", "37"],
  ["8", "90"],
  ["9", "94"],
  ["a", "92"],
  ["b", "96"],
  ["c", "91"],
  ["d", "95"],
  ["e", "93"],
  ["f", "97"]
] as const;

const rgbCases = [
  ["§x§f§f§0§0§0§0", "255;0;0", "rgb(255, 0, 0)"],
  ["§x§A§b§C§d§E§f", "171;205;239", "rgb(171, 205, 239)"],
  ["§x§a§b§c§d§e§f", "171;205;239", "rgb(171, 205, 239)"],
  ["§x§A§B§C§D§E§F", "171;205;239", "rgb(171, 205, 239)"],
  ["§x§1§2§3§4§5§6", "18;52;86", "rgb(18, 52, 86)"],
  ["§x§7§8§9§0§1§2", "120;144;18", "rgb(120, 144, 18)"],
  ["§x§0§0§0§0§0§0", "0;0;0", "rgb(0, 0, 0)"],
  ["§x§f§f§f§f§f§f", "255;255;255", "rgb(255, 255, 255)"]
] as const;

const plainTextCases = ["", "Hello, world!", "你好，世界！", "Minecraft 🎮", "  spaced text  "];

describe("parseTextToHTML", () => {
  it.each(plainTextCases)("returns an unformatted root span for %j", (text) => {
    const root = parseTextToHTML(text);

    expect(root).toBeInstanceOf(HTMLSpanElement);
    expect(root.className).toBe("cc-root");
    expect(root.textContent).toBe(text);
    expect(root.children).toHaveLength(0);
  });

  it("purifies encoding artifacts before parsing codes", () => {
    const root = parseTextToHTML("\u00c2§aGreen\u00c2§rPlain");

    expect(root.textContent).toBe("GreenPlain");
    expect(root.querySelector(".cc-a")?.textContent).toBe("Green");
    expect(root.lastChild?.textContent).toBe("Plain");
  });

  it.each(colorCases)("applies ordinary color code §%s", (code) => {
    const root = parseTextToHTML(`Before§${code}Text`);

    expect(root.textContent).toBe("BeforeText");
    expect(root.children).toHaveLength(1);
    expect(root.firstElementChild?.className).toBe(`cc-${code}`);
    expect(root.firstElementChild?.textContent).toBe("Text");
    expect(root.firstChild?.textContent).toBe("Before");
  });

  it.each(["k", "l", "m", "n", "o"])("applies formatting code §%s", (code) => {
    const root = parseTextToHTML(`§${code}Text`);

    expect(root.textContent).toBe("Text");
    expect(root.children).toHaveLength(1);
    expect(root.firstElementChild?.className).toBe(`cc-${code}`);
  });

  it("nests cumulative formatting inside the active color and returns to the root on reset", () => {
    const root = parseTextToHTML("Before§aGreen§lBold§nUnderlined§oItalic§mStruck§rPlain");

    expect(root.textContent).toBe("BeforeGreenBoldUnderlinedItalicStruckPlain");
    expect(root.querySelector(".cc-a")?.textContent).toBe("GreenBoldUnderlinedItalicStruck");
    expect(root.querySelector(".cc-a > .cc-l > .cc-n > .cc-o > .cc-m")?.textContent).toBe("Struck");
    expect(root.lastChild?.nodeType).toBe(Node.TEXT_NODE);
    expect(root.lastChild?.textContent).toBe("Plain");
  });

  it("starts a sibling span and clears formatting when the color changes", () => {
    const root = parseTextToHTML("§a§lGreenBold§cRed");

    expect(root.children).toHaveLength(2);
    expect(root.querySelector(".cc-a > .cc-l")?.textContent).toBe("GreenBold");
    expect(root.lastElementChild?.className).toBe("cc-c");
    expect(root.lastElementChild?.textContent).toBe("Red");
    expect(root.querySelector(".cc-l .cc-c")).toBeNull();
  });

  it.each(["§rPlain", "§r§rPlain"])("handles reset codes without active styling in %s", (text) => {
    const root = parseTextToHTML(text);

    expect(root.textContent).toBe("Plain");
    expect(root.children).toHaveLength(0);
  });

  it.each([
    ["§zText", "zText"],
    ["§AText", "AText"],
    ["§XText", "XText"],
    ["Text§", "Text"],
    ["§", ""]
  ])("drops unsupported or trailing section signs in %s", (text, expected) => {
    const root = parseTextToHTML(text);

    expect(root.textContent).toBe(expected);
    expect(root.children).toHaveLength(0);
  });

  it.each(rgbCases)("applies legacy RGB sequence %s", (sequence, _ansi, color) => {
    const root = parseTextToHTML(`Before${sequence}Text`);

    expect(root.textContent).toBe("BeforeText");
    expect(root.children).toHaveLength(1);
    expect((root.firstElementChild as HTMLSpanElement).style.color).toBe(color);
    expect(root.firstElementChild?.textContent).toBe("Text");
  });

  it("clears previous formatting on RGB colors and supports subsequent formatting and reset", () => {
    const root = parseTextToHTML("§a§lBefore§x§f§f§0§0§0§0Red§nUnderlined§rPlain");
    const rgbSpan = root.children[1] as HTMLSpanElement;

    expect(root.children).toHaveLength(2);
    expect(root.querySelector(".cc-a > .cc-l")?.textContent).toBe("Before");
    expect(rgbSpan.style.color).toBe("rgb(255, 0, 0)");
    expect(rgbSpan.textContent).toBe("RedUnderlined");
    expect(rgbSpan.querySelector(".cc-n")?.textContent).toBe("Underlined");
    expect(root.lastChild?.nodeType).toBe(Node.TEXT_NODE);
    expect(root.lastChild?.textContent).toBe("Plain");
  });

  it("handles consecutive RGB colors followed by an ordinary color", () => {
    const root = parseTextToHTML("§x§f§f§0§0§0§0Red§x§0§0§f§f§0§0Green§9Blue");

    expect(root.children).toHaveLength(3);
    expect((root.children[0] as HTMLSpanElement).style.color).toBe("rgb(255, 0, 0)");
    expect(root.children[0].textContent).toBe("Red");
    expect((root.children[1] as HTMLSpanElement).style.color).toBe("rgb(0, 255, 0)");
    expect(root.children[1].textContent).toBe("Green");
    expect(root.children[2].className).toBe("cc-9");
    expect(root.children[2].textContent).toBe("Blue");
  });

  it.each([
    ["§xText", "xText"],
    ["§x§Text", "xText"],
    ["§x§f§f§0§0§0Text", "xText"],
    ["§x§f§f§0§0§0§gText", "xgText"],
    ["§x§f§f§0§0§00Text", "x0Text"]
  ])("falls back to ordinary codes for malformed RGB sequence %s", (text, expected) => {
    const root = parseTextToHTML(text);

    expect(root.textContent).toBe(expected);
    expect(root.querySelector("[style]")).toBeNull();
  });

  it.each(["§a", "§l", "§r", "§x§f§f§0§0§0§0"])("does not display codes when %s has no following text", (text) => {
    expect(parseTextToHTML(text).textContent).toBe("");
  });

  it("preserves literal newlines as text with the default line limit", () => {
    const root = parseTextToHTML("First\nSecond");

    expect(root.textContent).toBe("First\nSecond");
    expect(root.querySelector("br")).toBeNull();
  });

  it("creates breaks for literal newlines until the line limit is reached", () => {
    expect(parseTextToHTML("First\nSecond\nThird", 2).innerHTML).toBe("First<br>Second\nThird");
  });

  it("preserves empty lines through leading, consecutive and trailing breaks", () => {
    expect(parseTextToHTML("\nFirst\n\n", 4).innerHTML).toBe("<br>First<br><br>");
  });

  it("converts escaped newlines to breaks", () => {
    expect(parseTextToHTML("First\\nSecond\\nThird", 3).innerHTML).toBe("First<br>Second<br>Third");
  });

  it("omits backslashes that do not form escaped newlines", () => {
    expect(parseTextToHTML("A\\B\\tC\\").textContent).toBe("ABtC");
  });

  it("keeps the active style across line breaks", () => {
    const root = parseTextToHTML("§aFirst\nSecond", 2);

    expect(root.querySelector(".cc-a")?.innerHTML).toBe("First<br>Second");
    expect(root.children).toHaveLength(1);
  });

  it.each([
    [0, ""],
    [1, "a"],
    [3, "abc"],
    [4, "abcd"],
    [8, "abcd"],
    [Infinity, "abcd"]
  ])("limits visible text to %s characters per line", (limit, expected) => {
    expect(parseTextToHTML("abcd", 1, limit).textContent).toBe(expected);
  });

  it("counts visible characters across formatting spans without counting codes", () => {
    const root = parseTextToHTML("§aAB§lCD§rEF", 1, 3);

    expect(root.textContent).toBe("ABC");
    expect(root.querySelector(".cc-l")?.textContent).toBe("C");
  });

  it("does not count legacy RGB sequences toward the character limit", () => {
    const root = parseTextToHTML("§x§f§f§0§0§0§0ABCD", 1, 2);

    expect(root.textContent).toBe("AB");
    expect((root.firstElementChild as HTMLSpanElement).style.color).toBe("rgb(255, 0, 0)");
  });

  it.each(["abcd\nefgh", "abcd\\nefgh"])("resets the character count on line breaks in %j", (text) => {
    expect(parseTextToHTML(text, 2, 3).innerHTML).toBe("abc<br>efg");
  });

  it("does not share DOM nodes or styles between calls", () => {
    const styled = parseTextToHTML("§aStyled");
    const plain = parseTextToHTML("Plain");

    expect(plain).not.toBe(styled);
    expect(plain.innerHTML).toBe("Plain");
    expect(styled.querySelector(".cc-a")?.textContent).toBe("Styled");
  });
});

describe("parseTextToANSI", () => {
  it.each(plainTextCases)("preserves unformatted text %j without adding resets", (text) => {
    expect(parseTextToANSI(text)).toBe(text);
  });

  it("purifies encoding artifacts before parsing codes", () => {
    expect(parseTextToANSI("\u00c2§aGreen\u00c2§rPlain"))
      .toBe("\x1b[0m\x1b[92mGreen\x1b[0mPlain");
  });

  it.each(colorCases)("converts ordinary color code §%s to ANSI %s", (code, ansi) => {
    expect(parseTextToANSI(`Before§${code}Text`))
      .toBe(`Before\x1b[0m\x1b[${ansi}mText\x1b[0m`);
  });

  it.each([
    ["l", "1"],
    ["m", "9"],
    ["n", "4"],
    ["o", "3"]
  ])("converts formatting code §%s to ANSI %s", (code, ansi) => {
    expect(parseTextToANSI(`Before§${code}Text`))
      .toBe(`Before\x1b[${ansi}mText\x1b[0m`);
  });

  it("combines formatting codes without resetting between them", () => {
    expect(parseTextToANSI("§l§m§n§oText"))
      .toBe("\x1b[1m\x1b[9m\x1b[4m\x1b[3mText\x1b[0m");
  });

  it("clears active formatting when an ordinary color changes", () => {
    expect(parseTextToANSI("§aGreen§lBold§cRed"))
      .toBe("\x1b[0m\x1b[92mGreen\x1b[1mBold\x1b[0m\x1b[91mRed\x1b[0m");
  });

  it.each([
    ["§rPlain", "\x1b[0mPlain"],
    ["§r§rPlain", "\x1b[0m\x1b[0mPlain"],
    ["§a§lStyled§rPlain", "\x1b[0m\x1b[92m\x1b[1mStyled\x1b[0mPlain"],
    ["§lText§r", "\x1b[1mText\x1b[0m"]
  ])("handles explicit reset codes in %s without appending a redundant reset", (text, expected) => {
    expect(parseTextToANSI(text)).toBe(expected);
  });

  it.each([
    ["§a", "\x1b[0m\x1b[92m\x1b[0m"],
    ["§l", "\x1b[1m\x1b[0m"],
    ["§r", "\x1b[0m"],
    ["§x§f§f§0§0§0§0", "\x1b[0m\x1b[38;2;255;0;0m\x1b[0m"]
  ])("handles code-only input %s", (text, expected) => {
    expect(parseTextToANSI(text)).toBe(expected);
  });

  it("ignores §k without adding ANSI styling or a reset", () => {
    expect(parseTextToANSI("Before§kText")).toBe("BeforeText");
    expect(parseTextToANSI("§a§kText")).toBe("\x1b[0m\x1b[92mText\x1b[0m");
  });

  it.each(["§zText", "§AText", "§XText", "Text§", "§"])("preserves unsupported or trailing codes in %s", (text) => {
    expect(parseTextToANSI(text)).toBe(text);
  });

  it.each(["First\nSecond", "First\\nSecond", "A\\B\\tC\\", "\x1b[31mText\x1b[0m"])("preserves newlines, backslashes and existing ANSI in %j", (text) => {
    expect(parseTextToANSI(text)).toBe(text);
  });

  it("does not carry active codes into subsequent calls", () => {
    expect(parseTextToANSI("§aStyled")).toBe("\x1b[0m\x1b[92mStyled\x1b[0m");
    expect(parseTextToANSI("Plain")).toBe("Plain");
  });

  it.each(rgbCases)("converts legacy RGB sequence %s to truecolor ANSI", (sequence, rgb) => {
    expect(parseTextToANSI(`Before${sequence}Text`))
      .toBe(`Before\x1b[0m\x1b[38;2;${rgb}mText\x1b[0m`);
  });

  it("resets previous formatting on RGB colors and handles following formatting and reset codes", () => {
    expect(parseTextToANSI("§lBold§x§f§f§0§0§0§0Red§nUnderlined§rPlain"))
      .toBe("\x1b[1mBold\x1b[0m\x1b[38;2;255;0;0mRed\x1b[4mUnderlined\x1b[0mPlain");
  });

  it("handles consecutive RGB colors and subsequent ordinary color codes", () => {
    expect(parseTextToANSI("§x§f§f§0§0§0§0Red§x§0§0§f§f§0§0Green§9Blue"))
      .toBe("\x1b[0m\x1b[38;2;255;0;0mRed\x1b[0m\x1b[38;2;0;255;0mGreen\x1b[0m\x1b[94mBlue\x1b[0m");
  });

  it.each([
    "§xText",
    "§x§Text",
    "§x§f§f§0§0§0Text",
    "§x§f§f§0§0§0§gText",
    "§x§f§f§0§0§00Text"
  ])("does not consume malformed RGB sequence %s as truecolor", (text) => {
    const result = parseTextToANSI(text);

    expect(result).not.toContain("\x1b[38;2;");
    expect(result).toContain("§x");
    expect(result).toContain("Text");
  });

  it("purifies encoding artifacts within legacy RGB sequences", () => {
    expect(parseTextToANSI("\u00c2§x\u00c2§f\u00c2§f§0§0§0§0Text"))
      .toBe("\x1b[0m\x1b[38;2;255;0;0mText\x1b[0m");
  });

  it("renders legacy RGB as red through the terminal ANSI-to-HTML path", () => {
    const root = document.createElement("span");
    root.innerHTML = new AnsiConverter().toHtml(parseTextToANSI("§x§f§f§0§0§0§0Text"));

    expect(root.textContent).toBe("Text");
    expect(root.querySelector("span")?.style.color).toBe("rgb(255, 0, 0)");
  });
});
