import { useEffect, useRef } from "react";
import { WebSocketClient } from "@/lib/terminal/client";
import { cn } from "@/lib/utils";

export function TerminalConnector({
  client,
  className
}: {
  client: WebSocketClient | null
  className?: string
}) {
  const terminalRef = useRef<HTMLDivElement>(null);

  const pushLog = (log: string) => {
    if(!terminalRef.current) return;

    const elem = terminalRef.current;
    const p = document.createElement("p");
    p.innerText = log;
    elem.appendChild(p);
    elem.scrollTo({ top: elem.scrollHeight });
  };

  useEffect(() => {
    if(!client) return;
    
    client.onMessage((type, data) => {
      switch(type) {
        case "init":
          for(const item of data) {
            pushLog(item);
          }
          break;
        case "log":
          pushLog(data);
          break;
      }
    });
  }, [client]);
  
  return (
    <div
      className={cn(className, "flex-1 w-full border rounded-sm bg-background overflow-auto p-2 [&>p]:text-xs [&>p]:font-[Consolas]")}
      ref={terminalRef}/>
  );
}
