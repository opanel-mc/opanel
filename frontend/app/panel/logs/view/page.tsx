"use client";

import type { EditorRefType } from "@/lib/types";
import dynamic from "next/dynamic";
import { useCallback, useEffect, useRef, useState } from "react";
import { CloudUpload, Download, Trash2 } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import { useTheme } from "next-themes";
import { SubPage } from "@/app/panel/sub-page";
import { sendGetRequest, toastError } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { deleteLog, downloadLog, uploadLogToMclogs } from "../log-utils";
import { monacoSettingsOptions } from "@/lib/settings";
import { $ } from "@/lib/i18n";
import { Text } from "@/components/i18n-text";
import { emitter } from "@/lib/emitter";
import { Alert } from "@/components/alert";

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
    } finally {
      emitter.emit("loading-done");
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
    <SubPage
      title={$("logs.title")}
      subTitle={log ?? ""}
      category={$("sidebar.management")}
      pageClassName="min-h-0"
      className="flex-1 min-h-0 flex flex-col">
      <div className="mb-3 shrink-0 flex justify-between items-center gap-3 max-md:flex-col max-md:items-start">
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
        <div className="[&>*]:cursor-pointer max-md:self-end">
          {log && (
            <Alert
              title={$("logs.action.upload.confirm.title")}
              description={$("logs.action.upload.confirm.description")}
              onAction={() => uploadLogToMclogs(log)}
              asChild>
              <Button
                variant="outline"
                size="sm"
                className="mr-2">
                <CloudUpload />
                {$("logs.action.upload")}
              </Button>
            </Alert>
          )}
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
        height="100%"
        defaultLanguage="server-log"
        defaultValue={content}
        theme={theme === "dark" ? "server-log-theme-dark" : "server-log-theme"}
        options={{
          readOnly: true,
          readOnlyMessage: {
            value: $("logs.view.monaco.readonly")
          },
          contextmenu: false,
          ...monacoSettingsOptions
        }}
        className="flex-1 min-h-96 border rounded-md overflow-hidden"
        onMount={(editor) => editorRef.current = editor}/>
    </SubPage>
  );
}
