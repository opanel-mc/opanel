import type { Player } from "@/lib/types";
import type { PlayerMoveData } from "@/lib/ws/players";
import { useEffect, useState } from "react";
import { PlayersClient } from "@/lib/ws/players";

export function useMapPlayers(enabled: boolean) {
  const [players, setPlayers] = useState<PlayerMoveData[]>([]);

  useEffect(() => {
    if(!enabled) return;

    const online = new Map<string, PlayerMoveData>();
    const pending = new Map<string, PlayerMoveData>();
    const publish = () => setPlayers([...online.values()]);
    let active = true;
    const client = new PlayersClient(() => {
      if(!active) return;
      active = false;
      online.clear();
      pending.clear();
      publish();
    });

    client.subscribe("init", (players: Player[]) => {
      if(!active) return;
      online.clear();
      pending.clear();
      for(const { uuid, name, position, isOnline } of players) {
        if(isOnline && name) online.set(uuid, { uuid, name, position });
      }
      publish();
    });

    client.subscribe("join", ({ uuid, name, position }: Player) => {
      if(!active || !name) return;
      pending.delete(uuid);
      online.set(uuid, { uuid, name, position });
      publish();
    });

    client.subscribe("leave", ({ uuid }: Player) => {
      if(!active) return;
      pending.delete(uuid);
      if(online.delete(uuid)) publish();
    });

    client.subscribe("move", (moves: PlayerMoveData[]) => {
      if(!active) return;
      let worldChanged = false;
      for(const move of moves) {
        const player = online.get(move.uuid);
        if(!player) continue;

        if(player.position.save !== move.position.save) {
          pending.delete(move.uuid);
          online.set(move.uuid, move);
          worldChanged = true;
        } else {
          pending.set(move.uuid, move);
        }
      }
      if(worldChanged) publish();
    });

    const timer = window.setInterval(() => {
      if(!pending.size) return;
      for(const [uuid, move] of pending) online.set(uuid, move);
      pending.clear();
      publish();
    }, 5000);

    return () => {
      active = false;
      window.clearInterval(timer);
      client.close();
      setPlayers([]);
    };
  }, [enabled]);

  return players;
}
