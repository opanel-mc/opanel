"use client";

import type { PluginUpdateStatusResponse } from "@/lib/types";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Switch } from "@/components/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { $ } from "@/lib/i18n";
import { sendGetRequest, sendPostRequest, toastError } from "@/lib/api";
import { controlWidth, Section, SettingsItem } from "./settings-control";

const RESTART_STRATEGIES = ["defer", "restart-if-needed"] as const;

export function PluginUpdateSection() {
  const [status, setStatus] = useState<PluginUpdateStatusResponse | null>(null);

  const fetchStatus = async () => {
    try {
      setStatus(await sendGetRequest<PluginUpdateStatusResponse>("/api/plugins/update-status"));
    } catch (e: any) {
      toastError(e, $("settings.plugins.status.error"), [
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    }
  };

  useEffect(() => {
    fetchStatus();
  }, []);

  const save = async (patch: Partial<PluginUpdateStatusResponse>) => {
    if(!status) return;
    setStatus({ ...status, ...patch });
    try {
      await sendPostRequest("/api/plugins/update-settings", JSON.stringify(patch));
      toast.success($("settings.plugins.save.success"));
    } catch (e: any) {
      toastError(e, $("settings.plugins.save.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
      setStatus(status);
    }
  };

  if(!status) return null;

  return (
    <Section>
      <SettingsItem
        id="plugins.auto-check"
        name={$("settings.plugins.auto-check")}
        description={$("settings.plugins.auto-check.description")}
        control={
          <Switch
            checked={status.autoCheckPluginUpdates}
            onCheckedChange={(checked) => save({ autoCheckPluginUpdates: checked })}/>
        }/>
      <SettingsItem
        id="plugins.auto-apply"
        name={$("settings.plugins.auto-apply")}
        description={$("settings.plugins.auto-apply.description")}
        control={
          <Switch
            checked={status.autoApplyPluginUpdates}
            onCheckedChange={(checked) => save({ autoApplyPluginUpdates: checked })}/>
        }/>
      <SettingsItem
        id="plugins.restart-strategy"
        name={$("settings.plugins.restart-strategy")}
        description={$("settings.plugins.restart-strategy.description")}
        control={
          <Select
            value={status.pluginUpdateRestartStrategy}
            onValueChange={(value) => save({ pluginUpdateRestartStrategy: value })}>
            <SelectTrigger className={controlWidth}><SelectValue/></SelectTrigger>
            <SelectContent>
              {RESTART_STRATEGIES.map((strategy) => (
                <SelectItem key={strategy} value={strategy}>
                  {$(`settings.plugins.restart-strategy.${strategy}` as never)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        }/>
      <SettingsItem
        id="plugins.check-interval"
        name={$("settings.plugins.check-interval")}
        description={$("settings.plugins.check-interval.description")}
        control={
          <Input
            type="number"
            min={60}
            className={controlWidth}
            defaultValue={status.pluginUpdateCheckInterval}
            onBlur={(e) => {
              const value = (e.target as HTMLInputElement).valueAsNumber;
              if(value >= 60 && value !== status.pluginUpdateCheckInterval) {
                save({ pluginUpdateCheckInterval: Math.floor(value) });
              }
            }}/>
        }/>
    </Section>
  );
}
