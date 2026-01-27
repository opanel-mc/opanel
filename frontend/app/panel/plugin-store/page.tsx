"use client";

import { useContext, useEffect, useState } from "react";
import { Search, Store } from "lucide-react";
import { SubPage } from "@/app/panel/sub-page";
import { $ } from "@/lib/i18n";
import { VersionContext } from "@/contexts/api-context";
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { StoreProviderType } from "@/lib/plugin-store";
import { ModrinthStore } from "./modrinth";
import { CurseforgeStore } from "./curseforge";
import { changeSettings, getSettings } from "@/lib/settings";

export default function PluginStore() {
  const versionCtx = useContext(VersionContext);
  const [provider, setProvider] = useState<StoreProviderType>(getSettings("state.plugin-store.provider"));
  const [searchString, setSearchString] = useState("");

  useEffect(() => {
    changeSettings("state.plugin-store.provider", provider);
  }, [provider]);

  if(!versionCtx) return <></>;

  return (
    <SubPage
      title="插件"
      subTitle="插件商城"
      description="浏览、搜索并安装可用的插件 / 模组以扩展服务器功能和玩法。"
      category={$("sidebar.management")}
      icon={<Store />}
      pageClassName="overflow-hidden"
      className="min-h-0 flex flex-col gap-4">
      <div className="flex gap-2">
        <InputGroup>
          <InputGroupAddon>
            <Search />
          </InputGroupAddon>
          <InputGroupInput
            value={searchString}
            placeholder={$("plugins.search.placeholder")}
            onChange={(e) => setSearchString(e.target.value)}/>
        </InputGroup>
        <Select value={provider} onValueChange={(value) => setProvider(value as StoreProviderType)}>
          <SelectTrigger className="w-36">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={StoreProviderType.MODRINTH}>Modrinth</SelectItem>
            <SelectItem value={StoreProviderType.CURSEFORGE}>Curseforge</SelectItem>
          </SelectContent>
        </Select>
      </div>
      <div className="flex-1 min-h-0 overflow-y-auto o-scrollbar">
        <div className="pr-2 grid grid-cols-2 max-xl:flex flex-col gap-4">
          {(() => {
            switch(provider) {
              case StoreProviderType.MODRINTH: return <ModrinthStore />;
              case StoreProviderType.CURSEFORGE: return <CurseforgeStore />;
            }
          })()}
        </div>
      </div>
    </SubPage>
  );
}
