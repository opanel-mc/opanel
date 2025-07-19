"use client";

import type { PlayersResponse } from "@/lib/types";
import { useEffect, useState } from "react";
import { Users } from "lucide-react";
import { toast } from "sonner";
import { SubPage } from "../sub-page";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { bannedColumns, Player, playerColumns } from "./columns";
import { DataTable } from "@/components/data-table";
import { sendGetRequest } from "@/lib/api";

export default function Players() {
  const [players, setPlayers] = useState<Player[]>([]);
  const [maxPlayerCount, setMaxPlayerCount] = useState<number>(0);
  const [currentTab, setCurrentTab] = useState<string>("player-list");

  const fetchPlayerList = async () => {
    try {
      const res = await sendGetRequest<PlayersResponse>("/api/players");
      setPlayers(res.players);
      setMaxPlayerCount(res.maxPlayerCount);
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    } catch (e) {
      toast.error("无法获取玩家列表");
    }
  };

  useEffect(() => {
    fetchPlayerList();
  }, []);

  return (
    <SubPage title="玩家" subTitle={currentTab === "player-list" ? "玩家列表" : "封禁列表"} icon={<Users />}>
      <Tabs defaultValue="player-list" onValueChange={setCurrentTab}>
        <TabsList className="[&>*]:cursor-pointer">
          <TabsTrigger value="player-list">
            {`玩家列表 (${players.filter(({ isOnline }) => isOnline).length} / ${maxPlayerCount})`}
          </TabsTrigger>
          <TabsTrigger value="banned-list">
            {`封禁列表 (${players.filter(({ isBanned }) => isBanned).length})`}
          </TabsTrigger>
        </TabsList>
        <TabsContent value="player-list">
          <DataTable
            columns={playerColumns}
            data={players}
            pagination
            fallbackMessage="暂无玩家"/>
        </TabsContent>
        <TabsContent value="banned-list">
          <DataTable
            columns={bannedColumns}
            data={players.filter(({ isBanned }) => isBanned)}
            pagination
            fallbackMessage="暂无玩家"/>
        </TabsContent>
      </Tabs>
    </SubPage>
  );
}
