import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { enableAutomaticLayout } from "../monaco/editor-layout";

describe("enableAutomaticLayout", () => {
  let resizeCallback: ResizeObserverCallback;
  let animationFrameCallback: FrameRequestCallback;
  const observe = vi.fn();
  const disconnect = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();

    vi.stubGlobal("ResizeObserver", class {
      constructor(callback: ResizeObserverCallback) {
        resizeCallback = callback;
      }

      observe = observe;
      unobserve() {}
      disconnect = disconnect;
    });
    vi.stubGlobal("requestAnimationFrame", vi.fn((callback: FrameRequestCallback) => {
      animationFrameCallback = callback;
      return 1;
    }));
    vi.stubGlobal("cancelAnimationFrame", vi.fn());
  });

  afterEach(() => vi.unstubAllGlobals());

  it("lays out the editor on the initial resize notification", () => {
    const container = document.createElement("div");
    Object.defineProperties(container, {
      clientWidth: { value: 320 },
      clientHeight: { value: 240 }
    });
    const layout = vi.fn();
    const editor = {
      getContainerDomNode: () => container,
      onDidDispose: vi.fn(),
      layout
    };

    enableAutomaticLayout(editor as never);
    resizeCallback([
      { contentRect: { width: 320, height: 240 } } as ResizeObserverEntry
    ], {} as ResizeObserver);
    animationFrameCallback(0);

    expect(observe).toHaveBeenCalledWith(container);
    expect(layout).toHaveBeenCalledWith({ width: 320, height: 240 });
  });
});
