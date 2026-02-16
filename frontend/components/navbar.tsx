import Link from "next/link";

import { BookText, Info, LogOut, Settings, SquareArrowOutUpRight } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "./ui/button";
import { ThemeToggle } from "./theme-toggle";
import { SidebarTrigger } from "./ui/sidebar";
import { $ } from "@/lib/i18n";
import { logout } from "@/lib/api";

export function Navbar({ className, ...props }: React.ComponentProps<"nav">) {
  const handleLogout = async () => {
    await logout();
    window.location.href = "/login";
  };

  return (
    <nav
      className={cn("min-h-12 bg-background border-b border-b-sidebar-border flex items-center justify-end *:cursor-pointer", className)}
      {...props}>
      <SidebarTrigger className="mr-auto hidden max-md:flex cursor-pointer"/>
      <div className="space-x-2 mr-2 max-sm:mr-0 max-sm:space-x-0">
        <Button
          variant="ghost"
          asChild>
          <Link href="/panel/settings">
            <Settings />
            <span className="max-sm:hidden">{$("nav.settings")}</span>
          </Link>
        </Button>
        <Button
          variant="ghost"
          asChild>
          <Link
            href="https://opanel.cn/docs/quick-start.html"
            target="_blank"
            rel="noopener noreferrer">
            <BookText />
            <span className="max-sm:hidden">{$("nav.docs")}</span>
            <SquareArrowOutUpRight className="!size-3 ml-1 max-sm:hidden" stroke="var(--color-muted-foreground)"/>
          </Link>
        </Button>
      </div>
      <Button
        variant="ghost"
        size="icon"
        onClick={() => handleLogout()}>
        <LogOut />
      </Button>
      <ThemeToggle />
      <Button
        className="max-sm:hidden"
        variant="ghost"
        size="icon"
        asChild>
        <Link href="/about">
          <Info />
        </Link>
      </Button>
    </nav>
  );
}
