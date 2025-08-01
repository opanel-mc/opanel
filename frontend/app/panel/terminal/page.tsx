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
import { getCurrentArgumentNumber } from "@/lib/utils";
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

export default function Terminal() {
  const client = useTerminal();
  const inputRef = useRef<HTMLInputElement>(null);
  const [autocompleteList, setAutocompleteList] = useState<string[]>([]);
  const [historyList, setHistoryList] = useState<string[]>([]);
  const [logLevel, setLogLevel] = useState(defaultLogLevel);

  const handleClear = () => {
    if(!inputRef.current) return;

    inputRef.current.value = "";
  };

  const handleSend = useCallback(() => {
    if(!inputRef.current || !client) return;

    const command = inputRef.current.value;
    if(command.length === 0) {
      toast.warning("请输入指令以发送");
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

  return (
    <SubPage title="后台" icon={<SquareTerminal />} className="grid grid-cols-5 max-md:grid-cols-4 max-sm:grid-cols-1 gap-3">
      <div className="h-[500px] max-h-[500px] col-span-4 max-md:col-span-3 flex flex-col gap-3">
        <TerminalConnector client={client} level={logLevel} className="flex-1"/>
        <div className="flex gap-2">
          <Select
            defaultValue={defaultLogLevel}
            onValueChange={(value) => setLogLevel(value as ConsoleLogLevel)}>
            <SelectTrigger className="w-24 font-[Consolas]" title="日志等级">
              <SelectValue />
            </SelectTrigger>
            <SelectContent className="font-[Consolas]">
              <SelectItem value="INFO">INFO</SelectItem>
              <SelectItem value="WARN">WARN</SelectItem>
              <SelectItem value="ERROR">ERROR</SelectItem>
            </SelectContent>
          </Select>
          <AutocompleteInput
            className="flex-1 w-full rounded-sm font-[Consolas]"
            placeholder="发送消息 / 指令..."
            autoFocus
            itemList={autocompleteList}
            onKeyDown={(e) => handleKeydown(e)}
            onInput={() => handleInput()}
            ref={inputRef}/>
          <Button
            variant="ghost"
            size="icon"
            className="cursor-pointer"
            title="清空"
            onClick={() => handleClear()}>
            <X />
          </Button>
          <Button
            size="icon"
            className="cursor-pointer"
            title="发送"
            onClick={() => handleSend()}>
            <ArrowUp />
          </Button>
        </div>
      </div>
      <div className="flex flex-col gap-2 max-sm:hidden">
        <div className="px-3 flex justify-between items-center">
          <h2 className="text-md font-semibold">历史记录</h2>
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
              className="block px-2 py-0 rounded-xs text-left font-[Consolas] text-nowrap text-ellipsis overflow-hidden cursor-pointer"
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
