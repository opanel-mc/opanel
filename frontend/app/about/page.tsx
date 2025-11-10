"use client";

import {
  AtSign,
  BookText,
  ChevronLeft,
  Earth,
  FileText,
  Github,
  HandCoins,
  Milestone
} from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle
} from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableRow
} from "@/components/ui/table";
import { version } from "@/lib/global";
import { cn } from "@/lib/utils";
import { minecraftAE } from "@/lib/fonts";
import { Brand } from "@/components/logo";

const info = [
  {
    name: "版本",
    value: version,
    icon: Milestone
  },
  {
    name: "作者",
    value: "Norcleeh",
    icon: AtSign
  },
  {
    name: "源码",
    value: <a href="https://github.com/opanel-mc/opanel" target="_blank">opanel-mc/opanel</a>,
    icon: Github
  },
  {
    name: "许可",
    value: <a href="https://raw.githubusercontent.com/opanel-mc/opanel/refs/heads/main/LICENSE" target="_blank">MPL-2.0</a>,
    icon: FileText
  }
];

export default function About() {
  return (
    <Card className="w-3xl max-md:rounded-none">
      <CardHeader>
        <CardTitle>关于</CardTitle>
      </CardHeader>
      <CardContent className="space-y-2">
        <Brand className="w-fit mx-auto my-10 [&_svg]:w-72"/>
        <p>
          <span className={cn("text-theme font-semibold", minecraftAE.className)}>OPanel</span> 是一个开箱即用的 Minecraft 服务器管理面板，支持Bukkit、Fabric和Forge等多个平台。
        </p>
        <Table>
          <TableBody>
            {info.map((item, i) => (
              <TableRow key={i}>
                <TableCell className="flex items-center gap-2">
                  <item.icon size={17}/>
                  <span>{item.name}</span>
                </TableCell>
                <TableCell className="text-right [&_a]:underline [&_a]:underline-offset-2">{item.value}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        <p className="text-center text-lg font-bold">感谢使用 OPanel！</p>
      </CardContent>
      <CardFooter className="flex justify-between">
        <div>
          <Button
            className="cursor-pointer"
            variant="link"
            onClick={() => window.location.href = "/"}>
            <ChevronLeft />
            返回
          </Button>
        </div>
        <div className="space-x-2">
          <Button
            variant="secondary"
            size="icon"
            title="打赏"
            asChild>
            <Link href="https://afdian.com/a/opanel" target="_blank">
              <HandCoins />
            </Link>
          </Button>
          <Button
            variant="secondary"
            size="icon"
            title="Github主页"
            asChild>
            <Link href="https://github.com/opanel-mc" target="_blank">
              <Github />
            </Link>
          </Button>
          <Button
            variant="secondary"
            size="icon"
            title="OPanel官网"
            asChild>
            <Link href="https://opanel.cn" target="_blank">
              <Earth />
            </Link>
          </Button>
          <Button
            variant="secondary"
            size="icon"
            title="OPanel文档"
            asChild>
            <Link href="https://opanel.cn/docs/quick-start.html" target="_blank">
              <BookText />
            </Link>
          </Button>
        </div>
      </CardFooter>
    </Card>
  );
}
