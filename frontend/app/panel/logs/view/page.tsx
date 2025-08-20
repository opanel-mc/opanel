"use client";

import type { EditorRefType } from "@/lib/types";
import { useCallback, useEffect, useRef, useState } from "react";
import { Download, Trash2 } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import Editor from "@monaco-editor/react";
import { useTheme } from "next-themes";
import { SubPage } from "@/app/panel/sub-page";
import { sendGetRequest, toastError } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { deleteLog, downloadLog } from "../log-utils";
import { monacoSettingsOptions } from "@/lib/settings";

export default function LogView() {
  const searchParams = useSearchParams();
  const { push } = useRouter();
  const [content, setContent] = useState("");
  const { theme } = useTheme();
  const editorRef = useRef<EditorRefType>(null);
  const log = searchParams.get("log");

  const fetchLogContent = useCallback(async () => {
    try {
      const res = await sendGetRequest<string>(`/api/logs/${log}`);
      setContent(res);
    } catch (e: any) {
      toastError(e, "无法获取日志内容", [
        [400, "请求参数错误"],
        [401, "未登录"],
        [404, "找不到该日志"],
        [500, "服务器内部错误"]
      ]);
    }
  }, [log]);

  useEffect(() => {
    if(!editorRef.current) return;

    editorRef.current.setValue(content);
  }, [content]);

  useEffect(() => {
    fetchLogContent();
  }, [fetchLogContent]);

  if(!log) {
    push("/panel/logs");
  }

  return (
    <SubPage title="日志" subTitle={log ?? ""}>
      <div className="mb-3 flex justify-between items-center">
        <span className="text-sm text-muted-foreground">
          文件类型：{log?.endsWith(".log.gz") ? "gzip (已解压)" : "log"}
        </span>
        <div className="[&>*]:cursor-pointer">
          <Button
            variant="ghost"
            size="icon"
            title="下载日志"
            onClick={() => downloadLog(log ?? "")}>
            <Download />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            title="删除日志"
            onClick={async () => {
              await deleteLog(log ?? "");
              push("/panel/logs");
            }}>
            <Trash2 />
          </Button>
        </div>
      </div>
      <Editor
        height="500px"
        defaultLanguage="txt"
        defaultValue={content}
        theme={theme === "dark" ? "vs-dark" : "vs"}
        options={{
          readOnly: true,
          readOnlyMessage: {
            value: "日志不可编辑"
          },
          ...monacoSettingsOptions
        }}
        onMount={(editor) => editorRef.current = editor}/>
    </SubPage>
  );
}
