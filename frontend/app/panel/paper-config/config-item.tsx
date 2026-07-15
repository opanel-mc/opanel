import { FileCog } from "lucide-react";
import { FilesEditorSidebarListItem } from "@/components/ui/files-editor";

export function ConfigItem({
  name,
  isActive,
  isSaved = true,
  onClick
}: {
  name: string
  isActive: boolean
  isSaved?: boolean
  onClick?: () => void
}) {
  return (
    <FilesEditorSidebarListItem
      className="justify-between"
      isActive={isActive}
      onClick={() => onClick && onClick()}>
      <div className="flex items-center gap-2">
        <FileCog />
        <span>{name}</span>
      </div>
      {(isActive && !isSaved) && (
        <div className="rounded-full w-2 h-2 mr-2 bg-foreground"/>
      )}
    </FilesEditorSidebarListItem>
  );
}
