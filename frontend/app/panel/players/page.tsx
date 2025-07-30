"use client";

import type { Player, PlayersResponse } from "@/lib/types";
import { useEffect, useState } from "react";
import { Users } from "lucide-react";
import { toast } from "sonner";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { DataTable } from "@/components/data-table";
import { sendGetRequest } from "@/lib/api";
import { bannedColumns, playerColumns } from "./columns";
import { SubPage } from "../sub-page";
import { Badge } from "@/components/ui/badge";

export default function Players() {
  const [players, setPlayers] = useState<Player[]>([]);
  const [maxPlayerCount, setMaxPlayerCount] = useState<number>(0);
  const [isWhitelistEnabled, setIsWhitelistEnabled] = useState(false);
  const [currentTab, setCurrentTab] = useState<string>("player-list");

  const fetchPlayerList = async () => {
    try {
      const res = await sendGetRequest<PlayersResponse>("/api/players");
      setPlayers(res.players);
      setMaxPlayerCount(res.maxPlayerCount);
      setIsWhitelistEnabled(res.whitelist);
    } catch (e: any) {
      toast.error("无法获取玩家列表", { description: e.message });
    }
  };

  useEffect(() => {
    fetchPlayerList();
  }, []);

  return (
    <SubPage
      title="玩家"
      subTitle={currentTab === "player-list" ? "玩家列表" : "封禁列表"}
      icon={<Users />}
      className="flex flex-col gap-3">
      <span className="text-sm text-muted-foreground">点击玩家名以进行更多操作。</span>
      <Tabs defaultValue="player-list" onValueChange={setCurrentTab}>
        <div className="flex justify-between items-center">
          <TabsList className="[&>*]:cursor-pointer">
            <TabsTrigger value="player-list">
              {`玩家列表 (${players.filter(({ isOnline }) => isOnline).length} / ${maxPlayerCount})`}
            </TabsTrigger>
            <TabsTrigger value="banned-list">
              {`封禁列表 (${players.filter(({ isBanned }) => isBanned).length})`}
            </TabsTrigger>
          </TabsList>
          {isWhitelistEnabled && <Badge variant="secondary">已启用白名单</Badge>}
        </div>
        <TabsContent value="player-list">
          <DataTable
            columns={playerColumns}
            data={players.filter(({ isBanned }) => !isBanned)}
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
