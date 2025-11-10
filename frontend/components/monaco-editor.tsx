"use client";

import { useEffect } from "react";
import { Editor, type EditorProps } from "@monaco-editor/react";
import { loader } from "@monaco-editor/react";
import * as monaco from "monaco-editor";

// server-log language support
monaco.languages.register({ id: "server-log" });
monaco.languages.setMonarchTokensProvider("server-log", {
  tokenizer: {
    root: [
      /* log time */
      [/^\[\d+:\d+:\d+\]/, "log.time"], // common format
      [/^\[.*\d+:\d+:\d+.\d+\]/, "log.time"], // forge
      /* log thread & log level */
      [/\[[\w\s\d-#]+\/INFO\]:?/, "log.level.info"], // info
      [/\[[\w\s\d-#]+\/WARN\].*/, "log.level.warn"], // warning
      [/\[[\w\s\d-#]+\/(ERROR|FATAL)\].*/, "log.level.error"], // error
      /* log source */
      [/\[[\w.]+\/\w*\]:/, "log.source"],
      /* stacktrace */
      [/^(\sat )/, "log.stacktrace"],
      [/^Caused by:.*/, "log.stacktrace.cause"],
    ]
  }
});
monaco.languages.setLanguageConfiguration("server-log", {
  brackets: [["(", ")"]]
});
monaco.editor.defineTheme("server-log-theme", {
  base: "vs",
  inherit: true,
  rules: [
    { token: "log.time", foreground: "7A7A7A" },
    { token: "log.level.info", foreground: "008000" },
    { token: "log.level.warn", foreground: "B37E1B" },
    { token: "log.level.error", foreground: "E00000" },
    { token: "log.source", foreground: "7A7A7A" },
    { token: "log.stacktrace", foreground: "E00000" },
    { token: "log.stacktrace.cause", foreground: "E00000", fontStyle: "bold" },
  ],
  colors: {}
});
monaco.editor.defineTheme("server-log-theme-dark", {
  base: "vs-dark",
  inherit: true,
  rules: [
    { token: "log.time", foreground: "7A7A7A" },
    { token: "log.level.info", foreground: "608B4E" },
    { token: "log.level.warn", foreground: "CBA228" },
    { token: "log.level.error", foreground: "DD6A6F" },
    { token: "log.source", foreground: "7A7A7A" },
    { token: "log.stacktrace", foreground: "DD6A6F" },
    { token: "log.stacktrace.cause", foreground: "DD6A6F", fontStyle: "bold" },
  ],
  colors: {}
});

export default function MonacoEditor(props: EditorProps) {
  useEffect(() => {
    if(typeof window === "undefined") return;

    (window as any).MonacoEnvironment = {
      getWorker: (_workerId: never, label: string) => {
        switch(label) {
          case "json":
            return new Worker(
              new URL("monaco-editor/esm/vs/language/json/json.worker", import.meta.url)
            );
          default:
            return new Worker(
              new URL("monaco-editor/esm/vs/editor/editor.worker", import.meta.url)
            );
        }
      }
    };
  }, []);

  loader.config({ monaco });
  return <Editor {...props}/>;
}
