"use client";

import type { BulletinLevel, ResolvedBulletin } from "@/lib/bulletin";
import Link from "next/link";
import * as MarkdownJSX from "markdown-to-jsx";
import { OctagonAlert, SquareArrowOutUpRight, TriangleAlert } from "lucide-react";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { isSafeBulletinUrl } from "@/lib/bulletin";
import { $ } from "@/lib/i18n";
import { cn } from "@/lib/utils";

function LevelIcon({ level }: { level: BulletinLevel }) {
  switch(level) {
    case "critical":
      return <OctagonAlert className="inline-block mr-2 size-5 shrink-0 text-destructive"/>;
    case "warning":
      return <TriangleAlert className="inline-block mr-2 size-5 shrink-0 text-amber-600 dark:text-amber-400"/>;
    default:
      return null;
  }
}

function BulletinMarkdownLink({
  href,
  children,
}: {
  href?: string
  children?: React.ReactNode
}) {
  if(!href || !isSafeBulletinUrl(href)) {
    return <span>{children}</span>;
  }

  if(href.startsWith("/")) {
    return (
      <Link href={href}>
        {children}
      </Link>
    );
  }

  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer">
      {children}
    </a>
  );
}

export function BulletinMarkdown({
  content,
  className,
}: {
  content: string
  className?: string
}) {
  return (
    <MarkdownJSX.default
      options={{
        wrapper: "article",
        forceWrapper: true,
        disableParsingRawHTML: true,
        sanitizer: (value, _tag, attribute) => {
          if(attribute === "href") {
            return isSafeBulletinUrl(value) ? value : null;
          }
          if(attribute === "src") {
            return null;
          }
          return value;
        },
        overrides: {
          a: { component: BulletinMarkdownLink },
          img: { component: () => null },
        },
      }}
      className={cn("text-sm *:wrap-anywhere [&>p]:first:mt-0", className)}>
      {content}
    </MarkdownJSX.default>
  );
}

export function BulletinDialog({
  bulletin,
  open,
  onOpenChange,
  onDismiss,
}: {
  bulletin: ResolvedBulletin | null
  open: boolean
  onOpenChange: (open: boolean) => void
  onDismiss?: (bulletin: ResolvedBulletin) => void
}) {
  if(!bulletin) return null;

  const handleOpenChange = (nextOpen: boolean) => {
    if(!nextOpen) {
      onDismiss?.(bulletin);
    }
    onOpenChange(nextOpen);
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader className="gap-2.5 text-left">
          <DialogTitle className="leading-snug">
            <LevelIcon level={bulletin.level}/>
            {bulletin.title}
          </DialogTitle>
        </DialogHeader>
        <div className="max-h-72 overflow-y-auto pr-1 text-foreground o-scrollbar">
          <BulletinMarkdown content={bulletin.content}/>
        </div>
        <DialogFooter className="flex-wrap gap-2">
          {bulletin.actions.map((action, index) => {
            const isExternal = action.url.startsWith("https://");
            return (
              <Button
                key={`${action.url}-${index}`}
                variant={index === 0 ? "default" : "outline"}
                size="sm"
                className="cursor-pointer"
                asChild>
                {isExternal ? (
                  <a
                    href={action.url}
                    target="_blank"
                    rel="noopener noreferrer">
                    {action.label}
                    <SquareArrowOutUpRight className="!size-3"/>
                  </a>
                ) : (
                  <Link
                    href={action.url}
                    onClick={() => handleOpenChange(false)}>
                    {action.label}
                  </Link>
                )}
              </Button>
            );
          })}
          <DialogClose asChild>
            <Button
              variant={bulletin.actions.length > 0 ? "outline" : "default"}
              size="sm"
              className="ml-auto cursor-pointer">
              {$("bulletin.dismiss")}
            </Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
