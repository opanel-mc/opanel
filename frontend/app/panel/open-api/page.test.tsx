import type { ReactNode } from "react";
import userEvent from "@testing-library/user-event";
import { act, cleanup, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import OpenAPI from "./page";

const {
  copyToClipboardMock,
  sendGetRequestMock,
  sendPostRequestMock,
  toastErrorMock
} = vi.hoisted(() => ({
  copyToClipboardMock: vi.fn(),
  sendGetRequestMock: vi.fn(),
  sendPostRequestMock: vi.fn(),
  toastErrorMock: vi.fn()
}));

vi.mock("@/lib/api", () => ({
  sendGetRequest: sendGetRequestMock,
  sendPostRequest: sendPostRequestMock,
  toastError: toastErrorMock
}));

vi.mock("@/lib/utils", async () => {
  const actual = await vi.importActual<Record<string, unknown>>("@/lib/utils");
  return {
    ...actual,
    copyToClipboard: copyToClipboardMock
  };
});

vi.mock("../sub-page", () => ({
  SubPage: ({ children }: { children: ReactNode }) => <div>{children}</div>
}));

vi.mock("@/components/config-item", () => ({
  ConfigSection: ({ children }: { children: ReactNode }) => <section>{children}</section>,
  ConfigItem: ({ children }: { children: ReactNode }) => <div>{children}</div>
}));

vi.mock("@/components/i18n-text", () => ({
  Text: () => <span/>
}));

vi.mock("@/hooks/use-loading-done", () => ({
  useLoadingDone: vi.fn()
}));

vi.mock("./interface", () => ({
  InterfaceSection: ({ children }: { children: ReactNode }) => <section>{children}</section>,
  Interface: ({ children }: { children: ReactNode }) => <article>{children}</article>,
  InterfaceDescription: ({ children }: { children: ReactNode }) => <span>{children}</span>,
  InterfaceRequest: () => null,
  InterfaceResponse: () => null
}));

describe("Open API page", () => {
  afterEach(() => cleanup());

  beforeEach(() => {
    vi.clearAllMocks();
    copyToClipboardMock.mockResolvedValue(undefined);
    sendPostRequestMock.mockResolvedValue(undefined);
    sendGetRequestMock.mockImplementation((route: string) => {
      if(route === "/api/open-api") return Promise.resolve({ enabled: true });

      const interfaceName = route.substring(route.lastIndexOf("/") + 1);
      return Promise.resolve({
        enabled: interfaceName === "info" || interfaceName === "players"
      });
    });
  });

  it("copies a localized Markdown prompt containing only enabled interfaces", async () => {
    const user = userEvent.setup();
    render(<OpenAPI />);

    const copyButton = await screen.findByRole("button", {
      name: "[open-api.prompt.copy]"
    });
    await waitFor(() => expect(copyButton).toBeEnabled());
    await user.click(copyButton);

    expect(copyToClipboardMock).toHaveBeenCalledTimes(1);
    const prompt = copyToClipboardMock.mock.calls[0][0] as string;
    expect(prompt).toMatch(/^# \[open-api\.interfaces\.title\]/);
    expect(prompt).toContain("[open-api.prompt.description]");
    expect(prompt).toContain("### `GET /open-api/info`");
    expect(prompt).toContain("### `GET /open-api/players`");
    expect(prompt).toContain("### `GET /open-api/players/{uuid}`");
    expect(prompt).not.toContain("/open-api/monitor");
    expect(prompt).not.toContain("/open-api/plugins");
    expect(prompt).not.toContain("/open-api/logs");
  });

  it("keeps copying disabled until every interface status is loaded", async () => {
    let resolveLogs: (value: { enabled: boolean }) => void = () => {};
    sendGetRequestMock.mockImplementation((route: string) => {
      if(route === "/api/open-api") return Promise.resolve({ enabled: true });
      if(route === "/api/open-api/logs") {
        return new Promise((resolve) => {
          resolveLogs = resolve;
        });
      }
      return Promise.resolve({ enabled: true });
    });

    render(<OpenAPI />);

    const copyButton = await screen.findByRole("button", {
      name: "[open-api.prompt.copy]"
    });
    expect(copyButton).toBeDisabled();

    await act(async () => resolveLogs({ enabled: false }));
    await waitFor(() => expect(copyButton).toBeEnabled());
  });
});
