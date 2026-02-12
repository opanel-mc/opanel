"use client";

import type { Player, PlayersResponse, UnnamedPlayer } from "@/lib/types";
import { useEffect, useMemo, useState } from "react";
import { Ban, Contact, RotateCw, Search, UserPen, Users } from "lucide-react";
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
import { changeSettings, getSettings, type SettingsStorageType } from "@/lib/settings";
import { BannedIpsDialog } from "./banned-ips-dialog";
import { $ } from "@/lib/i18n";
import { PlayersClient } from "@/lib/ws/players";
import { useWebSocket } from "@/hooks/use-websocket";
import { useKeydown } from "@/hooks/use-keydown";

export default function Players() {
  type TabValueType = SettingsStorageType["state.players.tab"];

  const [players, setPlayers] = useState<Player[]>([]);
  const [unnamedPlayers, setUnnamedPlayers] = useState<UnnamedPlayer[]>([]);
  const [maxPlayerCount, setMaxPlayerCount] = useState<number>(0);
  const [isWhitelistEnabled, setWhitelistEnabledState] = useState(false);
  const [currentTab, setCurrentTab] = useState<TabValueType>(getSettings("state.players.tab"));
  const [searchString, setSearchString] = useState<string>("");
  const sortedPlayers = useMemo(() => players.sort((a, b) => a.name.localeCompare(b.name)), [players]);
  const nonBannedPlayers = useMemo(() => sortedPlayers.filter(({ isBanned }) => !isBanned), [sortedPlayers]);
  const bannedPlayers = useMemo(() => sortedPlayers.filter(({ isBanned }) => isBanned), [sortedPlayers]);
  const client = useWebSocket(PlayersClient);

  const fetchPlayerList = async () => {
    try {
      const res = await sendGetRequest<PlayersResponse>("/api/players");
      setMaxPlayerCount(res.maxPlayerCount);
      setWhitelistEnabledState(res.whitelist);
    } catch (e: any) {
      toastError(e, $("players.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")]
      ]);
    }
  };

  useEffect(() => {
    fetchPlayerList();

    emitter.on("refresh-data", () => fetchPlayerList());
    return () => {
      emitter.removeAllListeners("refresh-data");
    };
  }, []);

  useEffect(() => {
    if(!client) return;

    client.subscribe("init", (players: Player[]) => {
      const namedPlayers = players.filter(({ name }) => name !== undefined);

      setPlayers(namedPlayers);
      setUnnamedPlayers(players.filter(({ name }) => name === undefined) as UnnamedPlayer[]);
    });

    client.subscribe("join", (player: Player) => {
      setPlayers((prev) => {
        const index = prev.findIndex((p) => p.uuid === player.uuid);
        if(index > -1) {
          return prev.map((p, i) => i === index ? player : p);
        }
        return [...prev, player];
      });
    });

    client.subscribe("leave", (player: Player) => {
      setPlayers((prev) => prev.map((p) => p.uuid === player.uuid ? player : p));
    });

    client.subscribe("gamemode-change", (player: Player) => {
      setPlayers((prev) => prev.map((p) => p.uuid === player.uuid ? player : p));
    });

    emitter.on("refresh-data", () => client.send("fetch", null));

    return () => {
      emitter.removeAllListeners("refresh-data");
    };
  }, [client]);

  useKeydown("ArrowRight", { ctrl: true }, () => setCurrentTab("banned-list"));
  useKeydown("ArrowLeft", { ctrl: true }, () => setCurrentTab("player-list"));

  return (
    <SubPage
      title={$("players.title")}
      subTitle={currentTab === "player-list" ? $("players.player-list.title") : $("players.banned-list.title")}
      description={$("players.description")}
      category={$("sidebar.server")}
      icon={<Users />}
      className="flex flex-col gap-3">
      <span className="text-sm text-muted-foreground">{$("players.hint")}</span>
      <Tabs
        value={currentTab}
        onValueChange={(value) => {
          setCurrentTab(value as TabValueType);
          changeSettings("state.players.tab", value as TabValueType);
        }}>
        <div className="flex justify-between items-end max-lg:flex-col-reverse max-lg:items-start">
          <TabsList>
            <TabsTrigger value="player-list">
              {`${$("players.player-list.title")} (${players.filter(({ isOnline }) => isOnline).length} / ${maxPlayerCount})`}
            </TabsTrigger>
            <TabsTrigger value="banned-list">
              {`${$("players.banned-list.title")} (${players.filter(({ isBanned }) => isBanned).length})`}
            </TabsTrigger>
          </TabsList>
          <div className="min-w-fit border-b border-b-sidebar-border max-lg:border-b-transparent pb-1 flex gap-2 max-sm:flex-col max-sm:items-start *:cursor-pointer">
            <Button
              variant="ghost"
              title={$("players.action.refresh")}
              onClick={() => emitter.emit("refresh-data")}>
              <RotateCw />
            </Button>
            <InputGroup>
              <InputGroupAddon>
                <Search />
              </InputGroupAddon>
              <InputGroupInput
                value={searchString}
                placeholder={$("players.search.placeholder")}
                onChange={(e) => setSearchString(e.target.value)}/>
            </InputGroup>
            <BannedIpsDialog asChild>
              <Button variant="outline">
                <Ban />
                {$("players.banned-ips")}
              </Button>
            </BannedIpsDialog>
            {
              isWhitelistEnabled
              ? (
                <WhitelistSheet
                  onDisableWhitelist={() => {
                    setWhitelistEnabledState(false);
                    emitter.emit("refresh-data");
                  }}
                  asChild>
                  <Button variant="outline">
                    <UserPen />
                    {$("players.edit-whitelist")}
                  </Button>
                </WhitelistSheet>
              )
              : (
                <Button
                  variant="outline"
                  onClick={async () => {
                    await setWhitelistEnabled(true);
                    await fetchPlayerList();
                    /**
                     * We need to set the state manually here
                     * because the whitelist state fetched from the server has a delay.
                     */
                    setWhitelistEnabledState(true);
                    emitter.emit("refresh-data");
                  }}>
                  <Contact />
                  {$("players.enable-whitelist")}
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
              ...unnamedPlayers,
            ]}
            pagination
            fallbackMessage={$("players.empty")}/>
        </TabsContent>
        <TabsContent value="banned-list">
          <DataTable
            columns={bannedColumns}
            data={bannedPlayers.filter(({ name }) => name.toLowerCase().includes(searchString.toLowerCase()))}
            pagination
            fallbackMessage={$("players.empty")}/>
        </TabsContent>
      </Tabs>
    </SubPage>
  );
}
