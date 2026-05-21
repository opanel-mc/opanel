"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Fragment, useContext } from "react";
import { compare } from "semver";
import { Blocks, ClockFading, Earth, Gauge, HeartHandshake, MapIcon, PaintBucket, PencilRuler, ScrollText, SquareTerminal, Unplug, Users } from "lucide-react";
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
import { cn, isBukkit } from "@/lib/utils";
import { minecraftAE } from "@/lib/fonts";
import { Logo } from "./logo";
import { VersionContext } from "@/contexts/api-context";
import { $ } from "@/lib/i18n";

const serverGroupItems = [
  {
    name: $("sidebar.server.dashboard"),
    url: "/panel/dashboard",
    icon: Gauge
  },
  {
    name: $("sidebar.server.terminal"),
    url: "/panel/terminal",
    icon: SquareTerminal
  },
  {
    name: $("sidebar.server.map"),
    url: "/panel/map",
    icon: MapIcon
  }
];

const managementGroupItems = [
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
    icon: Blocks
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
    minVersion: "1.21.9"
  }
];

const configurationGroupItems = [
  {
    name: $("sidebar.config.tasks"),
    url: "/panel/tasks",
    icon: ClockFading
  },
  {
    name: $("sidebar.config.bukkit-config"),
    url: "/panel/bukkit-config",
    icon: PaintBucket
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

export function AppSidebar() {
  const pathname = usePathname();
  const versionCtx = useContext(VersionContext);

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
                item.url === "/panel/map" && !versionCtx?.map
                ? null
                : (
                  <SidebarMenuItem key={i}>
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
                )
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
        <SidebarGroup>
          <SidebarGroupLabel>{$("sidebar.management")}</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {managementGroupItems.map((item, i) => (!item.minVersion || (versionCtx && compare(versionCtx.version, item.minVersion) >= 0)) && (
                <SidebarMenuItem key={i}>
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
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
        <SidebarGroup>
          <SidebarGroupLabel>{$("sidebar.config")}</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {configurationGroupItems.map((item, i) => (
                (item.url === "/panel/bukkit-config" && (!versionCtx || !isBukkit(versionCtx.serverType)))
                ? <Fragment key={i}/>
                : (
                  <SidebarMenuItem key={i}>
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
                )
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
      <SidebarFooter className="p-4 bg-background items-end group-data-[state=collapsed]:px-0 group-data-[state=collapsed]:items-center">
        <SidebarTrigger className="cursor-pointer"/>
      </SidebarFooter>
    </Sidebar>
  );
}
