import type { ScheduledTask } from "@/lib/types";
import { useEffect, useState } from "react";
import { FileClock, Trash } from "lucide-react";
import { FilesEditorSidebarListItem } from "@/components/ui/files-editor";
import { base64ToString, cn } from "@/lib/utils";
import { googleSansCode } from "@/lib/fonts";
import { Button } from "@/components/ui/button";
import { sendDeleteRequest, sendPatchRequest, toastError } from "@/lib/api";
import { Switch } from "@/components/ui/switch";
import { $ } from "@/lib/i18n";

export function TaskItem({
  task,
  isActive,
  onClick,
  onDelete
}: {
  task: ScheduledTask
  isActive: boolean
  onClick?: () => void
  onDelete?: () => void
}) {
  const [enabled, setEnabled] = useState(task.enabled);

  const handleDelete = async () => {
    try {
      await sendDeleteRequest(`/api/tasks/${task.id}`);
      onDelete && onDelete();
    } catch (e: any) {
      toastError(e, $("tasks.delete.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [404, $("tasks.error.404")],
        [500, $("common.error.500")]
      ]);
    }
  };

  const handleToggle = async (enabled: boolean) => {
    try {
      await sendPatchRequest(`/api/tasks/${task.id}?enabled=${enabled ? "1" : "0"}`);
    } catch (e: any) {
      toastError(e, enabled ? $("tasks.toggle.enable.error") : $("tasks.toggle.disable.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [404, $("tasks.error.404")],
        [500, $("common.error.500")]
      ]);
    }
  };

  useEffect(() => {
    handleToggle(enabled);
  // oxlint-disable-next-line react/exhaustive-deps
  }, [enabled]);

  return (
    <FilesEditorSidebarListItem
      className="hover:bg-muted border grid grid-rows-2 grid-cols-2 gap-1"
      id={task.id}
      isActive={isActive}
      onClick={() => onClick && onClick()}>
      <div className="flex items-center gap-2">
        <FileClock />
        <span className="whitespace-nowrap overflow-hidden text-ellipsis">
          {base64ToString(task.name)}
        </span>
        {enabled && (
          <div className="rounded-full w-1 h-1 mr-2 bg-green-600 dark:bg-emerald-500"/>
        )}
      </div>
      <Button
        variant="ghost"
        size="icon-sm"
        className="ml-auto -mr-1 cursor-pointer"
        title={$("tasks.delete")}
        onClick={(e) => {
          e.stopPropagation();
          handleDelete();
        }}>
        <Trash />
      </Button>
      <span className={cn("align-bottom text-xs text-muted-foreground whitespace-nowrap overflow-hidden text-ellipsis", googleSansCode.className)}>
        {task.cron}
      </span>
      <Switch
        className="ml-auto"
        checked={enabled}
        onClick={(e) => e.stopPropagation()}
        onCheckedChange={setEnabled}/>
    </FilesEditorSidebarListItem>
  );
}
