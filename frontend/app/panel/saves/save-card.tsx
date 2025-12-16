import type { DownloadSaveResponse, Save } from "@/lib/types";
import { Download, FolderPen, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Card } from "@/components/ui/card";
import { base64ToString, cn, formatDataSize, gameModeToString } from "@/lib/utils";
import { MinecraftText } from "@/components/mc-text";
import { Badge } from "@/components/ui/badge";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { Button } from "@/components/ui/button";
import { apiUrl, sendDeleteRequest, sendGetRequest, sendPostRequest, toastError } from "@/lib/api";
import { Alert } from "@/components/alert";
import { SaveSheet } from "./save-sheet";
import { emitter } from "@/lib/emitter";
import { googleSansCode } from "@/lib/fonts";
import { $ } from "@/lib/i18n";

export function SaveCard({
  save,
  className
}: {
  save: Save
  className?: string
}) {
  const {
    name,
    displayName,
    path,
    size,
    isRunning,
    isCurrent,
    defaultGameMode
  } = save;

  const handleDownload = async () => {
    const res = await sendGetRequest<DownloadSaveResponse>(`/api/saves/${name}`);
    window.location.href = `${apiUrl}/file/${res.download}/${name}.zip`;
  };

  const handleDelete = async () => {
    try {
      await sendDeleteRequest(`/api/saves/${name}`);
      emitter.emit("refresh-data");
    } catch (e: any) {
      toast.error($("saves.list.item.delete.error", name), { description: e.message });
    }
  };

  const handleSetCurrent = async () => {
    if(isCurrent) return;
    try {
      await sendPostRequest(`/api/control/world?save=${name}`);
      emitter.emit("refresh-data");
      toast.success($("saves.list.item.switch.success"), {
        description: (
          !isRunning
          ? $("saves.list.item.switch.success.description1")
          : $("saves.list.item.switch.success.description2")
        )
      });
    } catch (e: any) {
      toastError(e, $("saves.list.item.switch.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [404, $("saves.list.item.switch.error.404")],
        [500, $("common.error.500")]
      ]);
    }
  };

  return (
    <Card className={cn(
      "rounded-md min-h-fit px-3 py-3 flex flex-col justify-between hover:bg-muted",
      isCurrent && "bg-green-50 hover:bg-background border-green-600 dark:bg-green-950 dark:border-green-900",
      className
    )}>
      <Tooltip>
        <TooltipTrigger asChild>
          <div
            className="w-full flex flex-col gap-1 px-1 overflow-hidden cursor-pointer"
            onClick={() => handleSetCurrent()}>
            <MinecraftText className="wrap-anywhere">{base64ToString(displayName)}</MinecraftText>
            <span className="text-sm text-muted-foreground w-full overflow-hidden whitespace-nowrap text-ellipsis">{name}</span>
          </div>
        </TooltipTrigger>
        <TooltipContent>{path}</TooltipContent>
      </Tooltip>
      <div className="flex justify-between items-center">
        <div className="flex items-center flex-wrap gap-3 pl-1">
          <span className="text-sm">{gameModeToString(defaultGameMode)}</span>
          {isRunning && (
            <Badge variant="outline" className="h-fit border-emerald-700 dark:border-green-900">
              <div className="w-2 h-2 rounded-full bg-green-600"/>
              {$("saves.list.item.current")}
            </Badge>
          )}
          <span className={cn("mr-2 text-xs text-muted-foreground", googleSansCode.className)}>{formatDataSize(size)}</span>
        </div>
        <div className="flex self-end [&_button]:cursor-pointer">
          {
            isCurrent
            ? (
              <Alert
                title={$("saves.list.item.download.alert.title", name)}
                description={$("saves.list.item.download.alert.description")}
                onAction={() => toast.promise(handleDownload(), {
                  loading: $("saves.list.item.download.loading"),
                  error: $("saves.list.item.download.error", name)
                })}
                asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  title={$("saves.list.item.download")}>
                  <Download />
                </Button>
              </Alert>
            )
            : (
              <Button
                variant="ghost"
                size="icon"
                title={$("saves.list.item.download")}
                onClick={() => toast.promise(handleDownload(), {
                  loading: $("saves.list.item.download.loading"),
                  error: $("saves.list.item.download.error", name)
                })}>
                <Download />
              </Button>
            )
          }
          <SaveSheet save={save} asChild>
            <Button
              variant="ghost"
              size="icon"
              title={$("saves.list.item.edit")}>
              <FolderPen />
            </Button>
          </SaveSheet>
          <Alert
            title={$("saves.list.item.delete.alert.title", name)}
            description={$("saves.list.item.delete.alert.description")}
            onAction={() => handleDelete()}
            asChild>
            <Button
              variant="ghost"
              size="icon"
              disabled={isRunning || isCurrent}
              title={$("saves.list.item.delete")}>
              <Trash2 />
            </Button>
          </Alert>
        </div>
      </div>
    </Card>
  );
}
