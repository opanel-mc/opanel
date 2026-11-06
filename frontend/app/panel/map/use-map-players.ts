import type { Player } from "@/lib/types";
import type { PlayerMoveData } from "@/lib/ws/players";
import { useEffect, useState } from "react";
import { PlayersClient } from "@/lib/ws/players";

export function useMapPlayers(enabled: boolean) {
  const [players, setPlayers] = useState<PlayerMoveData[]>([]);

  useEffect(() => {
    if(!enabled) return;

    const client = new PlayersClient();
    const online = new Map<string, PlayerMoveData>();
    const pending = new Map<string, PlayerMoveData>();
    const publish = () => setPlayers([...online.values()]);
    let active = true;

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
      for(const move of moves) {
        if(online.has(move.uuid)) pending.set(move.uuid, move);
      }
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
