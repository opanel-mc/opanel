import { useCallback, useEffect } from "react";

interface KeydownOptions {
  ctrl?: boolean
  shift?: boolean
  alt?: boolean
}

export function useKeydown(key: string, options: KeydownOptions, cb: (e: KeyboardEvent) => void) {
  const handleKeydown = useCallback((e: KeyboardEvent) => {
    if(typeof e.key !== "string" || e.key.toLowerCase() !== key.toLowerCase()) return;
    if(options.ctrl && !e.ctrlKey) return;
    if(options.shift && !e.shiftKey) return;
    if(options.alt && !e.altKey) return;

    cb(e);
  }, [key, options, cb]);

  useEffect(() => {
    document.body.addEventListener("keydown", handleKeydown);
    return () => document.body.removeEventListener("keydown", handleKeydown);
  }, [handleKeydown]);
}
