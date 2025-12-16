"use client";

import {
  type KeyboardEvent,
  useCallback,
  useEffect,
  useRef,
  useState
} from "react";
import { ArrowUp, SquareTerminal, Trash2, X } from "lucide-react";
import { toast } from "sonner";
import { useTerminal } from "@/hooks/use-terminal";
import { TerminalConnector } from "@/components/terminal-connector";
import { Button } from "@/components/ui/button";
import { AutocompleteInput } from "@/components/autocomplete-input";
import { cn, getCurrentArgumentNumber } from "@/lib/utils";
import { Card } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "@/components/ui/select";
import { defaultLogLevel, type ConsoleLogLevel } from "@/lib/terminal/log-levels";
import { SubPage } from "../sub-page";
import { changeSettings, getSettings } from "@/lib/settings";
import { googleSansCode } from "@/lib/fonts";
import { $ } from "@/lib/i18n";

export default function Terminal() {
  const client = useTerminal();
  const inputRef = useRef<HTMLInputElement>(null);
  const [autocompleteList, setAutocompleteList] = useState<string[]>([]);
  const [historyList, setHistoryList] = useState<string[]>(getSettings("state.terminal.history"));
  const [logLevel, setLogLevel] = useState(defaultLogLevel);

  const handleClear = () => {
    if(!inputRef.current) return;

    inputRef.current.value = "";
  };

  const handleSend = useCallback(() => {
    if(!inputRef.current || !client) return;

    const command = inputRef.current.value;
    if(command.length === 0) {
      toast.warning($("terminal.input.empty"));
      return;
    }

    client.send({ type: "command", data: command });
    setHistoryList((current) => [...current, command]);
    handleClear();
  }, [client]);

  const handleKeydown = (e: KeyboardEvent) => {
    if(!inputRef.current || !client) return;
    const elem = inputRef.current;

    if(document.activeElement !== elem) return;

    switch(e.key) {
      case "Enter":
        handleSend();
        setAutocompleteList([]);
        break;
    }
  };

  const handleInput = useCallback(() => {
    if(!inputRef.current || !client) return;
    const elem = inputRef.current;

    client.send({
      type: "autocomplete",
      data: getCurrentArgumentNumber(elem.value, elem.selectionStart ?? 0)
    });
  }, [client]);

  useEffect(() => {
    client?.onMessage((type, data) => {
      if(type === "autocomplete") {
        setAutocompleteList(data);
      }
    });
  }, [client]);

  useEffect(() => {
    changeSettings("state.terminal.history", historyList);
  }, [historyList]);

  return (
    <SubPage
      title={$("terminal.title")}
      icon={<SquareTerminal />}
      outerClassName="max-h-screen overflow-y-hidden"
      className="flex-1 min-h-0 flex gap-3">
      <div className="flex-4/5 max-lg:flex-3/4 max-md:flex-2/3 min-w-0 flex flex-col gap-3 border rounded-sm">
        <TerminalConnector client={client} level={logLevel} className="flex-1 border-none"/>
        <div className="p-3 flex gap-2">
          <Select
            defaultValue={defaultLogLevel}
            onValueChange={(value) => setLogLevel(value as ConsoleLogLevel)}>
            <SelectTrigger className={cn("w-24 max-sm:w-20", googleSansCode.className)} title="日志等级">
              <SelectValue />
            </SelectTrigger>
            <SelectContent className={googleSansCode.className}>
              <SelectItem value="INFO">INFO</SelectItem>
              <SelectItem value="WARN">WARN</SelectItem>
              <SelectItem value="ERROR">ERROR</SelectItem>
            </SelectContent>
          </Select>
          <AutocompleteInput
            className={cn("flex-1 w-full rounded-sm", googleSansCode.className)}
            placeholder={$("terminal.input.placeholder")}
            autoFocus
            itemList={autocompleteList}
            enabled={getSettings("terminal.autocomplete")}
            maxLength={256}
            onKeyDown={(e) => handleKeydown(e)}
            onInput={() => handleInput()}
            ref={inputRef}/>
          <Button
            variant="ghost"
            size="icon"
            className="cursor-pointer max-sm:hidden"
            title={$("terminal.clear")}
            onClick={() => handleClear()}>
            <X />
          </Button>
          <Button
            size="icon"
            className="cursor-pointer"
            title={$("terminal.send")}
            onClick={() => handleSend()}>
            <ArrowUp />
          </Button>
        </div>
      </div>
      <div className="flex-1/5 max-lg:flex-1/4 max-md:flex-1/3 min-w-0 flex flex-col gap-2 max-lg:hidden">
        <div className="px-3 flex justify-between items-center">
          <h2 className="text-md font-semibold">{$("terminal.history")}</h2>
          <Button
            variant="ghost"
            size="icon"
            className="cursor-pointer"
            onClick={() => setHistoryList([])}>
            <Trash2 />
          </Button>
        </div>
        <Card className="flex-1 rounded-sm p-1 flex flex-col gap-0 overflow-y-auto">
          {historyList.map((command, i) => (
            <Button
              variant="ghost"
              size="sm"
              className={cn("block px-2 py-0 rounded-xs text-left text-nowrap text-ellipsis overflow-hidden cursor-pointer", googleSansCode.className)}
              onClick={() => {
                if(inputRef.current) inputRef.current.value = command;
              }}
              onDoubleClick={() => handleSend()}
              key={i}>
              {command}
            </Button>
          ))}
        </Card>
      </div>
    </SubPage>
  );
}
