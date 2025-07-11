import { useContext } from "react";
import { Check, Users } from "lucide-react";
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
import { gameModeToString } from "@/lib/utils";

export function PlayersCard({
  className,
}: Readonly<{
  className?: string
}>) {
  const ctx = useContext(InfoContext);
  
  return (
    <FunctionalCard
      icon={Users}
      title={`在线玩家 (${ctx ? ctx.onlinePlayers.length : 0})`}
      moreLink="/panel/players"
      className={className}
      innerClassName="p-4 pt-0">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>玩家</TableHead>
            <TableHead className="text-center">游戏模式</TableHead>
            <TableHead className="text-right">OP</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {ctx && ctx.onlinePlayers.map(({ name, uuid, isOp, gameMode }, i) => (
            <TableRow key={i}>
              <TableCell className="font-semibold">
                <Tooltip>
                  <TooltipTrigger>{name}</TooltipTrigger>
                  <TooltipContent>{uuid}</TooltipContent>
                </Tooltip>
              </TableCell>
              <TableCell className="text-center">{gameModeToString(gameMode)}</TableCell>
              <TableCell className="[&>svg]:float-right">
                {
                  isOp
                  ? <Check size={18} color="var(--color-muted-foreground)"/>
                  : <></>
                }
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </FunctionalCard>
  );
}
