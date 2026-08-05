"use client";

import type { PluginUpdate } from "@/lib/types";
import Link from "next/link";
import { useEffect, useState } from "react";
import { Download, PackageCheck, RotateCw, SquareArrowOutUpRight } from "lucide-react";
import { SubPage } from "../sub-page";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";
import { toastError } from "@/lib/api";
import { emitter } from "@/lib/emitter";
import { googleSansCode } from "@/lib/fonts";
import { $ } from "@/lib/i18n";
import { checkPluginUpdates, updatePlugins } from "../plugins/plugin-utils";
import { PluginSourceBadge } from "../plugins/columns";
import { base64ToString, cn } from "@/lib/utils";

export default function UpdatesPage() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [updates, setUpdates] = useState<PluginUpdate[]>([]);
  const [updating, setUpdating] = useState(false);

  const fetchUpdates = async () => {
    setLoading(true);
    setError(false);
    try {
      setUpdates(await checkPluginUpdates(true));
    } catch (e: any) {
      toastError(e, $("plugins.update.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [502, $("plugins.update.error.502")]
      ]);
      setError(true);
    } finally {
      setLoading(false);
      emitter.emit("loading-done");
    }
  };

  useEffect(() => {
    fetchUpdates();
  }, []);

  const handleUpdate = async (fileNames: string[]) => {
    setUpdating(true);
    try {
      await updatePlugins(fileNames);
      await fetchUpdates();
    } catch (e: any) {
      toastError(e, $("plugins.update.action.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [409, $("plugins.update.action.error.409")],
        [404, $("plugins.update.action.error.404")],
        [500, $("common.error.500")]
      ]);
    } finally {
      setUpdating(false);
    }
  };

  return (
    <SubPage
      title={$("sidebar.server.updates")}
      description={$("plugins.update.description")}
      category={$("sidebar.server")}
      icon={<Download />}>
      <div className="flex items-center justify-end">
        <Button
          variant="outline"
          className="cursor-pointer"
          disabled={loading || updating}
          onClick={() => fetchUpdates()}>
          <RotateCw />
          {$("plugins.action.refresh")}
        </Button>
      </div>

      {
        loading
        ? (
          <div className="flex flex-1 flex-col items-center justify-center gap-3 py-10 text-muted-foreground">
            <Spinner />
            <span className="text-sm">{$("plugins.update.checking")}</span>
          </div>
        )
        : error
          ? (
            <div className="flex flex-1 flex-col items-center justify-center gap-3 py-10 text-muted-foreground">
              <span className="text-sm">{$("plugins.update.error")}</span>
              <Button
                variant="ghost"
                className="cursor-pointer"
                onClick={() => fetchUpdates()}>
                <RotateCw />
                {$("plugins.action.refresh")}
              </Button>
            </div>
          )
          : updates.length === 0
            ? (
              <div className="flex flex-1 flex-col items-center justify-center gap-3 rounded-xl border border-dashed bg-muted/20 py-16 text-muted-foreground">
                <PackageCheck className="size-8" />
                <span className="text-sm">{$("plugins.update.latest")}</span>
              </div>
            )
            : (
              <div className="space-y-3">
                <div className="flex items-center justify-between gap-3 max-sm:flex-col max-sm:items-start">
                  <p className="text-sm text-muted-foreground">
                    {$("plugins.update.description")}
                  </p>
                  <Button
                    className="cursor-pointer"
                    disabled={updating}
                    onClick={() => handleUpdate(updates.map((update) => base64ToString(update.fileName)))}>
                    <Download />
                    {$("plugins.update.update-all")}
                  </Button>
                </div>
                <ul className="space-y-3">
                  {updates.map((update) => (
                    <li
                      key={update.fileName}
                      className="rounded-xl border bg-card/80 px-4 py-3 shadow-sm">
                      <div className="flex items-start gap-4 max-md:flex-col">
                        <div className="min-w-0 flex-1 space-y-2">
                          <div className="space-y-1">
                            <p className="truncate text-base font-semibold">{update.name}</p>
                            <p className={cn("text-sm text-muted-foreground", googleSansCode.className)}>
                              {$("plugins.update.version", update.currentVersion, update.latestVersion)}
                            </p>
                          </div>
                          <div className="flex flex-wrap items-center gap-2">
                            <PluginSourceBadge source={update.source}/>
                            {update.channel && (
                              <Badge variant="outline" className="bg-muted/40 text-muted-foreground border-border">
                                {$(`plugins.update.channel.${update.channel}` as never)}
                              </Badge>
                            )}
                            {update.requiresRestart && (
                              <span className="text-xs text-amber-600 dark:text-amber-400">
                                {$("plugins.update.restart")}
                              </span>
                            )}
                          </div>
                        </div>
                        <div className="flex items-center gap-2 self-end max-md:self-start">
                          {update.projectUrl && (
                            <Button
                              variant="outline"
                              size="icon"
                              className="cursor-pointer"
                              title={$("plugins.update.project-link")}
                              asChild>
                              <Link href={update.projectUrl} target="_blank" rel="noopener noreferrer">
                                <SquareArrowOutUpRight className="!size-4"/>
                              </Link>
                            </Button>
                          )}
                          <Button
                            className="cursor-pointer"
                            disabled={updating}
                            onClick={() => handleUpdate([base64ToString(update.fileName)])}>
                            <Download />
                            {$("plugins.action.update")}
                          </Button>
                        </div>
                      </div>
                    </li>
                  ))}
                </ul>
              </div>
            )
      }
    </SubPage>
  );
}
