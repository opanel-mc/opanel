import type { ReactNode } from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { toast } from "sonner";
import { createMockTerminalWsClient, createTerminalSettingsState } from "@/test/terminal-helper";
import { createMockVersionContext } from "@/test/contexts-helper";
import { VersionContext } from "@/contexts/api-context";
import Terminal from "./page";

const { wsRef, settingsRef, changeSettingsSpy } = vi.hoisted(() => ({
  wsRef: {
    current: null as ReturnType<typeof createMockTerminalWsClient> | null
  },
  settingsRef: {
    current: null as ReturnType<typeof createTerminalSettingsState> | null
  },
  changeSettingsSpy: vi.fn()
}));

vi.mock("@/hooks/use-websocket", () => ({
  useWebSocket: () => wsRef.current?.client ?? null
}));

vi.mock("@/lib/settings", () => ({
  getSettings: (key: keyof ReturnType<typeof createTerminalSettingsState>) => settingsRef.current?.[key],
  changeSettings: (key: keyof ReturnType<typeof createTerminalSettingsState>, value: unknown) => {
    changeSettingsSpy(key, value);
    if(settingsRef.current) {
      (settingsRef.current as Record<string, unknown>)[key] = value;
    }
  }
}));

vi.mock("../sub-page", () => ({
  SubPage: ({ children }: { children: ReactNode }) => <div data-testid="terminal-sub-page">{children}</div>
}));

vi.mock("@/components/terminal-viewer", () => ({
  TerminalViewer: () => <div data-testid="terminal-viewer"/>
}));

vi.mock("@/components/autocomplete-input", () => ({
  AutocompleteInput: ({ ref, onInput, onKeyDown, itemList, enabled, prefix, ...props }: any) => (
    <input
      data-testid="terminal-input"
      data-prefix={prefix}
      data-item-list={JSON.stringify(itemList)}
      {...props}
      ref={(elem) => {
        if(ref) {
          ref.current = elem;
        }
      }}
      onInput={onInput}
      onKeyDown={onKeyDown}/>
  )
}));

vi.mock("./create-shortcut-dialog", () => ({
  CreateShortcutDialog: ({ children, onCreate }: { children: ReactNode, onCreate?: (data: { name: string, command: string }) => void }) => (
    <>
      {children}
      <button
        type="button"
        onClick={() => onCreate?.({ name: "Hello Shortcut", command: "say hello" })}>
        create-shortcut
      </button>
    </>
  )
}));

describe("test terminal page", () => {
  afterEach(() => {
    cleanup();
  });

  beforeEach(() => {
    vi.clearAllMocks();
    wsRef.current = createMockTerminalWsClient();
    settingsRef.current = createTerminalSettingsState();
  });

  it("should subscribe terminal websocket and send autocomplete request", async () => {
    render(<Terminal />);

    await waitFor(() => {
      expect(wsRef.current?.client.subscribe).toHaveBeenCalledWith("autocomplete", expect.any(Function));
    });

    const input = screen.getByTestId("terminal-input");
    fireEvent.input(input, { target: { value: "/say hi" } });

    await waitFor(() => {
      expect(wsRef.current?.client.send).toHaveBeenCalledWith(
        "autocomplete",
        expect.objectContaining({ command: "say hi" })
      );
    });
  });

  it("should send command and persist history when pressing Enter", async () => {
    render(<Terminal />);

    const input = screen.getByTestId("terminal-input");
    fireEvent.input(input, { target: { value: "say test" } });
    fireEvent.keyDown(input, { key: "Enter" });

    await waitFor(() => {
      expect(wsRef.current?.client.send).toHaveBeenCalledWith("command", "say test");
    });

    expect(screen.getByRole("button", { name: "say test" })).toBeInTheDocument();
    expect(changeSettingsSpy).toHaveBeenCalledWith(
      "state.terminal.history",
      expect.arrayContaining(["say test"])
    );
  });

  it("should persist multiple commands in order in history", async () => {
    settingsRef.current = createTerminalSettingsState({
      history: ["help"],
      shortcuts: [{ name: "Set Day", command: "time set day" }]
    });
    render(<Terminal />);

    const input = screen.getByTestId("terminal-input");
    fireEvent.input(input, { target: { value: "say one" } });
    fireEvent.keyDown(input, { key: "Enter" });
    fireEvent.input(input, { target: { value: "say two" } });
    fireEvent.keyDown(input, { key: "Enter" });
    fireEvent.input(input, { target: { value: "say three" } });
    fireEvent.keyDown(input, { key: "Enter" });

    await waitFor(() => {
      expect(wsRef.current?.client.send).toHaveBeenCalledWith("command", "say one");
      expect(wsRef.current?.client.send).toHaveBeenCalledWith("command", "say two");
      expect(wsRef.current?.client.send).toHaveBeenCalledWith("command", "say three");
    });

    expect(changeSettingsSpy).toHaveBeenLastCalledWith(
      "state.terminal.history",
      ["help", "say one", "say two", "say three"]
    );

    expect(screen.getByRole("button", { name: "say one" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "say two" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "say three" })).toBeInTheDocument();
  });

  it("should focus input and fill command from history on click, then execute on double click", async () => {
    settingsRef.current = createTerminalSettingsState({
      history: ["say from history"],
      shortcuts: [{ name: "Set Day", command: "time set day" }]
    });
    render(<Terminal />);

    const historyButton = screen.getByRole("button", { name: "say from history" });
    const input = screen.getByTestId("terminal-input") as HTMLInputElement;
    input.blur();

    fireEvent.click(historyButton);

    expect(input.value).toBe("say from history");
    expect(input).toHaveFocus();
    expect(wsRef.current?.client.send).not.toHaveBeenCalledWith("command", "say from history");

    fireEvent.doubleClick(historyButton);
    await waitFor(() => {
      expect(wsRef.current?.client.send).toHaveBeenCalledWith("command", "say from history");
    });
  });

  it("should warn when trying to send empty command", () => {
    render(<Terminal />);

    const input = screen.getByTestId("terminal-input");
    fireEvent.keyDown(input, { key: "Enter" });

    expect(toast.warning).toHaveBeenCalled();
    expect(wsRef.current?.client.send).not.toHaveBeenCalledWith("command", expect.any(String));
  });

  describe("mcdr command autocomplete", () => {
    const mcdrVersionCtx = createMockVersionContext({ mcdr: true });

    it("should set autocomplete item list to MCDR commands when typing MCDR prefix", async () => {
      render(
        <VersionContext.Provider value={mcdrVersionCtx}>
          <Terminal />
        </VersionContext.Provider>
      );

      const input = screen.getByTestId("terminal-input");
      fireEvent.input(input, { target: { value: "!!" } });

      await waitFor(() => {
        const itemList = JSON.parse(input.getAttribute("data-item-list") ?? "[]") as string[];
        expect(itemList).toContain("MCDR");
        expect(itemList).toContain("help");
      });
    });

    it("should switch AutocompleteInput prefix to !! when typing MCDR command, and revert to / after clearing it", async () => {
      render(
        <VersionContext.Provider value={mcdrVersionCtx}>
          <Terminal />
        </VersionContext.Provider>
      );

      const input = screen.getByTestId("terminal-input");

      expect(input).toHaveAttribute("data-prefix", "/");

      fireEvent.input(input, { target: { value: "!!" } });
      await waitFor(() => {
        expect(input).toHaveAttribute("data-prefix", "!!");
      });

      fireEvent.input(input, { target: { value: "/say hi" } });
      await waitFor(() => {
        expect(input).toHaveAttribute("data-prefix", "/");
      });
    });

    it("should not send WebSocket autocomplete request when typing MCDR command prefix", () => {
      render(
        <VersionContext.Provider value={mcdrVersionCtx}>
          <Terminal />
        </VersionContext.Provider>
      );

      const input = screen.getByTestId("terminal-input");
      fireEvent.input(input, { target: { value: "!!MCDR" } });

      expect(wsRef.current?.client.send).not.toHaveBeenCalledWith("autocomplete", expect.any(Object));
    });

    it("should resume WebSocket autocomplete after switching from MCDR prefix to normal command", async () => {
      render(
        <VersionContext.Provider value={mcdrVersionCtx}>
          <Terminal />
        </VersionContext.Provider>
      );

      const input = screen.getByTestId("terminal-input");
      fireEvent.input(input, { target: { value: "!!MCDR" } });
      fireEvent.input(input, { target: { value: "/say hi" } });

      await waitFor(() => {
        expect(wsRef.current?.client.send).toHaveBeenCalledWith(
          "autocomplete",
          expect.objectContaining({ command: "say hi" })
        );
      });
    });

    it("should not trigger MCDR mode when mcdr is disabled even if input starts with !!", async () => {
      render(
        <VersionContext.Provider value={createMockVersionContext({ mcdr: false })}>
          <Terminal />
        </VersionContext.Provider>
      );

      const input = screen.getByTestId("terminal-input");
      fireEvent.input(input, { target: { value: "!!MCDR" } });

      await waitFor(() => {
        expect(input).toHaveAttribute("data-prefix", "/");
      });

      const itemList = JSON.parse(input.getAttribute("data-item-list") ?? "[]") as string[];
      expect(itemList).not.toContain("MCDR");
      expect(itemList).not.toContain("help");
    });

    it("should not trigger MCDR mode when input starts with single ! (not !!)", async () => {
      render(
        <VersionContext.Provider value={mcdrVersionCtx}>
          <Terminal />
        </VersionContext.Provider>
      );

      const input = screen.getByTestId("terminal-input");
      fireEvent.input(input, { target: { value: "!single" } });

      await waitFor(() => {
        expect(input).toHaveAttribute("data-prefix", "/");
      });

      const itemList = JSON.parse(input.getAttribute("data-item-list") ?? "[]") as string[];
      expect(itemList).not.toContain("MCDR");
      expect(itemList).not.toContain("help");
    });

    it("should reset autocomplete item list when clearing MCDR prefix back to empty", async () => {
      render(
        <VersionContext.Provider value={mcdrVersionCtx}>
          <Terminal />
        </VersionContext.Provider>
      );

      const input = screen.getByTestId("terminal-input");
      fireEvent.input(input, { target: { value: "!!" } });

      await waitFor(() => {
        const itemList = JSON.parse(input.getAttribute("data-item-list") ?? "[]") as string[];
        expect(itemList).toContain("MCDR");
      });

      fireEvent.input(input, { target: { value: "" } });

      await waitFor(() => {
        const itemList = JSON.parse(input.getAttribute("data-item-list") ?? "[]") as string[];
        expect(itemList).toEqual([]);
        expect(input).toHaveAttribute("data-prefix", "/");
      });
    });
  });

  it("should create shortcut and execute it only on double click", async () => {
    render(<Terminal />);

    const input = screen.getByTestId("terminal-input") as HTMLInputElement;
    const setDayButton = screen.getByRole("button", { name: "Set Day" });
    input.blur();
    fireEvent.click(setDayButton);

    expect(input.value).toBe("time set day");
    expect(input).toHaveFocus();
    expect(wsRef.current?.client.send).not.toHaveBeenCalledWith("command", "time set day");

    fireEvent.doubleClick(setDayButton);
    await waitFor(() => {
      expect(wsRef.current?.client.send).toHaveBeenCalledWith("command", "time set day");
    });

    fireEvent.click(screen.getByRole("button", { name: "create-shortcut" }));
    expect(screen.getByRole("button", { name: "Hello Shortcut" })).toBeInTheDocument();

    expect(changeSettingsSpy).toHaveBeenCalledWith(
      "terminal.shortcuts",
      expect.arrayContaining([
        expect.objectContaining({ name: "Hello Shortcut", command: "say hello" })
      ])
    );

    const helloShortcutButton = screen.getByRole("button", { name: "Hello Shortcut" });
    input.blur();
    fireEvent.click(helloShortcutButton);

    expect(input.value).toBe("say hello");
    expect(input).toHaveFocus();
    expect(wsRef.current?.client.send).not.toHaveBeenCalledWith("command", "say hello");

    fireEvent.doubleClick(helloShortcutButton);
    await waitFor(() => {
      expect(wsRef.current?.client.send).toHaveBeenCalledWith("command", "say hello");
    });

    expect(changeSettingsSpy).toHaveBeenCalledWith(
      "state.terminal.history",
      expect.arrayContaining(["say hello"])
    );
  });
});
