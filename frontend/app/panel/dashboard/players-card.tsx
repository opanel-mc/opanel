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
import { googleSansCode } from "@/lib/fonts";
import { $ } from "@/lib/i18n";

export function PlayersCard({
  className,
}: Readonly<{
  className?: string
}>) {
  const ctx = useContext(InfoContext);
  
  return (
    <FunctionalCard
      icon={Users}
      title={`${$("dashboard.players.title")} (${ctx ? ctx.onlinePlayers.length : 0} / ${ctx?.maxPlayerCount ?? 0})`}
      moreLink="/panel/players"
      className={className}
      innerClassName="p-4 pt-0">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>{$("dashboard.players.table.player")}</TableHead>
            <TableHead className="text-center">{$("dashboard.players.table.gamemode")}</TableHead>
            <TableHead className="text-right">{$("dashboard.players.table.ping")}</TableHead>
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
              <TableCell className={cn("text-right", googleSansCode.className, (
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
