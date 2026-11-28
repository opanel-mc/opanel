"use client";

import type { ResolvedBulletin } from "@/lib/bulletin";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { SquareArrowOutUpRight } from "lucide-react";
import { checkBulletins, markBulletinDismissed, sortBulletinsByPriority } from "@/lib/bulletin";
import { BulletinDialog, BulletinMarkdown } from "./bulletin-dialog";

function showBulletinToast(bulletin: ResolvedBulletin) {
  const toastId = `bulletin:${bulletin.readKey}`;
  const toastFn = (
    bulletin.level === "critical"
      ? toast.error
      : (bulletin.level === "warning" ? toast.warning : toast.info)
  );

  const dismissAndRecord = () => {
    if(bulletin.policy === "once") {
      markBulletinDismissed(bulletin.readKey);
    }
  };

  toastFn(bulletin.title, {
    id: toastId,
    duration: Infinity,
    closeButton: true,
    description: (
      <div className="mt-1 space-y-2">
        <BulletinMarkdown content={bulletin.content} className="text-xs"/>
        {bulletin.actions.length > 0 && (
          <div className="flex flex-wrap gap-2 pt-0.5">
            {bulletin.actions.map((action, index) => {
              const isExternal = action.url.startsWith("https://");
              const linkClass = "inline-flex items-center gap-1 text-xs font-medium underline underline-offset-2 hover:opacity-80";
              return isExternal ? (
                <a
                  key={`${action.url}-${index}`}
                  href={action.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className={linkClass}>
                  {action.label}
                  <SquareArrowOutUpRight className="size-3"/>
                </a>
              ) : (
                <Link
                  key={`${action.url}-${index}`}
                  href={action.url}
                  className={linkClass}
                  onClick={() => {
                    dismissAndRecord();
                    toast.dismiss?.(toastId);
                  }}>
                  {action.label}
                </Link>
              );
            })}
          </div>
        )}
      </div>
    ),
    onDismiss: dismissAndRecord,
  });
}

export function Bulletin({ enabled }: {
  enabled: boolean
}) {
  const [modalQueue, setModalQueue] = useState<ResolvedBulletin[]>([]);

  const fetchBulletins = useCallback(async (isCancelled: () => boolean) => {
    try {
      const bulletins = await checkBulletins();
      if(isCancelled() || bulletins.length === 0) return;

      const ordered = sortBulletinsByPriority(bulletins);
      const toastBulletins = ordered.filter((item) => item.display === "toast");
      const modalBulletins = ordered.filter((item) => item.display === "modal");

      // Sonner prepends each newly added toast to index 0 (top/front for top-right),
      // so enqueue from lowest priority to highest priority so higher-priority toasts appear first.
      for(let i = toastBulletins.length - 1; i >= 0; i--) {
        showBulletinToast(toastBulletins[i]);
      }

      if(modalBulletins.length > 0) {
        setModalQueue(modalBulletins);
      }
    } catch {
      // Cloud bulletin check must always fail silently
    }
  }, []);

  useEffect(() => {
    if(!enabled) return;

    let cancelled = false;
    fetchBulletins(() => cancelled);

    return () => {
      cancelled = true;
    };
  }, [enabled, fetchBulletins]);

  const currentModal = modalQueue[0] ?? null;

  const handleDismissModal = (bulletin: ResolvedBulletin) => {
    if(bulletin.policy === "once") {
      markBulletinDismissed(bulletin.readKey);
    }
    setModalQueue((prev) => prev.filter((item) => item.readKey !== bulletin.readKey));
  };

  return (
    <BulletinDialog
      key={currentModal?.readKey ?? "empty"}
      bulletin={currentModal}
      open={currentModal !== null}
      onOpenChange={(nextOpen) => {
        if(!nextOpen && currentModal) {
          handleDismissModal(currentModal);
        }
      }}/>
  );
}
