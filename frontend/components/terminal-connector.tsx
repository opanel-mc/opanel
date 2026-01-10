import { useEffect, useRef, useState } from "react";
import { format } from "date-format-parse";
import AnsiConverter from "ansi-to-html";
import { cn, purifyUnsafeText } from "@/lib/utils";
import { getSettings } from "@/lib/settings";
import { googleSansCode } from "@/lib/fonts";
import {
  type ConsoleLog,
  type TerminalClient,
  defaultLogLevel,
  getLogLevelId,
  type ConsoleLogLevel
} from "@/lib/ws/terminal";

const MAX_LOG_LINES = getSettings("terminal.max-log-lines");

function Log({
  time,
  level,
  thread,
  source,
  line,
  thrownMessage,
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
    <p
      className={cn(
        "leading-[133%] space-x-1 selection:bg-foreground selection:text-background cursor-default",
        getSettings("terminal.word-wrap") ? "text-wrap wrap-break-word whitespace-pre-wrap" : "whitespace-pre",
        !visible ? "hidden" : "",
        googleSansCode.className
      )}
      style={{ fontSize: getSettings("terminal.font-size") +"px" }}>
      {getSettings("terminal.log-time") && (
        <span className="text-blue-500 dark:text-blue-400">{`[${format(new Date(time), "HH:mm:ss")}]`}</span>
      )}
      {(!simple && getSettings("terminal.thread-name")) && (
        <span className={threadLevelStyle}>{`[${thread}/${level}]`}</span>
      )}
      {(!simple && getSettings("terminal.source-name") && sourceName) && (
        <span className="text-emerald-600 dark:text-emerald-500 max-md:hidden">{`(${sourceName})`}</span>
      )}
      {
        thrownMessage == null
        ? (
          getSettings("terminal.convert-ansi-code")
          ? <span dangerouslySetInnerHTML={{ __html: new AnsiConverter().toHtml(line) }}/>
          : <span>{line}</span>
        )
        : (
          getSettings("terminal.convert-ansi-code")
          ? <span dangerouslySetInnerHTML={{ __html: new AnsiConverter().toHtml(line +"\n"+ thrownMessage) }}/>
          : <span>{line +"\n"+ thrownMessage}</span>
        )
      }
    </p>
  );
}

export function TerminalConnector({
  client,
  simple,
  level,
  className
}: {
  client: TerminalClient | null
  simple?: boolean
  level?: ConsoleLogLevel
  className?: string
}) {
  const terminalRef = useRef<HTMLDivElement>(null);
  const [logs, setLogs] = useState<ConsoleLog[]>([]);

  const pushLog = (log: ConsoleLog) => {
    setLogs((current) => {
      if(current.length + 1 > MAX_LOG_LINES) current.shift();
      log.line = purifyUnsafeText(log.line);
      return [...current, log];
    });
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

    client.subscribe("init", (data: ConsoleLog[]) => {
      for(let i = data.length - MAX_LOG_LINES > 0 ? data.length - MAX_LOG_LINES : 0; i < data.length; i++) {
        pushLog(data[i]);
      }
    });

    client.subscribe("log", (data: ConsoleLog) => pushLog(data));

    return () => clearLogs();
  }, [client]);
  
  return (
    <div
      className={cn(className, "border rounded-sm bg-background overflow-auto o-scrollbar p-2")}
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
