import type { ExoticComponent, PropsWithChildren } from "react";
import Link from "next/link";
import { type LucideIcon, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { $ } from "@/lib/i18n";

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
  moreDialog?: ExoticComponent<PropsWithChildren & { asChild?: boolean }>
  moreLink?: string
  className?: string
  innerClassName?: string
}>>) {
  return (
    <Card className={cn(className, "!p-0 flex flex-col gap-3 rounded-sm shadow-none")}>
      <div className="p-4 pb-0 flex justify-between items-center">
        <div className="flex items-center gap-3 pl-2">
          <props.icon size={20}/>
          <h2 className="text-lg font-semibold">{title}</h2>
        </div>
        {(props.moreDialog && !moreLink) && (
          <props.moreDialog asChild>
            <Button
              variant="ghost"
              size="sm"
              className="text-muted-foreground cursor-pointer"
              data-testid="functional-card-more-dialog-button">
              {$("functional-card.more")}
              <ChevronRight />
            </Button>
          </props.moreDialog>
        )}
        {(!props.moreDialog && moreLink) && (
          <Button
            variant="ghost"
            size="sm"
            className="text-muted-foreground cursor-pointer"
            asChild>
            <Link href={moreLink} data-testid="functional-card-more-link">
              {$("functional-card.more")}
              <ChevronRight />
            </Link>
          </Button>
        )}
      </div>
      <div className={cn(innerClassName, "overflow-auto")} data-testid="functional-card-content">
        {children}
      </div>
    </Card>
  );
}
