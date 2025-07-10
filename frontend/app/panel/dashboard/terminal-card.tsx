import { useRouter } from "next/navigation";
import { SquareTerminal } from "lucide-react";
import { FunctionalCard } from "@/components/functional-card";
import { Input } from "@/components/ui/input";

export function TerminalCard({
  className,
}: Readonly<{
  className?: string
}>) {
  const { push } = useRouter();

  return (
    <FunctionalCard
      icon={SquareTerminal}
      title="服务器后台"
      moreLink="/panel/terminal"
      className={className}
      innerClassName="p-2 pt-0 h-full flex flex-col gap-2 overflow-hidden">
      <div className="flex-1 w-full border rounded-sm bg-background overflow-auto p-2 [&>p]:text-xs [&>p]:font-[monospace]">
        {/** @todo */}
      </div>
      <Input
        className="w-full rounded-sm"
        placeholder="发送消息 / 指令..."
        onClick={() => push("/panel/terminal")}/>
    </FunctionalCard>
  );
}
