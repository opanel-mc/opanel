import { File, Trash } from "lucide-react";
import { Alert } from "@/components/alert";
import { Button } from "@/components/ui/button";
import { sendDeleteRequest, toastError } from "@/lib/api";
import { emitter } from "@/lib/emitter";
import { cn } from "@/lib/utils";
import { $ } from "@/lib/i18n";

export function CodeOfConductItem({
  lang,
  isActive,
  onClick
}: {
  lang: string
  isActive: boolean
  onClick?: () => void
}) {
  const handleDelete = async () => {
    try {
      await sendDeleteRequest(`/api/control/code-of-conduct?lang=${lang}`);
      emitter.emit("refresh-data");
    } catch (e: any) {
      toastError(e, $("coc.item.delete.error", lang), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [500, $("common.error.500")],
        [503, $("coc.error.503")]
      ]);
    }
  };

  return (
    <div
      className={cn("rounded-sm px-3 py-1 flex justify-between cursor-pointer", isActive && "bg-muted")}
      onClick={() => onClick && onClick()}>
      <div className="flex items-center gap-2">
        <File size={17}/>
        <span className="text-sm">{lang +".txt"}</span>
      </div>
      <Alert
        title={$("coc.item.delete.alert.title", lang)}
        description={$("coc.item.delete.alert.description")}
        onAction={() => handleDelete()}
        asChild>
        <Button
          variant="ghost"
          size="icon-sm"
          title={$("coc.item.delete")}
          className="cursor-pointer">
          <Trash />
        </Button>
      </Alert>
    </div>
  );
}
