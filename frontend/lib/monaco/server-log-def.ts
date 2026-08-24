import * as monaco from "monaco-editor";

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
  brackets: [["(", ")"], ["[", "]"]]
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
  colors: {
    "editor.background": "#FFFFFF",
    "editor.selectionBackground": "#d9d9d9ee",
  }
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
  colors: {
    "editor.background": "#0a0a0a",
    "editor.selectionBackground": "#3b3b3bee",
  }
});
