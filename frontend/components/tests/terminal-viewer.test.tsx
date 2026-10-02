import type { ConsoleLog, ConsoleLogLevel } from "@/lib/ws/terminal";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { TerminalViewer } from "../terminal-viewer";

const DEFAULT_LEVELS: ConsoleLogLevel[] = ["INFO", "WARN", "ERROR"];

const { settingsRef } = vi.hoisted(() => ({
  settingsRef: {
    current: {
      "terminal.max-log-lines": 3,
      "terminal.log-levels": ["INFO", "WARN", "ERROR"],
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

function createLog(i: number, overrides?: Partial<ConsoleLog>): ConsoleLog {
  return {
    mcdr: false,
    time: Date.now() + i,
    level: "INFO",
    thread: "Server thread",
    source: "net.opanel.test.Test",
    line: `line-${i}`,
    thrownMessage: null,
    ...overrides
  };
}

describe("test terminal viewer", () => {
  afterEach(() => cleanup());

  beforeEach(() => {
    settingsRef.current["terminal.rich-style"] = false;
    Object.defineProperty(HTMLElement.prototype, "scrollTo", {
      configurable: true,
      value: vi.fn()
    });
  });

  it.each(["init", "log", "mcdr-log"])("renders OSC 8 and ordinary links from %s packets without double escaping", async (packet) => {
    settingsRef.current["terminal.rich-style"] = true;
    const { client, emit } = createMockTerminalClient();
    const { container } = render(<TerminalViewer client={client as any} levels={DEFAULT_LEVELS}/>);
    const url = "https://github.com/Pumpkin-MC/Pumpkin?a=1&b=2";
    const log = createLog(1, {
      mcdr: packet === "mcdr-log",
      line: `\x1b]8;;${url}\x1b\\\x1b[4m[Github Repository]\x1b[24m\x1b]8;;\x1b\\ https://example.com/docs <img class=\"malicious\" src=x> & text`,
      thrownMessage: "\x1b]8;;https://example.com/error\x07查看错误\x1b]8;;\x07"
    });
    emit(packet, packet === "init" ? [log] : log);

    const link = await screen.findByRole("link", { name: "[Github Repository]" });
    expect(link).toHaveAttribute("href", url);
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
    expect(link.querySelector("u")).toHaveTextContent("[Github Repository]");
    expect(screen.getByRole("link", { name: "https://example.com/docs" })).toHaveAttribute("href", "https://example.com/docs");
    expect(screen.getByRole("link", { name: "查看错误" })).toHaveAttribute("href", "https://example.com/error");
    expect(screen.getAllByRole("link")).toHaveLength(3);
    expect(container.querySelector(".malicious, a a")).toBeNull();
    expect(container.querySelector("[data-slot='terminal-log']")).toHaveTextContent("<img class=\"malicious\" src=x> & text");
    expect(container.textContent).not.toContain("\x1b");
  });

  it("keeps URL-shaped labels inside their OSC 8 destination", async () => {
    settingsRef.current["terminal.rich-style"] = true;
    const { client, emit } = createMockTerminalClient();
    render(<TerminalViewer client={client as any} levels={DEFAULT_LEVELS}/>);
    emit("log", createLog(1, { line: "\x1b]8;;https://example.com/target\x07https://example.com/label\x1b]8;;\x07" }));

    const link = await screen.findByRole("link", { name: "https://example.com/label" });
    expect(link).toHaveAttribute("href", "https://example.com/target");
    expect(screen.getAllByRole("link")).toHaveLength(1);
  });

  it("does not create active links or HTML from unsafe OSC 8 input", async () => {
    settingsRef.current["terminal.rich-style"] = true;
    const { client, emit } = createMockTerminalClient();
    const { container } = render(<TerminalViewer client={client as any} levels={DEFAULT_LEVELS}/>);
    emit("log", createLog(1, { line: "\x1b]8;;javascript:alert(1)\x07<img class='malicious' src=x onerror='alert(1)'>\x1b]8;;\x07" }));

    await waitFor(() => expect(container.querySelector("[data-slot='terminal-log']")).toBeInTheDocument());
    expect(container.querySelector("a, .malicious, [onerror]")).toBeNull();
    expect(container.textContent).toContain("<img class='malicious' src=x onerror='alert(1)'>");
  });

  it("still links ordinary URLs with rich styles disabled", async () => {
    const { client, emit } = createMockTerminalClient();
    render(<TerminalViewer client={client as any} levels={DEFAULT_LEVELS}/>);
    emit("log", createLog(1, { line: "https://example.com/docs?a=1&b=2" }));

    expect(await screen.findByRole("link")).toHaveAttribute("href", "https://example.com/docs?a=1&b=2");
  });

  it("should only show the last n logs when init logs exceed max log lines", async () => {
    const { client, emit } = createMockTerminalClient();
    const { container } = render(<TerminalViewer client={client as any} levels={DEFAULT_LEVELS}/>);

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
    const { container } = render(<TerminalViewer client={client as any} levels={DEFAULT_LEVELS}/>);

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

  it("should only render logs whose line includes the string filter", async () => {
    const { client, emit } = createMockTerminalClient();
    const { container } = render(<TerminalViewer client={client as any} levels={DEFAULT_LEVELS} filter="line-2"/>);

    emit("init", [createLog(1), createLog(2), createLog(3)]);

    await waitFor(() => {
      expect(screen.queryByText("line-2")).toBeInTheDocument();
    });

    expect(container.querySelectorAll("[data-slot='terminal-log']").length).toBe(1);
    expect(screen.queryByText("line-1")).not.toBeInTheDocument();
    expect(screen.queryByText("line-3")).not.toBeInTheDocument();
  });

  it("should only render logs whose line matches the RegExp filter", async () => {
    const { client, emit } = createMockTerminalClient();
    const { container } = render(<TerminalViewer client={client as any} levels={DEFAULT_LEVELS} filter={/line-[13]/}/>);

    emit("init", [createLog(1), createLog(2), createLog(3)]);

    await waitFor(() => {
      expect(container.querySelectorAll("[data-slot='terminal-log']").length).toBe(2);
    });

    expect(screen.getByText("line-1")).toBeInTheDocument();
    expect(screen.queryByText("line-2")).not.toBeInTheDocument();
    expect(screen.getByText("line-3")).toBeInTheDocument();
  });

  it("should render all logs when filter is an empty string", async () => {
    const { client, emit } = createMockTerminalClient();
    const { container } = render(<TerminalViewer client={client as any} levels={DEFAULT_LEVELS} filter=""/>);

    emit("init", [createLog(1), createLog(2), createLog(3)]);

    await waitFor(() => {
      expect(container.querySelectorAll("[data-slot='terminal-log']").length).toBe(3);
    });

    expect(screen.getByText("line-1")).toBeInTheDocument();
    expect(screen.getByText("line-2")).toBeInTheDocument();
    expect(screen.getByText("line-3")).toBeInTheDocument();
  });

  it("should render no logs when string filter matches nothing", async () => {
    const { client, emit } = createMockTerminalClient();
    const { container, rerender } = render(<TerminalViewer client={client as any} levels={DEFAULT_LEVELS} filter="nomatch"/>);

    emit("init", [createLog(1), createLog(2), createLog(3)]);

    // confirm the buffer flushed by widening the filter and waiting for output
    rerender(<TerminalViewer client={client as any} levels={DEFAULT_LEVELS} filter=""/>);
    await waitFor(() => {
      expect(container.querySelectorAll("[data-slot='terminal-log']").length).toBe(3);
    });

    rerender(<TerminalViewer client={client as any} levels={DEFAULT_LEVELS} filter="nomatch"/>);
    expect(container.querySelectorAll("[data-slot='terminal-log']").length).toBe(0);
  });

  it("should apply the filter to logs received after init", async () => {
    const { client, emit } = createMockTerminalClient();
    const { container } = render(<TerminalViewer client={client as any} levels={DEFAULT_LEVELS} filter="line-3"/>);

    emit("init", [createLog(1)]);
    emit("log", createLog(2));
    emit("log", createLog(3));

    await waitFor(() => {
      expect(screen.queryByText("line-3")).toBeInTheDocument();
    });

    expect(container.querySelectorAll("[data-slot='terminal-log']").length).toBe(1);
    expect(screen.queryByText("line-1")).not.toBeInTheDocument();
    expect(screen.queryByText("line-2")).not.toBeInTheDocument();
  });

  it("should escape html tags in log line", async () => {
    const { client, emit } = createMockTerminalClient();
    const { container } = render(<TerminalViewer client={client as any} levels={DEFAULT_LEVELS}/>);

    const htmlTag = "<span class='malicious'>pwned</span>";
    emit("log", createLog(1, { line: htmlTag }));

    await waitFor(() => {
      expect(container.querySelector("[data-slot='terminal-log']")).toBeInTheDocument();
    });

    const log = container.querySelector("[data-slot='terminal-log']");
    expect(container.querySelector(".malicious")).not.toBeInTheDocument();
    expect(log?.innerHTML).not.toContain(htmlTag);
  });

  it("should escape html tags in thrown message", async () => {
    const { client, emit } = createMockTerminalClient();
    const { container } = render(<TerminalViewer client={client as any} levels={DEFAULT_LEVELS}/>);

    const htmlTag = "<div class='malicious'>pwned</div>";
    emit("log", createLog(1, { thrownMessage: htmlTag }));

    await waitFor(() => {
      expect(container.querySelector("[data-slot='terminal-log']")).toBeInTheDocument();
    });

    const log = container.querySelector("[data-slot='terminal-log']");
    expect(container.querySelector(".malicious")).not.toBeInTheDocument();
    expect(log?.innerHTML).not.toContain(htmlTag);
  });

  it("should escape html from init log packets", async () => {
    const { client, emit } = createMockTerminalClient();
    const { container } = render(<TerminalViewer client={client as any} levels={DEFAULT_LEVELS}/>);

    const htmlTag = "<script class='malicious'>alert(1)</script>";
    emit("init", [createLog(1, { line: htmlTag, thrownMessage: htmlTag })]);

    await waitFor(() => {
      expect(container.querySelector("[data-slot='terminal-log']")).toBeInTheDocument();
    });

    expect(container.querySelector("script.malicious")).not.toBeInTheDocument();
  });

  it("should escape html from mcdr log packets", async () => {
    const { client, emit } = createMockTerminalClient();
    const { container } = render(<TerminalViewer client={client as any} levels={DEFAULT_LEVELS}/>);

    const htmlTag = "<img class='malicious' src='x' onerror='alert(1)'/>";
    emit("mcdr-log", createLog(1, { mcdr: true, line: htmlTag }));

    await waitFor(() => {
      expect(container.querySelector("[data-slot='terminal-log']")).toBeInTheDocument();
    });

    expect(container.querySelector("img.malicious")).not.toBeInTheDocument();
  });
});
