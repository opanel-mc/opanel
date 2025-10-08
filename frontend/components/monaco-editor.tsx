"use client";

import { Editor, type EditorProps } from "@monaco-editor/react";
import { loader } from "@monaco-editor/react";
import * as monaco from "monaco-editor";

export default function MonacoEditor(props: EditorProps) {
  loader.config({ monaco });
  return <Editor {...props}/>;
}
