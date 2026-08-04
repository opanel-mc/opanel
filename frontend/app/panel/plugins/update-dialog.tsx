import type { PluginUpdate } from "@/lib/types";
import Link from "next/link";
import { useEffect, useState } from "react";
import { Download, PackageCheck, RotateCw, SquareArrowOutUpRight } from "lucide-react";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";
import { toastError } from "@/lib/api";
import { $ } from "@/lib/i18n";
import { base64ToString, cn } from "@/lib/utils";
import { googleSansCode } from "@/lib/fonts";
import { checkPluginUpdates, updatePlugins } from "./plugin-utils";

export function PluginUpdateDialog({
  open,
  onOpenChange,
  force
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  force: boolean
}) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [updates, setUpdates] = useState<PluginUpdate[]>([]);
  const [updating, setUpdating] = useState(false);

  const fetchUpdates = async () => {
    setLoading(true);
    setError(false);
    try {
      setUpdates(await checkPluginUpdates(force));
    } catch (e: any) {
      toastError(e, $("plugins.update.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [502, $("plugins.update.error.502")]
      ]);
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if(open) fetchUpdates();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, force]);

  const handleUpdate = async (fileNames: string[]) => {
    setUpdating(true);
    try {
      await updatePlugins(fileNames);
      onOpenChange(false);
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
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{$("plugins.update.title")}</DialogTitle>
          <DialogDescription>{$("plugins.update.description")}</DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-3">
          {
            loading
            ? (
              <div className="py-8 flex flex-col items-center gap-3 text-muted-foreground">
                <Spinner />
                <span className="text-sm">{$("plugins.update.checking")}</span>
              </div>
            )
            : (
              error
              ? (
                <div className="py-8 flex flex-col items-center gap-3 text-muted-foreground">
                  <span className="text-sm">{$("plugins.update.error")}</span>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="cursor-pointer"
                    onClick={() => fetchUpdates()}>
                    <RotateCw />
                    {$("plugins.action.refresh")}
                  </Button>
                </div>
              )
              : (
                updates.length === 0
                ? (
                  <div className="py-8 flex flex-col items-center gap-3 text-muted-foreground">
                    <PackageCheck />
                    <span className="text-sm">{$("plugins.update.latest")}</span>
                  </div>
                )
                : (
                  <ul className="max-h-80 space-y-2 overflow-y-auto o-scrollbar">
                    {
                      updates.map((update) => (
                        <li
                          key={update.fileName}
                          className="px-3 py-2 border rounded-md flex items-center gap-3">
                          <div className="min-w-0 flex-1">
                            <p className="font-semibold truncate">{update.name}</p>
                            <p className={cn("text-xs text-muted-foreground", googleSansCode.className)}>
                              {$("plugins.update.version", update.currentVersion, update.latestVersion)}
                            </p>
                          </div>
                          {update.projectUrl && (
                            <Button
                              variant="ghost"
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
                            size="sm"
                            className="cursor-pointer"
                            disabled={updating}
                            onClick={() => handleUpdate([base64ToString(update.fileName)])}>
                            <Download />
                            {$("plugins.action.update")}
                          </Button>
                        </li>
                      ))
                    }
                  </ul>
                )
              )
            )
          }
        </div>
        <DialogFooter className="justify-between!">
          {(!loading && !error && updates.length > 0) && (
            <Button
              className="cursor-pointer"
              disabled={updating}
              onClick={() => handleUpdate(updates.map((update) => base64ToString(update.fileName)))}>
              <Download />
              {$("plugins.update.update-all")}
            </Button>
          )}
          <DialogClose asChild>
            <Button variant="outline">
              {$("dialog.close")}
            </Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
