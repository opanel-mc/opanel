"use client";

import type { MarketplaceSelectedFile } from "@/lib/types";
import { Package, TriangleAlert } from "lucide-react";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { $ } from "@/lib/i18n";
import { base64ToString, formatDataSize } from "@/lib/utils";

export function InstallConfirmDialog({
  open,
  onOpenChange,
  missing,
  unresolvedCount,
  installing,
  onConfirm
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  missing: MarketplaceSelectedFile[]
  unresolvedCount: number
  installing: boolean
  onConfirm: () => Promise<void>
}) {
  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{$("marketplace.install-confirm.title")}</AlertDialogTitle>
          <AlertDialogDescription>
            {$("marketplace.install-confirm.description")}
          </AlertDialogDescription>
        </AlertDialogHeader>

        <div className="max-h-52 space-y-2 overflow-y-auto text-sm">
          <div className="rounded-lg border bg-muted/20 p-2 font-medium">
            {$("marketplace.install-confirm.dependencies", missing.length)}
          </div>
          <ul className="space-y-1.5">
            {missing.map((dependency) => (
              <li
                key={dependency.projectId}
                className="flex items-center gap-2 rounded-lg border bg-card/60 px-3 py-2">
                <Package className="size-4 shrink-0 text-muted-foreground"/>
                <span className="min-w-0 flex-1 truncate">
                  {base64ToString(dependency.projectTitle)}
                  <span className="ml-2 text-xs text-muted-foreground">
                    {base64ToString(dependency.versionNumber)}
                  </span>
                </span>
                <span className="shrink-0 text-xs text-muted-foreground">{formatDataSize(dependency.size)}</span>
              </li>
            ))}
          </ul>
          {unresolvedCount > 0 && (
            <div className="flex items-start gap-2 rounded-md border border-amber-500/30 bg-amber-500/10 p-2 text-amber-600 dark:text-amber-400">
              <TriangleAlert className="mt-0.5 size-4 shrink-0"/>
              <span>{$("marketplace.install-confirm.unresolved", unresolvedCount)}</span>
            </div>
          )}
        </div>

        <AlertDialogFooter>
          <Button variant="outline" disabled={installing} onClick={() => onOpenChange(false)}>
            {$("dialog.cancel")}
          </Button>
          <Button className="cursor-pointer" disabled={installing} onClick={() => void onConfirm()}>
            {$("marketplace.install-confirm.action", missing.length + 1)}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
