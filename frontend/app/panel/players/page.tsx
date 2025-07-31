"use client";

import type { Player, PlayersResponse } from "@/lib/types";
import { useEffect, useState } from "react";
import { UserPen, Users } from "lucide-react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { DataTable } from "@/components/data-table";
import { sendGetRequest, toastError } from "@/lib/api";
import { bannedColumns, playerColumns } from "./columns";
import { SubPage } from "../sub-page";
import { Button } from "@/components/ui/button";
import { WhitelistSheet } from "./whitelist-sheet";

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
      toastError(e, "无法获取玩家列表", [
        [400, "请求参数错误"],
        [401, "未登录"],
        [500, "服务器内部错误"]
      ]);
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
          {isWhitelistEnabled && (
            <WhitelistSheet asChild>
              <Button
                variant="outline"
                className="cursor-pointer">
                <UserPen />
                白名单
              </Button>
            </WhitelistSheet>
          )}
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
