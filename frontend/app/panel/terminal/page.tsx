"use client";

import type { CommandShortcut } from "@/lib/types";
import {
  type KeyboardEvent,
  useCallback,
  useEffect,
  useRef,
  useState
} from "react";
import { ArrowUp, Maximize, Minimize, Pen, Plus, SquareTerminal, Trash2, X } from "lucide-react";
import { toast } from "sonner";
import { useWebSocket } from "@/hooks/use-websocket";
import { TerminalViewer } from "@/components/terminal-viewer";
import { Button } from "@/components/ui/button";
import { AutocompleteInput } from "@/components/autocomplete-input";
import { cn, getCurrentArgumentIndex } from "@/lib/utils";
import { Card } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "@/components/ui/select";
import { SubPage } from "../sub-page";
import { changeSettings, getSettings } from "@/lib/settings";
import { googleSansCode } from "@/lib/fonts";
import { $ } from "@/lib/i18n";
import { type ConsoleLogLevel, defaultLogLevel, TerminalClient } from "@/lib/ws/terminal";
import { Toggle } from "@/components/ui/toggle";
import { CreateShortcutDialog } from "./create-shortcut-dialog";

export default function Terminal() {
  const client = useWebSocket(TerminalClient);
  const inputRef = useRef<HTMLInputElement | null>(null);
  const terminalContainerRef = useRef<HTMLDivElement | null>(null);
  const argIndexRef = useRef(0);
  const [autocompleteList, setAutocompleteList] = useState<string[]>([]);
  const [historyList, setHistoryList] = useState<string[]>(getSettings("state.terminal.history"));
  const [logLevel, setLogLevel] = useState(defaultLogLevel);
  const [fullscreen, setFullscreen] = useState(false);
  const [shortcuts, setShortcuts] = useState<CommandShortcut[]>(getSettings("terminal.shortcuts"));
  const [editingShortcuts, setEditingShortcuts] = useState(false);

  const handleSend = useCallback(() => {
    if(!inputRef.current || !client) return;

    const command = inputRef.current.value;
    if(command.length === 0) {
      toast.warning($("terminal.input.empty"));
      return;
    }

    client.send("command", command);
    argIndexRef.current = 0;
    setHistoryList((current) => [...current, command]);
    inputRef.current.value = "";
    inputRef.current?.focus();
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

  const handleInput = useCallback(async () => {
    if(!inputRef.current || !client) return;
    const elem = inputRef.current;
    const hasPrefix = elem.value.startsWith("/");
    const command = hasPrefix ? elem.value.substring(1) : elem.value;

    const realArgIndex = getCurrentArgumentIndex(command, (elem.selectionStart ?? 0) - (hasPrefix ? 1 : 0));
    if(realArgIndex !== argIndexRef.current) {
      client.send("autocomplete", {
        command,
        argIndex: realArgIndex
      });
      argIndexRef.current = realArgIndex;
    }
  }, [client]);

  const handleFullscreen = () => {
    if(!terminalContainerRef.current) return;

    const terminalContainer = terminalContainerRef.current;
    if(!document.fullscreenElement && !fullscreen) {
      terminalContainer.requestFullscreen();
      setFullscreen(true);
    } else if(document.fullscreenElement && fullscreen) {
      document.exitFullscreen();
      setFullscreen(false);
    }

    inputRef.current?.focus();
  };

  const handleFullscreenChange = () => {
    setFullscreen(!!document.fullscreenElement);
  };

  const handleRemoveShortcut = (index: number) => {
    const original = getSettings("terminal.shortcuts");
    if(index < 0 || index >= original.length) return;

    const newShortcuts = [];
    for(let i = 0; i < original.length; i++) {
      if(i !== index) newShortcuts.push(original[i]);
    }
    setShortcuts(newShortcuts);
  };

  useEffect(() => {
    client?.subscribe("autocomplete", (data: string[]) => {
      setAutocompleteList(data);
    });
  }, [client]);

  useEffect(() => {
    changeSettings("state.terminal.history", historyList);
  }, [historyList]);

  useEffect(() => {
    changeSettings("terminal.shortcuts", shortcuts);
  }, [shortcuts]);

  useEffect(() => {
    document.addEventListener("fullscreenchange", handleFullscreenChange);

    return () => document.removeEventListener("fullscreenchange", handleFullscreenChange);
  }, []);

  return (
    <SubPage
      title={$("terminal.title")}
      category={$("sidebar.server")}
      icon={<SquareTerminal />}
      outerClassName="max-h-screen overflow-y-hidden"
      className="flex-1 min-h-0 flex gap-3">
      <div className="flex-4/5 max-lg:flex-3/4 max-md:flex-2/3 min-w-0 flex flex-col gap-2">
        <div className={cn("px-2 flex flex-wrap items-center gap-1 transition-[gap]", editingShortcuts && "gap-3")}>
          {shortcuts.map((shortcut, i) => (
            <div
              className="relative *:cursor-pointer"
              key={i}>
              <Button
                variant="outline"
                size="xs"
                disabled={editingShortcuts}
                onClick={() => {
                  if(!inputRef.current) return;
                  inputRef.current.value = shortcut.command;
                  inputRef.current.focus();
                }}
                onDoubleClick={() => handleSend()}>
                {shortcut.name}
              </Button>
              {editingShortcuts && (
                <button
                  className="absolute -top-1 -left-2 rounded-full bg-accent p-0.5 z-10"
                  onClick={() => handleRemoveShortcut(i)}>
                  <X size={13}/>
                </button>
              )}
            </div>
          ))}
          <div className="flex *:cursor-pointer">
            <CreateShortcutDialog
              onCreate={(shortcut) => setShortcuts((current) => [...current, shortcut])}
              asChild>
              <Button
                variant="ghost"
                size="icon-xs">
                <Plus />
              </Button>
            </CreateShortcutDialog>
            <Toggle
              variant="ghost"
              size="icon-xs"
              className="data-[state=on]:*:fill-foreground"
              onPressedChange={(pressed) => setEditingShortcuts(pressed)}>
              <Pen />
            </Toggle>
          </div>
        </div>
        <div
          className="min-h-0 bg-background flex-1 flex flex-col border rounded-sm"
          ref={terminalContainerRef}>
          <TerminalViewer client={client} level={logLevel} className="flex-1 border-none"/>
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
              prefix="/"
              maxLength={256}
              onKeyDown={(e) => handleKeydown(e)}
              onInput={() => handleInput()}
              ref={inputRef}/>
            <Button
              variant="ghost"
              size="icon"
              className="cursor-pointer"
              title={fullscreen ? $("terminal.exit-fullscreen") : $("terminal.fullscreen")}
              onClick={() => handleFullscreen()}>
              {fullscreen ? <Minimize /> : <Maximize />}
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
        <Card className="dark:bg-transparent flex-1 rounded-sm p-1 flex flex-col gap-0 overflow-y-auto">
          {historyList.map((command, i) => (
            <Button
              variant="ghost"
              size="sm"
              className={cn("block px-2 py-0 rounded-xs text-left text-nowrap text-ellipsis overflow-hidden cursor-pointer", googleSansCode.className)}
              onClick={() => {
                if(!inputRef.current) return;
                inputRef.current.value = command;
                inputRef.current.focus();
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
