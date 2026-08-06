import type { NbtPath, NbtPathInput, NbtPathSegment, NbtValue } from "./types";
import { parsePath } from "./path";
import { NbtNumber, NbtString } from "./tags";

interface NbtPathAccess extends NbtValue {
  get<V extends NbtValue = NbtValue>(path: NbtPath): V | undefined;
  set(index: NbtPath, value: NbtValue): void;
}

interface NbtTypedArrayOptions {
  expectedUnit: string;
  valueError: string;
  indexError: string;
  prefix: string;
}

function normalizeGetPath(args: NbtPathInput[]): NbtPath {
  if(Array.isArray(args[0])) {
    const path = args[0];
    if(path.length === 1) {
      return parsePath(path[0]);
    }
    return path;
  }
  if(args.length === 1) {
    return parsePath(args[0] as NbtPathSegment);
  }
  return args as NbtPath;
}

function serializeKey(key: string): string {
  const requiresQuotes = key.length === 0
    || /^\d/.test(key)
    || [...key].some((char) => /\s/.test(char) || "{}[]:,'\"\\".includes(char));
  return requiresQuotes ? new NbtString(key).text() : key;
}

export class NbtObject implements NbtValue {
  children: Record<string, NbtValue> = {};

  constructor(children?: Record<string, NbtValue>) {
    if(children) {
      for(const index in children) {
        this.addChild(index, children[index]);
      }
    }
  }

  addChild(key: string, value: NbtValue): void {
    this.children[key] = value;
  }

  isEmpty(): boolean {
    return Object.keys(this.children).length === 0;
  }

  get(): this;
  get<V extends NbtValue = NbtValue>(path: NbtPathInput, ...rest: NbtPathSegment[]): V | undefined;
  get<V extends NbtValue = NbtValue>(...path: NbtPathInput[]): this | V | undefined {
    const args = normalizeGetPath(path);
    if(args.length === 1) {
      return this.children[args[0]] as V | undefined;
    }
    if(args.length > 1) {
      const child = this.children[args[0]];
      if(child === undefined) {
        return undefined;
      }
      return (child as unknown as NbtPathAccess).get(args.slice(1)) as V | undefined;
    }
    return this;
  }

  set(index: NbtPathInput, value: NbtValue): void {
    if(typeof index === "string" || typeof index === "number") {
      this.children[index] = value;
    } else if(Array.isArray(index)) {
      if(index.length === 0) {
        throw new Error("Empty path is not allowed");
      } else if(index.length === 1) {
        this.children[index[0]] = value;
      } else if(index.length > 1) {
        const parent = this.get<NbtPathAccess>(index.slice(0, -1));
        if(parent !== undefined) {
          parent.set(index.slice(-1), value);
        }
      }
    }
  }

  text(): string {
    const values: string[] = [];
    for(const key in this.children) {
      values.push(`${serializeKey(key)}: ${this.children[key].text()}`);
    }
    return `{${values.join(", ")}}`;
  }
}

export class NbtList implements NbtValue {
  children: NbtValue[] = [];

  constructor(children?: NbtValue[]) {
    if(children) {
      for(const child of children) {
        this.addChild(child);
      }
    }
  }

  addChild(value: NbtValue): void {
    this.children.push(value);
  }

  isEmpty(): boolean {
    return this.children.length === 0;
  }

  get(): this;
  get<V extends NbtValue = NbtValue>(path: NbtPathInput, ...rest: NbtPathSegment[]): V | undefined;
  get<V extends NbtValue = NbtValue>(...path: NbtPathInput[]): this | V | undefined {
    const args = normalizeGetPath(path);
    if(args.length === 1) {
      return this.children[args[0] as number] as unknown as V | undefined;
    }
    if(args.length > 1) {
      const child = this.children[args[0] as number];
      if(child === undefined) {
        return undefined;
      }
      return (child as unknown as NbtPathAccess).get(args.slice(1)) as V | undefined;
    }
    return this;
  }

  set(index: NbtPathInput, value: NbtValue): void {
    const path = Array.isArray(index) ? index : [index];
    if(path.length === 0) {
      throw new Error("Empty path is not allowed");
    }

    const listIndex = Number(path[0]);
    if(!Number.isInteger(listIndex) || listIndex < 0 || listIndex >= this.children.length) {
      throw new Error(`List index out of bounds: ${path[0]}`);
    }

    if(path.length === 1) {
      this.children[listIndex] = value;
    } else {
      (this.children[listIndex] as unknown as NbtPathAccess).set(path.slice(1), value);
    }
  }

  text(): string {
    const values: string[] = [];
    for(let index = 0; index < this.children.length; index++) {
      values.push(this.children[index].text());
    }
    return `[${values.join(", ")}]`;
  }
}

abstract class NbtTypedArray implements NbtValue {
  children: NbtNumber[] = [];

  constructor(
    private readonly options: NbtTypedArrayOptions,
    children?: NbtNumber[],
  ) {
    if(children) {
      for(const child of children) {
        this.addChild(child);
      }
    }
  }

  addChild(value: NbtNumber): void {
    if(value instanceof NbtNumber && value.unit === this.options.expectedUnit) {
      this.children.push(value);
      return;
    }
    throw new Error(this.options.valueError);
  }

  isEmpty(): boolean {
    return this.children.length === 0;
  }

  get(): this;
  get<V extends NbtValue = NbtValue>(path: NbtPathInput, ...rest: NbtPathSegment[]): V | undefined;
  get<V extends NbtValue = NbtValue>(...path: NbtPathInput[]): this | V | undefined {
    const args = normalizeGetPath(path);
    if(args.length === 1) {
      return this.children[args[0] as number] as unknown as V | undefined;
    }
    if(args.length > 1) {
      throw new Error(this.options.indexError);
    }
    return this;
  }

  set(index: NbtPathInput, value: NbtNumber): void {
    const path = Array.isArray(index) ? index : [index];
    if(path.length === 0) {
      throw new Error("Empty path is not allowed");
    }
    if(path.length === 1) {
      if(value instanceof NbtNumber && value.unit === this.options.expectedUnit) {
        this.children[path[0] as number] = value;
        return;
      }
      throw new Error(this.options.valueError);
    }
    if(path.length > 1) {
      throw new Error(this.options.indexError);
    }
  }

  text(): string {
    const values: string[] = [];
    for(let index = 0; index < this.children.length; index++) {
      values.push(this.children[index].text());
    }
    return `[${this.options.prefix}; ${values.join(", ")}]`;
  }
}

export class NbtIntArray extends NbtTypedArray {
  constructor(children?: NbtNumber[]) {
    super({
      expectedUnit: "",
      valueError: "NbtIntArray only accept NbtNumber without unit",
      indexError: "NbtIntArray only accept one index",
      prefix: "I",
    }, children);
  }
}

export class NbtLongArray extends NbtTypedArray {
  constructor(children?: NbtNumber[]) {
    super({
      expectedUnit: "l",
      valueError: 'NbtLongArray only accept NbtNumber with unit "l"',
      indexError: "NbtLongArray only accept one index",
      prefix: "L",
    }, children);
  }
}

export class NbtByteArray extends NbtTypedArray {
  constructor(children?: NbtNumber[]) {
    super({
      expectedUnit: "b",
      valueError: 'NbtByteArray only accept NbtNumber with unit "b"',
      indexError: "NbtByteArray only accept one index",
      prefix: "B",
    }, children);
  }
}
