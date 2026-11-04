import { afterEach, expect, it, vi } from "vitest";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { PerformanceIncidents } from "./performance-incidents";
import { sendGetRequest } from "@/lib/api";
vi.mock("@/lib/api", () => ({ sendGetRequest: vi.fn() }));
vi.mock("@/lib/i18n", () => ({ $: (key: string) => key }));
afterEach(() => { cleanup(); vi.clearAllMocks(); });
it("shows unavailable sampling separately from a healthy empty result", async () => {
  vi.mocked(sendGetRequest).mockImplementation(async () => ({ code: 200, error: "", available: false, incidents: [] }));
  render(<PerformanceIncidents />);
  await waitFor(() => expect(screen.getByRole("status")).toHaveTextContent("monitor.incidents.unavailable"));
  expect(screen.queryByText("monitor.incidents.empty")).toBeNull();
});
it("shows actual recorded evidence and preserves interrupted status", async () => {
  vi.mocked(sendGetRequest).mockImplementation(async () => ({ code: 200, error: "", available: true, incidents: [{
    id: "test-incident", startedAt: 1000, endedAt: 2000, status: "interrupted",
    samples: [{ timestamp: 1000, meanTickMs: 80.25 }]
  }] }));
  render(<PerformanceIncidents />);
  await waitFor(() => expect(screen.getByText("80.25 ms")).toBeInTheDocument());
  expect(screen.getByText(/monitor.incidents.interrupted/)).toBeInTheDocument();
});
it("reports request errors without presenting them as no incidents", async () => {
  vi.mocked(sendGetRequest).mockRejectedValue(new Error("offline"));
  render(<PerformanceIncidents />);
  await waitFor(() => expect(screen.getByRole("alert")).toHaveTextContent("monitor.incidents.error"));
  expect(screen.queryByText("monitor.incidents.empty")).toBeNull();
});
it("distinguishes disabled collection from unavailable sampling", async () => {
  vi.mocked(sendGetRequest).mockImplementation(async () => ({ code: 200, error: "", available: false, enabled: false, incidents: [] }));
  render(<PerformanceIncidents />);
  await waitFor(() => expect(screen.getByRole("status")).toHaveTextContent("monitor.incidents.disabled"));
});
it("opens the existing history for the incident window", async () => {
  vi.mocked(sendGetRequest).mockImplementation(async () => ({ code: 200, error: "", available: true, incidents: [{
    id: "history-event", startedAt: 120000, endedAt: 180000, status: "recovered", samples: []
  }] }));
  const onViewHistory = vi.fn();
  render(<PerformanceIncidents onViewHistory={onViewHistory}/>);
  await waitFor(() => expect(screen.getByText(/monitor.incidents.recovered/)).toBeInTheDocument());
  const button = screen.getByText("monitor.incidents.view-history");
  button.click();
  expect(onViewHistory).toHaveBeenCalledWith(60000, 240000);
});
