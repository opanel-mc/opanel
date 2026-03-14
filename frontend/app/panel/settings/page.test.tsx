import type { ReactNode } from "react";
import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import userEvent from "@testing-library/user-event";
import Settings from "./page";

const mockReplace = vi.fn();
let mockTab: string | null = null;
let mockPathname: string;
let mockQueryString: string;

vi.mock("next/navigation", () => ({
  usePathname: () => mockPathname,
  useRouter: () => ({ replace: mockReplace }),
  useSearchParams: () => ({
    get: (key: string) => (key === "tab" ? mockTab : null),
    toString: () => mockQueryString
  })
}));

vi.mock("@/lib/settings", () => ({
  getSettings: vi.fn((key: string) => {
    if (key === "dashboard.monitor-interval" || key === "terminal.font-size" || key === "monaco.font-size") return 14;
    if (key === "terminal.max-log-lines" || key === "code-of-conduct.auto-saving-interval") return 1000;
    if (key === "terminal.log-level") return "INFO";
    if (key === "system.language") return "zh-CN";
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
  sendDeleteRequest: vi.fn()
}));

describe("test settings page", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockPathname = "/panel/settings";
    mockQueryString = "";
  });

  afterEach(() => {
    cleanup();
    mockTab = null;
  });

  it("should select dashboard tab when URL has no tab query", () => {
    mockTab = null;
    mockQueryString = "";

    render(<Settings />);

    const selectedTab = screen.getByRole("tab", { selected: true });
    expect(selectedTab).toHaveTextContent("[settings.dashboard.title]");
  });

  it("should select dashboard tab when URL has invalid tab query", () => {
    mockTab = "invalid-tab";
    mockQueryString = "tab=invalid-tab";

    render(<Settings />);

    const selectedTab = screen.getByRole("tab", { selected: true });
    expect(selectedTab).toHaveTextContent("[settings.dashboard.title]");
  });

  it("should select the tab matching the tab query when valid", () => {
    mockTab = "players";
    mockQueryString = "tab=players";

    render(<Settings />);

    const selectedTab = screen.getByRole("tab", { selected: true });
    expect(selectedTab).toHaveTextContent("[settings.players.title]");
  });

  it("should select opanel tab when tab=opanel in URL", () => {
    mockTab = "opanel";
    mockQueryString = "tab=opanel";

    render(<Settings />);

    const selectedTab = screen.getByRole("tab", { selected: true });
    expect(selectedTab).toHaveTextContent("OPanel");
  });

  it("should call replace with new tab in query when user switches tab", async () => {
    mockTab = null;
    mockQueryString = "";

    render(<Settings />);

    const playersTab = screen.getByRole("tab", { name: "[settings.players.title]" });
    await userEvent.click(playersTab);

    expect(mockReplace).toHaveBeenCalledWith("/panel/settings?tab=players");
  });

  it("should preserve other query params when switching tab", async () => {
    mockTab = "dashboard";
    mockQueryString = "tab=dashboard&foo=bar";

    render(<Settings />);

    const playersTab = screen.getByRole("tab", { name: "[settings.players.title]" });
    await userEvent.click(playersTab);

    const callUrl = mockReplace.mock.calls[0][0];
    expect(callUrl).toContain("tab=players");
    expect(callUrl).toContain("foo=bar");
  });
});
