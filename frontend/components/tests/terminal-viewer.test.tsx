import type { ConsoleLog } from "@/lib/ws/terminal";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { TerminalViewer } from "../terminal-viewer";

const { settingsRef } = vi.hoisted(() => ({
  settingsRef: {
    current: {
      "terminal.max-log-lines": 3,
      "terminal.log-level": "INFO",
      "terminal.rich-style": false,
      "terminal.word-wrap": true,
      "terminal.font-size": 14,
      "terminal.log-time": true,
      "terminal.thread-name": true,
      "terminal.source-name": true
    } as Record<string, unknown>
  }
}));

vi.mock("@/lib/settings", () => ({
  getSettings: (key: string) => settingsRef.current[key]
}));

function createMockTerminalClient() {
  const handlers = new Map<string, (data: unknown) => void>();

  const client = {
    subscribe: vi.fn((type: string, cb: (data: unknown) => void) => {
      handlers.set(type, cb);
    })
  };

  const emit = (type: string, data: unknown) => {
    handlers.get(type)?.(data);
  };

  return { client, emit };
}

function createLog(i: number): ConsoleLog {
  return {
    time: Date.now() + i,
    level: "INFO",
    thread: "Server thread",
    source: "net.opanel.test.Test",
    line: `line-${i}`,
    thrownMessage: null
  };
}

describe("test terminal viewer", () => {
  afterEach(() => cleanup());

  beforeEach(() => {
    Object.defineProperty(HTMLElement.prototype, "scrollTo", {
      configurable: true,
      value: vi.fn()
    });
  });

  it("should only show the last n logs when init logs exceed max log lines", async () => {
    const { client, emit } = createMockTerminalClient();
    const { container } = render(<TerminalViewer client={client as any} level="INFO"/>);

    emit("init", [createLog(1), createLog(2), createLog(3), createLog(4), createLog(5)]);

    await waitFor(() => {
      expect(container.querySelectorAll("[data-slot='terminal-log']").length).toBe(3);
    });

    expect(screen.queryByText("line-1")).not.toBeInTheDocument();
    expect(screen.queryByText("line-2")).not.toBeInTheDocument();
    expect(screen.getByText("line-3")).toBeInTheDocument();
    expect(screen.getByText("line-4")).toBeInTheDocument();
    expect(screen.getByText("line-5")).toBeInTheDocument();
  });

  it("should keep only the latest n logs when receiving continuous log packets", async () => {
    const { client, emit } = createMockTerminalClient();
    const { container } = render(<TerminalViewer client={client as any} level="INFO"/>);

    emit("init", [createLog(1), createLog(2), createLog(3)]);
    emit("log", createLog(4));
    emit("log", createLog(5));

    await waitFor(() => {
      expect(container.querySelectorAll("[data-slot='terminal-log']").length).toBe(3);
    });

    expect(screen.queryByText("line-1")).not.toBeInTheDocument();
    expect(screen.queryByText("line-2")).not.toBeInTheDocument();
    expect(screen.getByText("line-3")).toBeInTheDocument();
    expect(screen.getByText("line-4")).toBeInTheDocument();
    expect(screen.getByText("line-5")).toBeInTheDocument();
  });
});
