"use client";

import { useEffect } from "react";
import { Editor, type EditorProps, loader } from "@monaco-editor/react";
import * as monaco from "monaco-editor";

import EditorWorker from "monaco-editor/esm/vs/editor/editor.worker?worker";
import JsonWorker from "monaco-editor/esm/vs/language/json/json.worker?worker";
import TypeScriptWorker from "monaco-editor/esm/vs/language/typescript/ts.worker?worker";
import YamlWorker from "monaco-yaml/yaml.worker?worker";

import { enableAutomaticLayout } from "@/lib/monaco/editor-layout";
import "@/lib/monaco/opanel-theme-def";
import "@/lib/monaco/server-log-def";
import "@/lib/monaco/task-command-def";

export default function MonacoEditor({
  onMount,
  options,
  autoFitHeight = false,
  ...props
}: EditorProps & {
  autoFitHeight?: boolean
}) {
  const automaticLayout = options?.automaticLayout ?? true;

  useEffect(() => {
    if(typeof window === "undefined") return;

    document.fonts.ready.then(() => {
      monaco.editor.remeasureFonts();
    });

    (window as any).MonacoEnvironment = {
      getWorker: (_workerId: never, label: string) => {
        switch(label) {
          case "json":
            return new JsonWorker();
          case "typescript":
          case "ts":
            return new TypeScriptWorker();
          case "yaml":
            return new YamlWorker();
          default:
            return new EditorWorker();
        }
      }
    };
  }, []);

  const handleEditorDidMount = (editor: monaco.editor.IStandaloneCodeEditor) => {
    if(automaticLayout) {
      enableAutomaticLayout(editor);
    }

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
      options={{
        ...options,
        // @monaco-editor/react enables this internally. Its synchronous
        // ResizeObserver callback can trigger a browser resize loop, so the
        // observer above performs the layout on the next animation frame.
        automaticLayout: false
      }}
      onMount={(editor, monaco) => {
        handleEditorDidMount(editor);
        onMount && onMount(editor, monaco);
      }}
      {...props}/>
  );
}
