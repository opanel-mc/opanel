"use client";

import { useRouter } from "next/navigation";
import { SquareTerminal } from "lucide-react";
import { FunctionalCard } from "@/components/functional-card";
import { Input } from "@/components/ui/input";
import { useWebSocket } from "@/hooks/use-websocket";
import { TerminalViewer } from "@/components/terminal-viewer";
import { $ } from "@/lib/i18n";
import { TerminalClient } from "@/lib/ws/terminal";
import { getSettings } from "@/lib/settings";

export function TerminalCard({
  className,
}: Readonly<{
  className?: string
}>) {
  const { push } = useRouter();
  const client = useWebSocket(TerminalClient);

  return (
    <FunctionalCard
      icon={SquareTerminal}
      title={$("dashboard.terminal.title")}
      moreLink="/panel/terminal"
      className={className}
      innerClassName="p-2 pt-0 flex-1 min-h-0 flex flex-col gap-2 !overflow-hidden">
      <TerminalViewer
        client={client}
        simple
        levels={getSettings("terminal.log-levels")}
        className="flex-1 min-h-0"/>
      <Input
        className="w-full rounded-sm cursor-pointer"
        placeholder={$("dashboard.terminal.input.placeholder")}
        title={$("dashboard.terminal.input.tooltip")}
        onClick={() => push("/panel/terminal")}/>
    </FunctionalCard>
  );
}
