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

const serverGroupItems = [
  {
    name: "仪表盘",
    url: "/panel/dashboard",
    icon: Gauge
  },
  {
    name: "存档",
    url: "/panel/saves",
    icon: Earth
  },
  {
    name: "玩家",
    url: "/panel/players",
    icon: Users
  }
];

const managementGroupItems = [
  {
    name: "游戏规则",
    url: "/panel/gamerules",
    icon: PencilRuler
  },
  {
    name: "插件",
    url: "/panel/plugins",
    icon: Blocks
  },
  {
    name: "后台",
    url: "/panel/terminal",
    icon: SquareTerminal
  },
  {
    name: "日志",
    url: "/panel/logs",
    icon: ScrollText
  },
  {
    name: "行为准则",
    url: "/panel/code-of-conduct",
    icon: HeartHandshake,
    minVersion: "1.21.9"
  }
];

const helpGroupItems = [
  {
    name: "设置",
    url: "/panel/settings",
    icon: Settings
  },
  {
    name: "关于",
    url: "/about",
    icon: Info
  },
  {
    name: "文档",
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
          <SidebarGroupLabel>服务器</SidebarGroupLabel>
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
          <SidebarGroupLabel>管理</SidebarGroupLabel>
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
          <SidebarGroupLabel>帮助</SidebarGroupLabel>
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
