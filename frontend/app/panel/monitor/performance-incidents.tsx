"use client";
import { useEffect, useState } from "react";
import { sendGetRequest } from "@/lib/api";
import { $ } from "@/lib/i18n";
import { IncidentScene, type Scene } from "./incident-scene";
import { Button } from "@/components/ui/button";
type Incident = { scene?: Scene | null; id: string; startedAt: number; endedAt: number | null; status: "active" | "recovered" | "interrupted"; samples: { timestamp: number; meanTickMs: number }[] };
type Response = { available: boolean; enabled?: boolean; incidents: Incident[] };
export function PerformanceIncidents({ onViewHistory }: { onViewHistory?: (from: number, to: number) => void }) {
  const [data, setData] = useState<Response>();
  const [failed, setFailed] = useState(false);
  const [selectedId, setSelectedId] = useState<string>();
  useEffect(() => {
    let disposed = false;
    let timer: ReturnType<typeof setTimeout>;
    const load = async () => {
      try {
        const result = await sendGetRequest<Response>(`/api/monitor/incidents${selectedId ? `?id=${encodeURIComponent(selectedId)}` : ""}`);
        if(!disposed) { setData(result); setFailed(false); }
      } catch { if(!disposed) setFailed(true); } finally { if(!disposed) timer = setTimeout(load, 5000); }
    };
    void load();
    return () => { disposed = true; clearTimeout(timer); };
  }, [selectedId]);
  return <section className="col-span-2 rounded-xl border p-5 space-y-3">
    <h2 className="font-semibold">{$("monitor.incidents.title")}</h2>
    <p className="text-sm text-muted-foreground">{$("monitor.incidents.description")}</p>
    {failed && <p role="alert">{$("monitor.incidents.error")}</p>}
    {!data && !failed && <p role="status">{$("monitor.incidents.loading")}</p>}
    {data && !data.available && <p role="status">{data.enabled === false ? $("monitor.incidents.disabled") : $("monitor.incidents.unavailable")}</p>}
    {data?.available && data.incidents.length === 0 && <p>{$("monitor.incidents.empty")}</p>}
    {data?.incidents.map(event => <details key={event.id} open={event.id === selectedId} className="rounded-lg border p-3">
      <summary className="cursor-pointer" onClick={e => { e.preventDefault(); setSelectedId(event.id === selectedId ? undefined : event.id); }}>{new Date(event.startedAt).toLocaleString()} · {$(`monitor.incidents.${event.status}`)}</summary>
      <p className="text-sm">{event.id}</p>
      <p className="text-sm text-muted-foreground">
        {$("monitor.incidents.end")}: {event.endedAt == null ? $("monitor.incidents.active") : new Date(event.endedAt).toLocaleString()}
      </p>
      {onViewHistory && <Button variant="outline" size="sm" onClick={() => onViewHistory(event.startedAt - 60_000, (event.endedAt ?? Date.now()) + 60_000)}>{$("monitor.incidents.view-history")}</Button>}
      {event.id === selectedId && <IncidentScene scene={event.scene}/>}
      <div className="max-h-64 overflow-auto"><table className="w-full text-sm">
        <thead><tr><th className="text-left">{$("monitor.incidents.time")}</th><th className="text-right">{$("monitor.incidents.duration")}</th></tr></thead>
        <tbody>{event.samples.map(sample => <tr key={sample.timestamp}><td>{new Date(sample.timestamp).toLocaleString()}</td><td className="text-right">{sample.meanTickMs.toFixed(2)} ms</td></tr>)}</tbody>
                                              </table>
      </div>
                                  </details>)}
         </section>;
}
