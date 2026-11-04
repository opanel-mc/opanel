import { afterEach, expect, it, vi } from "vitest";
import { act, cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { PlayerMarkers } from "./player-markers";
import { sendGetRequest } from "@/lib/api";
vi.mock("@/lib/api", () => ({ sendGetRequest: vi.fn() }));
vi.mock("@/lib/settings", () => ({ getSettings: () => "https://avatar.test/" }));
vi.mock("@/lib/i18n", () => ({ $: (key: string) => key }));
vi.mock("@/assets/images/steve-avatar.png", () => ({ default: { src: "/steve.png" } }));
afterEach(() => { cleanup(); vi.clearAllMocks(); vi.unstubAllGlobals(); vi.useRealTimers(); });
const viewport = { current: { camera: { x: 0, z: 0 }, zoom: 2, viewportPx: { width: 200, height: 100 } } };
const players = [
  { uuid: "a", name: "Alex", world: "world", x: 1, y: 64, z: 2 },
  { uuid: "b", name: "Nether", world: "world_nether", x: 1, y: 64, z: 2 },
];
it("expands the online list and locates same-world players without locating other worlds", async () => {
  setup();
  vi.mocked(sendGetRequest).mockImplementation(async () => ({ code: 200, error: "", available: true, sampledAt: Date.now(), players }));
  const locate = vi.fn();
  const zoom = vi.fn();
  render(<div onWheel={zoom}><PlayerMarkers visible save="world" viewport={viewport} onLocate={locate}/></div>);
  fireEvent.click(screen.getByRole("button", { name: /map.players.title/ }));
  const list = await screen.findByRole("region", { name: "map.players.online" });
  fireEvent.wheel(list, { deltaY: 100 });
  expect(zoom).not.toHaveBeenCalled();
  fireEvent.click(within(list).getByRole("button", { name: /Alex/ }));
  expect(locate).toHaveBeenCalledWith(players[0]);
  expect(within(list).getByRole("button", { name: /Nether/ })).toBeDisabled();
  fireEvent.click(screen.getByRole("button", { name: /map.players.title/ }));
  expect(screen.queryByRole("region", { name: "map.players.online" })).toBeNull();
  expect(screen.queryByRole("switch")).toBeNull();
});
it("polls every five seconds and keeps markers between updates", async () => {
  vi.useFakeTimers();
  setup();
  vi.mocked(sendGetRequest).mockImplementation(async () => ({ code: 200, error: "", available: true, sampledAt: Date.now(), players }));
  render(<PlayerMarkers visible save="world" viewport={viewport}/>);
  await act(async () => {});
  expect(sendGetRequest).toHaveBeenCalledTimes(1);
  await act(async () => { await vi.advanceTimersByTimeAsync(4999); });
  expect(sendGetRequest).toHaveBeenCalledTimes(1);
  expect(screen.getByAltText("Alex")).toBeInTheDocument();
  await act(async () => { await vi.advanceTimersByTimeAsync(1); });
  expect(sendGetRequest).toHaveBeenCalledTimes(2);
  expect(screen.getByAltText("Alex")).toBeInTheDocument();
});
function setup() {
  vi.stubGlobal("requestAnimationFrame", vi.fn(() => 1));
  vi.stubGlobal("cancelAnimationFrame", vi.fn());
}
it("does not fetch or show player positions by default", () => {
  setup();
  render(<PlayerMarkers save="world" viewport={viewport}/>);
  expect(sendGetRequest).not.toHaveBeenCalled();
  expect(screen.queryByRole("button")).toBeNull();
});
it("shows same-world avatars and filters immediately when the map world changes", async () => {
  setup();
  vi.mocked(sendGetRequest).mockImplementation(async () => ({ code: 200, error: "", available: true, sampledAt: Date.now(), players }));
  const view = render(<PlayerMarkers visible save="world" viewport={viewport}/>);
  await waitFor(() => expect(screen.getByAltText("Alex")).toBeInTheDocument());
  expect(screen.queryByAltText("Nether")).toBeNull();
  view.rerender(<PlayerMarkers visible save="world_nether" viewport={viewport}/>);
  expect(screen.getByAltText("Nether")).toBeInTheDocument();
  expect(screen.queryByAltText("Alex")).toBeNull();
});
it("uses the existing avatar provider and falls back to Steve", async () => {
  setup();
  vi.mocked(sendGetRequest).mockImplementation(async () => ({ code: 200, error: "", available: true, sampledAt: Date.now(), players }));
  const view = render(<PlayerMarkers visible save="world" viewport={viewport}/>);
  const avatar = await screen.findByAltText("Alex");
  expect(avatar).toHaveAttribute("src", "https://avatar.test/Alex");
  fireEvent.error(avatar);
  expect(avatar).toHaveAttribute("src", "/steve.png");
  view.rerender(<PlayerMarkers visible={false} save="world" viewport={viewport}/>);
  expect(screen.queryByAltText("Alex")).toBeNull();
});
it("does not show stale server positions", async () => {
  setup();
  vi.mocked(sendGetRequest).mockImplementation(async () => ({ code: 200, error: "", available: false, sampledAt: 1, players }));
  render(<PlayerMarkers visible save="world" viewport={viewport}/>);
  await waitFor(() => expect(screen.getByRole("status")).toHaveTextContent("map.players.unavailable"));
  expect(screen.queryByAltText("Alex")).toBeNull();
});
