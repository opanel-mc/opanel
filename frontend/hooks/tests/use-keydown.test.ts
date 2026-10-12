import { act, cleanup, renderHook } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { useKeydown } from "../use-keydown";

afterEach(() => cleanup());

describe("test useKeydown", () => {
  it("ignores keydown events without a key and still handles subsequent shortcuts", () => {
    const fn = vi.fn();
    const onError = vi.fn((e: ErrorEvent) => e.preventDefault());
    window.addEventListener("error", onError);
    renderHook(() => useKeydown("s", { ctrl: true }, fn));

    try {
      act(() => {
        document.body.dispatchEvent(new Event("keydown", { bubbles: true }));
      });
      expect(onError).not.toHaveBeenCalled();
      expect(fn).not.toHaveBeenCalled();

      act(() => {
        document.body.dispatchEvent(new KeyboardEvent("keydown", { key: "S", ctrlKey: true }));
      });
      expect(fn).toHaveBeenCalledTimes(1);
    } finally {
      window.removeEventListener("error", onError);
    }
  });

  it("triggers callback when the specified key is pressed", () => {
    const fn = vi.fn();
    renderHook(() => useKeydown("s", {}, fn));

    act(() => {
      const e = new KeyboardEvent("keydown", { key: "s" });
      document.body.dispatchEvent(e);
    });

    expect(fn).toHaveBeenCalled();
  });

  it("does not trigger callback when a different key is pressed", () => {
    const fn = vi.fn();
    renderHook(() => useKeydown("s", {}, fn));

    act(() => {
      const e = new KeyboardEvent("keydown", { key: "a" });
      document.body.dispatchEvent(e);
    });

    expect(fn).not.toHaveBeenCalled();
  });

  it("triggers callback when the specified key is pressed with the required modifier key(s)", () => {
    const fn = vi.fn();
    renderHook(() => useKeydown("s", { ctrl: true, shift: true }, fn));

    act(() => {
      const e = new KeyboardEvent("keydown", { key: "s", ctrlKey: true, shiftKey: true });
      document.body.dispatchEvent(e);
    });

    expect(fn).toHaveBeenCalled();
  });

  it("does not trigger callback when the specified key is pressed without a modifier key", () => {
    const fn = vi.fn();
    renderHook(() => useKeydown("s", { ctrl: true }, fn));
    
    act(() => {
      const e = new KeyboardEvent("keydown", { key: "s" });
      document.body.dispatchEvent(e);
    });

    expect(fn).not.toHaveBeenCalled();
  });

  it("does not trigger callback when the specified key is pressed without the required modifier key(s)", () => {
    const fn = vi.fn();
    renderHook(() => useKeydown("s", { ctrl: true, shift: true }, fn));
    
    act(() => {
      const e = new KeyboardEvent("keydown", { key: "s", ctrlKey: false, shiftKey: true });
      document.body.dispatchEvent(e);
    });

    expect(fn).not.toHaveBeenCalled();
  });
});
