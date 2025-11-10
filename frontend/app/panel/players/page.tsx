"use client";

import type { Player, PlayersResponse, UnnamedPlayer } from "@/lib/types";
import { useEffect, useMemo, useState } from "react";
import { Ban, Contact, Search, UserPen, Users } from "lucide-react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { DataTable } from "@/components/data-table";
import { sendGetRequest, toastError } from "@/lib/api";
import { bannedColumns, playerColumns } from "./columns";
import { SubPage } from "../sub-page";
import { Button } from "@/components/ui/button";
import { WhitelistSheet } from "./whitelist-sheet";
import { setWhitelistEnabled } from "./player-utils";
import { emitter } from "@/lib/emitter";
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group";
import { changeSettings, getSettings } from "@/lib/settings";
import { BannedIpsDialog } from "./banned-ips-dialog";

export default function Players() {
  const [players, setPlayers] = useState<Player[]>([]);
  const [unnamedPlayers, setUnnamedPlayers] = useState<UnnamedPlayer[]>([]);
  const [maxPlayerCount, setMaxPlayerCount] = useState<number>(0);
  const [isWhitelistEnabled, setWhitelistEnabledState] = useState(false);
  const [currentTab, setCurrentTab] = useState<string>("player-list");
  const [searchString, setSearchString] = useState<string>("");
  const nonBannedPlayers = useMemo(() => players.filter(({ isBanned }) => !isBanned), [players]);
  const bannedPlayers = useMemo(() => players.filter(({ isBanned }) => isBanned), [players]);

  const fetchPlayerList = async () => {
    try {
      const res = await sendGetRequest<PlayersResponse>("/api/players");
      const namedPlayers = res.players.filter(({ name }) => name !== undefined);
      const sortedPlayers = namedPlayers.sort((a, b) => a.name.localeCompare(b.name));

      setPlayers(sortedPlayers);
      setUnnamedPlayers(res.players.filter(({ name }) => name === undefined) as UnnamedPlayer[]);
      setMaxPlayerCount(res.maxPlayerCount);
      setWhitelistEnabledState(res.whitelist);
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

    emitter.on("refresh-data", () => fetchPlayerList());
  }, []);

  return (
    <SubPage
      title="玩家"
      subTitle={currentTab === "player-list" ? "玩家列表" : "封禁列表"}
      icon={<Users />}
      className="flex flex-col gap-3">
      <span className="text-sm text-muted-foreground">点击玩家名以进行更多操作。</span>
      <Tabs
        defaultValue={getSettings("state.players.tab")}
        onValueChange={(value) => {
          setCurrentTab(value);
          changeSettings("state.players.tab", value as any);
        }}>
        <div className="flex justify-between items-center max-lg:flex-col-reverse max-lg:items-start max-lg:gap-2">
          <TabsList className="[&>*]:cursor-pointer">
            <TabsTrigger value="player-list">
              {`玩家列表 (${players.filter(({ isOnline }) => isOnline).length} / ${maxPlayerCount})`}
            </TabsTrigger>
            <TabsTrigger value="banned-list">
              {`封禁列表 (${players.filter(({ isBanned }) => isBanned).length})`}
            </TabsTrigger>
          </TabsList>
          <div className="flex gap-2 max-sm:flex-col max-sm:items-start">
            <InputGroup>
              <InputGroupAddon>
                <Search />
              </InputGroupAddon>
              <InputGroupInput
                value={searchString}
                placeholder="搜索玩家..."
                onChange={(e) => setSearchString(e.target.value)}/>
            </InputGroup>
            <BannedIpsDialog asChild>
              <Button
                variant="outline"
                className="cursor-pointer">
                <Ban />
                管理封禁IP
              </Button>
            </BannedIpsDialog>
            {
              isWhitelistEnabled
              ? (
                <WhitelistSheet
                  onDisableWhitelist={() => setWhitelistEnabledState(false)}
                  asChild>
                  <Button
                    variant="outline"
                    className="cursor-pointer">
                    <UserPen />
                    编辑白名单
                  </Button>
                </WhitelistSheet>
              )
              : (
                <Button
                  variant="outline"
                  className="cursor-pointer"
                  onClick={async () => {
                    await setWhitelistEnabled(true);
                    await fetchPlayerList();
                    /**
                     * We need to set the state manually here
                     * because the whitelist state fetched from the server has a delay.
                     */
                    setWhitelistEnabledState(true);
                  }}>
                  <Contact />
                  启用白名单
                </Button>
              )
            }
          </div>
        </div>
        <TabsContent value="player-list">
          <DataTable
            columns={playerColumns}
            data={[
              ...nonBannedPlayers.filter(({ name, isOnline }) => name.toLowerCase().includes(searchString.toLowerCase()) && isOnline),
              ...nonBannedPlayers.filter(({ name, isOnline, isOp }) => name.toLowerCase().includes(searchString.toLowerCase()) && !isOnline && isOp),
              ...nonBannedPlayers.filter(({ name, isOnline, isOp }) => name.toLowerCase().includes(searchString.toLowerCase()) && !isOnline && !isOp),
              ...unnamedPlayers
            ]}
            pagination
            fallbackMessage="暂无玩家"/>
        </TabsContent>
        <TabsContent value="banned-list">
          <DataTable
            columns={bannedColumns}
            data={bannedPlayers.filter(({ name }) => name.toLowerCase().includes(searchString.toLowerCase()))}
            pagination
            fallbackMessage="暂无玩家"/>
        </TabsContent>
      </Tabs>
    </SubPage>
  );
}
