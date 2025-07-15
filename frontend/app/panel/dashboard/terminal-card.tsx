"use client";

import { useRouter } from "next/navigation";
import { SquareTerminal } from "lucide-react";
import { FunctionalCard } from "@/components/functional-card";
import { Input } from "@/components/ui/input";
import { useTerminal } from "@/hooks/use-terminal";
import { TerminalConnector } from "@/components/terminal-connector";

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
      title="服务器后台"
      moreLink="/panel/terminal"
      className={className}
      innerClassName="p-2 pt-0 h-full flex flex-col gap-2 overflow-hidden">
      <TerminalConnector client={client}/>
      <Input
        className="w-full rounded-sm"
        placeholder="发送消息 / 指令..."
        onClick={() => push("/panel/terminal")}/>
    </FunctionalCard>
  );
}
