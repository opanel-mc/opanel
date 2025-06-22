"use client";

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
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "./ui/sidebar";
import { Button } from "./ui/button";
import { ThemeToggle } from "./theme-toggle";

const serverGroupItems = [
  {
    name: "仪表盘",
    url: "/panel/dashboard",
    icon: Gauge
  },
  {
    name: "存档",
    url: "/panel/worlds",
    icon: Earth
  },
  {
    name: "玩家",
    url: "/panel/players",
    icon: Users
  },
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
  const handleLogout = () => {
    deleteCookie("token");
    window.location.href = "/login";
  };

  return (
    <Sidebar collapsible="icon">
      <SidebarHeader>
        <h1 className="m-2 text-lg font-bold group-data-[state=collapsed]:hidden">OPanel</h1>
      </SidebarHeader>
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>服务器</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {serverGroupItems.map((item, i) => (
                <SidebarMenuItem key={i}>
                  <SidebarMenuButton
                    isActive={window.location.pathname.startsWith(item.url)}
                    asChild>
                    <a href={item.url}>
                      <item.icon />
                      <span>{item.name}</span>
                    </a>
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
                  <SidebarMenuButton asChild>
                    <a href={item.url}>
                      <item.icon />
                      <span>{item.name}</span>
                    </a>
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
