"use client";

import type {
  MarketplaceInstallEntry,
  MarketplaceProjectHit,
  MarketplaceProjectResponse,
  MarketplaceSelectedFile,
  MarketplaceVersion
} from "@/lib/types";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { Download, Heart, Package, SquareArrowOutUpRight } from "lucide-react";
import { toast } from "sonner";
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
import { Badge } from "@/components/ui/badge";
import { Spinner } from "@/components/ui/spinner";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "@/components/ui/select";
import { $ } from "@/lib/i18n";
import { base64ToString, formatDataSize } from "@/lib/utils";
import { emitter } from "@/lib/emitter";
import { toastError } from "@/lib/api";
import { toastRestartAlert } from "@/components/restart-alert";
import { InstallConfirmDialog } from "./install-confirm-dialog";
import { formatNumber, getInstallPreview, getMarketplaceProject, installMarketplaceEntries } from "./marketplace-utils";

export function ProjectDialog({
  project,
  open,
  onOpenChange
}: {
  project: MarketplaceProjectHit
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const [detail, setDetail] = useState<MarketplaceProjectResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);
  const [selectedVersionId, setSelectedVersionId] = useState<string | null>(null);
  const [installing, setInstalling] = useState(false);
  const [confirmEntries, setConfirmEntries] = useState<{
    entries: MarketplaceInstallEntry[]
    missing: MarketplaceSelectedFile[]
    unresolved: number
  } | null>(null);

  useEffect(() => {
    if(!open) return;
    setDetail(null);
    setError(false);
    setSelectedVersionId(null);
    setConfirmEntries(null);
    let cancelled = false;
    (async () => {
      setLoading(true);
      try {
        const res = await getMarketplaceProject(project.id);
        if(cancelled) return;
        setDetail(res);
        const preferred = res.versions.find((version) => version.compatible) ?? res.versions[0];
        if(preferred) setSelectedVersionId(preferred.id);
      } catch (e: any) {
        if(cancelled) return;
        setError(true);
        toastError(e, $("marketplace.detail.error"), [
          [401, $("common.error.401")],
          [502, $("marketplace.detail.error.502")]
        ]);
      } finally {
        if(!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [open, project.id]);

  const selectedVersion = detail?.versions.find((version) => version.id === selectedVersionId) ?? null;
  const requiredDepsCount = selectedVersion?.dependencies
    .filter((dependency) => dependency.dependencyType === "required")
    .length ?? 0;
  const displayName = detail?.project.title
    ? base64ToString(detail.project.title)
    : base64ToString(project.title);

  const installingRef = useRef(false);
  const handleInstall = async () => {
    if(!detail || !selectedVersionId || installing || installingRef.current) return;
    installingRef.current = true;
    setInstalling(true);
    try {
      const preview = await getInstallPreview(detail.project.id, selectedVersionId);
      if(preview.conflicts.length > 0) {
        toast.error($("marketplace.install.conflict"), {
          description: $("marketplace.install.conflict.description", preview.conflicts[0].fileName)
        });
        return;
      }
      const entries: MarketplaceInstallEntry[] = [
        { projectId: preview.target.projectId, versionId: preview.target.versionId },
        ...preview.missingDependencies.map((dependency) => ({
          projectId: dependency.projectId,
          versionId: dependency.versionId
        }))
      ];
      if(preview.missingDependencies.length === 0) {
        await installEntries(entries, true);
      } else {
        setConfirmEntries({
          entries,
          missing: preview.missingDependencies,
          unresolved: preview.unresolvedDependencies.length
        });
      }
    } catch (e: any) {
      toastError(e, $("marketplace.install.error"), [
        [401, $("common.error.401")],
        [409, $("marketplace.install.conflict")],
        [502, $("marketplace.install.error.502")]
      ]);
    } finally {
      setInstalling(false);
      installingRef.current = false;
    }
  };

  const installEntries = async (entries: MarketplaceInstallEntry[], alreadyLocked = false) => {
    if(!alreadyLocked) {
      if(installingRef.current) return;
      installingRef.current = true;
      setInstalling(true);
    }
    try {
      const res = await installMarketplaceEntries(entries);
      toast.success($("marketplace.install.success"));
      if(res.requiresRestart) toastRestartAlert();
      emitter.emit("refresh-data");
      onOpenChange(false);
    } catch (e: any) {
      toastError(e, $("marketplace.install.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [409, $("marketplace.install.conflict")],
        [500, $("common.error.500")]
      ]);
    } finally {
      if(!alreadyLocked) {
        setInstalling(false);
        installingRef.current = false;
      }
    }
  };

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent>
          <DialogHeader className="h-20 flex-row gap-5">
            <div className="h-full overflow-hidden rounded-md border bg-muted/40">
              {
                detail?.project.iconUrl
                ? <img
                    className="h-full w-full object-cover"
                    src={detail.project.iconUrl}
                    alt=""/>
                : <div className="flex h-full w-full items-center justify-center text-muted-foreground px-4">
                    <Package className="size-8" />
                  </div>
              }
            </div>
            <div className="flex min-w-0 flex-col gap-1">
              <div className="flex items-center gap-2">
                <DialogTitle className="truncate">{displayName}</DialogTitle>
                <Badge variant="outline">{detail?.project.projectType ?? project.projectType}</Badge>
              </div>
              <DialogDescription className="line-clamp-2">
                {detail ? base64ToString(detail.project.author) : base64ToString(project.author)}
              </DialogDescription>
            </div>
          </DialogHeader>

          {loading ? (
            <div className="flex items-center justify-center py-10">
              <Spinner />
            </div>
          ) : error || !detail ? (
            <div className="flex items-center justify-center py-10 text-sm text-muted-foreground">
              {$("marketplace.detail.error")}
            </div>
          ) : (
            <>
              <div className="flex items-center gap-4 text-sm text-muted-foreground">
                <span className="inline-flex items-center gap-1">
                  <Download className="size-4" />
                  {formatNumber(detail.project.downloads)}
                </span>
                <span className="inline-flex items-center gap-1">
                  <Heart className="size-4" />
                  {formatNumber(detail.project.follows)}
                </span>
                <span className="ml-auto">{$("marketplace.detail.updated", detail.project.updatedAt.split("T")[0])}</span>
              </div>

              {detail.project.description && (
                <div className="max-h-36 min-h-16 overflow-y-auto rounded-lg border bg-muted/20 p-3 text-sm whitespace-pre-wrap break-words">
                  {base64ToString(detail.project.description)}
                </div>
              )}

              <div className="space-y-2">
                <p className="text-sm font-medium">{$("marketplace.detail.version")}</p>
                {
                  detail.versions.length === 0 ? (
                    <p className="text-sm text-muted-foreground">{$("marketplace.detail.no-version")}</p>
                  ) : (
                    <Select value={selectedVersionId ?? undefined} onValueChange={setSelectedVersionId}>
                      <SelectTrigger><SelectValue/></SelectTrigger>
                      <SelectContent>
                        {detail.versions.map((version) => (
                          <VersionItem key={version.id} version={version}/>
                        ))}
                      </SelectContent>
                    </Select>
                  )
                }
                {!detail.versionsFilteredByGame && detail.versions.length > 0 && (
                  <p className="text-xs text-amber-600 dark:text-amber-400">
                    {$("marketplace.detail.versions-fallback")}
                  </p>
                )}
                {selectedVersion && (
                  <div className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                    <span className="inline-flex items-center gap-1">
                      <Download className="size-3.5" />
                      {formatDataSize(selectedVersion.fileSize)}
                    </span>
                    {
                      selectedVersion.gameVersions.slice(0, 3).map((gameVersion) => (
                        <Badge key={gameVersion} variant="outline">MC {gameVersion}</Badge>
                      ))
                    }
                    {selectedVersion.loaders.slice(0, 3).map((loader) => (
                      <Badge key={loader} variant="outline">{loader}</Badge>
                    ))}
                    {requiredDepsCount > 0 && (
                      <Badge variant="outline" className="text-amber-600 dark:text-amber-400">
                        {$("marketplace.detail.dependencies", requiredDepsCount)}
                      </Badge>
                    )}
                  </div>
                )}
              </div>
            </>
          )}

          <DialogFooter className="justify-between">
            <Button
              variant="ghost"
              className="cursor-pointer"
              asChild>
              <Link
                href={detail?.project.projectUrl ?? project.projectUrl}
                target="_blank"
                rel="noopener noreferrer">
                <SquareArrowOutUpRight className="!size-4" />
                {$("marketplace.detail.project-link")}
              </Link>
            </Button>
            <div className="flex items-center gap-2">
              <DialogClose asChild>
                <Button variant="outline">{$("dialog.close")}</Button>
              </DialogClose>
              <Button
                className="cursor-pointer"
                disabled={!selectedVersion || installing || loading}
                onClick={handleInstall}>
                {installing && <Spinner className="mr-1 size-4"/>}
                <Download />
                {$("marketplace.action.install")}
              </Button>
            </div>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <InstallConfirmDialog
        open={confirmEntries !== null}
        onOpenChange={(open) => {
          if(!open && !installing) setConfirmEntries(null);
        }}
        missing={confirmEntries?.missing ?? []}
        unresolvedCount={confirmEntries?.unresolved ?? 0}
        installing={installing}
        onConfirm={async () => {
          if(confirmEntries) {
            await installEntries(confirmEntries.entries);
          }
        }}/>
    </>
  );
}

function VersionItem({ version }: { version: MarketplaceVersion }) {
  return (
    <SelectItem value={version.id}>
      <span className="inline-flex items-center gap-2">
        <span>{base64ToString(version.versionNumber)}</span>
        {
          version.compatible
          ? <Badge variant="outline">{$("marketplace.version.compatible")}</Badge>
          : <span className="text-xs text-muted-foreground">{$("marketplace.version.incompatible")}</span>
        }
      </span>
    </SelectItem>
  );
}
