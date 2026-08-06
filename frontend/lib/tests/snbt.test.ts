import { describe, expect, it } from "vitest";
import {
  NbtBool,
  NbtIntArray,
  NbtByteArray,
  NbtLongArray,
  NbtList,
  NbtNull,
  NbtNumber,
  NbtObject,
  NbtString,
  parseNbtString,
  parsePath,
} from "../snbt";

type NbtNumberValue = InstanceType<typeof NbtNumber>;

interface TypedArrayValue {
  children: NbtNumberValue[];
  addChild(value: NbtNumberValue): void;
  isEmpty(): boolean;
  get(...path: unknown[]): unknown;
  set(index: string | number | (string | number)[], value: NbtNumberValue): void;
  text(): string;
}

type TypedArrayConstructor = new (children?: NbtNumberValue[]) => TypedArrayValue;

function getWithoutPath(value: { get(...path: never[]): unknown }): unknown {
  return value.get();
}

function mustGet<V>(value: V | undefined): V {
  expect(value).toBeDefined();
  return value as V;
}

describe("snbt-js scalar tags", () => {
  describe("NbtNumber", () => {
    it.each([
      [1.4, "", 1, "1"],
      [1.5, "", 2, "2"],
      [-1.5, "", -1, "-1"],
      [126.6, "B", 127, "127b"],
      [200, "b", 127, "127b"],
      [-200, "b", -128, "-128b"],
      [32766.6, "S", 32767, "32767s"],
      [40000, "s", 32767, "32767s"],
      [-40000, "s", -32768, "-32768s"],
      [42.4, "L", 42, "42l"],
      [1, "F", 1, "1.0f"],
      [1.25, "f", 1.25, "1.25f"],
      [2, "D", 2, "2.0d"],
      [-2.5, "d", -2.5, "-2.5d"],
      [1.25, "i", 1, "1i"],
      [1.25, "custom", 1.25, "1.25custom"],
    ])("normalizes value %s with unit %s", (value, unit, expectedValue, expectedText) => {
      const number = new NbtNumber(value, unit);

      expect(number.value).toBe(expectedValue);
      expect(typeof number.value).toBe("number");
      expect(number.unit).toBe(unit.toLowerCase());
      expect(number.text()).toBe(expectedText);
    });

    it("clamps long values to the signed 64-bit range", () => {
      const maximum = new NbtNumber(Number.POSITIVE_INFINITY, "l");
      const minimum = new NbtNumber(Number.NEGATIVE_INFINITY, "l");

      const maximumValue = Number("9223372036854775807");
      const minimumValue = Number("-9223372036854775808");
      expect(maximum.value).toBe(maximumValue);
      expect(maximum.text()).toBe(`${maximumValue}l`);
      expect(minimum.value).toBe(minimumValue);
      expect(minimum.text()).toBe(`${minimumValue}l`);
    });

    it("preserves NaN through numeric normalization", () => {
      expect(new NbtNumber(Number.NaN).value).toBeNaN();
      expect(new NbtNumber(Number.NaN, "b").value).toBeNaN();
      expect(new NbtNumber(Number.NaN, "l").value).toBeNaN();
    });
  });

  describe("NbtString", () => {
    it("stores the original value", () => {
      expect(new NbtString("你好, Minecraft").value).toBe("你好, Minecraft");
    });

    it("serializes with single quotes and escapes supported characters", () => {
      const value = new NbtString("slash\\ quote' double\"\n\r\tend");

      expect(value.text()).toBe(String.raw`'slash\\ quote\' double"\n\r\tend'`);
    });

    it("serializes an empty string", () => {
      expect(new NbtString("").text()).toBe("''");
    });
  });

  describe("NbtBool", () => {
    it.each([
      [true, true],
      [false, false],
      [1, true],
      [0, false],
      ["yes", true],
      ["", false],
      [{}, true],
      [null, false],
      [undefined, false],
    ])("coerces %s to %s", (input, expected) => {
      const value = new NbtBool(input as boolean);

      expect(value.value).toBe(expected);
      expect(value.text()).toBe(String(expected));
    });
  });

  describe("NbtNull", () => {
    it("exposes null and serializes it", () => {
      const value = new NbtNull();

      expect(value.value).toBeNull();
      expect(value.text()).toBe("null");
    });
  });
});

describe("snbt-js objects and lists", () => {
  describe("NbtObject", () => {
    it("constructs empty and pre-populated objects", () => {
      const empty = new NbtObject();
      const populated = new NbtObject({
        name: new NbtString("Steve"),
        health: new NbtNumber(20),
      });

      expect(empty.children).toEqual({});
      expect(empty.isEmpty()).toBe(true);
      expect(populated.isEmpty()).toBe(false);
      expect(populated.children.name).toBeInstanceOf(NbtString);
      expect(populated.children.health).toBeInstanceOf(NbtNumber);
    });

    it("adds and replaces children", () => {
      const object = new NbtObject();
      object.addChild("value", new NbtNumber(1));
      object.addChild("value", new NbtNumber(2));

      expect(mustGet(object.get<NbtNumber>("value")).value).toBe(2);
      expect(Object.keys(object.children)).toEqual(["value"]);
    });

    it("gets itself when no path is supplied", () => {
      const object = new NbtObject();

      expect(getWithoutPath(object)).toBe(object);
    });

    it("gets direct, dotted, bracketed, array, and variadic paths", () => {
      const object = parseNbtString<NbtObject>(String.raw`{
        player: {
          inventory: [{id: "minecraft:stone"}, {id: "minecraft:diamond"}]
        },
        "literal.dot": 7
      }`);

      expect(object.get<NbtObject>("player")).toBeInstanceOf(NbtObject);
      expect(mustGet(object.get<NbtString>("player.inventory[1].id")).value).toBe("minecraft:diamond");
      expect(mustGet(object.get<NbtString>(["player", "inventory", 0, "id"])).value).toBe("minecraft:stone");
      expect(mustGet(object.get<NbtString>("player", "inventory", 1, "id")).value).toBe("minecraft:diamond");
      expect(mustGet(object.get<NbtNumber>(String.raw`"literal.dot"`)).value).toBe(7);
    });

    it.each([
      "missing",
      "player.missing",
      "player.inventory[4]",
      "player.inventory[4].id",
    ])("returns undefined for missing path %s", (path) => {
      const object = parseNbtString<NbtObject>("{player:{inventory:[{id:'stone'}]}}");

      expect(object.get(path)).toBeUndefined();
    });

    it("sets direct, single-segment, and existing nested paths", () => {
      const object = parseNbtString<NbtObject>("{player:{health:20},items:[{count:1}]}");

      object.set("direct", new NbtString("value"));
      object.set(2, new NbtString("numeric key"));
      object.set(["single"], new NbtBool(true));
      object.set(["player", "health"], new NbtNumber(15));
      object.set(["items", 0, "count"], new NbtNumber(64));

      expect(mustGet(object.get<NbtString>("direct")).value).toBe("value");
      expect(mustGet(object.get<NbtString>("2")).value).toBe("numeric key");
      expect(mustGet(object.get<NbtBool>("single")).value).toBe(true);
      expect(mustGet(object.get<NbtNumber>("player.health")).value).toBe(15);
      expect(mustGet(object.get<NbtNumber>("items[0].count")).value).toBe(64);
    });

    it("silently ignores nested sets whose parent does not exist", () => {
      const object = parseNbtString<NbtObject>("{player:{}}");

      expect(() => object.set(["player", "inventory", 0], new NbtString("stone"))).not.toThrow();
      expect(object.get("player.inventory")).toBeUndefined();
    });

    it("rejects an empty set path", () => {
      const object = new NbtObject();

      expect(() => object.set([], new NbtString("unused"))).toThrow("Empty path is not allowed");
      expect(object.isEmpty()).toBe(true);
    });

    it("serializes children in insertion order", () => {
      const object = new NbtObject({
        name: new NbtString("Alex"),
        health: new NbtNumber(20, "s"),
        alive: new NbtBool(true),
      });

      expect(object.text()).toBe("{name: 'Alex', health: 20s, alive: true}");
    });

    it("quotes and escapes unsafe object keys while serializing", () => {
      const object = new NbtObject({
        safe: new NbtNumber(0),
        "minecraft:key": new NbtNumber(1),
        "key with spaces": new NbtNumber(2),
        "1key": new NbtNumber(3),
        "quote'key": new NbtNumber(4),
        [String.raw`slash\key`]: new NbtNumber(5),
      });
      const serialized = object.text();

      expect(serialized).toBe(String.raw`{safe: 0, 'minecraft:key': 1, 'key with spaces': 2, '1key': 3, 'quote\'key': 4, 'slash\\key': 5}`);
      expect(parseNbtString<NbtObject>(serialized).text()).toBe(serialized);
    });
  });

  describe("NbtList", () => {
    it("constructs empty and pre-populated lists", () => {
      const empty = new NbtList();
      const populated = new NbtList([new NbtString("first"), new NbtNumber(2)]);

      expect(empty.children).toEqual([]);
      expect(empty.isEmpty()).toBe(true);
      expect(populated.isEmpty()).toBe(false);
      expect(populated.children).toHaveLength(2);
    });

    it("adds children and serializes heterogeneous values", () => {
      const list = new NbtList();
      list.addChild(new NbtNumber(1));
      list.addChild(new NbtString("two"));
      list.addChild(new NbtBool(false));
      list.addChild(new NbtNull());

      expect(list.text()).toBe("[1, 'two', false, null]");
    });

    it("gets itself and direct or nested values", () => {
      const list = new NbtList([
        new NbtObject({ value: new NbtString("zero") }),
        new NbtObject({ value: new NbtString("one") }),
      ]);

      expect(getWithoutPath(list)).toBe(list);
      expect(list.get<NbtObject>(0)).toBe(list.children[0]);
      expect(list.get<NbtObject>("1")).toBe(list.children[1]);
      expect(mustGet(list.get<NbtString>([1, "value"])).value).toBe("one");
      expect(mustGet(list.get<NbtString>(0, "value")).value).toBe("zero");
      expect(list.get(5)).toBeUndefined();
      expect(list.get([5, "value"])).toBeUndefined();
    });

    it("sets direct, array, and nested indices", () => {
      const list = new NbtList([
        new NbtObject({ count: new NbtNumber(1) }),
        new NbtString("second"),
        new NbtString("third"),
      ]);

      list.set(1, new NbtString("direct"));
      list.set([2], new NbtString("array"));
      list.set([0, "count"], new NbtNumber(3));

      expect(mustGet(list.get<NbtString>(1)).value).toBe("direct");
      expect(mustGet(list.get<NbtString>(2)).value).toBe("array");
      expect(mustGet(list.get<NbtNumber>([0, "count"])).value).toBe(3);
    });

    it("rejects direct and single-segment out-of-range indices", () => {
      const original = new NbtString("first");
      const list = new NbtList([original]);

      expect(() => list.set(1, new NbtString("second"))).toThrow("List index out of bounds: 1");
      expect(() => list.set([2], new NbtString("third"))).toThrow("List index out of bounds: 2");
      expect(list.children).toEqual([original]);
    });

    it("rejects nested sets through out-of-range indices", () => {
      const list = new NbtList();

      expect(() => list.set([4, "name"], new NbtString("missing"))).toThrow("List index out of bounds: 4");
      expect(list.children).toEqual([]);
    });

    it("rejects an empty set path", () => {
      const list = new NbtList();

      expect(() => list.set([], new NbtString("unused"))).toThrow("Empty path is not allowed");
      expect(list.children).toEqual([]);
    });
  });
});

describe("snbt-js typed arrays", () => {
  const typedArrays = [
    {
      name: "int array",
      Constructor: NbtIntArray as unknown as TypedArrayConstructor,
      unit: "",
      prefix: "I",
      valueError: "NbtIntArray only accept NbtNumber without unit",
      indexError: "NbtIntArray only accept one index",
    },
    {
      name: "long array",
      Constructor: NbtLongArray,
      unit: "l",
      prefix: "L",
      valueError: 'NbtLongArray only accept NbtNumber with unit "l"',
      indexError: "NbtLongArray only accept one index",
    },
    {
      name: "byte array",
      Constructor: NbtByteArray,
      unit: "b",
      prefix: "B",
      valueError: 'NbtByteArray only accept NbtNumber with unit "b"',
      indexError: "NbtByteArray only accept one index",
    },
  ];

  describe.each(typedArrays)("$name", ({ Constructor, unit, prefix, valueError, indexError }) => {
    const makeNumber = (value: number) => new NbtNumber(value, unit);

    it("constructs empty and populated values", () => {
      const empty = new Constructor();
      const populated = new Constructor([makeNumber(1), makeNumber(2)]);

      expect(empty.isEmpty()).toBe(true);
      expect(empty.text()).toBe(`[${prefix}; ]`);
      expect(populated.isEmpty()).toBe(false);
      expect(populated.children.map((child) => child.value)).toEqual([1, 2]);
      expect(populated.text()).toBe(`[${prefix}; 1${unit}, 2${unit}]`);
    });

    it("adds values with the required unit", () => {
      const array = new Constructor();
      const value = makeNumber(5);

      array.addChild(value);
      expect(array.children).toEqual([value]);
    });

    it("rejects values with the wrong unit in constructors and addChild", () => {
      const wrongValue = new NbtNumber(1, unit === "b" ? "l" : "b");

      expect(() => new Constructor([wrongValue])).toThrow(valueError);
      expect(() => new Constructor().addChild(wrongValue)).toThrow(valueError);
      expect(() => new Constructor().addChild(new NbtString("1") as unknown as NbtNumberValue)).toThrow(valueError);
    });

    it("gets itself or a single element", () => {
      const array = new Constructor([makeNumber(4)]);

      expect(getWithoutPath(array)).toBe(array);
      expect(array.get(0)).toBe(array.children[0]);
      expect(array.get([0])).toBe(array.children[0]);
      expect(array.get(2)).toBeUndefined();
    });

    it("rejects multi-segment gets", () => {
      const array = new Constructor([makeNumber(4)]);

      expect(() => array.get([0, "value"])).toThrow(indexError);
      expect(() => array.get(0, "value")).toThrow(indexError);
    });

    it("sets a single array or string index", () => {
      const array = new Constructor([makeNumber(1)]);
      const replacement = makeNumber(9);

      array.set([0], replacement);
      expect(array.children[0]).toBe(replacement);

      const otherReplacement = makeNumber(8);
      array.set("0", otherReplacement);
      expect(array.children[0]).toBe(otherReplacement);
    });

    it("sets a direct numeric index", () => {
      const array = new Constructor([makeNumber(1)]);
      const replacement = makeNumber(2);

      array.set(0, replacement);
      expect(array.children[0]).toBe(replacement);
    });

    it("rejects wrong-unit sets and multi-segment sets", () => {
      const array = new Constructor([makeNumber(1)]);
      const wrongValue = new NbtNumber(2, unit === "b" ? "l" : "b");

      expect(() => array.set([0], wrongValue)).toThrow(valueError);
      expect(() => array.set([0, 1], makeNumber(2))).toThrow(indexError);
    });

    it("rejects an empty set path", () => {
      const original = makeNumber(1);
      const array = new Constructor([original]);

      expect(() => array.set([], makeNumber(2))).toThrow("Empty path is not allowed");
      expect(array.children).toEqual([original]);
    });
  });
});

describe("snbt-js path parsing", () => {
  it.each([
    [0, [0]],
    [12, [12]],
    ["", []],
    ["player", ["player"]],
    ["player.inventory[2].tag.name", ["player", "inventory", 2, "tag", "name"]],
    ["root[ 002 ]", ["root", 2]],
    [String.raw`"minecraft:custom.name"`, ["minecraft:custom.name"]],
    [String.raw`root["key.with.dots"]`, ["root", "key.with.dots"]],
    [String.raw`root[ "key with spaces" ]`, ["root", "key with spaces"]],
    ["player.", ["player"]],
  ])("parses path %s", (path, expected) => {
    expect(parsePath(path)).toEqual(expected);
  });

  it.each([
    [".player", "Unexpected dot"],
    ["player..name", "Unexpected dot"],
    ["[0]", "Unexpected opening bracket"],
    ["items[0][1]", "Unexpected opening bracket"],
    ["items[[0]]", "Nested brackets are not allowed"],
    ["items[]", "Empty brackets are not allowed"],
    [String.raw`items[""]`, "Empty brackets are not allowed"],
    ["items[-1]", "Invalid character in bracket: '-'"],
    ["items[index]", "Invalid character in bracket: 'i'"],
    ["items[1.2]", "Dot not allowed inside brackets"],
    ["items]", "Unexpected closing bracket"],
    ["items[0", "Unclosed bracket"],
    [String.raw`"items`, "Unclosed quote"],
    [String.raw`items"name"`, "Unexpected double quote"],
  ])("rejects invalid path %s", (path, message) => {
    expect(() => parsePath(path)).toThrow(message);
  });
});

describe("snbt-js parser", () => {
  describe("top-level values", () => {
    it.each([
      ["'single quoted'", NbtString, "single quoted"],
      ['"double quoted"', NbtString, "double quoted"],
      ["42", NbtNumber, 42],
      ["true", NbtBool, true],
      ["false", NbtBool, false],
      ["null", NbtNull, null],
    ])("parses %s", (source, Constructor, expectedValue) => {
      const result = parseNbtString(source) as unknown as { value: unknown };

      expect(result).toBeInstanceOf(Constructor);
      expect(result.value).toBe(expectedValue);
    });

    it("ignores all surrounding JavaScript whitespace", () => {
      const result = parseNbtString<NbtNumber>(" \t\r\n\v\f 42 \t\r\n");

      expect(result.value).toBe(42);
    });
  });

  describe("objects and ordinary lists", () => {
    it("parses empty compounds and lists", () => {
      expect(parseNbtString<NbtObject>("{}").children).toEqual({});
      expect(parseNbtString<NbtList>("[]").children).toEqual([]);
    });

    it("parses nested heterogeneous structures", () => {
      const result = parseNbtString<NbtObject>(String.raw`{
        player: {
          name: "Steve",
          position: [100.0d, 64.0d, -200.0d],
          flags: [true, false, null],
          metadata: {}
        }
      }`);

      expect(mustGet(result.get<NbtString>("player.name")).value).toBe("Steve");
      expect(mustGet(result.get<NbtNumber>("player.position[2]")).value).toBe(-200);
      expect(mustGet(result.get<NbtBool>("player.flags[0]")).value).toBe(true);
      expect(mustGet(result.get<NbtNull>("player.flags[2]")).value).toBeNull();
      expect(mustGet(result.get<NbtObject>("player.metadata")).isEmpty()).toBe(true);
    });

    it("supports unquoted, quoted, Unicode, and escaped keys", () => {
      const result = parseNbtString<NbtObject>(String.raw`{
        simple_key: 1,
        $dollar: 2,
        中文键: 3,
        "minecraft:custom_data": 4,
        'key with spaces': 5,
        "line\nkey": 6,
        "unicode\u0020key": 7
      }`);

      expect(mustGet(result.get<NbtNumber>("simple_key")).value).toBe(1);
      expect(mustGet(result.get<NbtNumber>("$dollar")).value).toBe(2);
      expect(mustGet(result.get<NbtNumber>("中文键")).value).toBe(3);
      expect(mustGet(result.get<NbtNumber>("minecraft:custom_data")).value).toBe(4);
      expect(mustGet(result.get<NbtNumber>("key with spaces")).value).toBe(5);
      expect(result.children["line\nkey"]).toBeInstanceOf(NbtNumber);
      expect(result.children["unicode key"]).toBeInstanceOf(NbtNumber);
    });

    it("allows quoted keys to start with a digit", () => {
      const result = parseNbtString<NbtObject>(`{'1key':1,"2key":2}`);

      expect(mustGet(result.get<NbtNumber>("1key")).value).toBe(1);
      expect(mustGet(result.get<NbtNumber>("2key")).value).toBe(2);
    });

    it("overwrites duplicate compound keys with the last value", () => {
      const result = parseNbtString<NbtObject>("{value:1,value:2,value:3}");

      expect(mustGet(result.get<NbtNumber>("value")).value).toBe(3);
      expect(Object.keys(result.children)).toEqual(["value"]);
    });

    it("accepts trailing commas in compounds and lists", () => {
      const object = parseNbtString<NbtObject>("{first:1,second:[2,3,],}");

      expect(mustGet(object.get<NbtNumber>("first")).value).toBe(1);
      expect(mustGet(object.get<NbtList>("second")).children).toHaveLength(2);
    });
  });

  describe("numbers", () => {
    it.each([
      ["0", 0, ""],
      ["-0", -0, ""],
      ["2147483647", 2147483647, ""],
      ["1.6", 2, ""],
      ["-1.6", -2, ""],
      ["1e3", 1000, ""],
      ["1E+3", 1000, ""],
      ["1e-3f", 0.001, "f"],
      ["127B", 127, "b"],
      ["32767S", 32767, "s"],
      ["1.5I", 2, "i"],
      ["2147483648L", 2147483648, "l"],
      ["3.14F", 3.14, "f"],
      ["2D", 2, "d"],
    ])("parses %s", (source, expectedValue, expectedUnit) => {
      const result = parseNbtString<NbtNumber>(source);

      expect(result.value).toBe(expectedValue);
      expect(result.unit).toBe(expectedUnit);
    });

    it.each([
      ["128b", 127],
      ["-129b", -128],
      ["32768s", 32767],
      ["-32769s", -32768],
      ["1e100l", Number("9223372036854775807")],
      ["-1e100l", Number("-9223372036854775808")],
    ])("applies range limits while parsing %s", (source, expected) => {
      expect(parseNbtString<NbtNumber>(source).value).toBe(expected);
    });

    it.each([
      "01",
      "-01",
      ".5",
      "1.",
      "-",
      "+1",
      "1e",
      "1e+",
      "--1",
    ])("rejects invalid number %s", (source) => {
      expect(() => parseNbtString(source)).toThrow();
    });
  });

  describe("strings and escapes", () => {
    it("parses every supported simple escape", () => {
      const result = parseNbtString<NbtString>(String.raw`"\n\r\t\b\f\v\0\\\'\""`);

      expect(result.value).toBe("\n\r\t\b\f\v\0\\'\"");
    });

    it("parses Unicode and hexadecimal escapes", () => {
      const result = parseNbtString<NbtString>(String.raw`"A=\u0041, bang=\x21, emoji=\uD83D\uDE00"`);

      expect(result.value).toBe("A=A, bang=!, emoji=😀");
    });

    it("preserves unknown escape sequences", () => {
      const result = parseNbtString<NbtString>(String.raw`"\q\z"`);

      expect(result.value).toBe(String.raw`\q\z`);
      expect(parseNbtString<NbtString>(result.text()).value).toBe(String.raw`\q\z`);
    });

    it("round-trips strings serialized by NbtString.text", () => {
      const original = "quote' slash\\ line\ncarriage\rtab\t";
      const serialized = new NbtString(original).text();

      expect(parseNbtString<NbtString>(serialized).value).toBe(original);
    });

    it.each([
      [String.raw`"unterminated`, "Unterminated string"],
      ["'unterminated\\", "Unterminated string"],
      [String.raw`"\u12"`, "Incomplete Unicode escape sequence"],
      [String.raw`"\uZZZZ"`, "Invalid Unicode escape"],
      [String.raw`"\x1`, "Incomplete hexadecimal escape sequence"],
      [String.raw`"\xGG"`, "Invalid hexadecimal escape"],
    ])("rejects malformed string %s", (source, message) => {
      expect(() => parseNbtString(source)).toThrow(message);
    });
  });

  describe("typed arrays", () => {
    it("parses byte, int, and long arrays with whitespace and trailing commas", () => {
      const result = parseNbtString<NbtObject>(String.raw`{
        bytes: [ B ; 1B, -2b, ],
        ints: [ I ; 1, -2, ],
        longs: [ L ; 1L, -2l, ]
      }`);
      const bytes = result.get("bytes") as unknown as TypedArrayValue;
      const ints = result.get("ints") as unknown as TypedArrayValue;
      const longs = result.get("longs") as unknown as TypedArrayValue;

      expect(bytes).toBeInstanceOf(NbtByteArray);
      expect(ints).toBeInstanceOf(NbtIntArray);
      expect(longs).toBeInstanceOf(NbtLongArray);
      expect(bytes.children.map((value) => value.value)).toEqual([1, -2]);
      expect(ints.children.map((value) => value.value)).toEqual([1, -2]);
      expect(longs.children.map((value) => value.value)).toEqual([1, -2]);
    });

    it.each([
      ["[B;]", NbtByteArray, "[B; ]"],
      ["[I;]", NbtIntArray, "[I; ]"],
      ["[L;]", NbtLongArray, "[L; ]"],
    ])("parses empty typed array %s", (source, Constructor, expectedText) => {
      const result = parseNbtString(source) as unknown as TypedArrayValue;

      expect(result).toBeInstanceOf(Constructor);
      expect(result.children).toEqual([]);
      expect(result.text()).toBe(expectedText);
    });

    it.each([
      ["[B 1b]", "Expected semicolon"],
      ["[I,1]", "Expected semicolon"],
      ["[L:1l]", "Expected semicolon"],
      ["[B;1]", "NbtByteArray only accept"],
      ["[I;1b]", "NbtIntArray only accept"],
      ["[L;1]", "NbtLongArray only accept"],
      ["[b;1b]", "Unexpected character: b"],
    ])("rejects malformed typed array %s", (source, message) => {
      expect(() => parseNbtString(source)).toThrow(message);
    });
  });

  describe("syntax errors", () => {
    it.each([
      ["", "Unexpected end of input"],
      ["   ", "Unexpected end of input"],
      ["bare", "Unexpected character: b"],
      ["True", "Unexpected character: T"],
      ["undefined", "Unexpected character: u"],
      ["{} trailing", "Expected end of input"],
      ["truefalse", "Expected end of input"],
      ["{", "Unterminated object"],
      ["[", "Unterminated array"],
      ["{a:1 b:2}", "Expected comma"],
      ["[1 2]", "Expected comma"],
      ["{a 1}", "Expected colon"],
      ["{a:}", "Unexpected character: }"],
      ["{,a:1}", "Empty key is not allowed"],
      ["{a:1,,b:2}", "Empty key is not allowed"],
      ["[1,,2]", "Unexpected character: ,"],
      ["{1key:1}", "Key cannot start with a digit"],
    ])("rejects malformed input %s", (source, message) => {
      expect(() => parseNbtString(source)).toThrow(message);
    });

    it("adds the parser position and nearby context to errors", () => {
      expect(() => parseNbtString("{player:{health:20 missing:1}}"))
        .toThrow(/Expected comma at position \d+\. Context: \.\.\..*>>.*<<.*\.\.\./);
    });
  });

  describe("serialization round trips", () => {
    it("round-trips parser-produced values whose keys need no quoting", () => {
      const source = String.raw`{name:"Steve",health:20s,pos:[1.0d,64.0d,-2.5d],flags:[true,false,null],bytes:[B;1b,-2b]}`;
      const parsed = parseNbtString(source);
      const reparsed = parseNbtString(parsed.text());

      expect(reparsed.text()).toBe(parsed.text());
    });
  });
});
