"use client";

import { useContext, useEffect, useState } from "react";
import { Clock } from "lucide-react";
import { FunctionalCard } from "@/components/functional-card";
import { InfoContext } from "@/contexts/api-context";
import { gameTickToTime, millisToTime } from "@/lib/time";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { $ } from "@/lib/i18n";

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
      title={$("dashboard.time.title")}
      className={className}
      innerClassName="h-full p-4 pt-0 flex max-sm:flex-col max-sm:gap-4 [&>*]:flex-1">
      {
        ctx && (
          <>
            <div className="flex flex-col gap-1 justify-center items-center">
              <div className="text-2xl font-bold">{millisToTime(uptime)}</div>
              <span className="text-xs max-xl:text-sm text-muted-foreground">{$("dashboard.time.uptime")}</span>
            </div>
            <Tooltip>
              <TooltipTrigger>
                <div className="h-full flex flex-col gap-1 justify-center items-center">
                  <div className="text-2xl font-bold">{gameTickToTime(ingameTime % dayTicks)}</div>
                  <span className="text-xs max-xl:text-sm text-muted-foreground">{$("dashboard.time.ingame-time")}</span>
                </div>
              </TooltipTrigger>
              <TooltipContent>{$("dashboard.time.ingame-time.tooltip")}</TooltipContent>
            </Tooltip>
          </>
        )
      }
    </FunctionalCard>
  );
}
