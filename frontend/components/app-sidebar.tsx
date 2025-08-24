"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { deleteCookie } from "cookies-next/client";
import { Blocks, Earth, Gauge, Info, LogOut, PencilRuler, ScrollText, Settings, SquareTerminal, Users } from "lucide-react";
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
  }
];

export function AppSidebar() {
  const pathname = usePathname();

  const handleLogout = () => {
    deleteCookie("token");
    window.location.href = "/login";
  };

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
                      <span>{item.name}</span>
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
              {managementGroupItems.map((item, i) => (
                <SidebarMenuItem key={i}>
                  <SidebarMenuButton
                    isActive={pathname.startsWith(item.url)}
                    asChild>
                    <Link href={item.url} className="pl-3">
                      {pathname.startsWith(item.url) && <SidebarIndicator className="left-2"/>}
                      <item.icon />
                      <span>{item.name}</span>
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
                    <Link href={item.url} className="pl-3">
                      {pathname.startsWith(item.url) && <SidebarIndicator className="left-2"/>}
                      <item.icon />
                      <span>{item.name}</span>
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
