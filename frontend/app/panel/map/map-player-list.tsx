import type { PlayerMoveData } from "@/lib/ws/players";
import { Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { PlayerAvatar } from "@/components/player-avatar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { $ } from "@/lib/i18n";

export function MapPlayerList({
  players,
  disconnected,
  saves,
  onSelect,
}: {
  players: PlayerMoveData[]
  disconnected: boolean
  saves: string[]
  onSelect: (player: PlayerMoveData) => void
}) {
  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button variant="outline" className="absolute top-6 right-6 bg-accent shadow-xl">
          <Users /> {disconnected ? $("map.players.disconnected") : `${$("map.players")} (${players.length})`}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-64 bg-accent p-2">
        {players.length === 0 ? (
          <p className="px-2 py-3 text-sm text-muted-foreground">
            {$(disconnected ? "map.players.disconnected" : "map.players.empty")}
          </p>
        ) : (
          <ul className="o-scrollbar max-h-64 overflow-y-auto">
            {players.map((player) => {
              const { uuid, name, position } = player;
              return (
                <li key={uuid}>
                  <Button
                    variant="ghost"
                    className="w-full justify-start"
                    disabled={!position.save || !saves.includes(position.save)}
                    onClick={() => onSelect(player)}>
                    <PlayerAvatar name={name} />
                    <span className="truncate">{name}</span>
                    <span className="ml-auto truncate text-xs text-muted-foreground">{position.save}</span>
                  </Button>
                </li>
              );
            })}
          </ul>
        )}
      </PopoverContent>
    </Popover>
  );
}
