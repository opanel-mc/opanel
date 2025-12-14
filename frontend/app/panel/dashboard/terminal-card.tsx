"use client";

import { useRouter } from "next/navigation";
import { SquareTerminal } from "lucide-react";
import { FunctionalCard } from "@/components/functional-card";
import { Input } from "@/components/ui/input";
import { useTerminal } from "@/hooks/use-terminal";
import { TerminalConnector } from "@/components/terminal-connector";
import { $ } from "@/lib/i18n";

export function TerminalCard({
  className,
}: Readonly<{
  className?: string
}>) {
  const { push } = useRouter();
  const client = useTerminal();

  return (
    <FunctionalCard
      icon={SquareTerminal}
      title={$("dashboard.terminal.title")}
      moreLink="/panel/terminal"
      className={className}
      innerClassName="p-2 pt-0 h-full max-xl:h-96 flex flex-col gap-2 overflow-hidden">
      <TerminalConnector client={client} simple className="flex-1"/>
      <Input
        className="w-full rounded-sm cursor-pointer"
        placeholder={$("dashboard.terminal.input.placeholder")}
        title={$("dashboard.terminal.input.tooltip")}
        onClick={() => push("/panel/terminal")}/>
    </FunctionalCard>
  );
}
