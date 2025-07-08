import Link from "next/link";
import {
  Blocks,
  Earth,
  PencilRuler,
  ScrollText
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

const recommendedPage = [
  {
    name: "存档",
    url: "/panel/worlds",
    icon: Earth
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
    name: "日志",
    url: "/panel/logs",
    icon: ScrollText
  }
];

export function RecommendedCard({
  className,
}: Readonly<{
  className?: string
}>) {
  return (
    <Card className={cn(className, "flex flex-row gap-2")}>
      {recommendedPage.map((item, i) => (
        <Button
          className="h-full flex flex-1 flex-col"
          variant="ghost"
          asChild
          key={i}>
          <Link href={item.url}>
            <item.icon className="!w-6 !h-6"/>
            <span>{item.name}</span>
          </Link>
        </Button>
      ))}
    </Card>
  );
}
