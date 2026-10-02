"use client";

import type { APIResponse, ServerType, VersionResponse } from "@/lib/types";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useContext } from "react";
import { compare } from "semver";
import { type LucideIcon, Activity, Blocks, Box, ClockFading, Earth, Gauge, HeartHandshake, MapIcon, PaintBucket, PencilRuler, ScrollText, Settings, SquareTerminal, Unplug, Users } from "lucide-react";
import { SiModelcontextprotocol } from "@icons-pack/react-simple-icons";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarIndicator,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarTrigger,
} from "./ui/sidebar";
import { Button } from "./ui/button";
import { cn } from "@/lib/utils";
import { minecraftAE } from "@/lib/fonts";
import { Logo } from "./logo";
import { ExtensionsContext, VersionContext } from "@/contexts/api-context";
import { $ } from "@/lib/i18n";
import { serverType } from "@/lib/global";

type SidebarItemDef = {
  name: string
  url: string
  icon: LucideIcon
  condition?: (versionCtx: APIResponse<VersionResponse>) => boolean
  excludedServerTypes?: ServerType[]
};

const serverGroupItems: SidebarItemDef[] = [
  {
    name: $("sidebar.server.dashboard"),
    url: "/panel/dashboard",
    icon: Gauge
  },
  {
    name: $("sidebar.server.monitor"),
    url: "/panel/monitor",
    icon: Activity
  },
  {
    name: $("sidebar.server.terminal"),
    url: "/panel/terminal",
    icon: SquareTerminal
  },
  {
    name: $("sidebar.server.map"),
    url: "/panel/map",
    icon: MapIcon,
    condition: ({ map }) => map,
    excludedServerTypes: ["Pumpkin"]
  }
];

const managementGroupItems: SidebarItemDef[] = [
  {
    name: $("sidebar.management.saves"),
    url: "/panel/saves",
    icon: Earth
  },
  {
    name: $("sidebar.management.players"),
    url: "/panel/players",
    icon: Users
  },
  {
    name: $("sidebar.management.gamerules"),
    url: "/panel/gamerules",
    icon: PencilRuler
  },
  {
    name: $("sidebar.management.plugins"),
    url: "/panel/plugins",
    icon: Blocks,
    excludedServerTypes: ["Pumpkin"]
  },
  {
    name: $("sidebar.management.logs"),
    url: "/panel/logs",
    icon: ScrollText
  },
  {
    name: $("sidebar.management.code-of-conduct"),
    url: "/panel/code-of-conduct",
    icon: HeartHandshake,
    condition: ({ version }) => compare(version, "1.21.9") >= 0,
    excludedServerTypes: ["Pumpkin"]
  }
];

const configurationGroupItems: SidebarItemDef[] = [
  {
    name: $("sidebar.config.tasks"),
    url: "/panel/tasks",
    icon: ClockFading
  },
  {
    name: $("sidebar.config.paper-config"),
    url: "/panel/paper-config",
    icon: PaintBucket,
    excludedServerTypes: ["Fabric", "Forge", "NeoForge", "Pumpkin"]
  },
  {
    name: "MCP",
    url: "/panel/mcp",
    icon: SiModelcontextprotocol
  },
  {
    name: $("sidebar.config.open-api"),
    url: "/panel/open-api",
    icon: Unplug
  }
];

function ItemRenderer(item: SidebarItemDef) {
  const pathname = usePathname();
  const versionCtx = useContext(VersionContext);

  if(item.excludedServerTypes && item.excludedServerTypes.includes(serverType)) {
    return <></>;
  }

  if(item.condition && (!versionCtx || !item.condition(versionCtx))) {
    return <></>;
  }

  return (
    <SidebarMenuItem>
      <SidebarMenuButton
        isActive={pathname.startsWith(item.url)}
        asChild>
        <Link href={item.url} className="pl-3">
          {pathname.startsWith(item.url) && <SidebarIndicator className="left-2"/>}
          <item.icon />
          <span className="whitespace-nowrap">{item.name}</span>
        </Link>
      </SidebarMenuButton>
    </SidebarMenuItem>
  );
}

export function AppSidebar() {
  const extensionPages = useContext(ExtensionsContext);

  return (
    <Sidebar collapsible="icon">
      <SidebarHeader className="h-12 pl-4 bg-background border-b border-b-sidebar-border flex flex-row items-center gap-0 group-data-[state=collapsed]:justify-center group-data-[state=collapsed]:pt-3 group-data-[state=collapsed]:pl-2">
        <Logo size={26}/>
        <h1 className={cn("m-2 text-lg text-theme font-semibold select-none group-data-[state=collapsed]:hidden", minecraftAE.className)}>OPanel</h1>
      </SidebarHeader>
      <SidebarContent className="bg-background">
        <SidebarGroup>
          <SidebarGroupLabel>{$("sidebar.server")}</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {serverGroupItems.map((item, i) => (
                <ItemRenderer {...item} key={i}/>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
        <SidebarGroup>
          <SidebarGroupLabel>{$("sidebar.management")}</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {managementGroupItems.map((item, i) => (
                <ItemRenderer {...item} key={i}/>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
        <SidebarGroup>
          <SidebarGroupLabel>{$("sidebar.config")}</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {configurationGroupItems.map((item, i) => (
                <ItemRenderer {...item} key={i}/>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
        {extensionPages.length > 0 && (
          <SidebarGroup>
            <SidebarGroupLabel>{$("sidebar.extensions")}</SidebarGroupLabel>
            <SidebarGroupContent>
              <SidebarMenu>
                {extensionPages.map((item, i) => (
                  <ItemRenderer {...item} icon={Box} key={i}/>
                ))}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        )}
      </SidebarContent>
      <SidebarFooter className="p-4 max-sm:p-2 bg-background min-sm:items-end max-sm:flex-row max-sm:justify-between group-data-[state=collapsed]:px-0 group-data-[state=collapsed]:items-center">
        <Button
          variant="ghost"
          className="min-sm:hidden"
          asChild>
          <Link href="/panel/settings">
            <Settings />
            {$("nav.settings")}
          </Link>
        </Button>
        <SidebarTrigger className="cursor-pointer"/>
      </SidebarFooter>
    </Sidebar>
  );
}
