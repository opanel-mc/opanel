import { useContext } from "react";
import { Users } from "lucide-react";
import { InfoContext } from "@/contexts/api-context";
import { FunctionalCard } from "@/components/functional-card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from "@/components/ui/table";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger
} from "@/components/ui/tooltip";
import { cn, gameModeToString } from "@/lib/utils";

export function PlayersCard({
  className,
}: Readonly<{
  className?: string
}>) {
  const ctx = useContext(InfoContext);
  
  return (
    <FunctionalCard
      icon={Users}
      title={`在线玩家 (${ctx ? ctx.onlinePlayers.length : 0} / ${ctx?.maxPlayerCount ?? 0})`}
      moreLink="/panel/players"
      className={className}
      innerClassName="p-4 pt-0">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>玩家</TableHead>
            <TableHead className="text-center">游戏模式</TableHead>
            <TableHead className="text-right">延迟</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {ctx && ctx.onlinePlayers.map(({ name, uuid, gamemode, ping }, i) => (
            <TableRow key={i}>
              <TableCell className="font-semibold">
                <Tooltip>
                  <TooltipTrigger>{name}</TooltipTrigger>
                  <TooltipContent>{uuid}</TooltipContent>
                </Tooltip>
              </TableCell>
              <TableCell className="text-center">{gameModeToString(gamemode)}</TableCell>
              <TableCell className={cn("font-[Consolas] text-right", (
                ping < 100
                ? "text-green-600"
                : (
                  ping < 200
                  ? "text-yellow-600"
                  : "text-red-700"
                )
              ))}>
                {ping +"ms"}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </FunctionalCard>
  );
}
