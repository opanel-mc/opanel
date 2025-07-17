"use client";

import {
  type KeyboardEvent,
  useCallback,
  useEffect,
  useRef,
  useState
} from "react";
import { ArrowUp, SquareTerminal, X } from "lucide-react";
import { SubPage } from "../sub-page";
import { useTerminal } from "@/hooks/use-terminal";
import { TerminalConnector } from "@/components/terminal-connector";
import { Button } from "@/components/ui/button";
import { AutocompleteInput } from "@/components/autocomplete-input";
import { getCurrentArgumentNumber } from "@/lib/utils";

export default function Terminal() {
  const client = useTerminal();
  const inputRef = useRef<HTMLInputElement>(null);
  const [autocompleteList, setAutocompleteList] = useState<string[]>([]);

  const handleClear = () => {
    if(!inputRef.current) return;

    inputRef.current.value = "";
  };

  const handleSend = useCallback(() => {
    if(!inputRef.current || !client) return;

    const msg = inputRef.current.value;
    client.send({ type: "command", data: msg });
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

    client.send({ type: "autocomplete", data: getCurrentArgumentNumber(elem.value, elem.selectionStart ?? 0) });
  }, [client]);

  useEffect(() => {
    client?.onMessage((type, data) => {
      if(type === "autocomplete") {
        setAutocompleteList(data);
      }
    });
  }, [client]);

  return (
    <SubPage title="后台" icon={<SquareTerminal />} className="h-[500px] flex flex-col gap-3">
      <TerminalConnector client={client} className="flex-1"/>
      <div className="flex gap-1">
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
    </SubPage>
  );
}
