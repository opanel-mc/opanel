"use client";

import { type ExoticComponent, type PropsWithChildren, useContext } from "react";
import { type LucideIcon, Computer, Cpu,  Gpu,  MemoryStick,  Server } from "lucide-react";
import { FunctionalCard } from "@/components/functional-card";
import { InfoContext } from "@/contexts/api-context";
import { cn, formatDataSize } from "@/lib/utils";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger
} from "@/components/ui/dialog";
import { googleSansCode } from "@/lib/fonts";
import { $ } from "@/lib/i18n";
import { Button } from "@/components/ui/button";

function SystemInfoItem({
  name,
  children,
  className,
  ...props
}: PropsWithChildren & {
  icon: LucideIcon
  name: string
  className?: string
}) {
  return (
    <div className={cn("min-w-fit h-fit flex items-center gap-4", className)}>
      <div className="min-w-20 flex items-center self-start gap-2">
        <props.icon size={18}/>
        <span>{name}:</span>
      </div>
      <span className={cn("text-sm text-muted-foreground space-y-1", googleSansCode.className)}>
        {children}
      </span>
    </div>
  );
}

function SystemDialog({
  children,
  asChild
}: PropsWithChildren & {
  asChild?: boolean
}) {
  const ctx = useContext(InfoContext);

  if(!ctx) return <></>;

  return (
    <Dialog>
      <DialogTrigger asChild={asChild}>{children}</DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{$("dashboard.system.dialog.title")}</DialogTitle>
          <DialogDescription>
            {$("dashboard.system.dialog.description")}
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <h3 className="text-lg font-semibold">{ctx.system.os}</h3>
          <div className="flex flex-col gap-2">
            <SystemInfoItem
              icon={Computer}
              name={$("dashboard.system.item.arch")}>
              {ctx.system.arch ?? "-"}
            </SystemInfoItem>
            <SystemInfoItem
              icon={Cpu}
              name="CPU">
              <div>{ctx.system.cpuName ?? "-"}</div>
              <div className="text-foreground">({ctx.system.cpuCore} Core(s))</div>
            </SystemInfoItem>
            {ctx.system.gpus.length > 0 && (
              <SystemInfoItem
                icon={Gpu}
                name="GPU">
                {
                  ctx.system.gpus.map((gpu, i) => (
                    <div key={i}>{gpu}</div>
                  ))
                }
              </SystemInfoItem>
            )}
            <SystemInfoItem
              icon={MemoryStick}
              name={$("dashboard.system.item.memory")}
              className="*:text-foreground">
              {formatDataSize(ctx?.system.memory ?? 0)}
            </SystemInfoItem>
          </div>
        </div>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline">{$("dialog.close")}</Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export function SystemCard({
  className,
}: Readonly<{
  className?: string
}>) {
  const ctx = useContext(InfoContext);

  return (
    <FunctionalCard
      icon={Cpu}
      title={$("dashboard.system.title")}
      moreDialog={SystemDialog as ExoticComponent}
      className={className}
      innerClassName="h-full mt-2 pb-4 px-6 grid grid-rows-subgrid grid-cols-2 *:col-span-2 *:whitespace-nowrap gap-2 overflow-x-auto o-scrollbar">
      <SystemInfoItem
        icon={Server}
        name="OS">
        {ctx?.system.os ?? "-"}
      </SystemInfoItem>
      <SystemInfoItem
        icon={Computer}
        name={$("dashboard.system.item.arch")}>
        {ctx?.system.arch ?? "-"}
      </SystemInfoItem>
      <SystemInfoItem
        icon={Cpu}
        name="CPU">
        {ctx?.system.cpuName ?? "-"}
      </SystemInfoItem>
      <SystemInfoItem
        icon={Gpu}
        name="GPU">
        {
          (!ctx || ctx?.system.gpus.length === 0)
          ? "-"
          : (
            ctx.system.gpus.length === 1
            ? ctx.system.gpus[0]
            : (
              <>
                <span>{ctx.system.gpus[0]}&nbsp;</span>
                <span>...({ctx.system.gpus.length - 1})</span>
              </>
            )
          )
        }
      </SystemInfoItem>
      <SystemInfoItem
        icon={Cpu}
        name={$("dashboard.system.item.core")}
        className="col-span-1! *:text-foreground">
        {ctx?.system.cpuCore ?? "-"}
      </SystemInfoItem>
      <SystemInfoItem
        icon={MemoryStick}
        name={$("dashboard.system.item.memory")}
        className="ml-10 col-start-2 col-span-1! *:text-foreground">
        {formatDataSize(ctx?.system.memory ?? 0)}
      </SystemInfoItem>
    </FunctionalCard>
  );
}
