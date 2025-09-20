"use client";

import { useContext } from "react";
import { PenLine, Power, RotateCw, Settings, UserPen } from "lucide-react";
import { toast } from "sonner";
import { Card } from "@/components/ui/card";
import { base64ToString, cn } from "@/lib/utils";
import { apiUrl, sendPostRequest } from "@/lib/api";
import { InfoContext } from "@/contexts/api-context";
import { MinecraftText } from "@/components/mc-text";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Alert } from "@/components/alert";
import { WhitelistSheet } from "../players/whitelist-sheet";
import { ServerSheet } from "./server-sheet";
import { MotdEditor } from "./motd-editor";

import PackIcon from "@/assets/images/pack.png";

function ControlButtonGroup({
  className
}: {
  className?: string
}) {
  const ctx = useContext(InfoContext);

  return (
    <div className={cn("flex gap-1 [&>*]:cursor-pointer", className)}>
      {ctx?.whitelist && (
        <WhitelistSheet asChild>
          <Button
            variant="ghost"
            size="icon"
            title="编辑白名单">
            <UserPen />
          </Button>
        </WhitelistSheet>
      )}
      <MotdEditor motd={base64ToString(ctx?.motd ?? "")} asChild>
        <Button
          variant="ghost"
          size="icon"
          title="编辑Motd">
          <PenLine />
        </Button>
      </MotdEditor>
      <ServerSheet asChild>
        <Button
          variant="ghost"
          size="icon"
          title="编辑server.properties">
          <Settings />
        </Button>
      </ServerSheet>
      <Button
        variant="ghost"
        size="icon"
        title="重载服务器"
        onClick={() => toast.promise(sendPostRequest("/api/control/reload"), {
          loading: "正在重载服务器...",
          success: "重载完毕",
          error: "重载失败"
        })}>
        <RotateCw />
      </Button>
      <Alert
        title="确定要停止服务器吗？"
        description="此操作将保存所有服务器信息并关闭服务器，OPanel也将无法访问，之后您可以从后台手动启动服务器。"
        onAction={() => {
          sendPostRequest("/api/control/stop");
          toast.loading("正在停止服务器...");
        }}
        asChild>
        <Button
          variant="outline"
          size="icon"
          title="停止服务器">
          <Power />
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
  const ctx = useContext(InfoContext);

  return (
    <Card className={cn(className, "flex flex-col rounded-md max-lg:gap-3")}>
      <div className="min-lg:flex-1 flex max-md:flex-col gap-6 max-lg:border-b max-lg:pb-3">
        <img
          className="aspect-square h-full rounded-xs image-pixelated"
          src={(ctx && ctx.favicon) ? (apiUrl + ctx.favicon) : PackIcon.src}
          alt="favicon"/>
        
        <div className="flex-1 flex flex-col gap-2">
          <div className="flex max-lg:flex-col gap-4 max-lg:gap-1 [&>*]:space-x-2">
            <div>
              <span className="font-semibold text-nowrap">版本:</span>
              <span>{ctx?.version}</span>
            </div>
            <div>
              <span className="font-semibold text-nowrap">端口:</span>
              <span className="text-emerald-500 font-[Consolas]">{ctx ? ctx.port : ""}</span>
            </div>
          </div>
          <div className="h-fit text-sm">
            {ctx && <MinecraftText maxLines={2}>{"§7"+ base64ToString(ctx.motd)}</MinecraftText>}
          </div>
        </div>
        <div className="flex flex-col justify-between">
          <Badge className="self-end max-lg:hidden" variant="outline">
            <div className={cn("w-2 h-2 rounded-full", ctx ? "bg-green-600" : "bg-red-700")}/>
            {ctx ? "正在运行" : "未运行"}
          </Badge>
          <ControlButtonGroup className="self-end max-lg:hidden"/>
        </div>
      </div>
      <ControlButtonGroup className="hidden flex-row-reverse self-start max-lg:flex"/>
    </Card>
  );
}
