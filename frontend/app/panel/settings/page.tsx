"use client";

import type { ConsoleLogLevel } from "@/lib/terminal/log-levels";
import { Settings as SettingsIcon } from "lucide-react";
import { changeSettings, getSettings, resetSettings, type SettingsStorageType } from "@/lib/settings";
import { SubPage } from "../sub-page";
import { Section } from "./section";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "@/components/ui/select";
import { controlWidth, SettingsNumberInput, SettingsSwitch } from "./settings-control";
import { Button } from "@/components/ui/button";
import { SecurityDialog } from "./security-dialog";
import { cn } from "@/lib/utils";
import { googleSansCode } from "@/lib/fonts";
import { AvatarProvider, CapeProvider, SkinProvider } from "@/lib/types";
import { type LanguageCode, languages } from "@/lang";
import { $ } from "@/lib/i18n";

function SettingsItem<K extends keyof SettingsStorageType>({
  name,
  description,
  control,
  id
}: {
  id: K
  name: string
  description?: string
  control: React.ReactNode
}) {
  return (
    <div id={id} className="flex justify-between items-center px-4 py-3 border-b last:border-b-0">
      <div className="flex flex-col gap-1">
        <span className="text-sm">{name}</span>
        <span className="text-xs text-muted-foreground">{description}</span>
      </div>
      {control}
    </div>
  );
}

export default function Settings() {
  return (
    <SubPage title={$("settings.title")} icon={<SettingsIcon />} className="px-64 max-xl:px-0">
      <span className="text-sm text-muted-foreground">{$("settings.hint")}</span>
      <div className="flex flex-col gap-7 mt-4">
        <Section title={$("settings.dashboard.title")}>
          <SettingsItem
            id="dashboard.monitor-interval"
            name={$("settings.dashboard.monitor-interval")}
            description={$("settings.dashboard.monitor-interval.description")}
            control={<SettingsNumberInput id="dashboard.monitor-interval" min={1}/>}/>
        </Section>
        <Section title={$("settings.players.title")}>
          <SettingsItem
            id="players.avatar-provider"
            name={$("settings.players.avatar-provider")}
            description={$("settings.players.avatar-provider.description")}
            control={
              <Select
                defaultValue={getSettings("players.avatar-provider")}
                onValueChange={(value) => {
                  value !== "$custom" && changeSettings("players.avatar-provider", value);
                }}>
                <SelectTrigger className={controlWidth}>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={AvatarProvider.CRAFATAR} title={AvatarProvider.CRAFATAR}>Crafatar</SelectItem>
                  <SelectItem value={AvatarProvider.MINEATAR} title={AvatarProvider.MINEATAR}>Mineatar</SelectItem>
                  <SelectItem value={AvatarProvider.MCHEADS} title={AvatarProvider.MCHEADS}>MCHeads</SelectItem>
                </SelectContent>
              </Select>
            }/>
          <SettingsItem
            id="players.skin-provider"
            name={$("settings.players.skin-provider")}
            description={$("settings.players.skin-provider.description")}
            control={
              <Select
                defaultValue={getSettings("players.skin-provider")}
                onValueChange={(value) => {
                  value !== "$custom" && changeSettings("players.skin-provider", value);
                }}>
                <SelectTrigger className={controlWidth}>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={SkinProvider.CRAFATAR} title={SkinProvider.CRAFATAR}>Crafatar</SelectItem>
                  <SelectItem value={SkinProvider.MINEATAR} title={SkinProvider.MINEATAR}>Mineatar</SelectItem>
                  <SelectItem value={SkinProvider.MCHEADS} title={SkinProvider.MCHEADS}>MCHeads</SelectItem>
                </SelectContent>
              </Select>
            }/>
          <SettingsItem
            id="players.cape-provider"
            name={$("settings.players.cape-provider")}
            description={$("settings.players.cape-provider.description")}
            control={
              <Select
                defaultValue={getSettings("players.cape-provider")}
                onValueChange={(value) => {
                  value !== "$custom" && changeSettings("players.cape-provider", value);
                }}>
                <SelectTrigger className={controlWidth}>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={CapeProvider.CRAFATAR} title={CapeProvider.CRAFATAR}>Crafatar</SelectItem>
                </SelectContent>
              </Select>
            }/>
        </Section>
        <Section title={$("settings.terminal.title")}>
          <SettingsItem
            id="terminal.autocomplete"
            name={$("settings.terminal.autocomplete")}
            control={<SettingsSwitch id="terminal.autocomplete"/>}/>
          <SettingsItem
            id="terminal.word-wrap"
            name={$("settings.terminal.word-wrap")}
            control={<SettingsSwitch id="terminal.word-wrap"/>}/>
          <SettingsItem
            id="terminal.font-size"
            name={$("settings.terminal.font-size")}
            description={$("settings.terminal.font-size.description")}
            control={<SettingsNumberInput id="terminal.font-size" min={1} max={30}/>}/>
          <SettingsItem
            id="terminal.max-log-lines"
            name={$("settings.terminal.max-log-lines")}
            description={$("settings.terminal.max-log-lines.description")}
            control={<SettingsNumberInput id="terminal.max-log-lines" min={100} max={20000}/>}/>
          <SettingsItem
            id="terminal.log-level"
            name={$("settings.terminal.log-level")}
            description={$("settings.terminal.log-level.description")}
            control={
              <Select
                defaultValue={getSettings("terminal.log-level")}
                onValueChange={(value) => changeSettings("terminal.log-level", value as ConsoleLogLevel)}>
                <SelectTrigger className={cn(controlWidth, googleSansCode.className)}>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent className={googleSansCode.className}>
                  <SelectItem value="INFO">INFO</SelectItem>
                  <SelectItem value="WARN">WARN</SelectItem>
                  <SelectItem value="ERROR">ERROR</SelectItem>
                </SelectContent>
              </Select>
            }/>
          <SettingsItem
            id="terminal.log-time"
            name={$("settings.terminal.log-time")}
            control={<SettingsSwitch id="terminal.log-time"/>}/>
          <SettingsItem
            id="terminal.thread-name"
            name={$("settings.terminal.thread-name")}
            control={<SettingsSwitch id="terminal.thread-name"/>}/>
          <SettingsItem
            id="terminal.source-name"
            name={$("settings.terminal.source-name")}
            control={<SettingsSwitch id="terminal.source-name"/>}/>
          <SettingsItem
            id="terminal.convert-ansi-code"
            name={$("settings.terminal.convert-ansi-code")}
            description={$("settings.terminal.convert-ansi-code.description")}
            control={<SettingsSwitch id="terminal.convert-ansi-code"/>}/>
        </Section>
        <Section title={$("settings.code-of-conduct.title")}>
          <SettingsItem
            id="code-of-conduct.auto-saving-interval"
            name={$("settings.code-of-conduct.auto-saving-interval")}
            description={$("settings.code-of-conduct.auto-saving-interval.description")}
            control={<SettingsNumberInput id="code-of-conduct.auto-saving-interval" min={1000}/>}/>
        </Section>
        <Section title={$("settings.monaco.title")}>
          <SettingsItem
            id="monaco.word-wrap"
            name={$("settings.monaco.word-wrap")}
            control={<SettingsSwitch id="monaco.word-wrap"/>}/>
          <SettingsItem
            id="monaco.font-size"
            name={$("settings.monaco.font-size")}
            description={$("settings.monaco.font-size.description")}
            control={<SettingsNumberInput id="monaco.font-size" min={1} max={30}/>}/>
        </Section>
        <Section title={$("settings.system.title")}>
          <SettingsItem
            id="system.language"
            name={$("settings.system.language")}
            control={
              <Select
                defaultValue={getSettings("system.language")}
                onValueChange={(value) => changeSettings("system.language", value as LanguageCode)}>
                <SelectTrigger className={controlWidth}>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.keys(languages).map((lang, i) => (
                    <SelectItem value={lang} key={i}>{languages[lang]["$lang"]}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            }/>
          <SettingsItem
            id="system.access-key"
            name={$("settings.system.access-key")}
            control={
              <SecurityDialog asChild>
                <Button className="cursor-pointer" size="sm">{$("settings.system.access-key.modify")}</Button>
              </SecurityDialog>
            }/>
        </Section>
        <div>
          <Button
            variant="outline"
            className="cursor-pointer"
            onClick={() => {
              resetSettings();
              window.location.reload();
            }}>
            {$("settings.reset")}
          </Button>
        </div>
      </div>
    </SubPage>
  );
}
