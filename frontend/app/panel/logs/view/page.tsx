"use client";

import type { LogResponse } from "@/lib/types";
import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";
import Editor, { type OnMount } from "@monaco-editor/react";
import { useTheme } from "next-themes";
import { SubPage } from "@/app/panel/sub-page";
import { sendGetRequest } from "@/lib/api";

export default function LogView() {
  const searchParams = useSearchParams();
  const { push } = useRouter();
  const [content, setContent] = useState("");
  const { theme } = useTheme();
  const editorRef = useRef<Parameters<OnMount>[0]>(null);
  const log = searchParams.get("log");

  const fetchLogContent = useCallback(async () => {
    try {
      const res = await sendGetRequest<LogResponse>(`/api/logs/${log}`);
      setContent(res.log.replaceAll("\t", "\n"));
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    } catch (e) {
      toast.error("无法获取日志内容");
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
      <Editor
        height="500px"
        defaultLanguage="txt"
        defaultValue={content}
        theme={theme === "dark" ? "vs-dark" : "vs"}
        onMount={(editor) => editorRef.current = editor}/>
    </SubPage>
  );
}
