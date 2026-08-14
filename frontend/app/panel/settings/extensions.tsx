"use client";

import type { Extension, ExtensionsResponse } from "@/lib/types";
import Link from "next/link";
import { type DragEvent, useCallback, useEffect, useRef, useState } from "react";
import { Ban, Check, Download, PackagePlus, Puzzle, RotateCw, Search, SquareArrowOutUpRight, Trash2, Upload, X } from "lucide-react";
import { toast } from "sonner";
import { sendGetRequest, toastError, uploadFile } from "@/lib/api";
import { $ } from "@/lib/i18n";
import { emitter } from "@/lib/emitter";
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
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle
} from "@/components/ui/empty";
import { Input } from "@/components/ui/input";
import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput
} from "@/components/ui/input-group";
import { Label } from "@/components/ui/label";
import { Progress } from "@/components/ui/progress";
import { Badge } from "@/components/ui/badge";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger
} from "@/components/ui/tooltip";
import { Text } from "@/components/i18n-text";
import { base64ToString, cn, formatDataSize } from "@/lib/utils";
import { googleSansCode } from "@/lib/fonts";
import { deleteExtension, downloadExtension, toggleExtension } from "./extensions-utils";

function ExtensionUploadDialog({
  open,
  onOpenChange,
  onInputFile
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  onInputFile: (fileList: FileList | null) => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogTrigger asChild>
        <Button className="cursor-pointer">
          <Upload />
          {$("extensions.action.upload")}
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{$("extensions.action.upload.title")}</DialogTitle>
          <DialogDescription>{$("extensions.action.upload.description")}</DialogDescription>
        </DialogHeader>
        <Label htmlFor="extension-file">{$("extensions.action.upload.input.label")}</Label>
        <Input
          id="extension-file"
          type="file"
          accept=".jar"
          onChange={(e) => onInputFile(e.currentTarget.files)}/>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline">{$("dialog.close")}</Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function ExtensionItem({ extension }: { extension: Extension }) {
  const fileName = base64ToString(extension.fileName);
  const description = base64ToString(extension.description);

  return (
    <div className="grid grid-cols-[minmax(10rem,1fr)_minmax(12rem,1.5fr)_auto_auto] items-center gap-4 border-b px-3 py-2 last:border-b-0 max-lg:grid-cols-[minmax(0,1fr)_auto_auto] max-sm:grid-cols-[minmax(0,1fr)_auto]">
      <Tooltip>
        <TooltipTrigger asChild>
          <div className="min-w-0 leading-5">
            <div className="flex min-w-0 items-baseline gap-2">
              <span className="text-sm truncate font-semibold">{extension.name}</span>
              <span className="shrink-0 text-xs text-muted-foreground">v{extension.version}</span>
            </div>
            <div className={cn("truncate text-xs text-muted-foreground", googleSansCode.className)}>
              {extension.extId}
            </div>
          </div>
        </TooltipTrigger>
        <TooltipContent>
          {fileName}
        </TooltipContent>
      </Tooltip>
      <div className="min-w-0 leading-5 max-lg:hidden">
        <div className="truncate text-sm">{description}</div>
        <div className="truncate text-xs text-muted-foreground">
          {extension.author}
          <span className="mx-2">·</span>
          <span className={googleSansCode.className}>{formatDataSize(extension.size)}</span>
        </div>
      </div>
      <Badge
        variant="outline"
        className={
          extension.enabled
          ? "text-green-600 dark:text-green-400"
          : "text-muted-foreground"
        }>
        {extension.enabled ? <Check /> : <X />}
        {
          extension.enabled
          ? $("extensions.item.enabled")
          : $("extensions.item.disabled")
        }
      </Badge>
      <div className="flex min-md:justify-end [&>*]:cursor-pointer">
        {extension.hasWebIndex && (
          <Button
            variant="ghost"
            size="icon-sm"
            title={$("extensions.action.open")}
            asChild>
            <Link
              href={`/panel/ext/${encodeURIComponent(extension.extId)}`}
              target="_blank"
              rel="noopener noreferrer">
              <SquareArrowOutUpRight />
            </Link>
          </Button>
        )}
        <Button
          variant="ghost"
          size="icon-sm"
          title={$("extensions.action.download")}
          onClick={() => downloadExtension(fileName)}>
          <Download />
        </Button>
        <Button
          variant="ghost"
          size="icon-sm"
          title={extension.enabled
            ? $("extensions.action.toggle.disable")
            : $("extensions.action.toggle.enable")}
          onClick={() => toggleExtension(fileName, !extension.enabled)}>
          {extension.enabled
            ? <Ban className="stroke-red-400"/>
            : <PackagePlus className="stroke-green-600"/>}
        </Button>
        <Button
          variant="ghost"
          size="icon-sm"
          title={$("extensions.action.delete")}
          onClick={() => deleteExtension(fileName)}>
          <Trash2 className="stroke-red-400"/>
        </Button>
      </div>
    </div>
  );
}

export function Extensions() {
  const [extensions, setExtensions] = useState<Extension[]>([]);
  const [folderPath, setFolderPath] = useState("");
  const [searchString, setSearchString] = useState("");
  const [uploadVisible, setUploadVisible] = useState(false);
  const [uploadName, setUploadName] = useState<string | null>(null);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const dragDepthRef = useRef(0);

  const fetchExtensions = useCallback(async () => {
    try {
      const res = await sendGetRequest<ExtensionsResponse>("/api/extensions");
      setExtensions(res.extensions.sort((a, b) => a.name.localeCompare(b.name)));
      setFolderPath(res.folderPath);
    } catch (e: any) {
      toastError(e, $("extensions.fetch.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    } finally {
      emitter.emit("loading-done");
    }
  }, []);

  const handleUpload = async (file: File) => {
    setUploadVisible(false);

    if(!file.name.endsWith(".jar")) {
      toast.error($("extensions.action.upload.error"), {
        description: $("extensions.action.upload.error.description")
      });
      return;
    }

    setUploadName(file.name);
    setUploadProgress(0);
    try {
      await uploadFile("/api/extensions", file, setUploadProgress);
      await fetchExtensions();
    } catch (e: any) {
      toastError(e, $("extensions.action.upload.error"), [
        [400, $("extensions.action.upload.error.400")],
        [401, $("common.error.401")],
        [403, $("extensions.action.error.403")],
        [409, $("extensions.action.upload.error.409")],
        [500, $("common.error.500")]
      ]);
    } finally {
      setUploadName(null);
      setUploadProgress(null);
    }
  };

  const handleInputExtensionFile = (fileList: FileList | null) => {
    const file = fileList?.[0];
    if(file) handleUpload(file);
    setUploadDialogOpen(false);
  };

  const hasDraggedFiles = (event: DragEvent<HTMLElement>) => (
    Array.from(event.dataTransfer.types).includes("Files")
  );

  const resetDragState = () => {
    dragDepthRef.current = 0;
    setUploadVisible(false);
  };

  useEffect(() => {
    fetchExtensions();

    emitter.on("refresh-data", fetchExtensions);
    return () => {
      emitter.off("refresh-data", fetchExtensions);
    };
  }, [fetchExtensions]);

  const normalizedSearch = searchString.trim().toLowerCase();
  const visibleExtensions = extensions.filter((extension) => {
    if(!normalizedSearch) return true;

    const values = [
      extension.name,
      extension.extId,
      extension.author,
      base64ToString(extension.description),
      base64ToString(extension.fileName)
    ];
    return values.some((value) => value.toLowerCase().includes(normalizedSearch));
  });

  return (
    <div
      className="relative flex flex-col gap-3"
      onDragEnter={(e) => {
        if(!hasDraggedFiles(e)) return;
        e.preventDefault();
        dragDepthRef.current += 1;
        setUploadVisible(true);
      }}
      onDragOver={(e) => {
        if(!hasDraggedFiles(e)) return;
        e.preventDefault();
      }}
      onDragLeave={(e) => {
        if(!hasDraggedFiles(e)) return;
        e.preventDefault();
        dragDepthRef.current = Math.max(dragDepthRef.current - 1, 0);
        if(dragDepthRef.current === 0) setUploadVisible(false);
      }}
      onDrop={(e) => {
        if(!hasDraggedFiles(e)) return;
        e.preventDefault();
        resetDragState();
        const file = e.dataTransfer.files[0];
        if(file) handleUpload(file);
      }}>
      {/* Drag and Drop Area */}
      <div className={cn(
        "pointer-events-none absolute inset-0 z-50 flex flex-col items-center justify-center gap-4 rounded-md bg-background/90",
        !uploadVisible && "hidden"
      )}>
        <div className="absolute inset-0 rounded-md border-4 border-dashed"/>
        <Upload size={60} stroke="var(--color-muted-foreground)"/>
        <span className="text-muted-foreground">{$("extensions.dnd.label")}</span>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <InputGroup className="mr-auto min-w-56 flex-1">
          <InputGroupAddon>
            <Search />
          </InputGroupAddon>
          <InputGroupInput
            value={searchString}
            placeholder={$("extensions.search.placeholder")}
            onChange={(e) => setSearchString(e.target.value)}/>
        </InputGroup>
        <Button
          variant="ghost"
          size="icon"
          title={$("extensions.action.refresh")}
          onClick={() => emitter.emit("refresh-data")}>
          <RotateCw />
        </Button>
        <ExtensionUploadDialog
          open={uploadDialogOpen}
          onOpenChange={setUploadDialogOpen}
          onInputFile={handleInputExtensionFile}/>
      </div>

      <div className="flex min-h-5 items-center justify-between gap-3 text-sm text-muted-foreground max-sm:flex-col max-sm:items-stretch">
        <span className="truncate max-md:text-xs" title={folderPath}>
          {$("extensions.hint", folderPath)}
        </span>
        {uploadProgress !== null && (
          <div className="flex w-72 shrink-0 flex-col items-end gap-1 max-sm:w-full">
            <span>{$("extensions.progress.label", uploadName ?? "")}</span>
            <Progress value={uploadProgress * 100} className="h-1"/>
          </div>
        )}
      </div>

      <div className="o-scrollbar flex max-h-[calc(100dvh-26rem)] flex-col overflow-y-auto rounded-md border bg-background dark:bg-transparent max-sm:max-h-[calc(100dvh-18rem)]">
        {visibleExtensions.length > 0
          ? visibleExtensions.map((extension, i) => (
            <ExtensionItem extension={extension} key={i}/>
          ))
          : (
            <Empty>
              <EmptyHeader>
                <EmptyMedia variant="icon">
                  <Puzzle />
                </EmptyMedia>
                <EmptyTitle>{$("extensions.empty.title")}</EmptyTitle>
                <EmptyDescription>
                  {normalizedSearch
                    ? $("extensions.empty.search-description")
                    : $("extensions.empty.description")}
                </EmptyDescription>
              </EmptyHeader>
            </Empty>
          )}
      </div>
      <footer className="shrink-0">
        <Text
          id="extensions.footer.description"
          args={[
            <Link
              href="https://opanel.cn/docs/extension"
              target="_blank"
              rel="noopener noreferrer"
              key={0}>
              {$("extensions.footer.guide")}
            </Link>
          ]}
          className="text-sm text-muted-foreground"/>
      </footer>
    </div>
  );
}
