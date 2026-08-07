import type { NbtValue } from "./types";

const MAX_LONG = Number("9223372036854775807");
const MIN_LONG = Number("-9223372036854775808");

function clampLong(value: number): number {
  return Math.max(MIN_LONG, Math.min(MAX_LONG, Math.round(value)));
}

export class NbtNumber implements NbtValue {
  value: number;
  unit: string;

  constructor(value: number, unit = "") {
    const normalizedUnit = unit.toLowerCase();
    if(normalizedUnit === "b") {
      this.value = Math.max(-128, Math.min(127, Math.round(value)));
    } else if(normalizedUnit === "s") {
      this.value = Math.max(-32768, Math.min(32767, Math.round(value)));
    } else if(normalizedUnit === "l") {
      this.value = clampLong(value);
    } else if(normalizedUnit === "" || normalizedUnit === "i") {
      this.value = Math.round(value);
    } else {
      this.value = value;
    }
    this.unit = normalizedUnit;
  }

  text(): string {
    const value = (this.unit === "d" || this.unit === "f") && Number.isInteger(this.value)
      ? this.value.toFixed(1)
      : this.value.toString();
    return `${value}${this.unit ? this.unit : ""}`;
  }
}

export class NbtString implements NbtValue {
  constructor(public value: string) {
  }

  text(): string {
    const escapedValue = this.value
      .replace(/\\/g, "\\\\")
      .replace(/'/g, "\\'")
      .replace(/\n/g, "\\n")
      .replace(/\r/g, "\\r")
      .replace(/\t/g, "\\t");

    return `'${escapedValue}'`;
  }
}

export class NbtBool implements NbtValue {
  value: boolean;

  constructor(value: unknown) {
    this.value = !!value;
  }

  text(): string {
    return `${this.value}`;
  }
}

export class NbtNull implements NbtValue {
  readonly value = null;

  text(): string {
    return "null";
  }
}
