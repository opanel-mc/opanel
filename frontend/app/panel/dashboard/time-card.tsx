"use client";

import { useContext, useEffect, useState } from "react";
import { Clock } from "lucide-react";
import { FunctionalCard } from "@/components/functional-card";
import { InfoContext } from "@/contexts/api-context";
import { gameTickToTime, millisToTime } from "@/lib/time";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";

const dayTicks = 24000;

export function TimeCard({
  className,
}: Readonly<{
  className?: string
}>) {
  const ctx = useContext(InfoContext);
  const [uptime, setUptime] = useState(ctx?.uptime ?? 0);
  const [ingameTime, setIngameTime] = useState(ctx?.ingameTime.current ?? 0);

  useEffect(() => {
    if(!ctx) return;

    setUptime(ctx.uptime);
    setIngameTime(ctx.ingameTime.current);
  }, [ctx]);

  useEffect(() => {
    const timer = setInterval(() => {
      setUptime((current) => current + 1000);
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    if(!ctx?.ingameTime.doDaylightCycle || ctx.ingameTime.paused) return;

    const timer = setInterval(() => {
      setIngameTime((current) => current + 16.6);
    }, ctx.ingameTime.mspt * 16.6);

    return () => clearInterval(timer);
  }, [ctx?.ingameTime.doDaylightCycle, ctx?.ingameTime.paused, ctx?.ingameTime.mspt]);

  return (
    <FunctionalCard
      icon={Clock}
      title="时间"
      className={className}
      innerClassName="h-full p-4 pt-0 flex [&>*]:flex-1">
      {
        ctx && (
          <>
            <div className="flex flex-col gap-2 items-center">
              <div className="text-2xl font-bold">{millisToTime(uptime)}</div>
              <span className="text-sm text-muted-foreground">服务器已正常运行</span>
            </div>
            <Tooltip>
              <TooltipTrigger>
                <div className="flex flex-col gap-2 items-center">
                  <div className="text-2xl font-bold">{gameTickToTime(ingameTime % dayTicks)}</div>
                  <span className="text-sm text-muted-foreground">游戏内时间</span>
                </div>
              </TooltipTrigger>
              <TooltipContent>请以游戏内实际时间为准，此处仅供参考</TooltipContent>
            </Tooltip>
          </>
        )
      }
    </FunctionalCard>
  );
}
