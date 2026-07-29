"use client";

import type { CommandShortcut } from "@/lib/types";
import {
  type KeyboardEvent,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState
} from "react";
import { ArrowUp, CaseSensitive, Filter, Maximize, Minimize, Pen, Plus, Regex, SquareTerminal, TextSearch, Trash2, X } from "lucide-react";
import { toast } from "sonner";
import { useWebSocket } from "@/hooks/use-websocket";
import { TerminalViewer } from "@/components/terminal-viewer";
import { Button } from "@/components/ui/button";
import { AutocompleteInput } from "@/components/autocomplete-input";
import { cn, getCurrentArgumentIndex } from "@/lib/utils";
import { Card } from "@/components/ui/card";
import { SubPage } from "../sub-page";
import { changeSettings, getSettings } from "@/lib/settings";
import { googleSansCode } from "@/lib/fonts";
import { $ } from "@/lib/i18n";
import { getLogLevels, TerminalClient } from "@/lib/ws/terminal";
import { Toggle } from "@/components/ui/toggle";
import { CreateShortcutDialog } from "./create-shortcut-dialog";
import { VersionContext } from "@/contexts/api-context";
import { emitter } from "@/lib/emitter";
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuTrigger
} from "@/components/ui/dropdown-menu";
import {
  InputGroup,
  InputGroupAddon,
  InputGroupButton,
  InputGroupInput
} from "@/components/ui/input-group";
import { useKeydown } from "@/hooks/use-keydown";
import { useIsMobile } from "@/hooks/use-mobile";

const MCDR_COMMAND_PREFIX = "!!";
const MCDR_AUTOCOMPLETE_LIST = [
  "MCDR",
  "help"
];

function parseRegex(regex: string): RegExp {
  try {
    return new RegExp(regex);
  } catch {
    return new RegExp("");
  }
}

export default function Terminal() {
  const isMobile = useIsMobile();
  const versionCtx = useContext(VersionContext);
  const client = useWebSocket(TerminalClient);
  const inputRef = useRef<HTMLInputElement | null>(null);
  const terminalContainerRef = useRef<HTMLDivElement | null>(null);
  const argIndexRef = useRef(0);
  const [autocompleteList, setAutocompleteList] = useState<string[]>([]);
  const [historyList, setHistoryList] = useState<string[]>(getSettings("state.terminal.history"));
  const historyIndexRef = useRef(historyList.length);
  const [showInfoLevel, setShowInfoLevel] = useState(getSettings("terminal.log-levels").includes("INFO"));
  const [showWarnLevel, setShowWarnLevel] = useState(getSettings("terminal.log-levels").includes("WARN"));
  const [showErrorLevel, setShowErrorLevel] = useState(getSettings("terminal.log-levels").includes("ERROR"));
  const [fullscreen, setFullscreen] = useState(false);
  const [shortcuts, setShortcuts] = useState<CommandShortcut[]>(getSettings("terminal.shortcuts"));
  const [editingShortcuts, setEditingShortcuts] = useState(false);
  const [isTypingMCDRCommand, setTypingMCDRCommand] = useState(false);

  const [showSearchBox, setShowSearchBox] = useState(false);
  const [searchString, setSearchString] = useState("");
  const [searchCaseSensitive, setSearchCaseSensitive] = useState(false);
  const [searchRegexMode, setSearchRegexMode] = useState(false);
  const searchInputRef = useRef<HTMLInputElement | null>(null);

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
    historyIndexRef.current = historyList.length + 1;
    inputRef.current.value = "";
    inputRef.current?.focus();
  }, [client, historyList]);

  const handleKeydown = (e: KeyboardEvent) => {
    if(!inputRef.current || !client) return;
    const elem = inputRef.current;

    if(document.activeElement !== elem) return;

    switch(e.key) {
      case "Enter":
        handleSend();
        setAutocompleteList([]);
        break;
      case "ArrowUp":
        if(e.defaultPrevented || historyList.length === 0) break;
        e.preventDefault();
        historyIndexRef.current = Math.max(0, historyIndexRef.current - 1);
        elem.value = historyList[historyIndexRef.current];
        break;
      case "ArrowDown":
        if(e.defaultPrevented || historyList.length === 0) break;
        e.preventDefault();
        historyIndexRef.current = Math.min(historyList.length, historyIndexRef.current + 1);
        elem.value = (
          historyIndexRef.current === historyList.length
          ? ""
          : historyList[historyIndexRef.current]
        );
        break;
    }
  };

  const handleInput = useCallback(async () => {
    if(!inputRef.current || !client) return;
    const elem = inputRef.current;

    if(versionCtx?.mcdr) {
      const hasMCDRPrefix = elem.value.startsWith(MCDR_COMMAND_PREFIX);
      // not typing MCDR command -> start typing MCDR command
      if(hasMCDRPrefix) {
        setAutocompleteList(MCDR_AUTOCOMPLETE_LIST);
        setTypingMCDRCommand(true);
        return;
      }
      // typing MCDR command -> not typing MCDR command
      // reset the states if the previous state is typing MCDR command
      if(isTypingMCDRCommand) {
        setAutocompleteList([]);
        setTypingMCDRCommand(false);
        argIndexRef.current = 0;
      }
    }

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
  }, [client, versionCtx, isTypingMCDRCommand]);

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
    client?.subscribe("connect", () => {
      emitter.emit("loading-done");
    });

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
    changeSettings("terminal.log-levels", getLogLevels(showInfoLevel, showWarnLevel, showErrorLevel));
  }, [showInfoLevel, showWarnLevel, showErrorLevel]);

  useEffect(() => {
    document.addEventListener("fullscreenchange", handleFullscreenChange);

    return () => document.removeEventListener("fullscreenchange", handleFullscreenChange);
  }, []);

  useKeydown("f", { ctrl: true }, (e) => {
    e.preventDefault();
    setShowSearchBox(true);
  });

  useKeydown("a", { ctrl: true }, (e) => {
    if(showSearchBox) {
      searchInputRef.current?.select();
    }
  });

  useKeydown("Escape", {}, () => {
    if(showSearchBox) {
      setSearchString("");
      setShowSearchBox(false);
      inputRef.current?.focus();
    }
  });

  return (
    <SubPage
      title={$("terminal.title")}
      category={$("sidebar.server")}
      icon={<SquareTerminal />}
      outerClassName="max-h-[100dvh] overflow-y-hidden"
      className="flex-1 min-h-0 flex gap-3"
      pageClassName="min-h-0 max-sm:p-0!">
      <div
        className="flex-4/5 max-lg:flex-3/4 max-md:flex-2/3 min-w-0 min-h-0 bg-background flex flex-col border rounded-sm max-sm:rounded-none max-sm:border-none"
        ref={terminalContainerRef}>
        <TerminalViewer
          client={client}
          levels={getLogLevels(showInfoLevel, showWarnLevel, showErrorLevel)}
          filter={searchRegexMode ? parseRegex(searchString) : searchString}
          filterCaseSensitive={searchCaseSensitive}
          className="flex-1 border-none"/>
        <div className="px-3 pt-1 flex justify-between items-center max-md:flex-col max-md:items-start max-md:gap-2">
          <div className={cn("flex flex-wrap items-center gap-1 transition-[gap]", editingShortcuts && "gap-3")}>
            {versionCtx?.mcdr && (
              <Button
                size="xs"
                disabled={editingShortcuts}
                className={cn("cursor-pointer", googleSansCode.className)}
                onClick={() => {
                  if(!inputRef.current) return;
                  inputRef.current.value = "!!MCDR ";
                  inputRef.current.focus();
                }}
                onDoubleClick={() => handleSend()}>
                !!MCDR
              </Button>
            )}
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
          {showSearchBox && (
            <div className="min-w-72 py-1 min-md:self-end max-sm:min-w-full">
              <InputGroup className="h-6 rounded-sm">
                <InputGroupAddon className="pl-2">
                  <TextSearch />
                </InputGroupAddon>
                <InputGroupInput
                  autoFocus
                  placeholder={$("terminal.filter.placeholder")}
                  value={searchString}
                  onChange={(e) => setSearchString(e.target.value)}
                  className={cn("px-2 text-xs!", googleSansCode.className)}
                  ref={searchInputRef}/>
                <InputGroupAddon align="inline-end" className="pr-2 gap-0 *:px-1.5!">
                  <InputGroupButton
                    onClick={() => {
                      setSearchCaseSensitive((prev) => !prev);
                      searchInputRef.current?.focus();
                    }}
                    className={cn("cursor-pointer transition-none hover:text-muted-foreground hover:bg-transparent!", searchCaseSensitive && "text-theme hover:text-theme")}>
                    <CaseSensitive className="size-4"/>
                  </InputGroupButton>
                  <InputGroupButton
                    onClick={() => {
                      setSearchRegexMode((prev) => !prev);
                      searchInputRef.current?.focus();
                    }}
                    className={cn("cursor-pointer transition-none hover:text-muted-foreground hover:bg-transparent!", searchRegexMode && "text-theme hover:text-theme")}>
                    <Regex />
                  </InputGroupButton>
                </InputGroupAddon>
              </InputGroup>
            </div>
          )}
        </div>
        <div className="p-3 pt-2 flex gap-2">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="cursor-pointer">
                <Filter />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start" className={googleSansCode.className}>
              <DropdownMenuCheckboxItem checked={showInfoLevel} onCheckedChange={setShowInfoLevel}>
                INFO
              </DropdownMenuCheckboxItem>
              <DropdownMenuCheckboxItem checked={showWarnLevel} onCheckedChange={setShowWarnLevel}>
                WARN
              </DropdownMenuCheckboxItem>
              <DropdownMenuCheckboxItem checked={showErrorLevel} onCheckedChange={setShowErrorLevel}>
                ERROR
              </DropdownMenuCheckboxItem>
            </DropdownMenuContent>
          </DropdownMenu>
          <AutocompleteInput
            className={cn("flex-1 w-full rounded-sm", googleSansCode.className)}
            placeholder={$("terminal.input.placeholder")}
            autoFocus
            itemList={autocompleteList}
            enabled={getSettings("terminal.autocomplete")}
            prefix={versionCtx?.mcdr && isTypingMCDRCommand ? MCDR_COMMAND_PREFIX : "/"}
            maxLength={256}
            onKeyDown={(e) => handleKeydown(e)}
            onInput={() => handleInput()}
            ref={inputRef}/>
          <Button
            variant="ghost"
            size="icon"
            className={cn("cursor-pointer", isMobile && "hidden")}
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
