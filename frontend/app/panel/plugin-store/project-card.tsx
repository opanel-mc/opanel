import type { PluginStoreProject } from "@/lib/types";
import Link from "next/link";
import { type PropsWithChildren } from "react";
import * as MarkdownJSX from "markdown-to-jsx";
import { BookText, CodeXml, Download, Ellipsis, HandCoins, SquareArrowOutUpRight } from "lucide-react";
import { Card } from "@/components/ui/card";
import PackIcon from "@/assets/images/pack.png";
import { Button } from "@/components/ui/button";
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
import { $ } from "@/lib/i18n";
import { Badge } from "@/components/ui/badge";

export function ProjectCard({
  id,
  title,
  description,
  detailedUrl,
  sourceUrl,
  docsUrl,
  donationUrl,
  iconUrl,
  loaders,
  downloads,
  readme,
  onInstall
}: PluginStoreProject & {
  onInstall?: () => void
}) {
  return (
    <ProjectDialog
      id={id}
      title={title}
      description={description}
      detailedUrl={detailedUrl}
      sourceUrl={sourceUrl}
      docsUrl={docsUrl}
      donationUrl={donationUrl}
      iconUrl={iconUrl}
      loaders={loaders}
      downloads={downloads}
      readme={readme}
      onInstall={onInstall}
      asChild>
      <Card className="rounded-md h-32 p-0 dark:bg-transparent flex flex-row gap-4 overflow-hidden cursor-pointer">
        <img
          className="aspect-square h-full object-cover"
          src={iconUrl ?? PackIcon.src}
          alt={title}/>
        <div className="flex-1 py-3 flex flex-col gap-2">
          <h3 className="font-semibold">{title}</h3>
          <span className="max-w-full text-sm text-muted-foreground line-clamp-2">{description}</span>
          <div className="mt-auto space-x-1">
            {loaders.map((loader, i) => (
              <Badge
                variant="secondary"
                className=""
                key={i}>
                {loader}
              </Badge>
            ))}
          </div>
        </div>
        <div className="w-fit ml-auto p-3 flex flex-col justify-between">
          <div className="flex justify-end items-center gap-2">
            <span className="text-xs">{downloads}</span>
            <Download size={16}/>
          </div>
          <Button
            variant="outline"
            size="sm"
            className="cursor-pointer"
            onClick={(e) => {
              e.stopPropagation();
              onInstall && onInstall();
            }}>
            <Download />
            安装
          </Button>
        </div>
      </Card>
    </ProjectDialog>
  );
}

function ProjectDialog({
  id,
  title,
  description,
  detailedUrl,
  sourceUrl,
  docsUrl,
  donationUrl,
  iconUrl,
  loaders,
  downloads,
  readme,
  children,
  onInstall,
  asChild
}: PropsWithChildren & PluginStoreProject & {
  onInstall?: () => void
  asChild?: boolean
}) {
  return (
    <Dialog>
      <DialogTrigger asChild={asChild}>{children}</DialogTrigger>
      <DialogContent className="min-sm:min-w-[700px]">
        <DialogHeader className="h-20 flex-row gap-5">
          <img
            className="aspect-square h-full object-cover rounded-xs"
            src={iconUrl ?? PackIcon.src}
            alt={title}/>
          <div className="flex flex-col gap-2">
            <DialogTitle>{title}</DialogTitle>
            <DialogDescription className="line-clamp-2">{description}</DialogDescription>
          </div>
        </DialogHeader>
        <div className="flex flex-col gap-2">
          <div className="pb-2 border-b flex max-md:flex-wrap *:cursor-pointer">
            {docsUrl && (
              <Button
                variant="ghost"
                size="sm"
                asChild>
                <Link href={docsUrl} target="_blank">
                  <BookText />
                  文档
                  <SquareArrowOutUpRight className="!size-3 ml-1" stroke="var(--color-muted-foreground)"/>
                </Link>
              </Button>
            )}
            {sourceUrl && (
              <Button
                variant="ghost"
                size="sm"
                asChild>
                <Link href={sourceUrl} target="_blank">
                  <CodeXml />
                  源代码
                  <SquareArrowOutUpRight className="!size-3 ml-1" stroke="var(--color-muted-foreground)"/>
                </Link>
              </Button>
            )}
            {donationUrl && (
              <Button
                variant="ghost"
                size="sm"
                asChild>
                <Link href={donationUrl} target="_blank">
                  <HandCoins />
                  捐助
                  <SquareArrowOutUpRight className="!size-3 ml-1" stroke="var(--color-muted-foreground)"/>
                </Link>
              </Button>
            )}
            <Button
              variant="ghost"
              size="sm"
              className="ml-auto max-sm:ml-0"
              asChild>
              <Link href={detailedUrl} target="_blank">
                <Ellipsis />
                详情
              </Link>
            </Button>
            <Button
              variant="outline"
              size="sm"
              className="ml-2"
              onClick={() => onInstall && onInstall()}>
              <Download />
              安装
            </Button>
          </div>
          <div className="max-h-96 pr-1 overflow-y-auto o-scrollbar">
            <MarkdownJSX.default
              options={{
                wrapper: "article",
                forceWrapper: true
              }}
              className="text-sm [&>p]:font-normal [&>span]:font-normal *:wrap-anywhere">
              {readme ?? ""}
            </MarkdownJSX.default>
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
