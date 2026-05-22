"use client";

import Link from "next/link";
import { ChevronLeft } from "lucide-react";
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
  TableHead,
  TableHeader,
  TableRow
} from "@/components/ui/table";
import { minecraftAE } from "@/lib/fonts";
import { $ } from "@/lib/i18n";
import { cn } from "@/lib/utils";
import { Text } from "@/components/i18n-text";
import { useLoadingDone } from "@/hooks/use-loading-done";

const thanksList = [
  {
    name: "Javalin",
    author: "David Åse",
    repo: "javalin/javalin"
  },
  {
    name: "Item-NBT-API",
    author: "tr7zw",
    repo: "tr7zw/Item-NBT-API"
  },
  {
    name: "minecraft-textures",
    author: "destruc7i0n",
    repo: "destruc7i0n/minecraft-textures"
  },
  {
    name: "minecraft-skin-viewer",
    author: "James Harrison",
    repo: "MinecraftCapes/minecraft-skin-viewer"
  },
  {
    name: "snbt-js",
    author: "myworldzycpc",
    repo: "myworldzycpc/snbt-js"
  },
  {
    name: "Shadcn UI",
    author: "shadcn",
    repo: "shadcn/ui"
  },
  {
    name: "Lucide Icons",
    author: "Lucide Contributors",
    repo: "lucide-icons/lucide"
  },
  {
    name: "ansi-to-html",
    author: "Rob Burns",
    repo: "rburns/ansi-to-html"
  }
];

export default function Thanks() {
  useLoadingDone();

  return (
    <Card className="min-w-0 max-md:rounded-none">
      <CardHeader>
        <CardTitle>{$("about.title")}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-2">
        <Text
          id="about.thanks-list.intro"
          args={[
            <span className={cn("text-theme font-semibold", minecraftAE.className)} key={0}>OPanel</span>
          ]}/>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{$("about.thanks-list.columns.name")}</TableHead>
              <TableHead>{$("about.thanks-list.columns.author")}</TableHead>
              <TableHead className="text-right">{$("about.thanks-list.columns.repo")}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {thanksList.map((item, i) => (
              <TableRow key={i}>
                <TableCell className="font-semibold">
                  {item.name}
                </TableCell>
                <TableCell>
                  {item.author}
                </TableCell>
                <TableCell className="text-right [&_a]:underline [&_a]:underline-offset-2">
                  <a href={`https://github.com/${item.repo}`} target="_blank" rel="noopener noreferrer">
                    {item.repo}
                  </a>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        <p className="text-right text-sm text-muted-foreground italic">
          {$("about.thanks-list.footer")}
        </p>
        <h2 className="text-lg font-semibold">{$("about.thanks-list.special")}</h2>
        <div className="flex max-md:flex-col justify-between items-center gap-2 text-sm [&_a]:underline [&_a]:underline-offset-2">
          <p className="leading-6">
            <b>NekoMaid</b> {$("about.thanks-list.special.description1")}<br />
            {$("about.thanks-list.special.description2")}
          </p>
          <div className="flex flex-col items-end max-md:items-center *:whitespace-nowrap">
            <span className="font-semibold">Shirasawa</span>
            <a href={`https://github.com/neko-craft/NekoMaid`} target="_blank" rel="noopener noreferrer">
              neko-craft/NekoMaid
            </a>
          </div>
        </div>
      </CardContent>
      <CardFooter className="flex">
        <Button
          className="mr-auto cursor-pointer"
          variant="link"
          asChild>
          <Link href="/about">
            <ChevronLeft />
            {$("about.footer.back")}
          </Link>
        </Button>
      </CardFooter>
    </Card>
  );
}
