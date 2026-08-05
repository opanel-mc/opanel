"use client";

import { useEffect } from "react";
import { Editor, type EditorProps, loader } from "@monaco-editor/react";
import * as monaco from "monaco-editor";

import EditorWorker from "monaco-editor/esm/vs/editor/editor.worker?worker";
import JsonWorker from "monaco-editor/esm/vs/language/json/json.worker?worker";
import TypeScriptWorker from "monaco-editor/esm/vs/language/typescript/ts.worker?worker";
import YamlWorker from "monaco-yaml/yaml.worker?worker";

import "@/lib/monaco/opanel-theme";
import "@/lib/monaco/server-log";
import "@/lib/monaco/task-command";

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
      const container = editor.getContainerDomNode();
      let animationFrame: number | undefined;
      let lastWidth = container.clientWidth;
      let lastHeight = container.clientHeight;

      const resizeObserver = new ResizeObserver(([entry]) => {
        const width = entry?.contentRect.width ?? container.clientWidth;
        const height = entry?.contentRect.height ?? container.clientHeight;
        if(width === lastWidth && height === lastHeight) return;

        lastWidth = width;
        lastHeight = height;
        if(animationFrame !== undefined) cancelAnimationFrame(animationFrame);
        animationFrame = requestAnimationFrame(() => {
          animationFrame = undefined;
          editor.layout({ width, height });
        });
      });

      resizeObserver.observe(container);
      editor.onDidDispose(() => {
        resizeObserver.disconnect();
        if(animationFrame !== undefined) cancelAnimationFrame(animationFrame);
      });
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
