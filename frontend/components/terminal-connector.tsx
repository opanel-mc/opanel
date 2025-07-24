import type { ConsoleLog, WebSocketClient } from "@/lib/terminal/client";
import { useEffect, useRef, useState } from "react";
import { format } from "date-format-parse";
import { cn } from "@/lib/utils";
import { defaultLogLevel, getLogLevelId, type ConsoleLogLevel } from "@/lib/terminal/log-levels";

function Log({
  time,
  level,
  thread,
  source,
  line,
  simple,
  visible
}: ConsoleLog & {
  simple?: boolean
  visible: boolean
}) {
  const sourceStrArr = source.split(".");
  const sourceName = sourceStrArr[sourceStrArr.length - 1];

  let threadLevelStyle;
  switch(level) {
    case "INFO":
      threadLevelStyle = "text-green-700 dark:text-green-500";
      break;
    case "WARN":
      threadLevelStyle = "text-yellow-700 dark:text-yellow-600";
      break;
    case "ERROR":
      threadLevelStyle = "text-red-700 dark:text-red-400";
      break;
  }

  return (
    <p className={cn("text-xs text-nowrap font-[Consolas] space-x-1", !visible ? "hidden" : "")}>
      <span className="text-blue-500 dark:text-blue-400">{`[${format(new Date(time), "HH:mm:ss")}]`}</span>
      {!simple && <span className={threadLevelStyle}>{`[${thread}/${level}]`}</span>}
      {!simple && <span className="text-emerald-600 dark:text-emerald-500">{`(${sourceName})`}</span>}
      <span>{line}</span>
    </p>
  );
}

export function TerminalConnector({
  client,
  simple,
  level,
  className
}: {
  client: WebSocketClient | null
  simple?: boolean
  level?: ConsoleLogLevel
  className?: string
}) {
  const terminalRef = useRef<HTMLDivElement>(null);
  const [logs, setLogs] = useState<ConsoleLog[]>([]);

  const pushLog = (log: ConsoleLog) => {
    setLogs((current) => [...current, log]);
  };

  const clearLogs = () => {
    if(!terminalRef.current) return;

    terminalRef.current.innerHTML = "";
  };

  useEffect(() => {
    if(!terminalRef.current) return;

    const elem = terminalRef.current;
    elem.scrollTo({ top: elem.scrollHeight });
  }, [logs]);

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

    return () => clearLogs();
  }, [client]);
  
  return (
    <div
      className={cn(className, "border rounded-sm bg-background overflow-auto p-2")}
      ref={terminalRef}>
      {logs.map((log, i) => (
        <Log
          {...log}
          simple={simple}
          visible={getLogLevelId(log.level) >= getLogLevelId(level ?? defaultLogLevel)}
          key={i}/>
      ))}
    </div>
  );
}
