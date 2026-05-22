import type { ReactNode } from "react";
import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import userEvent from "@testing-library/user-event";
import { toast } from "sonner";
import Settings from "./page";

const mockReplace = vi.fn();
let mockTab: string | null = null;
let mockPathname: string;
let mockQueryString: string;
let mockHasOpenLaunchCommand = false;

vi.mock("next/navigation", () => ({
  usePathname: () => mockPathname,
  useRouter: () => ({ replace: mockReplace }),
  useSearchParams: () => ({
    get: (key: string) => (key === "tab" ? mockTab : null),
    has: (key: string) => key === "openLaunchCommand" ? mockHasOpenLaunchCommand : false,
    toString: () => mockQueryString
  })
}));

vi.mock("@/lib/settings", () => ({
  getSettings: vi.fn((key: string) => {
    if(key === "dashboard.monitor-interval" || key === "terminal.font-size" || key === "monaco.font-size") return 14;
    if(key === "terminal.max-log-lines" || key === "code-of-conduct.auto-saving-interval") return 1000;
    if(key === "terminal.log-level") return "INFO";
    if(key === "system.language") return "zh-CN";
    return "";
  }),
  changeSettings: vi.fn(),
  resetSettings: vi.fn()
}));

vi.mock("../sub-page", () => ({
  SubPage: ({ children }: { children: ReactNode }) => <div data-testid="sub-page">{children}</div>
}));

vi.mock("@/lib/api", () => ({
  apiUrl: "",
  sendDeleteRequest: vi.fn(),
  sendGetRequest: vi.fn(() => Promise.resolve({ enabled: false })),
  sendPostRequest: vi.fn(() => Promise.resolve()),
  toastError: vi.fn()
}));

describe("test settings page", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockPathname = "/panel/settings";
    mockQueryString = "";
    mockHasOpenLaunchCommand = false;
  });

  afterEach(() => {
    cleanup();
    mockTab = null;
  });

  it("should select general tab when URL has no tab query", () => {
    mockTab = null;
    mockQueryString = "";

    render(<Settings />);

    const selectedTab = screen.getByRole("tab", { selected: true });
    expect(selectedTab).toHaveTextContent("[settings.general.title]");
  });

  it("should select general tab when URL has invalid tab query", () => {
    mockTab = "invalid-tab";
    mockQueryString = "tab=invalid-tab";

    render(<Settings />);

    const selectedTab = screen.getByRole("tab", { selected: true });
    expect(selectedTab).toHaveTextContent("[settings.general.title]");
  });

  it("should select the tab matching the tab query when valid", () => {
    mockTab = "terminal";
    mockQueryString = "tab=terminal";

    render(<Settings />);

    const selectedTab = screen.getByRole("tab", { selected: true });
    expect(selectedTab).toHaveTextContent("[settings.terminal.title]");
  });

  it("should call replace with new tab in query when user switches tab", async () => {
    mockTab = null;
    mockQueryString = "";

    render(<Settings />);

    const terminalTab = screen.getByRole("tab", { name: "[settings.terminal.title]" });
    await userEvent.click(terminalTab);

    expect(mockReplace).toHaveBeenCalledWith("/panel/settings?tab=terminal");
  });

  it("should preserve other query params when switching tab", async () => {
    mockTab = "general";
    mockQueryString = "tab=general&foo=bar";

    render(<Settings />);

    const terminalTab = screen.getByRole("tab", { name: "[settings.terminal.title]" });
    await userEvent.click(terminalTab);

    const callUrl = mockReplace.mock.calls[0][0];
    expect(callUrl).toContain("tab=terminal");
    expect(callUrl).toContain("foo=bar");
  });

  it("should switch to server tab and remove openLaunchCommand from URL when openLaunchCommand param is present", () => {
    mockHasOpenLaunchCommand = true;
    mockTab = null;
    mockQueryString = "openLaunchCommand";

    render(<Settings />);

    expect(mockReplace).toHaveBeenCalledWith("/panel/settings?");
  });

  it("should show warning toast when openLaunchCommand param is present", () => {
    mockHasOpenLaunchCommand = true;
    mockTab = null;
    mockQueryString = "openLaunchCommand";

    render(<Settings />);

    expect(vi.mocked(toast.warning)).toHaveBeenCalledWith("[settings.server.launch-command.required]");
  });
});
