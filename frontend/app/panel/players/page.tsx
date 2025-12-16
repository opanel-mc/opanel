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
import { changeSettings, getSettings, type SettingsStorageType } from "@/lib/settings";
import { BannedIpsDialog } from "./banned-ips-dialog";
import { $ } from "@/lib/i18n";

export default function Players() {
  type TabValueType = SettingsStorageType["state.players.tab"];

  const [players, setPlayers] = useState<Player[]>([]);
  const [unnamedPlayers, setUnnamedPlayers] = useState<UnnamedPlayer[]>([]);
  const [maxPlayerCount, setMaxPlayerCount] = useState<number>(0);
  const [isWhitelistEnabled, setWhitelistEnabledState] = useState(false);
  const [currentTab, setCurrentTab] = useState<TabValueType>(getSettings("state.players.tab"));
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
      toastError(e, $("players.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    }
  };

  useEffect(() => {
    fetchPlayerList();

    emitter.on("refresh-data", () => fetchPlayerList());
  }, []);

  useEffect(() => {
    document.body.addEventListener("keydown", (e) => {
      if(e.ctrlKey && e.key === "ArrowRight") {
        setCurrentTab("banned-list");
      } else if(e.ctrlKey && e.key === "ArrowLeft") {
        setCurrentTab("player-list");
      }
    });
  }, []);

  return (
    <SubPage
      title={$("players.title")}
      subTitle={currentTab === "player-list" ? $("players.player-list.title") : $("players.banned-list.title")}
      icon={<Users />}
      className="flex flex-col gap-3">
      <span className="text-sm text-muted-foreground">{$("players.hint")}</span>
      <Tabs
        value={currentTab}
        onValueChange={(value) => {
          setCurrentTab(value as TabValueType);
          changeSettings("state.players.tab", value as TabValueType);
        }}>
        <div className="flex justify-between items-center max-lg:flex-col-reverse max-lg:items-start max-lg:gap-2">
          <TabsList className="[&>*]:cursor-pointer">
            <TabsTrigger value="player-list">
              {`${$("players.player-list.title")} (${players.filter(({ isOnline }) => isOnline).length} / ${maxPlayerCount})`}
            </TabsTrigger>
            <TabsTrigger value="banned-list">
              {`${$("players.banned-list.title")} (${players.filter(({ isBanned }) => isBanned).length})`}
            </TabsTrigger>
          </TabsList>
          <div className="flex gap-2 max-sm:flex-col max-sm:items-start">
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
              <Button
                variant="outline"
                className="cursor-pointer">
                <Ban />
                {$("players.banned-ips")}
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
                    {$("players.edit-whitelist")}
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
              ...unnamedPlayers
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
