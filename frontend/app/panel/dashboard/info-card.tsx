"use client";

import { useContext } from "react";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger
} from "@/components/ui/tooltip";
import { apiUrl } from "@/lib/global";
import { InfoContext } from "@/contexts/api-context";
import { parseMotd } from "@/lib/motd";

import PackIcon from "@/assets/images/pack.png";

export function InfoCard({
  className,
}: Readonly<{
  className?: string
}>) {
  const ctx = useContext(InfoContext);

  if(!ctx) return <></>;

  const { favicon, ip, port, motd } = ctx;

  return (
    <Card className={cn(className, "flex flex-row")}>
      <img
        className="aspect-square h-full rounded-sm"
        src={favicon ? (apiUrl + favicon) : PackIcon.src}
        alt="favicon"/>
      
      <div className="flex-1 flex flex-col gap-1">
        <div className="flex justify-between items-center *:text-nowrap">
          <div className="flex gap-4 [&>*]:space-x-2">
            <div>
              <span className="font-semibold">IP:</span>
              {
                ip
                ? <span>{ip}</span>
                : <span className="text-muted-foreground italic">未设置</span>
              }
            </div>
            <div>
              <span className="font-semibold">端口:</span>
              <span className="text-emerald-500 font-[Consolas]">{port}</span>
            </div>
          </div>
          <div className="pr-2">
            <Tooltip>
              <TooltipTrigger>
                <div className="w-2 h-2 rounded-full bg-green-600"/>
              </TooltipTrigger>
              <TooltipContent>
                <p>正在运行</p>
              </TooltipContent>
            </Tooltip>
          </div>
        </div>
        <div className="flex-1">
          <div dangerouslySetInnerHTML={{ __html: parseMotd(atob(motd)).outerHTML }}/>
        </div>
      </div>
    </Card>
  );
}
