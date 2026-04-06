import type { ReactNode } from "react";
import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import Dashboard from "./page";

const subPagePropsRef = vi.hoisted(() => ({
  className: "",
  pageClassName: ""
}));

vi.mock("@/lib/api", () => ({
  sendGetRequest: vi.fn((path: string) => {
    if(path === "/api/info") return Promise.resolve({ version: "2.0.0" });
    return Promise.resolve({ cpu: 0, memory: 0, tps: 20 });
  }),
  toastError: vi.fn()
}));

vi.mock("@/lib/utils", () => ({
  getCurrentState: vi.fn(() => Promise.resolve(new Array(50).fill({ cpu: 0, memory: 0, tps: 20 })))
}));

vi.mock("@/lib/settings", () => ({
  getSettings: vi.fn(() => 1000)
}));

vi.mock("../sub-page", () => ({
  SubPage: ({ children, className, pageClassName }: { children: ReactNode, className?: string, pageClassName?: string }) => {
    subPagePropsRef.className = className ?? "";
    subPagePropsRef.pageClassName = pageClassName ?? "";
    return <div data-testid="dashboard-sub-page">{children}</div>;
  }
}));

vi.mock("./info-card", () => ({
  InfoCard: ({ className }: { className?: string }) => <div className={className} data-testid="info-card"/>
}));
vi.mock("./time-card", () => ({ TimeCard: () => <div data-testid="time-card"/> }));
vi.mock("./players-card", () => ({ PlayersCard: ({ className }: { className?: string }) => <div className={className} data-testid="players-card"/> }));
vi.mock("./monitor-card", () => ({ MonitorCard: ({ className }: { className?: string }) => <div className={className} data-testid="monitor-card"/> }));
vi.mock("./terminal-card", () => ({ TerminalCard: ({ className }: { className?: string }) => <div className={className} data-testid="terminal-card"/> }));
vi.mock("./tps-card", () => ({ TPSCard: () => <div data-testid="tps-card"/> }));
vi.mock("./system-card", () => ({ SystemCard: ({ className }: { className?: string }) => <div className={className} data-testid="system-card"/> }));

describe("test dashboard page", () => {
  afterEach(() => cleanup());

  beforeEach(() => {
    vi.clearAllMocks();
    subPagePropsRef.className = "";
    subPagePropsRef.pageClassName = "";
  });

  it("should use desktop-only full-height class on dashboard layout", () => {
    render(<Dashboard />);

    expect(screen.getByTestId("dashboard-sub-page")).toBeInTheDocument();
    const classList = subPagePropsRef.className.split(" ").filter(Boolean);
    expect(classList).toContain("min-xl:h-full");
    expect(classList).not.toContain("h-full");
  });

  it("should only hide right-side overflow on xl and larger screens", () => {
    render(<Dashboard />);

    const rightColumn = screen.getByTestId("system-card").parentElement;
    expect(rightColumn).toHaveClass("min-xl:overflow-hidden");
    expect(rightColumn).not.toHaveClass("overflow-hidden");
  });

  it("should keep terminal card minimum height on small screens", () => {
    render(<Dashboard />);

    expect(screen.getByTestId("terminal-card")).toHaveClass("max-xl:min-h-96");
  });
});
