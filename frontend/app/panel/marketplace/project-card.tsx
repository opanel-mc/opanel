"use client";

import type { MarketplaceProjectHit } from "@/lib/types";
import { Download, Heart, Package } from "lucide-react";
import { base64ToString, cn } from "@/lib/utils";
import { $ } from "@/lib/i18n";
import { formatNumber } from "./marketplace-utils";

export function ProjectCard({
  project,
  onClick
}: {
  project: MarketplaceProjectHit
  onClick: () => void
}) {
  return (
    <div className="rounded-xl border bg-card/80 shadow-sm transition hover:border-primary/40">
      <button
        type="button"
        className="flex w-full flex-col gap-3 p-4 text-left"
        onClick={onClick}>
        <div className="flex items-center gap-3">
          <div className="h-10 w-10 min-w-10 overflow-hidden rounded-md border bg-muted/40">
            {
              project.iconUrl
              ? <img
                  className="h-full w-full object-cover"
                  src={project.iconUrl}
                  alt=""
                  loading="lazy"/>
              : <div className="flex h-full w-full items-center justify-center text-muted-foreground">
                  <Package className="size-5" />
                </div>
            }
          </div>
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-semibold">{base64ToString(project.title)}</p>
            <p className="truncate text-xs text-muted-foreground">{base64ToString(project.author)}</p>
          </div>
        </div>
        <p className="line-clamp-2 text-sm text-muted-foreground">{base64ToString(project.summary)}</p>
        <div className="mt-auto flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
          <span className="inline-flex items-center gap-1">
            <Download className="size-3.5" />
            {formatNumber(project.downloads)}
          </span>
          <span className="inline-flex items-center gap-1">
            <Heart className="size-3.5" />
            {formatNumber(project.follows)}
          </span>
          <span className={cn(
            "ml-auto rounded-sm bg-muted/60 px-1.5 py-0.5 font-medium uppercase tracking-wide",
            "text-[10px] text-muted-foreground"
          )}>
            {project.projectType}
          </span>
        </div>
        <span className="sr-only">{$("marketplace.card.view-detail")}</span>
      </button>
    </div>
  );
}