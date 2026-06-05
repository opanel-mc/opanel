"use client";

import { usePathname } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { cn } from "@/lib/utils";
import { emitter } from "@/lib/emitter";

const DURATION = 300;
const INITIAL_PROGRESS = 15;
const UPDATE_INTERVAL_MS = 700;

export function LoadingBar() {
  const pathname = usePathname();
  const [progress, setProgress] = useState(0);
  const [visible, setVisible] = useState(false);
  const timerRef = useRef<NodeJS.Timeout | null>(null);
  const doneTimersRef = useRef<NodeJS.Timeout[]>([]);
  const neverMountedRef = useRef(true);
  const isActiveRef = useRef(false);

  const handleDone = () => {
    if(!isActiveRef.current) return;
    isActiveRef.current = false;
    doneTimersRef.current.forEach(clearTimeout);
    doneTimersRef.current = [];
    setProgress(100);

    doneTimersRef.current.push(
      setTimeout(() => setVisible(false), DURATION),
      setTimeout(() => setProgress(0), 2 * DURATION)
    );
  };

  useEffect(() => {
    if(neverMountedRef.current) {
      neverMountedRef.current = false;
      return;
    }

    doneTimersRef.current.forEach(clearTimeout);
    doneTimersRef.current = [];
    isActiveRef.current = true;
    setVisible(true);
    setProgress(INITIAL_PROGRESS);
    timerRef.current = setInterval(() => {
      setProgress((prev) => prev >= 80 ? prev : prev + 10);
    }, UPDATE_INTERVAL_MS);

    return () => {
      if(timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
    };
  }, [pathname]);

  useEffect(() => {
    emitter.on("loading-done", handleDone);

    return () => {
      emitter.off("loading-done", handleDone);
    };
  }, []);

  return (
    <div
      className={cn(
        "absolute top-0 left-0 right-0 h-0.5 bg-[#f89d13] dark:bg-highlight-primary transition-[width,opacity] ease-out z-20",
        visible ? "opacity-100" : "opacity-0"
      )}
      style={{
        width: `${progress}%`,
        transitionDuration: `${DURATION}ms`
      }}/>
  );
}
