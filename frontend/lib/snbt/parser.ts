import type { NbtValue } from "./types";
import { NbtByteArray, NbtIntArray, NbtList, NbtLongArray, NbtObject } from "./collections";
import { NbtBool, NbtNull, NbtNumber, NbtString } from "./tags";

class NbtParser {
  private index = 0;

  constructor(private readonly input: string) {}

  parse<V extends NbtValue = NbtValue>(): V {
    try {
      const value = this.parseValue();
      this.ensureEnd();
      return value as V;
    } catch (error) {
      const context = this.input.substring(Math.max(0, this.index - 10), this.index)
        + `>>${this.input[this.index] || ""}<<`
        + this.input.substring(this.index + 1, this.index + 10);
      const message = error instanceof Error ? error.message : String(error);
      throw new Error(`${message} at position ${this.index}. Context: ...${context}...`);
    }
  }

  private get length(): number {
    return this.input.length;
  }

  private ensureEnd(): void {
    this.skipWhitespace();
    if(this.index < this.length) {
      throw new Error(`Unexpected character: '${this.input[this.index]}'. Expected end of input.`);
    }
  }

  private parseValue(): NbtValue {
    this.skipWhitespace();
    if(this.index >= this.length) {
      throw new Error("Unexpected end of input");
    }

    const char = this.input[this.index];
    if(char === "{") {
      return this.parseObject();
    }
    if(char === "[") {
      return this.parseArray();
    }
    if(char === "'" || char === "\"") {
      return this.parseString(char);
    }
    if(/[0-9-]/.test(char)) {
      return this.parseNumber();
    }
    if(char === "t" && this.index + 4 <= this.length && this.input.slice(this.index, this.index + 4) === "true") {
      this.index += 4;
      return new NbtBool(true);
    }
    if(char === "f" && this.index + 5 <= this.length && this.input.slice(this.index, this.index + 5) === "false") {
      this.index += 5;
      return new NbtBool(false);
    }
    if(char === "n" && this.index + 4 <= this.length && this.input.slice(this.index, this.index + 4) === "null") {
      this.index += 4;
      return new NbtNull();
    }
    throw new Error(`Unexpected character: ${char}`);
  }

  private parseObject(): NbtObject {
    this.index++;
    const obj = new NbtObject();
    let expectComma = false;

    while(this.index < this.length) {
      this.skipWhitespace();
      if(this.input[this.index] === "}") {
        this.index++;
        return obj;
      }

      if(expectComma) {
        if(this.input[this.index] === ",") {
          this.index++;
          this.skipWhitespace();
          if(this.input[this.index] === "}") continue;
        } else {
          throw new Error("Expected comma");
        }
      }

      const { key, quoted } = this.parseKey();
      this.skipWhitespace();
      if(!quoted && /^\d/.test(key)) {
        throw new Error(`Key cannot start with a digit: ${key}`);
      }
      if(this.input[this.index] !== ":") {
        throw new Error("Expected colon");
      }
      this.index++;
      this.skipWhitespace();

      obj.addChild(key, this.parseValue());
      expectComma = true;
      this.skipWhitespace();
    }
    throw new Error("Unterminated object");
  }

  private parseArray(): NbtList | NbtByteArray | NbtIntArray | NbtLongArray {
    this.index++;
    this.skipWhitespace();

    let arr: NbtList | NbtByteArray | NbtIntArray | NbtLongArray;
    if(this.input[this.index] === "B") {
      this.index++;
      this.skipTypedArraySeparator();
      arr = new NbtByteArray();
    } else if(this.input[this.index] === "I") {
      this.index++;
      this.skipTypedArraySeparator();
      arr = new NbtIntArray();
    } else if(this.input[this.index] === "L") {
      this.index++;
      this.skipTypedArraySeparator();
      arr = new NbtLongArray();
    } else {
      arr = new NbtList();
    }

    let expectComma = false;
    while(this.index < this.length) {
      this.skipWhitespace();
      if(this.input[this.index] === "]") {
        this.index++;
        return arr;
      }

      if(expectComma) {
        if(this.input[this.index] === ",") {
          this.index++;
          this.skipWhitespace();
          if(this.input[this.index] === "]") continue;
        } else {
          throw new Error("Expected comma");
        }
      }

      const value = this.parseValue();
      if(arr instanceof NbtList) {
        arr.addChild(value);
      } else {
        arr.addChild(value as NbtNumber);
      }

      expectComma = true;
      this.skipWhitespace();
    }
    throw new Error("Unterminated array");
  }

  private skipTypedArraySeparator(): void {
    this.skipWhitespace();
    if(this.input[this.index] !== ";") {
      throw new Error("Expected semicolon");
    }
    this.index++;
  }

  private parseString(quoteChar: string): NbtString {
    this.index++;
    return new NbtString(this.parseStringContent(quoteChar));
  }

  private parseStringContent(quoteChar: string): string {
    let result = "";
    let escaped = false;

    while(this.index < this.length) {
      const char = this.input[this.index++];
      if(escaped) {
        switch(char) {
          case "n": result += "\n"; break;
          case "r": result += "\r"; break;
          case "t": result += "\t"; break;
          case "b": result += "\b"; break;
          case "f": result += "\f"; break;
          case "v": result += "\v"; break;
          case "0": result += "\0"; break;
          case "\\": result += "\\"; break;
          case "'": result += "'"; break;
          case "\"": result += "\""; break;
          case "u":
            result += this.parseUnicodeEscape();
            break;
          case "x":
            result += this.parseHexadecimalEscape();
            break;
          default:
            result += `\\${char}`;
        }
        escaped = false;
      } else if(char === "\\") {
        escaped = true;
      } else if(char === quoteChar) {
        return result;
      } else {
        result += char;
      }
    }
    throw new Error("Unterminated string");
  }

  private parseUnicodeEscape(): string {
    if(this.index + 4 > this.length) {
      throw new Error("Incomplete Unicode escape sequence");
    }
    const hex = this.input.substring(this.index, this.index + 4);
    if(!/^[0-9a-fA-F]{4}$/.test(hex)) {
      throw new Error(`Invalid Unicode escape: \\u${hex}`);
    }
    this.index += 4;
    return String.fromCharCode(parseInt(hex, 16));
  }

  private parseHexadecimalEscape(): string {
    if(this.index + 2 > this.length) {
      throw new Error("Incomplete hexadecimal escape sequence");
    }
    const hex = this.input.substring(this.index, this.index + 2);
    if(!/^[0-9a-fA-F]{2}$/.test(hex)) {
      throw new Error(`Invalid hexadecimal escape: \\x${hex}`);
    }
    this.index += 2;
    return String.fromCharCode(parseInt(hex, 16));
  }

  private parseNumber(): NbtNumber {
    const start = this.index;
    if(this.input[this.index] === "-") {
      this.index++;
    }

    while(this.index < this.length && /[0-9]/.test(this.input[this.index])) {
      this.index++;
    }
    if(this.input[this.index] === ".") {
      this.index++;
      while(this.index < this.length && /[0-9]/.test(this.input[this.index])) {
        this.index++;
      }
    }
    if(/[eE]/.test(this.input[this.index])) {
      this.index++;
      if(/[+-]/.test(this.input[this.index])) {
        this.index++;
      }
      while(this.index < this.length && /[0-9]/.test(this.input[this.index])) {
        this.index++;
      }
    }

    const numberString = this.input.substring(start, this.index);
    let unit = "";
    if(this.index < this.length && /[bfdisl]/i.test(this.input[this.index])) {
      unit = this.input[this.index++];
    }
    if(!/^-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?$/.test(numberString)) {
      throw new Error(`Invalid number format: ${numberString}`);
    }

    const value = parseFloat(numberString);
    if(Number.isNaN(value)) {
      throw new Error(`Invalid number: ${numberString}`);
    }
    return new NbtNumber(value, unit);
  }

  private parseKey(): { key: string; quoted: boolean } {
    this.skipWhitespace();
    if(this.index >= this.length) {
      throw new Error("Unexpected end of input while parsing key");
    }
    if(this.input[this.index] === "'" || this.input[this.index] === "\"") {
      const quote = this.input[this.index];
      this.index++;
      return { key: this.parseStringContent(quote), quoted: true };
    }

    let key = "";
    while(this.index < this.length) {
      const char = this.input[this.index];
      if(!/\s/.test(char) && !/[{}[\]:,]/.test(char)) {
        key += char;
        this.index++;
      } else {
        break;
      }
    }
    if(!key) {
      throw new Error("Empty key is not allowed");
    }
    return { key, quoted: false };
  }

  private skipWhitespace(): void {
    while(this.index < this.length && /\s/.test(this.input[this.index])) {
      this.index++;
    }
  }
}

export function parseNbtString<V extends NbtValue = NbtValue>(str: string): V {
  return new NbtParser(str).parse<V>();
}
