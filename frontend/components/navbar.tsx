import Link from "next/link";

import { BookText, Ellipsis, HandCoins, Info, LogOut, Settings, SquareArrowOutUpRight } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "./ui/button";
import { ThemeToggle } from "./theme-toggle";
import { SidebarTrigger } from "./ui/sidebar";
import { $ } from "@/lib/i18n";
import { logout } from "@/lib/api";
import { Badge } from "./ui/badge";
import { googleSansCode } from "@/lib/fonts";
import { version } from "@/lib/global";
import { UpdateDialog } from "@/app/panel/settings/update-dialog";
import { getUpdateCheckInfo } from "@/lib/update";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuTrigger
} from "./ui/dropdown-menu";

export function Navbar({
  className,
  title,
  subTitle,
  ...props
}: React.ComponentProps<"nav"> & {
  title: string
  subTitle?: string
}) {
  const handleLogout = async () => {
    await logout();
    window.location.href = "/login";
  };

  return (
    <nav
      className={cn("min-h-12 bg-background border-b border-b-sidebar-border flex items-center justify-end max-sm:justify-between *:cursor-pointer", className)}
      {...props}>
      <SidebarTrigger className="mr-auto max-sm:mr-0 hidden max-md:flex cursor-pointer"/>
      <UpdateDialog asChild>
        <Badge
          variant="outline"
          className={cn("max-sm:hidden mr-2", googleSansCode.className)}>
          {getUpdateCheckInfo().hasNewUpdate && (
            <div className="w-2 h-2 rounded-full bg-blue-600 dark:bg-blue-500"/>
          )}
          {`v${version}`}
        </Badge>
      </UpdateDialog>
      <div className="mr-2 max-sm:mr-0 max-sm:space-x-0 max-sm:hidden">
        <Button
          variant="ghost"
          asChild>
          <Link href="/panel/settings">
            <Settings />
            {$("nav.settings")}
          </Link>
        </Button>
        <Button
          variant="ghost"
          asChild>
          <Link
            href="https://opanel.cn/docs/quick-start"
            target="_blank"
            rel="noopener noreferrer">
            <BookText />
            {$("nav.docs")}
            <SquareArrowOutUpRight className="!size-3 ml-1 max-sm:hidden" stroke="var(--color-muted-foreground)"/>
          </Link>
        </Button>
        <Button
          variant="ghost"
          asChild>
          <Link
            href="https://nocp.space/donate"
            target="_blank"
            rel="noopener noreferrer">
            <HandCoins />
            {$("nav.donate")}
            <SquareArrowOutUpRight className="!size-3 ml-1 max-sm:hidden" stroke="var(--color-muted-foreground)"/>
          </Link>
        </Button>
      </div>
      <Button
        className="max-sm:hidden"
        variant="ghost"
        size="icon"
        onClick={() => handleLogout()}>
        <LogOut />
      </Button>
      <ThemeToggle className="max-sm:hidden"/>
      <Button
        className="max-sm:hidden"
        variant="ghost"
        size="icon"
        asChild>
        <Link href="/about">
          <Info />
        </Link>
      </Button>

      {/* Mobile only */}
      <h1 className="text-sm font-semibold min-sm:hidden">
        {subTitle ?? title}
      </h1>

      {/* Mobile only */}
      <div className="min-sm:hidden">
        <ThemeToggle />
        <Button
          variant="ghost"
          size="icon"
          onClick={() => handleLogout()}>
          <LogOut />
        </Button>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size="icon">
              <Ellipsis />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent>
            <DropdownMenuGroup>
              <DropdownMenuItem asChild>
                <Link href="/panel/settings">
                  <Settings />
                  {$("nav.settings")}
                </Link>
              </DropdownMenuItem>
              <DropdownMenuItem asChild>
                <Link
                  href="https://opanel.cn/docs/quick-start"
                  target="_blank"
                  rel="noopener noreferrer">
                  <BookText />
                  {$("nav.docs")}
                  <SquareArrowOutUpRight className="!size-3 ml-1 max-sm:hidden" stroke="var(--color-muted-foreground)"/>
                </Link>
              </DropdownMenuItem>
              <DropdownMenuItem asChild>
                <Link
                  href="https://nocp.space/donate"
                  target="_blank"
                  rel="noopener noreferrer">
                  <HandCoins />
                  {$("nav.donate")}
                  <SquareArrowOutUpRight className="!size-3 ml-1 max-sm:hidden" stroke="var(--color-muted-foreground)"/>
                </Link>
              </DropdownMenuItem>
            </DropdownMenuGroup>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </nav>
  );
}
