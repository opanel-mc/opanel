import { renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { usePrevious } from "../use-previous";

describe("test usePrevious", () => {
  it("should return undefined on the first render", () => {
    const { result } = renderHook(() => usePrevious(0));
    expect(result.current).toBeUndefined();
  });

  it("should return the previous value on subsequent renders", () => {
    let value = 0;
    const { result, rerender } = renderHook(() => usePrevious(value));
    expect(result.current).toBeUndefined();

    value++;
    rerender();
    expect(result.current).toBe(0);

    value += 233;
    rerender();
    expect(result.current).toBe(1);

    rerender();
    expect(result.current).toBe(234);
  });
});
