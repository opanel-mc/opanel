"use client";

import { useContext } from "react";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { apiUrl } from "@/lib/api";
import { InfoContext } from "@/contexts/api-context";
import { MinecraftText } from "@/components/mc-text";
import { Badge } from "@/components/ui/badge";

import PackIcon from "@/assets/images/pack.png";

export function InfoCard({
  className,
}: Readonly<{
  className?: string
}>) {
  const ctx = useContext(InfoContext);

  return (
    <Card className={cn(className, "flex flex-row rounded-md")}>
      <img
        className="aspect-square h-full rounded-xs"
        src={(ctx && ctx.favicon) ? (apiUrl + ctx.favicon) : PackIcon.src}
        alt="favicon"/>
      
      <div className="flex-1 flex flex-col gap-2">
        <div className="flex justify-between items-center *:text-nowrap">
          <div className="flex gap-4 [&>*]:space-x-2">
            <div>
              <span className="font-semibold">IP:</span>
              {
                (ctx && ctx.ip)
                ? <span>{ctx.ip}</span>
                : <span className="text-muted-foreground italic">未设置</span>
              }
            </div>
            <div>
              <span className="font-semibold">端口:</span>
              <span className="text-emerald-500 font-[Consolas]">{ctx ? ctx.port : ""}</span>
            </div>
          </div>
          <div className="ml-2">
            <Badge variant="outline">
              <div className={cn("w-2 h-2 rounded-full", ctx ? "bg-green-600" : "bg-red-700")}/>
              {ctx ? "正在运行" : "未运行"}
            </Badge>
          </div>
        </div>
        <div className="flex-1 text-sm">
          {ctx && <MinecraftText>{"§7"+ atob(ctx.motd)}</MinecraftText>}
        </div>
      </div>
    </Card>
  );
}
