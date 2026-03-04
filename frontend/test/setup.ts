import "@testing-library/jest-dom/vitest";
import { vi } from "vitest";

vi.mock("next/font/local", () => ({
  default: () => ({
    className: "mocked-font-class",
    variable: "--mocked-font-variable",
    style: { fontFamily: "mocked-font" }
  })
}));

vi.mock("@/lib/i18n", () => ({
  $: (id: string, ...args: unknown[]) => `[${id}]${args.length > 0 ? `(${args.join(",")})` : ""}`,
  $mc: (id: string) => `[${id}]`,
  localize: (id: string) => `[${id}]`,
  localizeRich: (id: string, ...args: unknown[]) => `[${id}]${args.length > 0 ? `(${args.join(",")})` : ""}`,
}));

vi.mock("sonner", () => ({
  toast: {
    info: vi.fn(),
    success: vi.fn(),
    warning: vi.fn(),
    error: vi.fn()
  }
}));
