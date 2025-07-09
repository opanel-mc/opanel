import Link from "next/link";
import { type LucideIcon, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { PropsWithChildren } from "react";

export function FunctionalCard({
  title,
  moreLink,
  className,
  children,
  ...props
}: Readonly<PropsWithChildren<{
  icon: LucideIcon
  title: string
  moreLink?: string
  className?: string
}>>) {
  return (
    <Card className={cn(className, "p-1 flex flex-col gap-3 rounded-md")}>
      <div className="flex justify-between items-center">
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
      <div className="overflow-auto">
        {children}
      </div>
    </Card>
  );
}
