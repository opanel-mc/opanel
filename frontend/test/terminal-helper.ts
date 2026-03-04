import type { CommandShortcut } from "@/lib/types";
import { vi } from "vitest";

type Handler = (data: unknown) => void;

export function createMockTerminalWsClient() {
  const handlers = new Map<string, Handler[]>();

  const subscribe = vi.fn((type: string, cb: Handler) => {
    const current = handlers.get(type) ?? [];
    current.push(cb);
    handlers.set(type, current);
  });

  const send = vi.fn();
  const close = vi.fn();

  const emit = (type: string, data: unknown) => {
    const current = handlers.get(type) ?? [];
    for(const handler of current) {
      handler(data);
    }
  };

  return {
    client: {
      subscribe,
      send,
      close
    },
    emit,
    handlers
  };
}

export function createTerminalSettingsState(overrides?: {
  history?: string[]
  shortcuts?: CommandShortcut[]
}) {
  return {
    "state.terminal.history": overrides?.history ?? ["list", "time set day"],
    "terminal.shortcuts": overrides?.shortcuts ?? [
      { name: "Set Day", command: "time set day" }
    ],
    "terminal.autocomplete": true,
    "terminal.log-level": "INFO"
  };
}
