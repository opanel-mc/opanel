import type { Save } from "@/lib/types";
import { FolderPen, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Card } from "@/components/ui/card";
import { cn, gameModeToString } from "@/lib/utils";
import { MinecraftText } from "@/components/mc-text";
import { Badge } from "@/components/ui/badge";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { Button } from "@/components/ui/button";
import { sendDeleteRequest } from "@/lib/api";
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
    isCurrent,
    defaultGameMode
  } = save;

  const handleDelete = async () => {
    try {
      await sendDeleteRequest(`/api/saves/${name}`);
      emitter.emit("refresh-data");
    } catch (e: any) {
      toast.success(`无法删除存档 ${name}`, { description: e.message });
    }
  };

  return (
    <Card className={cn("rounded-md min-h-fit px-3 py-3 flex flex-col justify-between", className)}>
      <Tooltip>
        <TooltipTrigger asChild>
          <div className="flex justify-between px-1">
            <div className="flex flex-col gap-1">
              <MinecraftText>{displayName}</MinecraftText>
              <span className="text-sm text-muted-foreground">{name}</span>
            </div>
            {isCurrent && (
              <Badge variant="outline" className="h-fit">
                <div className="w-2 h-2 rounded-full bg-green-600"/>
                当前存档
              </Badge>
            )}
          </div>
        </TooltipTrigger>
        <TooltipContent>{path}</TooltipContent>
      </Tooltip>
      <div className="flex justify-between items-center">
        <span className="text-sm pl-1">{gameModeToString(defaultGameMode)}</span>
        <div className="[&_button]:cursor-pointer">
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
              disabled={isCurrent}
              title="删除存档">
              <Trash2 />
            </Button>
          </Alert>
        </div>
      </div>
    </Card>
  );
}
