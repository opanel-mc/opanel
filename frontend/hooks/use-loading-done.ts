import { useEffect } from "react";
import { emitter } from "@/lib/emitter";

export function useLoadingDone() {
  useEffect(() => {
    emitter.emit("loading-done");
  }, []);
}
