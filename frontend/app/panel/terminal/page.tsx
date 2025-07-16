"use client";

import { useCallback, useEffect, useRef } from "react";
import { ArrowUp, SquareTerminal, X } from "lucide-react";
import { SubPage } from "../sub-page";
import { useTerminal } from "@/hooks/use-terminal";
import { TerminalConnector } from "@/components/terminal-connector";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";

export default function Terminal() {
  const client = useTerminal();
  const inputRef = useRef<HTMLInputElement>(null);

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

  useEffect(() => {
    const handleKeydown = (e: KeyboardEvent) => {
      switch(e.key) {
        case "Enter":
          handleSend();
          break;
      }
    };

    document.body.addEventListener("keydown", handleKeydown);
    return () => document.body.removeEventListener("keydown", handleKeydown);
  }, [handleSend]);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  return (
    <SubPage title="后台" icon={<SquareTerminal />} className="h-[500px] flex flex-col gap-3">
      <TerminalConnector client={client} className="flex-1"/>
      <div className="flex gap-1">
        <Input
          className="flex-1 w-full rounded-sm font-[Consolas]"
          placeholder="发送消息 / 指令..."
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
