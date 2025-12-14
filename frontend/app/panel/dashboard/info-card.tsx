"use client";

import { useContext, useRef, useState } from "react";
import { Pencil, PenLine, Power, RotateCw, Settings, UserPen } from "lucide-react";
import { toast } from "sonner";
import { Card } from "@/components/ui/card";
import { base64ToString, cn } from "@/lib/utils";
import { apiUrl, sendPostRequest } from "@/lib/api";
import { InfoContext, MonitorContext, VersionContext } from "@/contexts/api-context";
import { MinecraftText } from "@/components/mc-text";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Alert } from "@/components/alert";
import { Spinner } from "@/components/ui/spinner";
import { WhitelistSheet } from "../players/whitelist-sheet";
import { ServerSheet } from "./server-sheet";
import { MotdEditor } from "./motd-editor";
import { FaviconDialog } from "./favicon-dialog";
import { googleSansCode } from "@/lib/fonts";
import { $ } from "@/lib/i18n";

import PackIcon from "@/assets/images/pack.png";

function ControlButtonGroup({
  className
}: {
  className?: string
}) {
  const versionCtx = useContext(VersionContext);
  const ctx = useContext(InfoContext);
  const [isReloadingServer, setIsReloadingServer] = useState(false);
  const [isStoppingServer, setIsStoppingServer] = useState(false);

  return (
    <div className={cn("flex gap-1 [&>*]:cursor-pointer", className)}>
      {ctx?.whitelist && (
        <WhitelistSheet asChild>
          <Button
            variant="ghost"
            size="icon"
            title={$("dashboard.info.controls.edit-whitelist")}>
            <UserPen />
          </Button>
        </WhitelistSheet>
      )}
      <MotdEditor motd={base64ToString(ctx?.motd ?? "")} asChild>
        <Button
          variant="ghost"
          size="icon"
          title={$("dashboard.info.controls.edit-motd")}>
          <PenLine />
        </Button>
      </MotdEditor>
      <ServerSheet asChild>
        <Button
          variant="ghost"
          size="icon"
          title={$("dashboard.info.controls.edit-properties")}>
          <Settings />
        </Button>
      </ServerSheet>
      <Button
        variant="ghost"
        size="icon"
        title={$("dashboard.info.controls.reload")}
        disabled={versionCtx?.serverType === "Folia" || isReloadingServer}
        onClick={() => {
          setIsReloadingServer(true);
          toast.promise(sendPostRequest("/api/control/reload"), {
            loading: $("dashboard.info.controls.reload.loading"),
            success: () => {
              setIsReloadingServer(false);
              if(
                versionCtx?.serverType === "Bukkit"
                || versionCtx?.serverType === "Spigot"
                || versionCtx?.serverType === "Paper"
                || versionCtx?.serverType === "Folia"
              ) {
                window.location.reload();
              }
              return {
                message: $("dashboard.info.controls.reload.success")
              };
            },
            error: $("dashboard.info.controls.reload.error")
          });
        }}>
        <RotateCw />
      </Button>
      <Alert
        title={$("dashboard.info.controls.stop.alert.title")}
        description={$("dashboard.info.controls.stop.alert.description")}
        onAction={() => {
          sendPostRequest("/api/control/stop");
          setIsStoppingServer(true);
          toast.loading($("dashboard.info.controls.stop.loading"));
        }}
        asChild>
        <Button
          variant="outline"
          size="icon"
          title={$("dashboard.info.controls.stop")}
          disabled={isStoppingServer}>
          {
            isStoppingServer
            ? <Spinner />
            : <Power />
          }
        </Button>
      </Alert>
    </div>
  );
}

export function InfoCard({
  className,
}: Readonly<{
  className?: string
}>) {
  const versionCtx = useContext(VersionContext);
  const ctx = useContext(InfoContext);
  const monitorCtx = useContext(MonitorContext);
  const faviconRef = useRef<HTMLImageElement>(null);
  const warningState = monitorCtx[monitorCtx.length - 1].cpu >= 80 || monitorCtx[monitorCtx.length - 1].tps <= 16;

  return (
    <Card className={cn(className, "flex flex-col rounded-md max-lg:gap-3")}>
      <div className="min-lg:flex-1 flex max-md:flex-col gap-6 max-lg:border-b max-lg:pb-3">
        <div className="relative group/favicon">
          <img
            className="aspect-square h-full max-md:w-32 max-md:h-32 rounded-xs image-pixelated"
            src={(ctx && ctx.favicon) ? (apiUrl + ctx.favicon) : PackIcon.src}
            alt="favicon"
            ref={faviconRef}/>
          <FaviconDialog asChild>
            <Button
              variant="secondary"
              size="icon-sm"
              className="absolute bottom-0 ml-2 mb-2 cursor-pointer hidden group-hover/favicon:flex">
              <Pencil />
            </Button>
          </FaviconDialog>
        </div>
        
        <div className="flex-1 flex flex-col gap-2">
          <div className="flex max-lg:flex-col gap-4 max-lg:gap-1 [&>*]:space-x-2">
            <div>
              <span className="font-semibold text-nowrap">{$("dashboard.info.version")}</span>
              <span>{`${versionCtx?.serverType} ${versionCtx?.version}`}</span>
            </div>
            <div>
              <span className="font-semibold text-nowrap">{$("dashboard.info.port")}</span>
              <span className={cn("text-emerald-500", googleSansCode.className)}>{ctx ? ctx.port : ""}</span>
            </div>
          </div>
          <div className="h-fit text-sm">
            {ctx && <MinecraftText maxLines={2} maxCharPerLine={45}>{"§7"+ base64ToString(ctx.motd)}</MinecraftText>}
          </div>
        </div>
        <div className="flex flex-col justify-between">
          <Badge className="self-end max-lg:hidden cursor-default" variant="outline">
            <div className={cn("w-2 h-2 rounded-full", ctx ? (warningState ? "bg-yellow-600" : "bg-green-600") : "bg-red-700")}/>
            {
              ctx
              ? (
                warningState
                ? $("dashboard.info.status.warning")
                : $("dashboard.info.status.running")
              )
              : $("dashboard.info.status.stopped")
            }
          </Badge>
          <ControlButtonGroup className="self-end max-lg:hidden"/>
        </div>
      </div>
      <ControlButtonGroup className="hidden flex-row-reverse self-start max-lg:flex"/>
    </Card>
  );
}
