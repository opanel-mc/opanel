import type { Player } from "@/lib/types";
import { useContext, useEffect, useState } from "react";
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
import { cn, gameModeToString, pingValueToColorClassName } from "@/lib/utils";
import { googleSansCode } from "@/lib/fonts";
import { $ } from "@/lib/i18n";
import { useWebSocket } from "@/hooks/use-websocket";
import { PlayersClient } from "@/lib/ws/players";
import { getSettings } from "@/lib/settings";

export function PlayersCard({
  className,
}: Readonly<{
  className?: string
}>) {
  const ctx = useContext(InfoContext);
  const client = useWebSocket(PlayersClient);
  const [onlinePlayers, setOnlinePlayers] = useState<Player[]>([]);

  useEffect(() => {
    if(!client) return;

    let pingTimer: ReturnType<typeof setInterval> | undefined;

    client.subscribe("init", (players: Player[]) => {
      setOnlinePlayers(players.filter(({ isOnline }) => isOnline));

      clearInterval(pingTimer);
      pingTimer = setInterval(() => {
        client.send("fetch-ping", null);
      }, getSettings("dashboard.ping-interval"));
    });

    client.subscribe("join", (player: Player) => {
      setOnlinePlayers((prev) => [...prev, player]);
    });

    client.subscribe("leave", ({ uuid }: Player) => {
      setOnlinePlayers((prev) => prev.filter((p) => p.uuid !== uuid));
    });

    client.subscribe("ping", (pings: Record<string, number>) => {
      setOnlinePlayers((prev) => prev.map((player) => (
        pings[player.uuid] !== undefined ? { ...player, ping: pings[player.uuid] } : player
      )));
    });

    return () => clearInterval(pingTimer);
  }, [client]);
  
  return (
    <FunctionalCard
      icon={Users}
      title={`${$("dashboard.players.title")} (${onlinePlayers.length} / ${ctx?.maxPlayerCount ?? 0})`}
      moreLink="/panel/players"
      className={cn(className, "max-xl:max-h-96")}
      innerClassName="mr-1 mb-2 px-4 o-scrollbar">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>{$("dashboard.players.table.player")}</TableHead>
            <TableHead className="text-center">{$("dashboard.players.table.gamemode")}</TableHead>
            <TableHead className="text-right">{$("dashboard.players.table.ping")}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {onlinePlayers.map(({ name, uuid, gamemode, ping }, i) => (
            <TableRow key={i}>
              <TableCell className="font-semibold">
                <Tooltip>
                  <TooltipTrigger>{name}</TooltipTrigger>
                  <TooltipContent>{uuid}</TooltipContent>
                </Tooltip>
              </TableCell>
              <TableCell className="text-center">{gameModeToString(gamemode)}</TableCell>
              <TableCell className={cn("text-right", googleSansCode.className, pingValueToColorClassName(ping!))}>
                {ping! +"ms"}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </FunctionalCard>
  );
}
