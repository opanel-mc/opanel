import Link from "next/link";
import { $ } from "@/lib/i18n";
import { densityEntityName, densityBlockName } from "../map/density-names";
export type Scene = {
  requestedAt: number; completedAt: number; candidates: number;
  players: { uuid: string; name: string; world: string; x: number; y: number; z: number }[];
  chunks: { world: string; x: number; z: number; sampledAt: number; status: string; entities: Record<string, number>; blockEntities: Record<string, number> }[];
};
const total = (values: Record<string, number>) => Object.values(values).reduce((a, b) => a + b, 0);
export function IncidentScene({ scene }: { scene?: Scene | null }) {
  if(!scene) return <p className="text-sm text-muted-foreground">{$("monitor.scene.missing")}</p>;
  const chunks = [...scene.chunks].sort((a, b) => total(b.entities) + total(b.blockEntities) - total(a.entities) - total(a.blockEntities));
  return <section className="space-y-2 my-3">
    <h3 className="font-medium">{$("monitor.scene.title")}</h3>
    <p className="text-xs text-muted-foreground">{$("monitor.scene.scope")}</p>
    <p className="text-sm">{new Date(scene.requestedAt).toLocaleString()} · {scene.completedAt ? new Date(scene.completedAt).toLocaleString() : $("monitor.scene.pending")} · {scene.chunks.length}/{scene.candidates}</p>
    {scene.chunks.length < scene.candidates && <p className="text-sm">{$("monitor.scene.partial")}</p>}
    {chunks.length === 0 && <p className="text-sm">{$("monitor.scene.empty")}</p>}
    <div className="max-h-96 overflow-auto space-y-2">
      {chunks.map(chunk => <details key={`${chunk.world}:${chunk.x}:${chunk.z}`} className="rounded border p-2 text-sm">
        <summary className="cursor-pointer">{chunk.world} · ({chunk.x}, {chunk.z}) · {$("map.density.entities")}: {total(chunk.entities)} · {$("map.density.block-entities")}: {total(chunk.blockEntities)}</summary>
        <p>{$("map.density.sampled-at")}: {new Date(chunk.sampledAt).toLocaleString()}</p>
        <p>{$(chunk.status === "sampled" ? "monitor.scene.sampled" : chunk.status === "truncated" ? "monitor.scene.truncated" : chunk.status === "unloaded" ? "monitor.scene.unloaded" : "monitor.scene.entities-unavailable")}</p>
        <p>{$("monitor.scene.center")}: {chunk.x * 16 + 8}, {chunk.z * 16 + 8}</p>
        <Link className="underline" href={`/panel/map?world=${encodeURIComponent(chunk.world)}&x=${chunk.x * 16 + 8}&z=${chunk.z * 16 + 8}`}>{$("monitor.scene.map")}</Link>
        {Object.entries(chunk.entities).map(([type, count]) => <p key={type}>{densityEntityName(type)}: {count}</p>)}
        {Object.entries(chunk.blockEntities).map(([type, count]) => <p key={type}>{densityBlockName(type)}: {count}</p>)}
        <p className="text-xs text-muted-foreground">{$("monitor.scene.players")}</p>
        {scene.players.filter(player => player.world === chunk.world && Math.floor(player.x / 16) === chunk.x && Math.floor(player.z / 16) === chunk.z).map(player => <p key={player.uuid}>{player.name}: {Math.floor(player.x)} {Math.floor(player.y)} {Math.floor(player.z)}</p>)}
                           </details>)}
    </div>
    <details className="text-sm"><summary className="cursor-pointer">{$("monitor.scene.players")}</summary>{scene.players.map(player => <p key={player.uuid}>{player.name} · {player.world} · {Math.floor(player.x)} {Math.floor(player.y)} {Math.floor(player.z)}</p>)}</details>
         </section>;
}
