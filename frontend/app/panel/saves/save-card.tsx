import type { Save } from "@/lib/types";
import { Download, FolderPen, Trash2 } from "lucide-react";
import { toast } from "sonner";
import download from "downloadjs";
import { Card } from "@/components/ui/card";
import { cn, formatDataSize, gameModeToString } from "@/lib/utils";
import { MinecraftText } from "@/components/mc-text";
import { Badge } from "@/components/ui/badge";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { Button } from "@/components/ui/button";
import { sendDeleteRequest, sendGetBlobRequest, sendPostRequest, toastError } from "@/lib/api";
import { Alert } from "@/components/alert";
import { SaveSheet } from "./save-sheet";
import { emitter } from "@/lib/emitter";

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
    const res = await sendGetBlobRequest(`/api/saves/${name}`);
    download(res, `${name}.zip`, "application/zip");
  };

  const handleDelete = async () => {
    try {
      await sendDeleteRequest(`/api/saves/${name}`);
      emitter.emit("refresh-data");
    } catch (e: any) {
      toast.error(`无法删除存档 ${name}`, { description: e.message });
    }
  };

  const handleSetCurrent = async () => {
    if(isCurrent) return;
    try {
      await sendPostRequest(`/api/control/world?save=${name}`);
      emitter.emit("refresh-data");
      toast.success("切换成功", { description: !isRunning ? "重启服务器以使改动生效" : "当前存档正在运行" });
    } catch (e: any) {
      toastError(e, "无法切换当前存档", [
        [400, "请求参数错误"],
        [401, "未登录"],
        [404, "找不到该存档"],
        [500, "服务器内部错误"]
      ]);
    }
  };

  return (
    <Card className={cn(
      "rounded-md min-h-fit px-3 py-3 flex flex-col justify-between hover:bg-muted",
      isCurrent && "bg-green-50 border-green-600 dark:bg-green-950 dark:border-green-900",
      className
    )}>
      <Tooltip>
        <TooltipTrigger asChild>
          <div
            className="w-full flex flex-col gap-1 px-1 overflow-hidden cursor-pointer"
            onClick={() => handleSetCurrent()}>
            <MinecraftText className="wrap-anywhere">{displayName}</MinecraftText>
            <span className="text-sm text-muted-foreground w-full overflow-hidden whitespace-nowrap text-ellipsis">{name}</span>
          </div>
        </TooltipTrigger>
        <TooltipContent>{path}</TooltipContent>
      </Tooltip>
      <div className="flex justify-between items-center">
        <div className="flex items-center gap-3">
          <span className="text-sm pl-1">{gameModeToString(defaultGameMode)}</span>
          {isRunning && (
            <Badge variant="outline" className="h-fit border-emerald-700 dark:border-green-900">
              <div className="w-2 h-2 rounded-full bg-green-600"/>
              正在运行
            </Badge>
          )}
          <span className="mr-2 text-sm text-muted-foreground font-[Consolas]">{formatDataSize(size)}</span>
        </div>
        <div className="[&_button]:cursor-pointer">
          {
            isCurrent
            ? (
              <Alert
                title={`确定要保存并下载当前存档 "${name}" 吗？`}
                description="将要下载的存档是当前服务器正在运行的存档，在处理存档前服务器会对存档进行保存，此操作可能影响服务器性能。"
                onAction={() => toast.promise(handleDownload(), {
                  loading: "正在处理文件...",
                  error: `无法下载存档 ${name}.zip`
                })}
                asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  title="下载存档">
                  <Download />
                </Button>
              </Alert>
            )
            : (
              <Button
                variant="ghost"
                size="icon"
                title="下载存档"
                onClick={() => toast.promise(handleDownload(), {
                  loading: "正在处理文件...",
                  error: `无法下载存档 ${name}.zip`
                })}>
                <Download />
              </Button>
            )
          }
          <SaveSheet save={save} asChild>
            <Button
              variant="ghost"
              size="icon"
              title="编辑存档">
              <FolderPen />
            </Button>
          </SaveSheet>
          <Alert
            title={`确定要删除存档 "${name}" 吗？`}
            description="此操作不可逆，被删除的存档将无法恢复。"
            onAction={() => handleDelete()}
            asChild>
            <Button
              variant="ghost"
              size="icon"
              disabled={isRunning || isCurrent}
              title="删除存档">
              <Trash2 />
            </Button>
          </Alert>
        </div>
      </div>
    </Card>
  );
}
