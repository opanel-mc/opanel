import { describe, expect, it } from "vitest";
import Filter from "../ansi-to-html";

describe("OSC 8 hyperlinks", () => {
  function parseHtml(html: string): HTMLDivElement {
    const element = document.createElement("div");
    element.innerHTML = html;
    return element;
  }

  it.each([
    ["ST", "\x1b\\", "\x1b\\", ""],
    ["BEL", "\x07", "\x07", ""],
    ["mixed terminators", "\x07", "\x1b\\", ""],
    ["parameters", "\x1b\\", "\x07", "id=docs:foo=bar"]
  ])("renders hyperlinks with %s", (_name, start, end, params) => {
    const filter = new Filter();
    const html = filter.toHtml(`before \x1b]8;${params};https://example.com${start}打开文档 😀\x1b]8;;${end} after`);

    expect(html).toBe("before <a href=\"https://example.com\" target=\"_blank\" rel=\"noopener noreferrer\">打开文档 😀</a> after");
  });

  it("preserves URL queries, semicolons and non-ASCII characters", () => {
    const url = "https://example.com/文档;a=1?x=1&y=2&amp;literal=3#介绍";
    const element = parseHtml(new Filter().toHtml(`\x1b]8;id=docs;${url}\x07文档\x1b]8;;\x07`));

    expect(element.querySelector("a")?.getAttribute("href")).toBe(url);
    expect(element.textContent).toBe("文档");
  });

  it("renders the Pumpkin startup links with styled labels", () => {
    const input = "\x1b]8;;https://github.com/Pumpkin-MC/Pumpkin\x1b\\\x1b[4m[Github Repository]\x1b[24m\x1b]8;;\x1b\\ "
      + "\x1b]8;;https://pumpkinmc.org/donate/\x1b\\\x1b[31m[Do\x1b[32mnate]\x1b[0m\x1b]8;;\x1b\\ "
      + "\x1b]8;;https://pumpkinmc.org/\x1b\\\x1b[34m[Website]\x1b[0m\x1b]8;;\x1b\\";
    const element = parseHtml(new Filter().toHtml(input));
    const links = [...element.querySelectorAll("a")];

    expect(links.map(link => [link.textContent, link.getAttribute("href")])).toEqual([
      ["[Github Repository]", "https://github.com/Pumpkin-MC/Pumpkin"],
      ["[Donate]", "https://pumpkinmc.org/donate/"],
      ["[Website]", "https://pumpkinmc.org/"]
    ]);
    expect(links[0].querySelector("u")?.textContent).toBe("[Github Repository]");
    expect(links[1].querySelectorAll("span")).toHaveLength(2);
    expect(element.textContent).toBe("[Github Repository] [Donate] [Website]");
    expect(element.querySelector("a a")).toBeNull();
  });

  it("preserves styles across hyperlink boundaries and SGR resets", () => {
    const html = new Filter().toHtml("\x1b[1mprefix \x1b]8;;https://example.com\x07bold\x1b[0m plain\x1b[3m italic\x1b]8;;\x07 suffix\x1b[23m end");

    expect(html).toBe("<b>prefix </b><a href=\"https://example.com\" target=\"_blank\" rel=\"noopener noreferrer\"><b>bold</b> plain<i> italic</i></a><i> suffix</i> end");
  });

  it("switches destinations without nesting links and ignores redundant closes", () => {
    const html = new Filter().toHtml("\x1b]8;;\x07\x1b]8;;https://one.example\x07one\x1b]8;;https://two.example\x07two\x1b]8;;\x07\x1b]8;;\x07 plain");
    const element = parseHtml(html);

    expect([...element.querySelectorAll("a")].map(link => [link.textContent, link.getAttribute("href")])).toEqual([
      ["one", "https://one.example"],
      ["two", "https://two.example"]
    ]);
    expect(element.textContent).toBe("onetwo plain");
    expect(element.querySelector("a a")).toBeNull();
  });

  it("does not leak unterminated links into later non-streaming conversions", () => {
    const filter = new Filter();

    expect(parseHtml(filter.toHtml("\x1b]8;;https://example.com\x07link")).querySelector("a")?.textContent).toBe("link");
    expect(filter.toHtml("plain")).toBe("plain");
  });

  it("preserves link state across streaming calls independently of styles", () => {
    const filter = new Filter({ stream: true });
    filter.toHtml("\x1b]8;;https://example.com\x07\x1b[1mbold");
    const middle = parseHtml(filter.toHtml("still bold\x1b[0m plain"));

    expect(middle.querySelector("a")?.textContent).toBe("still bold plain");
    expect(middle.querySelector("a b")?.textContent).toBe("still bold");
    const end = parseHtml(filter.toHtml("\x1b]8;;\x07outside"));
    expect(end.textContent).toBe("outside");
    expect(end.querySelector("a")?.textContent ?? "").toBe("");
    expect(filter.toHtml("plain")).toBe("plain");
  });

  it.each(["\x07", "\x1b\\"])("buffers OSC 8 sequences split at any position (%j)", (terminator) => {
    const input = `before \x1b]8;id=docs;https://example.com?a=1&b=2${terminator}文档\x1b]8;;${terminator} after`;

    for(let split = 0; split <= input.length; split++) {
      const filter = new Filter({ stream: true });
      const html = filter.toHtml(input.slice(0, split)) + filter.toHtml(input.slice(split));
      const element = parseHtml(html);
      const links = [...element.querySelectorAll("a")];

      expect(element.textContent).toBe("before 文档 after");
      expect(links.map(link => link.textContent).join("")).toBe("文档");
      expect(links.every(link => link.getAttribute("href") === "https://example.com?a=1&b=2")).toBe(true);
      expect(filter.toHtml("plain")).toBe("plain");
    }
  });

  it("accepts string arrays containing split hyperlink sequences", () => {
    const html = new Filter().toHtml(["\x1b]8;;https://example.com\x1b", "\\link\x1b]8;;\x1b", "\\"]);

    expect(parseHtml(html).querySelector("a")?.textContent).toBe("link");
  });

  it.each([
    "javascript:alert(1)", "JaVaScRiPt:alert(1)", "data:text/html,<script>alert(1)</script>",
    "file:///etc/passwd", "//example.com", "http://", "https:example.com", "https://example.com/\nonclick=alert(1)"
  ])("displays only the label for unsupported or invalid URLs (%j)", (url) => {
    const html = new Filter().toHtml(`\x1b]8;;${url}\x07label\x1b]8;;\x07`);

    expect(html).toBe("label");
  });

  it("closes the previous link when a disallowed destination follows it", () => {
    const element = parseHtml(new Filter().toHtml("\x1b]8;;https://example.com\x07safe\x1b]8;;javascript:alert(1)\x07plain\x1b]8;;\x07"));

    expect(element.querySelectorAll("a")).toHaveLength(1);
    expect(element.querySelector("a")?.textContent).toBe("safe");
    expect(element.textContent).toBe("safeplain");
  });

  it.each([false, true])("escapes link attributes regardless of escapeXML (%j)", (escapeXML) => {
    const url = "https://example.com/?q=\"><img/src=x/onerror=alert(1)>&b='value'";
    const element = parseHtml(new Filter({ escapeXML }).toHtml(`\x1b]8;;${url}\x07link\x1b]8;;\x07`));

    expect(element.querySelector("a")?.getAttribute("href")).toBe(url);
    expect(element.querySelector("img, [onerror]")).toBeNull();
    expect(element.textContent).toBe("link");
  });

  it("escapes linked text when escapeXML is enabled", () => {
    const element = parseHtml(new Filter({ escapeXML: true }).toHtml("\x1b]8;;https://example.com\x07<img src=x onerror=alert(1)> & \"text\"\x1b]8;;\x07"));

    expect(element.querySelector("img")).toBeNull();
    expect(element.querySelector("a")?.textContent).toBe("<img src=x onerror=alert(1)> & \"text\"");
  });

  it.each([false, true])("preserves multiline labels with newline=%j", (newline) => {
    const html = new Filter({ newline }).toHtml("\x1b]8;;https://example.com\x07first\nsecond\x1b]8;;\x07");

    expect(html).toBe(`<a href="https://example.com" target="_blank" rel="noopener noreferrer">first${newline ? "<br/>" : "\n"}second</a>`);
  });

  it("drops incomplete OSC 8 commands in non-streaming mode", () => {
    const filter = new Filter();

    expect(filter.toHtml("before\x1b]8;;https://example.com")).toBe("before");
    expect(filter.toHtml("plain")).toBe("plain");
  });

  it("consumes malformed OSC 8 commands without exposing control data", () => {
    expect(new Filter().toHtml("\x1b]8;missing-separator\x07label\x1b]8;;\x07")).toBe("label");
  });
});

function test(text: string | string[], result: string, opts?: ConstructorParameters<typeof Filter>[0]): void {
  if(!opts) {
    opts = {};
  }

  const f = new Filter(opts);

  function filtered(memo: string, t: string): string {
    return memo + f.toHtml(t);
  }

  const chunks = typeof text === "string" ? [text] : text;
  expect(chunks.reduce(filtered, "")).toBe(result);
}

describe("ansi to html", () => {
  describe("constructed with no options", () => {
    it("doesn't modify the input string", () => {
      const text = "some text";
      const result = "some text";

      return test(text, result);
    });

    it("returns plain text when given plain text with LF", () => {
      const text = "test\ntest\n";
      const result = "test\ntest\n";

      return test(text, result);
    });

    it("returns plain text when given plain text with multiple LF", () => {
      const text = "test\n\n\ntest\n";
      const result = "test\n\n\ntest\n";

      return test(text, result);
    });

    it("returns plain text when given plain text with CR", () => {
      const text = "testCRLF\rtest";
      const result = "testCRLF\rtest";

      return test(text, result);
    });

    it("returns plain text when given plain text with multiple CR", () => {
      const text = "testCRLF\r\r\rtest";
      const result = "testCRLF\r\r\rtest";

      return test(text, result);
    });

    it("returns plain text when given plain text with CR & LF", () => {
      const text = "testCRLF\r\ntest";
      const result = "testCRLF\r\ntest";

      return test(text, result);
    });

    it("returns plain text when given plain text with multiple CR & LF", () => {
      const text = "testCRLF\r\n\r\ntest";
      const result = "testCRLF\r\n\r\ntest";

      return test(text, result);
    });

    it("renders foreground colors", () => {
      const text = "colors: \x1b[30mblack\x1b[37mwhite";
      const result = "colors: <span style=\"color:#000\">black<span style=\"color:#AAA\">white</span></span>";

      return test(text, result);
    });

    it("renders light foreground colors", () => {
      const text = "colors: \x1b[90mblack\x1b[97mwhite";
      const result = "colors: <span style=\"color:#555\">black<span style=\"color:#FFF\">white</span></span>";

      return test(text, result);
    });

    it("renders background colors", () => {
      const text = "colors: \x1b[40mblack\x1b[47mwhite";
      const result = "colors: <span style=\"background-color:#000\">black<span style=\"background-color:#AAA\">white</span></span>";

      return test(text, result);
    });

    it("renders light background colors", () => {
      const text = "colors: \x1b[100mblack\x1b[107mwhite";
      const result = "colors: <span style=\"background-color:#555\">black<span style=\"background-color:#FFF\">white</span></span>";

      return test(text, result);
    });

    it("renders strikethrough", () => {
      const text = "strike: \x1b[9mthat";
      const result = "strike: <strike>that</strike>";

      return test(text, result);
    });

    it("renders blink", () => {
      const text = "blink: \x1b[5mwhat";
      const result = "blink: <blink>what</blink>";

      return test(text, result);
    });

    it("renders underline", () => {
      const text = "underline: \x1b[4mstuff";
      const result = "underline: <u>stuff</u>";

      return test(text, result);
    });

    it("renders bold", () => {
      const text = "bold: \x1b[1mstuff";
      const result = "bold: <b>stuff</b>";

      return test(text, result);
    });

    it("renders italic", () => {
      const text = "italic: \x1b[3mstuff";
      const result = "italic: <i>stuff</i>";

      return test(text, result);
    });

    it("handles resets", () => {
      const text = "\x1b[1mthis is bold\x1b[0m, but this isn't";
      const result = "<b>this is bold</b>, but this isn't";

      return test(text, result);
    });

    it("handles multiple resets", () => {
      const text = "normal, \x1b[1mbold, \x1b[4munderline, \x1b[31mred\x1b[0m, normal";
      const result = "normal, <b>bold, <u>underline, <span style=\"color:" + "#A00\">red</span></u></b>, normal";

      return test(text, result);
    });

    it("handles resets with implicit 0", () => {
      const text = "\x1b[1mthis is bold\x1b[m, but this isn't";
      const result = "<b>this is bold</b>, but this isn't";

      return test(text, result);
    });

    it("renders multi-attribute sequences", () => {
      const text = "normal, \x1b[1;4;31mbold, underline, and red\x1b[0m, normal";
      const result = "normal, <b><u><span style=\"color:#A00\">bold, underline," + " and red</span></u></b>, normal";

      return test(text, result);
    });

    it("renders multi-attribute sequences with a semi-colon", () => {
      const text = "normal, \x1b[1;4;31;mbold, underline, and red\x1b[0m, normal";
      const result = "normal, <b><u><span style=\"color:#A00\">bold, underline, and red</span></u></b>, normal";

      return test(text, result);
    });

    it("eats malformed sequences", () => {
      const text = "\x1b[25oops forgot the 'm'";
      const result = "oops forgot the 'm'";

      return test(text, result);
    });

    it("renders xterm 256 foreground sequences", () => {
      const text = "\x1b[38;5;196mhello";
      const result = "<span style=\"color:#ff0000\">hello</span>";

      return test(text, result);
    });

    it("renders xterm 256 background sequences", () => {
      const text = "\x1b[48;5;196mhello";
      const result = "<span style=\"background-color:#ff0000\">hello</span>";

      return test(text, result);
    });

    it("renders foreground rgb sequences", () => {
      const text = "\x1b[38;2;210;60;114mhello";
      const result = "<span style=\"color:#d23c72\">hello</span>";

      return test(text, result);
    });

    it("renders background rgb sequences", () => {
      const text = "\x1b[48;2;155;42;45mhello";
      const result = "<span style=\"background-color:#9b2a2d\">hello</span>";

      return test(text, result);
    });

    it("handles resetting to default foreground color", () => {
      const text = "\x1b[30mblack\x1b[39mdefault";
      const result = "<span style=\"color:#000\">black<span style=\"color:#FFF\">default</span></span>";

      return test(text, result);
    });

    it("handles resetting to default background color", () => {
      const text = "\x1b[100mblack\x1b[49mdefault";
      const result = "<span style=\"background-color:#555\">black<span style=\"background-color:#000\">default</span></span>";

      return test(text, result);
    });

    it("is able to disable underline", () => {
      const text = "underline: \x1b[4mstuff\x1b[24mthings";
      const result = "underline: <u>stuff</u>things";

      return test(text, result);
    });

    it("is able to skip disabling underline", () => {
      const text = "not underline: stuff\x1b[24mthings";
      const result = "not underline: stuffthings";

      return test(text, result);
    });

    it("renders two escape sequences in sequence", () => {
      const text = "months remaining\x1b[1;31mtimes\x1b[m\x1b[1;32mmultiplied by\x1b[m $10";
      const result = "months remaining<b><span style=\"color:#A00\">times</span></b><b><span style=\"color:#0A0\">multiplied by</span></b> $10";

      return test(text, result);
    });

    it("drops EL code with no parameter", () => {
      const text = "\x1b[Khello";
      const result = "hello";

      return test(text, result);
    });

    it("drops EL code with 0 parameter", () => {
      const text = "\x1b[0Khello";
      const result = "hello";

      return test(text, result);
    });

    it("drops EL code with 0 parameter after new line character", () => {
      const text = "HELLO\n\x1b[0K\u001b[33;1mWORLD\u001b[0m\n";
      const result = "HELLO\n<span style=\"color:#A50\"><b>WORLD</b></span>\n";

      return test(text, result);
    });

    it("drops EL code with 1 parameter", () => {
      const text = "\x1b[1Khello";
      const result = "hello";

      return test(text, result);
    });

    it("drops EL code with 2 parameter", () => {
      const text = "\x1b[2Khello";
      const result = "hello";

      return test(text, result);
    });

    it("drops ED code with 0 parameter", () => {
      const text = "\x1b[Jhello";
      const result = "hello";

      return test(text, result);
    });

    it("drops ED code with 1 parameter", () => {
      const text = "\x1b[1Jhello";
      const result = "hello";

      return test(text, result);
    });

    it("drops HVP code with 0 parameter", () => {
      const text = "\x1b[;fhello";
      const result = "hello";

      return test(text, result);
    });

    it("drops HVP code with 1 parameter", () => {
      const text = "\x1b[123;fhello";
      const result = "hello";

      return test(text, result);
    });

    it("drops HVP code with 2 parameter", () => {
      const text = "\x1b[123;456fhello";
      const result = "hello";

      return test(text, result);
    });

    it("drops setusg0 sequence", () => {
      const text = "\x1b[(Bhello";
      const result = "hello";

      return test(text, result);
    });

    it("renders un-italic code appropriately", () => {
      const text = "\x1b[3mHello\x1b[23m World";
      const result = "<i>Hello</i> World";

      return test(text, result);
    });

    it("skips rendering un-italic code appropriately", () => {
      const text = "Hello\x1b[23m World";
      const result = "Hello World";

      return test(text, result);
    });

    it("renders overline", () => {
      const text = "\x1b[53mHello World";
      const result = "<span style=\"text-decoration:overline\">Hello World</span>";

      return test(text, result);
    });

    it("renders normal text", () => {
      const text = "\x1b[22mnormal text";
      const result = "<span style=\"font-weight:normal;text-decoration:none;font-style:normal\">normal text</span>";

      return test(text, result);
    });

    it("renders text following carriage return (CR, mac style line break)", () => {
      const text = "ANSI Hello\rWorld";
      const result = "ANSI Hello\rWorld";

      return test(text, result);
    });
  });

  describe("with escapeXML option enabled", () => {
    it("escapes XML entities", () => {
      const text = "normal, \x1b[1;4;31;mbold, <underline>, and red\x1b[0m, normal";
      const result = "normal, <b><u><span style=\"color:#A00\">bold, &lt;underline&gt;, and red</span></u></b>, normal";

      return test(text, result, {escapeXML: true});
    });
  });

  describe("with newline option enabled", () => {
    it("renders line breaks", () => {
      const text = "test\ntest\n";
      const result = "test<br/>test<br/>";

      return test(text, result, {newline: true});
    });

    it("renders multiple line breaks", () => {
      const text = "test\n\ntest\n";
      const result = "test<br/><br/>test<br/>";

      return test(text, result, {newline: true});
    });

    it("renders mac styled line breaks (CR)", () => {
      const text = "test\rtest\r";
      const result = "test<br/>test<br/>";

      return test(text, result, {newline: true});
    });

    it("renders multiple mac styled line breaks (CR)", () => {
      const text = "test\r\rtest\r";
      const result = "test<br/><br/>test<br/>";

      return test(text, result, {newline: true});
    });

    it("renders windows styled line breaks (CR+LF)", () => {
      const text = "testCRLF\r\ntestLF";
      const result = "testCRLF<br/>testLF";

      return test(text, result, {newline: true});
    });

    it("renders windows styled line breaks (multi CR+LF)", () => {
      const text = "testCRLF\r\n\r\ntestLF";
      const result = "testCRLF<br/><br/>testLF";

      return test(text, result, {newline: true});
    });

  });

  describe("with stream option enabled", () => {
    it("persists styles between toHtml() invocations", () => {
      const text = ["\x1b[31mred", "also red"];
      const result = "<span style=\"color:#A00\">red</span><span style=\"color:#A00\">also red</span>";

      return test(text, result, {stream: true});
    });

    it("persists styles between more than two toHtml() invocations", () => {
      const text = ["\x1b[31mred", "also red", "and red"];
      const result = "<span style=\"color:#A00\">red</span><span style=\"color:#A00\">also red</span><span style=\"color:#A00\">and red</span>";

      return test(text, result, {stream: true});
    });

    it("does not persist styles beyond their usefulness", () => {
      const text = ["\x1b[31mred", "also red", "\x1b[30mblack", "and black"];
      const result = "<span style=\"color:#A00\">red</span><span style=\"color:#A00\">also red</span><span style=\"color:#A00\"><span style=\"color:#000\">black</span></span><span style=\"color:#000\">and black</span>";

      return test(text, result, {stream: true});
    });

    it("removes one state when encountering a reset", () => {
      const text = ["\x1b[1mthis is bold\x1b[0m, but this isn't", " nor is this"];
      const result = "<b>this is bold</b>, but this isn't nor is this";

      return test(text, result, {stream: true});
    });

    it("removes multiple state when encountering a reset", () => {
      const text = ["\x1b[1mthis \x1b[9mis bold\x1b[0m, but this isn't", " nor is this"];
      const result = "<b>this <strike>is bold</strike></b>, but this isn't nor is this";

      return test(text, result, {stream: true});
    });
  });

  describe("with custom colors enabled", () => {
    it("renders basic colors", () => {
      const text = ["\x1b[31mblue", "not blue"];
      const result = "<span style=\"color:#00A\">blue</span>not blue";

      return test(text, result, {colors: {1: "#00A"}});
    });

    it("renders basic colors with streaming", () => {
      const text = ["\x1b[31mblue", "also blue"];
      const result = "<span style=\"color:#00A\">blue</span><span style=\"color:#00A\">also blue</span>";

      return test(text, result, {stream: true, colors: {1: "#00A"}});
    });

    it("renders custom colors and default colors", () => {
      const text = ["\x1b[31mblue", "not blue", "\x1b[94mlight blue", "not colored"];
      const result = "<span style=\"color:#00A\">blue</span>not blue<span style=\"color:#55F\">light blue</span>not colored";

      return test(text, result, {colors: {1: "#00A"}});
    });

    it("renders custom colors and default colors together", () => {
      const text = ["\x1b[31mblue", "not blue", "\x1b[94mlight blue", "not colored"];
      const result = "<span style=\"color:#00A\">blue</span>not blue<span style=\"color:#55F\">light blue</span>not colored";

      return test(text, result, {colors: {1: "#00A"}});
    });

    it("renders custom 8/ 16 colors", () => {
      // code - 90 + 8 = color
      // so 94 - 90 + 8 = 12
      const text = ["\x1b[94mlighter blue"];
      const result = "<span style=\"color:#33F\">lighter blue</span>";

      return test(text, result, {colors: {12: "#33F"}});
    });

    it("renders custom 256 colors", () => {
      // code - 90 + 8 = color
      // so 94 - 90 + 8 = 12
      const text = ["\x1b[38;5;125mdark red", "then \x1b[38;5;126msome other color"];
      const result = "<span style=\"color:#af005f\">dark red</span>then <span style=\"color:#af225f\">some other color</span>";

      return test(text, result, {colors: {126: "#af225f"}});
    });
  });
});
