"use client";

import { useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { SquareTerminal } from "lucide-react";
import { FunctionalCard } from "@/components/functional-card";
import { Input } from "@/components/ui/input";
import { WebSocketClient } from "@/lib/terminal/client";

export function TerminalCard({
  className,
}: Readonly<{
  className?: string
}>) {
  const { push } = useRouter();
  const terminalRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const client = new WebSocketClient();
    client.onMessage((msg) => {
      if(!terminalRef.current) return;

      const elem = terminalRef.current;
      if(!elem.getAttribute("data-initialized")) {
        elem.setAttribute("data-initialized", "true");
        elem.innerHTML = "";
      }

      const p = document.createElement("p");
      p.innerText = msg;
      elem.appendChild(p);
    });
    
    return () => client.close();
  }, []);

  return (
    <FunctionalCard
      icon={SquareTerminal}
      title="服务器后台"
      moreLink="/panel/terminal"
      className={className}
      innerClassName="p-2 pt-0 h-full flex flex-col gap-2 overflow-hidden">
      <div
        className="flex-1 w-full border rounded-sm bg-background overflow-auto p-2 [&>p]:text-xs [&>p]:font-[monospace]"
        ref={terminalRef}>
        <p>连接中...</p>
      </div>
      <Input
        className="w-full rounded-sm"
        placeholder="发送消息 / 指令..."
        onClick={() => push("/panel/terminal")}/>
    </FunctionalCard>
  );
}
