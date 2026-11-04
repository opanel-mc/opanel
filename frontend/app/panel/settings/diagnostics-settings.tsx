"use client";

import { useEffect, useState } from "react";
import { Switch } from "@/components/ui/switch";
import { Button } from "@/components/ui/button";
import { toastRestartAlert } from "@/components/restart-alert";
import { sendGetRequest, sendPostRequest } from "@/lib/api";
import { $ } from "@/lib/i18n";
import { serverType } from "@/lib/global";

type Settings = { diagnosticsEnabled: boolean; performanceIncidentsEnabled: boolean; active: boolean; restartRequired: boolean };
export function DiagnosticsSettings() {
  const [settings, setSettings] = useState<Settings>();
  const [busy, setBusy] = useState(false);
  const [failed, setFailed] = useState(false);
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    let disposed = false;
    setFailed(false);
    setBusy(true);
    void sendGetRequest<Settings>("/api/diagnostics/settings").then(result => {
      if(!disposed) setSettings(result);
    }).catch(() => { if(!disposed) setFailed(true); }).finally(() => { if(!disposed) setBusy(false); });
    return () => { disposed = true; };
  }, [retry]);
  const save = async (next: Settings) => {
    setBusy(true);
    setFailed(false);
    try {
      const result = await sendPostRequest<Settings>("/api/diagnostics/settings", {
        diagnosticsEnabled: next.diagnosticsEnabled,
        performanceIncidentsEnabled: next.performanceIncidentsEnabled,
      });
      setSettings(result);
      if(result.restartRequired) toastRestartAlert();
    } catch { setFailed(true); } finally { setBusy(false); }
  };
  return (
    <section id="server.diagnostics" className="mt-4 rounded-md border bg-background dark:bg-transparent">
      <div className="flex justify-between items-center gap-4 px-4 py-3 border-b">
        <div><label className="text-sm" htmlFor="diagnostics-enabled">{$("settings.server.diagnostics")}</label><p className="text-xs text-muted-foreground mt-1">{$("settings.server.diagnostics.description")}</p></div>
        <Switch id="diagnostics-enabled" aria-label={$("settings.server.diagnostics")} checked={settings?.diagnosticsEnabled ?? false}
          disabled={!settings || busy} onCheckedChange={enabled => { if(settings) void save({ ...settings, diagnosticsEnabled: enabled }); }}/>
      </div>
      <div className="flex justify-between items-center gap-4 px-4 py-3">
        <div><label className="text-sm" htmlFor="incidents-enabled">{$("settings.server.incidents")}</label><p className="text-xs text-muted-foreground mt-1">{$("settings.server.incidents.description")}</p></div>
        <Switch id="incidents-enabled" aria-label={$("settings.server.incidents")} checked={settings?.performanceIncidentsEnabled ?? false}
          disabled={serverType === "Folia" || !settings?.diagnosticsEnabled || busy} onCheckedChange={enabled => { if(settings) void save({ ...settings, performanceIncidentsEnabled: enabled }); }}/>
      </div>
      {settings?.restartRequired && <p className="px-4 pb-3 text-sm" role="status">{$("settings.server.diagnostics.restart")}</p>}
      {!settings && !failed && <p className="px-4 pb-3 text-sm" role="status">{$("settings.server.diagnostics.loading")}</p>}
      {failed && <div className="px-4 pb-3 flex items-center gap-2"><p className="text-sm" role="alert">{$("settings.server.diagnostics.error")}</p><Button size="sm" variant="outline" disabled={busy} onClick={() => setRetry(value => value + 1)}>{$("settings.server.diagnostics.retry")}</Button></div>}
    </section>
  );
}
