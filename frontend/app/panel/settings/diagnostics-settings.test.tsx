import { afterEach, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { DiagnosticsSettings } from "./diagnostics-settings";
import { sendGetRequest, sendPostRequest } from "@/lib/api";
import { toastRestartAlert } from "@/components/restart-alert";
vi.mock("@/lib/api", () => ({ sendGetRequest: vi.fn(), sendPostRequest: vi.fn() }));
vi.mock("@/lib/i18n", () => ({ $: (key: string) => key }));
vi.mock("@/components/restart-alert", () => ({ toastRestartAlert: vi.fn() }));
afterEach(() => { cleanup(); vi.clearAllMocks(); });
const off = { code: 200, error: "", diagnosticsEnabled: false, performanceIncidentsEnabled: false, active: false, restartRequired: false };
it("loads the server settings and requires the master switch before incident recording", async () => {
  vi.mocked(sendGetRequest).mockImplementation(async () => off);
  render(<DiagnosticsSettings/>);
  await waitFor(() => expect(screen.getByRole("switch", { name: "settings.server.diagnostics" })).not.toBeDisabled());
  expect(screen.getByRole("switch", { name: "settings.server.incidents" })).toBeDisabled();
  expect(screen.getByRole("switch", { name: "settings.server.diagnostics" })).not.toBeChecked();
});
it("persists through the API and explains that a restart is required", async () => {
  vi.mocked(sendGetRequest).mockImplementation(async () => off);
  vi.mocked(sendPostRequest).mockImplementation(async () => ({ ...off, diagnosticsEnabled: true, restartRequired: true }));
  render(<DiagnosticsSettings/>);
  const toggle = screen.getByRole("switch", { name: "settings.server.diagnostics" });
  await waitFor(() => expect(toggle).not.toBeDisabled());
  fireEvent.click(toggle);
  await waitFor(() => expect(sendPostRequest).toHaveBeenCalledWith("/api/diagnostics/settings", { diagnosticsEnabled: true, performanceIncidentsEnabled: false }));
  await waitFor(() => expect(toggle).toBeChecked());
  expect(screen.getByRole("status")).toHaveTextContent("settings.server.diagnostics.restart");
  expect(toastRestartAlert).toHaveBeenCalled();
});
it("does not report an enabled switch when saving fails", async () => {
  vi.mocked(sendGetRequest).mockImplementation(async () => off);
  vi.mocked(sendPostRequest).mockRejectedValue(new Error("offline"));
  render(<DiagnosticsSettings/>);
  const toggle = screen.getByRole("switch", { name: "settings.server.diagnostics" });
  await waitFor(() => expect(toggle).not.toBeDisabled());
  fireEvent.click(toggle);
  await waitFor(() => expect(screen.getByRole("alert")).toBeInTheDocument());
  expect(toggle).not.toBeChecked();
});
