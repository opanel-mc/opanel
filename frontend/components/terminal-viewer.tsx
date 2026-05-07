import { memo, useCallback, useEffect, useRef, useState } from "react";
import { format } from "date-format-parse";
import AnsiConverter from "ansi-to-html";
import { v7 as uuidv7 } from "uuid";
import { cn, purifyUnsafeText } from "@/lib/utils";
import { getSettings } from "@/lib/settings";
import { googleSansCode } from "@/lib/fonts";
import {
  type ConsoleLog,
  type TerminalClient,
  defaultLogLevel,
  getLogLevelId,
  type ConsoleLogLevel,
} from "@/lib/ws/terminal";
import { parseTextToANSI, secSign } from "@/lib/formatting-codes/text";

const MAX_LOG_LINES = getSettings("terminal.max-log-lines");
const STOP_SCROLLING_TIME = 5000;

const ansiConverter = new AnsiConverter();

/** @see https://stackoverflow.com/questions/3809401/what-is-a-good-regular-expression-to-match-a-url */
const urlRegex = /https?:\/\/(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,4}\b([-a-zA-Z0-9@:;%_\+.~#?&//=]*)/g;

function preprocessLogLine(line: string): string {
  if(getSettings("terminal.rich-style")) {
    line = ansiConverter.toHtml(parseTextToANSI(line.replaceAll("\x7f", secSign)));
  }

  return line.replace(urlRegex, (url) => `<a href="${url}" target="_blank" rel="noopener noreferrer">${url}</a>`);
}

const Log = memo(({
  time,
  level,
  thread,
  source,
  line,
  thrownMessage,
  mcdr,
  uuid,
  simple,
  visible
}: ConsoleLog & {
  simple?: boolean
  visible: boolean
}) => {
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
      data-slot="terminal-log"
      className={cn(
        "leading-[133%] space-x-1 selection:bg-foreground selection:text-background cursor-default",
        getSettings("terminal.word-wrap") ? "text-wrap wrap-break-word whitespace-pre-wrap" : "whitespace-pre",
        !visible ? "hidden" : "",
        googleSansCode.className
      )}
      style={{ fontSize: getSettings("terminal.font-size") +"px" }}
      data-time={time}
      data-id={uuid}>
      {getSettings("terminal.log-time") && (
        <span className="text-blue-500 dark:text-blue-400">{`[${format(new Date(time), "HH:mm:ss")}]`}</span>
      )}
      {mcdr && (
        <span className="text-[#c8723f]">[MCDR]</span>
      )}
      {(!simple && getSettings("terminal.thread-name")) && (
        <span className={threadLevelStyle}>{`[${thread}/${level}]`}</span>
      )}
      {(!mcdr && !simple && getSettings("terminal.source-name") && sourceName) && (
        <span className="text-emerald-600 dark:text-emerald-500 max-md:hidden">{`(${sourceName})`}</span>
      )}
      {
        thrownMessage == null
        ? <span dangerouslySetInnerHTML={{ __html: preprocessLogLine(line) }}/>
        : <span dangerouslySetInnerHTML={{ __html: preprocessLogLine(line +"\n"+ thrownMessage) }}/>
      }
    </p>
  );
});

export function TerminalViewer({
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
  const logsBufferRef = useRef<ConsoleLog[]>([]);
  const flushLogsRafRef = useRef<number | null>(null);
  const [logs, setLogs] = useState<ConsoleLog[]>([]);
  const scrollingRef = useRef(false);
  const scrollingTimerRef = useRef<NodeJS.Timeout | null>(null);

  const flushLogsBuffer = () => {
    if(logsBufferRef.current.length === 0) return;
    const buffer = [...logsBufferRef.current];
    logsBufferRef.current = [];

    setLogs((current) => {
      if(terminalRef.current) {
        const elem = terminalRef.current;
        if(elem.scrollTop + elem.clientHeight >= elem.scrollHeight - 20) {
          if(scrollingTimerRef.current) {
            clearTimeout(scrollingTimerRef.current);
            scrollingTimerRef.current = null;
          }
          scrollingRef.current = false;
        }
      }

      const newLogs = [...current, ...buffer];
      if(newLogs.length > MAX_LOG_LINES) {
        newLogs.splice(0, newLogs.length - MAX_LOG_LINES);
      }
      return newLogs;
    });
  };

  const scheduleFlushLogsBuffer = () => {
    if(flushLogsRafRef.current) return;

    flushLogsRafRef.current = requestAnimationFrame(() => {
      flushLogsRafRef.current = null;
      flushLogsBuffer();
    });
  };

  const clearLogs = () => {
    if(!terminalRef.current) return;

    terminalRef.current.innerHTML = "";
  };

  const handleScroll = useCallback(() => {
    if(!terminalRef.current) return;

    const elem = terminalRef.current;
    const scrolling = elem.scrollTop + elem.clientHeight < elem.scrollHeight - 150;
    if(scrolling && scrollingTimerRef.current) {
      clearTimeout(scrollingTimerRef.current);
      scrollingTimerRef.current = null;
    }
    scrollingRef.current = scrolling;
    
    if(!scrollingTimerRef.current) {
      scrollingTimerRef.current = setTimeout(() => {
        scrollingRef.current = false;
        scrollingTimerRef.current = null;
      }, STOP_SCROLLING_TIME);
    }
  }, []);

  useEffect(() => {
    if(!terminalRef.current) return;

    const elem = terminalRef.current;
    if(!scrollingRef.current) {
      elem.scrollTo({ top: elem.scrollHeight });
    }
  }, [logs]);

  useEffect(() => {
    if(!terminalRef.current) return;

    const elem = terminalRef.current;
    elem.addEventListener("scroll", handleScroll);

    return () => {
      if(scrollingTimerRef.current) {
        clearTimeout(scrollingTimerRef.current);
        scrollingTimerRef.current = null;
      }
      elem.removeEventListener("scroll", handleScroll);
    };
  }, [handleScroll]);

  useEffect(() => {
    if(!client) return;

    client.subscribe("init", (data: ConsoleLog[]) => {
      for(let i = data.length - MAX_LOG_LINES > 0 ? data.length - MAX_LOG_LINES : 0; i < data.length; i++) {
        data[i].line = purifyUnsafeText(data[i].line);
        data[i].uuid = uuidv7();
        logsBufferRef.current.push(data[i]);
      }
      scheduleFlushLogsBuffer();
    });

    client.subscribe("log", (data: ConsoleLog) => {
      data.line = purifyUnsafeText(data.line);
      data.uuid = uuidv7();
      logsBufferRef.current.push(data);
      scheduleFlushLogsBuffer();
    });

    client.subscribe("mcdr-log", (data: ConsoleLog) => {
      data.line = purifyUnsafeText(data.line);
      data.uuid = uuidv7();
      logsBufferRef.current.push(data);
      scheduleFlushLogsBuffer();
    });

    return () => clearLogs();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [client]);
  
  return (
    <div
      className={cn(className, "border rounded-sm bg-background overflow-auto o-scrollbar p-2")}
      ref={terminalRef}>
      {logs.map((log) => (
        <Log
          {...log}
          simple={simple}
          visible={getLogLevelId(log.level) >= getLogLevelId(level ?? defaultLogLevel)}
          key={log.uuid}/>
      ))}
    </div>
  );
}
