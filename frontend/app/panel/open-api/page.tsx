"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Bot, Unplug } from "lucide-react";
import { SubPage } from "../sub-page";
import { $ } from "@/lib/i18n";
import { ConfigItem, ConfigSection } from "@/components/config-item";
import { Switch } from "@/components/ui/switch";
import { sendGetRequest, sendPostRequest, toastError } from "@/lib/api";
import { Interface, InterfaceDescription, InterfaceRequest, InterfaceResponse, InterfaceSection } from "./interface";
import { Text } from "@/components/i18n-text";
import { useLoadingDone } from "@/hooks/use-loading-done";
import { Button } from "@/components/ui/button";
import { copyToClipboard } from "@/lib/utils";
import {
  generateOpenAPIPrompt,
  OPEN_API_INTERFACES,
  type OpenAPIInterfaceName,
  type OpenAPIInterfaceState
} from "./interface-data";

export default function OpenAPI() {
  const [enabled, setEnabled] = useState(false);
  const [interfaceStates, setInterfaceStates] = useState<OpenAPIInterfaceState>({});
  const [updatingInterfaces, setUpdatingInterfaces] = useState<Set<OpenAPIInterfaceName>>(
    () => new Set()
  );

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

  const fetchOpenAPIInterfaceEnabled = async (name: string) => {
    try {
      const { enabled: interfaceEnabled } = await sendGetRequest<{ enabled: boolean }>(`/api/open-api/${name}`);

      setInterfaceStates((current) => ({
        ...current,
        [name]: interfaceEnabled
      }));
    } catch (e: any) {
      toastError(e, `${$("open-api.fetch.error")} (${name})`, [
        [400, $("common.error.400")],
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

  const handleToggleInterface = async (
    interfaceName: OpenAPIInterfaceName,
    interfaceEnabled: boolean
  ) => {
    setUpdatingInterfaces((current) => new Set(current).add(interfaceName));

    try {
      await sendPostRequest(`/api/open-api/${interfaceName}?enabled=${interfaceEnabled ? "1" : "0"}`);
      setInterfaceStates((current) => ({
        ...current,
        [interfaceName]: interfaceEnabled
      }));
    } catch (e: any) {
      toastError(
        e,
        `${interfaceEnabled ? $("open-api.toggle.enable.error") : $("open-api.toggle.disable.error")} (${interfaceName})`,
        [
          [400, $("common.error.400")],
          [401, $("common.error.401")],
          [500, $("common.error.500")]
        ]
      );
    } finally {
      setUpdatingInterfaces((current) => {
        const next = new Set(current);
        next.delete(interfaceName);
        return next;
      });
    }
  };

  useEffect(() => {
    fetchOpenAPIEnabled();
  }, []);

  useEffect(() => {
    setInterfaceStates({});

    if(enabled) {
      for(const { name } of OPEN_API_INTERFACES) {
        fetchOpenAPIInterfaceEnabled(name);
      }
    }
  }, [enabled]);

  const interfaceStatesLoaded = OPEN_API_INTERFACES.every(
    ({ name }) => interfaceStates[name] !== undefined
  );

  const handleCopyPrompt = () => {
    if(!interfaceStatesLoaded) return;

    copyToClipboard(generateOpenAPIPrompt(interfaceStates, window.location.origin));
  };

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
          <div className="mb-3 px-1 flex items-center justify-between gap-2">
            <h2 className="text-lg font-semibold">
              {$("open-api.interfaces.title")}
            </h2>
            <Button
              variant="outline"
              size="sm"
              className="cursor-pointer"
              disabled={!interfaceStatesLoaded || updatingInterfaces.size > 0}
              onClick={handleCopyPrompt}>
              <Bot />
              {$("open-api.prompt.copy")}
            </Button>
          </div>
          {OPEN_API_INTERFACES.map(({ name, icon, endpoints }) => (
            <InterfaceSection
              key={name}
              interfaceName={name}
              icon={icon}
              enabled={interfaceStates[name] ?? false}
              disabled={interfaceStates[name] === undefined || updatingInterfaces.has(name)}
              onEnabledChange={(enabled) => handleToggleInterface(name, enabled)}>
              {endpoints.map(({ method, route, description, request, response }) => (
                <Interface
                  key={`${method}-${route}`}
                  method={method}
                  route={route}>
                  <InterfaceDescription>
                    {$(description)}
                  </InterfaceDescription>
                  <InterfaceRequest def={request}/>
                  {response !== undefined && (
                    <InterfaceResponse def={response}/>
                  )}
                </Interface>
              ))}
            </InterfaceSection>
          ))}
        </>
      )}
    </SubPage>
  );
}
