import type { editor as MonacoEditor } from "monaco-editor";

export function enableAutomaticLayout(editor: MonacoEditor.IStandaloneCodeEditor) {
  const container = editor.getContainerDomNode();
  let raf: number | undefined;
  // Always process the observer's initial notification. Monaco may have
  // measured the container before its parent finished becoming visible.
  let lastWidth = -1;
  let lastHeight = -1;

  const resizeObserver = new ResizeObserver(([entry]) => {
    const width = entry?.contentRect.width ?? container.clientWidth;
    const height = entry?.contentRect.height ?? container.clientHeight;
    if(width === lastWidth && height === lastHeight) return;

    lastWidth = width;
    lastHeight = height;
    if(raf !== undefined) cancelAnimationFrame(raf);
    raf = requestAnimationFrame(() => {
      raf = undefined;
      editor.layout({ width, height });
    });
  });

  resizeObserver.observe(container);
  editor.onDidDispose(() => {
    resizeObserver.disconnect();
    if(raf !== undefined) cancelAnimationFrame(raf);
  });
}
