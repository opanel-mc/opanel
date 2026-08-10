"use client";

import type { OidcConfigResponse } from "@/lib/types";
import { useEffect, useState } from "react";
import { ShieldCheck, Trash2, Plus, ShieldEllipsis } from "lucide-react";
import { toast } from "sonner";
import { SubPage } from "../sub-page";
import { $ } from "@/lib/i18n";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { sendGetRequest, sendPostRequest, sendDeleteRequest, toastError } from "@/lib/api";
import { ConfigItem, ConfigSection } from "@/components/config-item";
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle
} from "@/components/ui/empty";
import { emitter } from "@/lib/emitter";
import { googleSansCode } from "@/lib/fonts";
import { cn } from "@/lib/utils";

export default function OIDCConfiguration() {
  const [config, setConfig] = useState<Partial<OidcConfigResponse>>({});
  const [allowedUserIds, setAllowedUserIds] = useState<string[]>([]);
  const [newUserId, setNewUserId] = useState("");

  const fetchOidcStatus = async () => {
    try {
      const res = await sendGetRequest<OidcConfigResponse>("/api/auth/oidc/config", false);
      setConfig({
        enabled: res.enabled,
        displayName: res.displayName,
        discoveryUrl: res.discoveryUrl,
        clientId: res.clientId
      });
      if(res.enabled) {
        const { allowedUserIds: ids } = await sendGetRequest<{ allowedUserIds: string[] }>("/api/auth/oidc/allowed-users");
        setAllowedUserIds(ids ?? []);
      }
    } catch (e: any) {
        toastError(e, $("oidc.fetch.error"), [
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
      setConfig({ enabled: false });
    } finally {
      emitter.emit("loading-done");
    }
  };

  const handleAdd = async () => {
    const trimmed = newUserId.trim();
    if(!trimmed) return;

    try {
      await sendPostRequest("/api/auth/oidc/allowed-users", { userId: trimmed });
      setNewUserId("");
      toast.success($("oidc.add.success"));
      await fetchOidcStatus();
    } catch (e: any) {
      toastError(e, $("oidc.add.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    }
  };

  const handleRemove = async (userId: string) => {
    try {
      await sendDeleteRequest("/api/auth/oidc/allowed-users", { userId });
      toast.success($("oidc.remove.success"));
      await fetchOidcStatus();
    } catch (e: any) {
      toastError(e, $("oidc.remove.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    }
  };

  useEffect(() => {
    fetchOidcStatus();
  }, []);

  return (
    <SubPage
      title="OIDC"
      subTitle="OIDC"
      description={$("oidc.description")}
      category={$("nav.settings")}
      icon={<ShieldCheck />}
      pageClassName="min-xl:px-64!">
      {config.enabled ? (
        <>
          <h2 className="text-lg font-semibold mb-3">{$("oidc.item.basic-info")}</h2>
          <ConfigSection>
            {config.displayName && (
              <ConfigItem name={$("oidc.item.provider")}>
                <span className="text-sm font-medium">{config.displayName}</span>
              </ConfigItem>
            )}
            {config.discoveryUrl && (
              <ConfigItem name={$("oidc.item.discovery-url")}>
                <span className={cn("text-sm", googleSansCode.className)}>{config.discoveryUrl}</span>
              </ConfigItem>
            )}
            {config.clientId && (
              <ConfigItem name={$("oidc.item.client-id")}>
                <span className={cn("text-sm", googleSansCode.className)}>{config.clientId}</span>
              </ConfigItem>
            )}
          </ConfigSection>
          <h2 className="text-lg font-semibold mt-6 mb-3">{$("oidc.item.allowed-users")}</h2>
          <div className="bg-background dark:bg-transparent border rounded-md flex flex-col overflow-hidden max-h-[360px]">
            <div className="overflow-y-auto">
              {allowedUserIds.length === 0 ? (
                <span className="block px-4 py-3 text-sm text-muted-foreground">
                  {$("oidc.item.allowed-users.empty")}
                </span>
              ) : (
                allowedUserIds.map((userId) => (
                  <div key={userId} className="flex items-center gap-2 px-4 py-2 border-b last:border-b-0">
                    <span className={cn("text-sm mr-auto", googleSansCode.className)}>{userId}</span>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="cursor-pointer text-destructive hover:text-destructive"
                      onClick={() => handleRemove(userId)}>
                      <Trash2 size={16}/>
                    </Button>
                  </div>
                ))
              )}
            </div>
            <div className="sticky bottom-0 bg-background border-t p-2 flex gap-2">
              <Input
                className="flex-1"
                placeholder={$("oidc.item.allowed-users.placeholder")}
                value={newUserId}
                onChange={(e) => setNewUserId(e.target.value)}
                onKeyDown={(e) => { if(e.key === "Enter") handleAdd(); }}/>
              <Button
                className="cursor-pointer"
                disabled={!newUserId.trim()}
                onClick={handleAdd}>
                <Plus />
                {$("oidc.add")}
              </Button>
            </div>
          </div>
        </>
      ) : (
        <Empty className="border rounded-md border-solid">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <ShieldEllipsis />
            </EmptyMedia>
            <EmptyTitle>{$("oidc.empty.title")}</EmptyTitle>
            <EmptyDescription>
              {$("oidc.empty.description")}
            </EmptyDescription>
          </EmptyHeader>
        </Empty>
      )}
    </SubPage>
  );
}
