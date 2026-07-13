"use client";

import { useEffect } from "react";
import { Editor, type EditorProps } from "@monaco-editor/react";
import { loader } from "@monaco-editor/react";
import * as monaco from "monaco-editor";

import "@/lib/monaco/opanel-theme";
import "@/lib/monaco/server-log";
import "@/lib/monaco/task-command";

export default function MonacoEditor({
  onMount,
  autoFitHeight = false,
  ...props
}: EditorProps & {
  autoFitHeight?: boolean
}) {
  useEffect(() => {
    if(typeof window === "undefined") return;

    document.fonts.ready.then(() => {
      monaco.editor.remeasureFonts();
    });

    (window as any).MonacoEnvironment = {
      getWorker: (_workerId: never, label: string) => {
        switch(label) {
          case "json":
            return new Worker(
              new URL("monaco-editor/esm/vs/language/json/json.worker", import.meta.url)
            );
          case "typescript":
          case "ts":
            return new Worker(
              new URL("monaco-editor/esm/vs/language/typescript/ts.worker", import.meta.url)
            );
          case "yaml":
            return new Worker(
              new URL("monaco-yaml/yaml.worker", import.meta.url)
            );
          default:
            return new Worker(
              new URL("monaco-editor/esm/vs/editor/editor.worker", import.meta.url)
            );
        }
      }
    };
  }, []);

  const handleEditorDidMount = (editor: monaco.editor.IStandaloneCodeEditor) => {
    if(!autoFitHeight) return;

    const container = editor.getDomNode();
    if(!container) return;

    const lineCount = editor.getModel()?.getLineCount() ?? 1;
    const lineHeight = editor.getOption(monaco.editor.EditorOption.lineHeight);
    const fitHeight = lineCount * lineHeight + editor.getOption(monaco.editor.EditorOption.padding).top + editor.getOption(monaco.editor.EditorOption.padding).bottom;
    container.style.height = `${fitHeight}px`;
    editor.layout();
  };

  loader.config({ monaco });
  return (
    <Editor
      onMount={(editor, monaco) => {
        handleEditorDidMount(editor);
        onMount && onMount(editor, monaco);
      }}
      {...props}/>
  );
}
