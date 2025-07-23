import type { EditorRefType, ServerPropertiesResponse } from "@/lib/types";
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
import { sendGetRequest, sendPostRequest } from "@/lib/api";

export function ServerSheet({
  children,
  asChild
}: PropsWithChildren & {
  asChild?: boolean
}) {
  const [value, setValue] = useState<string>("");
  const { theme } = useTheme();
  const editorRef = useRef<EditorRefType>(null);

  const fetchConfigFile = async () => {
    try {
      const res = await sendGetRequest<ServerPropertiesResponse>(`/api/control/properties`);
      setValue(res.properties);
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    } catch (e) {
      toast.error("无法获取server.properties");
    }
  };

  const saveConfigFile = async () => {
    if(!editorRef.current) return;
    const newValue = editorRef.current.getValue();

    if(newValue === value) return;

    try {
      await sendPostRequest(`/api/control/properties`, editorRef.current.getValue());
      toast.success("保存成功", { description: "重启服务器以使改动生效" });
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    } catch (e) {
      toast.error("无法保存server.properties");
    }
  };
  
  return (
    <Sheet onOpenChange={(open) => open && fetchConfigFile()}>
      <SheetTrigger asChild={asChild}>{children}</SheetTrigger>
      <SheetContent>
        <SheetHeader>
          <SheetTitle>编辑 server.properties</SheetTitle>
          <SheetDescription>
            在此编辑服务器配置文件，你也可以直接编辑服务器目录下的<code>server.properties</code>文件。
          </SheetDescription>
        </SheetHeader>
        <div className="flex flex-col h-full">
          <Editor
            defaultLanguage="ini"
            defaultValue={value}
            theme={theme === "dark" ? "vs-dark" : "vs"}
            options={{
              minimap: { enabled: false },
              automaticLayout: true
            }}
            onMount={(editor) => editorRef.current = editor}/>
        </div>
        <SheetFooter>
          <span className="text-sm text-muted-foreground">需重启服务器以使改动生效。</span>
          <SheetClose asChild>
            <Button
              className="cursor-pointer"
              onClick={() => saveConfigFile()}>保存设置</Button>
          </SheetClose>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}
