"use client";

import {
  AtSign,
  ChevronLeft,
  Earth,
  Github,
  HandCoins,
  Milestone,
  UsersRound
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
    name: "打赏",
    value: <Link href="https://nocp.space/donate" target="_blank" className="hover:underline underline-offset-4 decoration-1">https://nocp.space/donate</Link>,
    icon: HandCoins
  }
];

export default function About() {
  return (
    <Card className="w-3xl max-md:rounded-none">
      <CardHeader>
        <CardTitle>关于</CardTitle>
      </CardHeader>
      <CardContent className="space-y-2">
        <p>
          <span className={cn("text-theme font-semibold", minecraftAE.className)}>OPanel</span> 是一个开箱即用的 Minecraft 服务器管理面板，支持Bukkit、Fabric和Forge平台。
        </p>
        <Table>
          <TableBody>
            {info.map((item, i) => (
              <TableRow key={i}>
                <TableCell className="flex items-center gap-2">
                  <item.icon size={17}/>
                  <span>{item.name}</span>
                </TableCell>
                <TableCell className="text-right">{item.value}</TableCell>
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
            title="参与人员"
            asChild>
            <Link href="/about/credits">
              <UsersRound />
            </Link>
          </Button>
          <Button
            variant="secondary"
            size="icon"
            title="作者个人网站"
            asChild>
            <Link href="https://nocp.space">
              <Earth />
            </Link>
          </Button>
          <Button
            variant="secondary"
            size="icon"
            title="作者Github主页"
            asChild>
            <Link href="https://github.com/NriotHrreion">
              <Github />
            </Link>
          </Button>
        </div>
      </CardFooter>
    </Card>
  );
}
