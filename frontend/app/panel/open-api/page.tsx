"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Blocks, Gauge, Info, Unplug, Users } from "lucide-react";
import { SubPage } from "../sub-page";
import { $ } from "@/lib/i18n";
import { ConfigItem, ConfigSection } from "@/components/config-item";
import { Switch } from "@/components/ui/switch";
import { sendGetRequest, sendPostRequest, toastError } from "@/lib/api";
import { Interface, InterfaceDescription, InterfaceRequest, InterfaceResponse, InterfaceSection } from "./interface";
import { Text } from "@/components/i18n-text";
import { useLoadingDone } from "@/hooks/use-loading-done";

export default function OpenAPI() {
  const [enabled, setEnabled] = useState(false);

  const fetchOpenAPIEnabled = async () => {
    try {
      const { enabled: openAPIEnabled } = await sendGetRequest<{ enabled: boolean }>("/api/open-api");
      setEnabled(openAPIEnabled);
    } catch (e: any) {
      toastError(e, $("open-api.fetch.error"), [
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    }
  };

  const handleToggleOpenAPI = async (enabled: boolean) => {
    try {
      await sendPostRequest(`/api/open-api?enabled=${enabled ? "1" : "0"}`);
      setEnabled(enabled);
    } catch (e: any) {
      toastError(e, enabled ? $("open-api.toggle.enable.error") : $("open-api.toggle.disable.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    }
  };

  useEffect(() => {
    fetchOpenAPIEnabled();
  }, []);

  useLoadingDone();

  return (
    <SubPage
      title={$("open-api.title")}
      description={$("open-api.description")}
      category={$("sidebar.config")}
      icon={<Unplug />}
      pageClassName="min-xl:px-64!">
      <ConfigSection>
        <ConfigItem name={$("open-api.item.enabled")}>
          <Switch
            checked={enabled}
            onCheckedChange={(enabled) => handleToggleOpenAPI(enabled)}/>
        </ConfigItem>
      </ConfigSection>
      {enabled && (
        <>
          <Text
            className="block text-sm text-muted-foreground mb-4"
            id="open-api.hint"
            args={[
              <Link
                href="https://opanel.cn"
                target="_blank"
                rel="noopener noreferrer"
                key={0}>
                opanel.cn
              </Link>
            ]}/>
          <h2 className="text-lg font-semibold pl-1 mb-3">{$("open-api.interfaces.title")}</h2>
          <InterfaceSection interfaceName="info" icon={Info}>
            <Interface method="GET" route="/open-api/info">
              <InterfaceDescription>
                {$("open-api.interfaces.info.description")}
              </InterfaceDescription>
              <InterfaceRequest def={`{}`}/>
              <InterfaceResponse def={`{
  motd: string
  port: number
  maxPlayerCount: number
  whitelist: boolean
  uptime: number
  ingameTime: number
  system: {
    os: string
    arch: string
    cpuName: string
    cpuCore: number
    memory: number
    gpus: string[]
    java: string
  }
}`}/>
            </Interface>
          </InterfaceSection>
          <InterfaceSection interfaceName="monitor" icon={Gauge}>
            <Interface method="GET" route="/open-api/monitor">
              <InterfaceDescription>
                {$("open-api.interfaces.monitor.description")}
              </InterfaceDescription>
              <InterfaceRequest def={`{}`}/>
              <InterfaceResponse def={`{
  cpu: number
  memory: number
  tps: number
}`}/>
            </Interface>
          </InterfaceSection>
          <InterfaceSection interfaceName="plugins" icon={Blocks}>
            <Interface method="GET" route="/open-api/plugins">
              <InterfaceDescription>
                {$("open-api.interfaces.plugins.description")}
              </InterfaceDescription>
              <InterfaceRequest def={`{}`}/>
              <InterfaceResponse def={`{
  plugins: {
    fileName: string
    name: string
    version?: string
    description?: string
    authors: string[]
    website?: string
    icon?: string
    size: number
    enabled: boolean
    loaded: boolean
  }[]
}`}/>
            </Interface>
          </InterfaceSection>
          <InterfaceSection interfaceName="players" icon={Users}>
            <Interface method="GET" route="/open-api/players">
              <InterfaceDescription>
                {$("open-api.interfaces.players.description")}
              </InterfaceDescription>
              <InterfaceRequest def={`{}`}/>
              <InterfaceResponse def={`{
  players: {
    name: string
    uuid: string
    isOnline: boolean
    isBanned: boolean
    gamemode: "adventure" | "creative" | "survival" | "spectator"
    banReason?: string
  }[]
}`}/>
            </Interface>
            <Interface method="GET" route="/open-api/players/{uuid}">
              <InterfaceDescription>
                {$("open-api.interfaces.player.description")}
              </InterfaceDescription>
              <InterfaceRequest def={`{
  uuid: string // path param
}`}/>
              <InterfaceResponse def={`{
  name: string
  uuid: string
  isOnline: boolean
  isBanned: boolean
  gamemode: "adventure" | "creative" | "survival" | "spectator"
  banReason?: string
}`}/>
            </Interface>
          </InterfaceSection>
        </>
      )}
    </SubPage>
  );
}

