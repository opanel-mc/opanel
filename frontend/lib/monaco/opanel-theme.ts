import * as monaco from "monaco-editor";

monaco.editor.defineTheme("opanel-theme", {
  base: "vs",
  inherit: true,
  rules: [],
  colors: {
    "editor.background": "#FFFFFF",
    "editor.selectionBackground": "#d9d9d9ee",
  }
});
monaco.editor.defineTheme("opanel-theme-dark", {
  base: "vs-dark",
  inherit: true,
  rules: [],
  colors: {
    "editor.background": "#0a0a0a",
    "editor.selectionBackground": "#3b3b3bee",
  }
});
monaco.editor.defineTheme("opanel-theme-dark-default", {
  base: "vs-dark",
  inherit: true,
  rules: [],
  colors: {
    "editor.selectionBackground": "#3b3b3bee",
  }
});
