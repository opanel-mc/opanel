import { useState } from "react";
import { History, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet";
import { googleSansCode } from "@/lib/fonts";
import { $ } from "@/lib/i18n";
import { cn } from "@/lib/utils";

export function HistorySheet({
  history,
  container,
  onSelect,
  onExecute,
  onClear,
  onClose
}: {
  history: string[]
  container?: HTMLElement | null
  onSelect: (command: string) => void
  onExecute: () => void
  onClear: () => void
  onClose: () => void
}) {
  const [open, setOpen] = useState(false);

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          className="cursor-pointer"
          title={$("terminal.history")}
          aria-label={$("terminal.history")}>
          <History />
        </Button>
      </SheetTrigger>
      <SheetContent
        side="right"
        container={container}
        className="gap-2"
        aria-describedby={undefined}
        onCloseAutoFocus={(e) => {
          e.preventDefault();
          onClose();
        }}>
        <SheetHeader className="shrink-0 flex-row items-center gap-2 pl-6 pr-12 pb-2">
          <SheetTitle>{$("terminal.history")}</SheetTitle>
          <Button
            variant="ghost"
            size="icon"
            className="cursor-pointer"
            onClick={onClear}>
            <Trash2 />
          </Button>
        </SheetHeader>
        <div className="min-h-0 flex-1 overflow-y-auto px-4 pb-4">
          {history.map((command, i) => (
            <Button
              variant="ghost"
              size="sm"
              className={cn("block w-full px-2 py-0 text-left truncate cursor-pointer", googleSansCode.className)}
              title={command}
              onClick={() => onSelect(command)}
              onDoubleClick={() => {
                onSelect(command);
                onExecute();
                setOpen(false);
              }}
              key={i}>
              {command}
            </Button>
          ))}
        </div>
      </SheetContent>
    </Sheet>
  );
}
