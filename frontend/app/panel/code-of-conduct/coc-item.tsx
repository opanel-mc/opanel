import { File, Trash } from "lucide-react";
import { Alert } from "@/components/alert";
import { Button } from "@/components/ui/button";
import { sendDeleteRequest, toastError } from "@/lib/api";
import { emitter } from "@/lib/emitter";
import { cn } from "@/lib/utils";

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
      toastError(e, `无法删除行为准则: ${lang}.txt`, [
        [400, "请求参数错误"],
        [401, "未登录"],
        [500, "服务器内部错误"],
        [503, "该服务端版本不支持行为准则"]
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
        title={`确定要删除行为准则文档 ${lang}.txt 吗？`}
        description="被删除的行为准则文档将不可恢复。"
        onAction={() => handleDelete()}
        asChild>
        <Button
          variant="ghost"
          size="icon-sm"
          className="cursor-pointer">
          <Trash />
        </Button>
      </Alert>
    </div>
  );
}
