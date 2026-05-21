"use client";

import type { ConsoleLogLevel } from "@/lib/ws/terminal";
import type { PropsWithChildren } from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Settings as SettingsIcon } from "lucide-react";
import { changeSettings, getSettings, resetSettings, type SettingsStorageType } from "@/lib/settings";
import { SubPage } from "../sub-page";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { controlWidth, SettingsNumberInput, SettingsSwitch } from "./settings-control";
import { Button } from "@/components/ui/button";
import { LoginBannerDialog } from "./login-banner-dialog";
import { LaunchCommandDialog } from "./launch-command-dialog";
import { SecurityDialog } from "./security-dialog";
import { UpdateDialog } from "./update-dialog";
import { cn } from "@/lib/utils";
import { googleSansCode } from "@/lib/fonts";
import { AvatarProvider, SkinProvider } from "@/lib/types";
import { type LanguageCode, languages } from "@/lang";
import { $ } from "@/lib/i18n";
import { sendDeleteRequest, sendGetRequest, sendPostRequest, toastError } from "@/lib/api";
import { Switch } from "@/components/ui/switch";
import { emitter } from "@/lib/emitter";
import { toastRestartAlert } from "@/components/restart-alert";

const SETTINGS_TAB_VALUES = ["general", "server", "terminal", "editor"] as const;

function Section({ children }: PropsWithChildren) {
  return (
    <div className="bg-background dark:bg-transparent border rounded-md flex flex-col">{children}</div>
  );
}

function SectionGroup({
  title,
  children
}: PropsWithChildren & {
  title: string
}) {
  return (
    <div className="mb-4 flex flex-col gap-3">
      <h3 className="mx-1 font-semibold">{title}</h3>
      {children}
    </div>
  );
}

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
    <div id={id} className="flex justify-between items-center flex-wrap gap-2 px-4 py-3 border-b last:border-b-0">
      <div className="flex flex-col gap-1">
        <span className="text-sm">{name}</span>
        <span className="text-xs text-muted-foreground whitespace-pre-line">{description}</span>
      </div>
      {control}
    </div>
  );
}

export default function Settings() {
  const { replace } = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const tabFromUrl = searchParams.get("tab");
  const currentTab = (SETTINGS_TAB_VALUES as readonly string[]).includes(tabFromUrl ?? "")
    ? (tabFromUrl as (typeof SETTINGS_TAB_VALUES)[number])
    : "general";
  const [openLaunchCommand, setOpenLaunchCommand] = useState(false);
  const [mapFeatureEnabled, setMapFeatureEnabled] = useState(false);

  const setTab = (value: string) => {
    const next = new URLSearchParams(searchParams.toString());
    next.set("tab", value);
    replace(`${pathname}?${next.toString()}`);
  };
  
  const fetchMapFeatureEnabled = async () => {
    try {
      const { enabled: mapEnabled } = await sendGetRequest<{ enabled: boolean }>("/api/map");
      setMapFeatureEnabled(mapEnabled);
    } catch (e: any) {
      toastError(e, $("map.fetch-enabled.error"), [
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    }
  };
  
  const handleToggleMap = async (enabled: boolean) => {
    try {
      await sendPostRequest(`/api/map?enabled=${enabled ? "1" : "0"}`);
      setMapFeatureEnabled(enabled);
      toastRestartAlert();
    } catch (e: any) {
      toastError(e, enabled ? $("map.toggle.enable.error") : $("map.toggle.disable.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    }
  };

  useEffect(() => {
    if(searchParams.has("openLaunchCommand")) {
      setOpenLaunchCommand(true);
      toast.warning($("settings.server.launch-command.required"));
      
      const next = new URLSearchParams(searchParams.toString());
      next.delete("openLaunchCommand");
      replace(`${pathname}?${next.toString()}`);
      
      setTab("server");
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    fetchMapFeatureEnabled();

    emitter.on("refresh-data", () => fetchMapFeatureEnabled());
  }, []);

  return (
    <SubPage
      title={$("settings.title")}
      icon={<SettingsIcon />}
      pageClassName="min-xl:px-64!">
      <div className="mb-4 flex justify-end">
        <Button
          variant="outline"
          className="cursor-pointer"
          onClick={async () => {
            resetSettings();
            await sendDeleteRequest("/assets/reset/login-banner");
            window.location.reload();
          }}>
          {$("settings.reset")}
        </Button>
      </div>
      <Tabs
        value={currentTab}
        onValueChange={setTab}
        className="mt-4 [&>[data-slot=tabs-content]]:mt-4">
        <TabsList>
          {SETTINGS_TAB_VALUES.map((value) => (
            <TabsTrigger key={value} value={value}>
              {$(`settings.${value}.title`)}
            </TabsTrigger>
          ))}
        </TabsList>
        <TabsContent value="general" className="space-y-3">
          <Section>
            <SettingsItem
              id="dashboard.monitor-interval"
              name={$("settings.dashboard.monitor-interval")}
              description={$("settings.dashboard.monitor-interval.description")}
              control={<SettingsNumberInput id="dashboard.monitor-interval" min={1}/>}/>
          </Section>
          <Section>
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
                    <SelectItem value={AvatarProvider.MINOTAR} title={AvatarProvider.MINOTAR}>Minotar</SelectItem>
                    <SelectItem value={AvatarProvider.MINEATAR} title={AvatarProvider.MINEATAR}>Mineatar</SelectItem>
                    <SelectItem value={AvatarProvider.MCHEADS} title={AvatarProvider.MCHEADS}>MC Heads</SelectItem>
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
                    <SelectItem value={SkinProvider.MINOTAR} title={SkinProvider.MINOTAR}>Minotar</SelectItem>
                    <SelectItem value={SkinProvider.MINEATAR} title={SkinProvider.MINEATAR}>Mineatar</SelectItem>
                    <SelectItem value={SkinProvider.MCHEADS} title={SkinProvider.MCHEADS}>MC Heads</SelectItem>
                  </SelectContent>
                </Select>
              }/>
            {/* <SettingsItem
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
              }/> */}
          </Section>
          <SectionGroup title="OPanel">
            <Section>
              <SettingsItem
                id="system.language"
                name="🇨🇳 🇩🇪 🇺🇸 🇫🇷 🇰🇷 🇰🇵 🇯🇵"
                control={
                  <Select
                    defaultValue={getSettings("system.language")}
                    onValueChange={(value) => {
                      changeSettings("system.language", value as LanguageCode);
                      window.location.reload();
                    }}>
                    <SelectTrigger className="w-40">
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
                id="system.login-banner"
                name={$("settings.login-banner.title")}
                description={$("settings.login-banner.description")}
                control={
                  <LoginBannerDialog asChild>
                    <Button className="cursor-pointer" size="sm">{$("settings.system.login-banner.modify")}</Button>
                  </LoginBannerDialog>
                }/>
              <SettingsItem
                id="system.mcp"
                name={$("settings.system.mcp")}
                description={$("settings.system.mcp.description")}
                control={
                  <Button
                    className="cursor-pointer"
                    size="sm"
                    asChild>
                    <Link href="/panel/mcp">{$("settings.system.mcp.configure")}</Link>
                  </Button>
                }/>
              <SettingsItem
                id="system.access-key"
                name={$("settings.system.access-key")}
                control={
                  <SecurityDialog asChild>
                    <Button className="cursor-pointer" size="sm">{$("settings.system.access-key.modify")}</Button>
                  </SecurityDialog>
                }/>
              <SettingsItem
                id="system.check-update"
                name={$("settings.system.check-update")}
                control={
                  <UpdateDialog asChild>
                    <Button className="cursor-pointer" size="sm">{$("settings.system.check-update.check")}</Button>
                  </UpdateDialog>
                }/>
            </Section>
          </SectionGroup>
        </TabsContent>
        <TabsContent value="server">
          <Section>
            <SettingsItem
              id="server.launch-command"
              name={$("settings.server.launch-command")}
              description={$("settings.server.launch-command.description")}
              control={
                <LaunchCommandDialog
                  asChild
                  open={openLaunchCommand}
                  onOpenChange={setOpenLaunchCommand}>
                  <Button className="cursor-pointer" size="sm">{$("settings.server.launch-command.modify")}</Button>
                </LaunchCommandDialog>
              }/>
            <SettingsItem
              id="server.map-feature"
              name={$("settings.server.map-feature")}
              description={$("settings.server.map-feature.description")}
              control={<Switch checked={mapFeatureEnabled} onCheckedChange={handleToggleMap}/>}/>
          </Section>
        </TabsContent>
        <TabsContent value="terminal">
          <Section>
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
              id="terminal.rich-style"
              name={$("settings.terminal.rich-style")}
              description={$("settings.terminal.rich-style.description")}
              control={<SettingsSwitch id="terminal.rich-style"/>}/>
          </Section>
        </TabsContent>
        <TabsContent value="editor">
          <SectionGroup title={$("settings.code-of-conduct.title")}>
            <Section>
              <SettingsItem
                id="code-of-conduct.auto-saving-interval"
                name={$("settings.code-of-conduct.auto-saving-interval")}
                description={$("settings.code-of-conduct.auto-saving-interval.description")}
                control={<SettingsNumberInput id="code-of-conduct.auto-saving-interval" min={1000}/>}/>
            </Section>
          </SectionGroup>
          <SectionGroup title={$("settings.monaco.title")}>
            <Section>
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
          </SectionGroup>
        </TabsContent>
      </Tabs>
    </SubPage>
  );
}
