import type { EditorRefType, WhitelistResponse } from "@/lib/types";
import { useRef, useState, type PropsWithChildren } from "react";
import { Editor } from "@monaco-editor/react";
import { useTheme } from "next-themes";
import { toast } from "sonner";
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
  SheetTrigger
} from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { sendGetRequest, sendPostRequest, toastError } from "@/lib/api";
import { setWhitelistEnabled } from "./player-utils";

export function WhitelistSheet({
  children,
  asChild
}: PropsWithChildren & {
  asChild?: boolean
}) {
  const [value, setValue] = useState<string>("");
  const { theme } = useTheme();
  const editorRef = useRef<EditorRefType>(null);

  const fetchServerWhitelist = async () => {
    try {
      const res = await sendGetRequest<WhitelistResponse>("/api/whitelist");
      setValue(JSON.stringify(res.whitelist, undefined, 2));
    } catch (e: any) {
      toastError(e, "无法获取白名单", [
        [401, "未登录"],
        [500, "服务器内部错误"]
      ]);
    }
  };

  const saveServerWhitelist = async () => {
    if(!editorRef.current) return;
    const newValue = editorRef.current.getValue();

    if(newValue === value) return;

    try {
      await sendPostRequest("/api/whitelist/write", JSON.parse(editorRef.current.getValue()));
      toast.success("保存成功");
    } catch (e: any) {
      toastError(e, "无法保存白名单", [
        [401, "未登录"],
        [500, "服务器内部错误"]
      ]);
    }
  };

  return (
    <Sheet onOpenChange={(open) => open && fetchServerWhitelist()}>
      <SheetTrigger asChild={asChild}>{children}</SheetTrigger>
      <SheetContent>
        <SheetHeader>
          <SheetTitle>编辑白名单</SheetTitle>
          <SheetDescription>
            在此编辑服务器玩家白名单，你也可以直接编辑服务器目录下的<code>whitelist.json</code>文件。
          </SheetDescription>
        </SheetHeader>
        <div className="flex flex-col h-full">
          <Editor
            defaultLanguage="json"
            defaultValue={value}
            theme={theme === "dark" ? "vs-dark" : "vs"}
            options={{
              minimap: { enabled: false },
              automaticLayout: true
            }}
            onMount={(editor) => editorRef.current = editor}/>
        </div>
        <SheetFooter>
          <Button
            variant="destructive"
            className="cursor-pointer"
            onClick={async () => {
              await setWhitelistEnabled(false);
              window.location.reload();
            }}>
            禁用白名单
          </Button>
          <SheetClose asChild>
            <Button
              className="cursor-pointer"
              onClick={() => saveServerWhitelist()}>保存设置</Button>
          </SheetClose>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}
