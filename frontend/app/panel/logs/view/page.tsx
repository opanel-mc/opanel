"use client";

import type { EditorRefType } from "@/lib/types";
import dynamic from "next/dynamic";
import { useCallback, useEffect, useRef, useState } from "react";
import { Download, Trash2 } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import { useTheme } from "next-themes";
import { SubPage } from "@/app/panel/sub-page";
import { sendGetRequest, toastError } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { deleteLog, downloadLog } from "../log-utils";
import { monacoSettingsOptions } from "@/lib/settings";
import { $ } from "@/lib/i18n";
import { Text } from "@/components/i18n-text";

const MonacoEditor = dynamic(() => import("@/components/monaco-editor"), { ssr: false });

export default function LogView() {
  const searchParams = useSearchParams();
  const { push } = useRouter();
  const [content, setContent] = useState("");
  const { theme } = useTheme();
  const editorRef = useRef<EditorRefType>(null);
  const log = searchParams.get("log");

  const fetchLogContent = useCallback(async () => {
    if(!log) {
      push("/panel/logs");
      return;
    }

    try {
      const res = await sendGetRequest<string>(`/api/logs/${log}`);
      setContent(res);
    } catch (e: any) {
      toastError(e, $("logs.view.fetch.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [404, $("logs.view.fetch.error.404")],
        [500, $("common.error.500")]
      ]);
    }
  }, [log, push]);

  useEffect(() => {
    if(!editorRef.current) return;

    editorRef.current.setValue(content);
  }, [content]);

  useEffect(() => {
    fetchLogContent();
  }, [fetchLogContent]);

  return (
    <SubPage title={$("logs.title")} subTitle={log ?? ""}>
      <div className="mb-3 flex justify-between items-center">
        <Text
          id="logs.view.hint"
          args={[
            (
              log?.endsWith(".log.gz")
              ? `gzip (${$("logs.view.decompressed")})`
              : "log"
            )
          ]}
          className="text-sm text-muted-foreground"/>
        <div className="[&>*]:cursor-pointer">
          <Button
            variant="ghost"
            size="icon"
            title={$("logs.action.download")}
            onClick={() => downloadLog(log ?? "")}>
            <Download />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            title={$("logs.action.delete")}
            disabled={log?.endsWith(".log")}
            onClick={async () => {
              await deleteLog(log ?? "");
              push("/panel/logs");
            }}>
            <Trash2 />
          </Button>
        </div>
      </div>
      <MonacoEditor
        height="550px"
        defaultLanguage="server-log"
        defaultValue={content}
        theme={theme === "dark" ? "server-log-theme-dark" : "server-log-theme"}
        options={{
          readOnly: true,
          readOnlyMessage: {
            value: $("logs.view.monaco.readonly")
          },
          ...monacoSettingsOptions
        }}
        onMount={(editor) => editorRef.current = editor}/>
    </SubPage>
  );
}
