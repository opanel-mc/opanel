"use client";

import type { MapViewport } from "./map-viewport";
import { useEffect, useState, type RefObject } from "react";
import Link from "next/link";
import { ChevronDown } from "lucide-react";
import { Button } from "@/components/ui/button";
import { sendGetRequest } from "@/lib/api";
import { getSettings } from "@/lib/settings";
import { $ } from "@/lib/i18n";
import SteveAvatar from "@/assets/images/steve-avatar.png";
import { playersInWorld, playerScreenPosition, type MapPlayer } from "./player-marker-data";

type Positions = { available: boolean; enabled?: boolean; sampledAt: number; players: MapPlayer[] };
export function PlayerMarkers({ save, viewport, onLocate, visible = false }: { visible?: boolean; save: string; viewport: RefObject<MapViewport>; onLocate?: (player: MapPlayer) => void }) {
  const [data, setData] = useState<Positions>();
  const [failed, setFailed] = useState(false);
  const [selected, setSelected] = useState<string>();
  const [listOpen, setListOpen] = useState(false);
  useEffect(() => { setData(undefined); setListOpen(false); }, [visible]);
  const [view, setView] = useState<MapViewport>(() => ({ ...viewport.current, camera: { ...viewport.current.camera } }));
  useEffect(() => {
    if(!data) return;
    const expiry = setTimeout(() => { setData(undefined); setFailed(true); }, 15000);
    return () => clearTimeout(expiry);
  }, [data]);
  useEffect(() => {
    if(!visible) return;
    let disposed = false;
    let timer: ReturnType<typeof setTimeout>;
    const load = async () => {
      try {
        if(document.visibilityState !== "hidden") {
          const positions = await sendGetRequest<Positions>("/api/map-info/player-positions");
          if(!disposed) { setData(positions); setFailed(false); }
        } else if(!disposed) setData(undefined);
      } catch {
        if(!disposed) { setData(undefined); setFailed(true); }
      } finally {
        if(!disposed) timer = setTimeout(load, 5000);
      }
    };
    void load();
    return () => { disposed = true; clearTimeout(timer); };
  }, [visible]);
  useEffect(() => {
    if(!visible) return;
    let frame: number;
    let previous = "";
    const update = () => {
      const value = viewport.current;
      const key = `${value.camera.x}:${value.camera.z}:${value.zoom}:${value.viewportPx.width}:${value.viewportPx.height}`;
      if(previous !== key) {
        previous = key;
        setView({ camera: { ...value.camera }, zoom: value.zoom, viewportPx: { ...value.viewportPx } });
      }
      frame = requestAnimationFrame(update);
    };
    frame = requestAnimationFrame(update);
    return () => cancelAnimationFrame(frame);
  }, [viewport, visible]);
  const players = visible && data?.available ? playersInWorld(data.players, save) : [];
  return (
    <div className="absolute inset-0 pointer-events-none overflow-hidden">
      {players.map(player => {
        const point = playerScreenPosition(player, view);
        if(point.left < -32 || point.top < -32 || point.left > view.viewportPx.width + 32 || point.top > view.viewportPx.height + 32) return null;
        const label = `${player.name} · ${Math.floor(player.x)} ${Math.floor(player.y)} ${Math.floor(player.z)}`;
        return (
          <button key={player.uuid} type="button" title={label} aria-label={label}
            style={{ left: point.left, top: point.top, transform: "translate(-50%, -100%)" }}
            className="absolute pointer-events-auto flex flex-col items-center cursor-pointer"
            onPointerDown={event => event.stopPropagation()} onPointerUp={event => event.stopPropagation()}
            onClick={() => setSelected(value => value === player.uuid ? undefined : player.uuid)}>
            <img src={getSettings("players.avatar-provider") + encodeURIComponent(player.name)} alt={player.name}
              width={28} height={28} referrerPolicy="no-referrer" className="image-pixelated rounded-sm border-2 border-background shadow-md"
              onError={event => { const image = event.currentTarget; if(image.getAttribute("src") !== SteveAvatar.src) image.src = SteveAvatar.src; }}/>
            <span className="rounded bg-background/90 px-1 text-xs text-foreground shadow-sm">{player.name}</span>
            {selected === player.uuid && <span className="rounded bg-background/95 px-2 text-xs font-mono">{Math.floor(player.x)} {Math.floor(player.y)} {Math.floor(player.z)}</span>}
          </button>
        );
      })}
      {visible && (
      <div className="absolute right-6 top-6 pointer-events-auto text-right space-y-1" onPointerDown={event => event.stopPropagation()} onPointerUp={event => event.stopPropagation()} onWheel={event => event.stopPropagation()}>
        <div className="flex items-center justify-between gap-3 rounded-lg border bg-background/95 px-3 py-2 shadow-sm">
          <Button variant="ghost" size="sm" disabled={!visible} aria-expanded={listOpen} aria-controls="map-online-players"
            onClick={() => setListOpen(value => !value)}>{$("map.players.title")}{visible && data?.available ? ` (${data.players.length})` : ""}
            <ChevronDown aria-hidden="true" className={`size-4 ${listOpen ? "rotate-180" : ""}`}/>
          </Button>
        </div>
        {visible && listOpen && data?.available && (
          <section id="map-online-players" className="max-h-72 w-72 overflow-auto rounded-lg border bg-background/95 p-2 text-left shadow-lg" aria-label={$("map.players.online")}>
            <p className="px-2 py-1 text-xs text-muted-foreground">{$("map.players.locate-hint")}</p>
            {data.players.length === 0 && <p className="p-2 text-sm">{$("map.players.empty")}</p>}
            {data.players.map(player => (
              <button key={player.uuid} type="button" disabled={player.world !== save || ![player.x, player.y, player.z].every(Number.isFinite)}
                className="flex w-full items-center gap-3 rounded-md px-2 py-2 text-left hover:bg-accent disabled:opacity-50 disabled:cursor-not-allowed"
                onClick={() => { setSelected(player.uuid); onLocate?.(player); }}>
                <img src={getSettings("players.avatar-provider") + encodeURIComponent(player.name)} alt="" width={24} height={24} className="image-pixelated rounded-sm"
                  onError={event => { if(event.currentTarget.getAttribute("src") !== SteveAvatar.src) event.currentTarget.src = SteveAvatar.src; }}/>
                <span className="min-w-0 flex-1"><span className="block truncate text-sm font-medium">{player.name}</span><span className="block text-xs text-muted-foreground">{player.world}{player.world !== save ? ` · ${$("map.players.other-world")}` : ""}</span></span>
                <span className="text-xs font-mono">{Math.floor(player.x)}, {Math.floor(player.z)}</span>
              </button>
            ))}
          </section>
        )}
        {visible && listOpen && !data && !failed && <p className="rounded bg-background/95 px-2 py-1 text-xs" role="status">{$("map.players.loading")}</p>}
        {visible && failed && <p className="rounded bg-background/95 px-2 py-1 text-xs" role="alert">{$("map.players.error")}</p>}
        {visible && data && !data.available && <p className="rounded bg-background/95 px-2 py-1 text-xs" role="status">{data.enabled === false ? $("map.players.disabled") : $("map.players.unavailable")}</p>}
        {visible && data?.enabled === false && <Link className="block rounded bg-background/95 px-2 py-1 text-xs underline" href="/panel/settings?tab=server#server.diagnostics">{$("map.diagnostics.settings")}</Link>}
      </div>
      )}
    </div>
  );
}
