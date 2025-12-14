"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useContext } from "react";
import { deleteCookie } from "cookies-next/client";
import { compare } from "semver";
import { Blocks, BookText, Earth, Gauge, HeartHandshake, Info, LogOut, PencilRuler, ScrollText, Settings, SquareArrowOutUpRight, SquareTerminal, Users } from "lucide-react";
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
} from "./ui/sidebar";
import { Button } from "./ui/button";
import { ThemeToggle } from "./theme-toggle";
import { cn } from "@/lib/utils";
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
    name: $("sidebar.server.saves"),
    url: "/panel/saves",
    icon: Earth
  },
  {
    name: $("sidebar.server.players"),
    url: "/panel/players",
    icon: Users
  }
];

const managementGroupItems = [
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
    name: $("sidebar.management.terminal"),
    url: "/panel/terminal",
    icon: SquareTerminal
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

const helpGroupItems = [
  {
    name: $("sidebar.help.settings"),
    url: "/panel/settings",
    icon: Settings
  },
  {
    name: $("sidebar.help.about"),
    url: "/about",
    icon: Info
  },
  {
    name: $("sidebar.help.docs"),
    url: "https://opanel.cn/docs/quick-start.html",
    icon: BookText,
    newTab: true
  },
];

export function AppSidebar() {
  const pathname = usePathname();
  const versionCtx = useContext(VersionContext);

  const handleLogout = () => {
    deleteCookie("token");
    window.location.href = "/login";
  };

  if(!versionCtx) return <></>;

  return (
    <Sidebar collapsible="icon">
      <SidebarHeader className="pl-4 flex flex-row items-center gap-0 group-data-[state=collapsed]:justify-center group-data-[state=collapsed]:pt-3 group-data-[state=collapsed]:pl-2">
        <Logo size={26}/>
        <h1 className={cn("m-2 text-lg text-theme font-semibold group-data-[state=collapsed]:hidden", minecraftAE.className)}>OPanel</h1>
      </SidebarHeader>
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>{$("sidebar.server")}</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {serverGroupItems.map((item, i) => (
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
          <SidebarGroupLabel>{$("sidebar.management")}</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {managementGroupItems.map((item, i) => (!item.minVersion || (item.minVersion && compare(versionCtx?.version, item.minVersion) >= 0)) && (
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
          <SidebarGroupLabel>{$("sidebar.help")}</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {helpGroupItems.map((item, i) => (
                <SidebarMenuItem key={i}>
                  <SidebarMenuButton
                    isActive={pathname.startsWith(item.url)}
                    asChild>
                    <Link
                      href={item.url}
                      target={item.newTab ? "_blank" : "_self"}
                      className="pl-3">
                      {pathname.startsWith(item.url) && <SidebarIndicator className="left-2"/>}
                      <item.icon />
                      <span className="whitespace-nowrap">{item.name}</span>
                      {item.newTab && <SquareArrowOutUpRight className="!size-3 ml-1" stroke="var(--color-muted-foreground)"/>}
                    </Link>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
      <SidebarFooter className="flex-row gap-1 justify-end">
        <ThemeToggle />
        <Button
          className="group-data-[state=collapsed]:hidden cursor-pointer"
          variant="secondary"
          size="icon"
          onClick={() => handleLogout()}>
          <LogOut />
        </Button>
      </SidebarFooter>
    </Sidebar>
  );
}
