import type { PropsWithChildren } from "react";
import Link from "next/link";
import { type LucideIcon, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

export function FunctionalCard({
  title,
  moreLink,
  className,
  innerClassName,
  children,
  ...props
}: Readonly<PropsWithChildren<{
  icon: LucideIcon
  title: string
  moreLink?: string
  className?: string
  innerClassName?: string
}>>) {
  return (
    <Card className={cn(className, "!p-0 flex flex-col gap-3 rounded-md")}>
      <div className="p-4 pb-0 flex justify-between items-center">
        <div className="flex items-center gap-3 pl-2">
          <props.icon size={20}/>
          <h2 className="text-lg font-semibold">{title}</h2>
        </div>
        {moreLink && <Button
          variant="ghost"
          size="sm"
          className="text-muted-foreground cursor-pointer"
          asChild>
          <Link href={moreLink}>
            更多
            <ChevronRight />
          </Link>
        </Button>}
      </div>
      <div className={cn(innerClassName, "overflow-auto")}>
        {children}
      </div>
    </Card>
  );
}
