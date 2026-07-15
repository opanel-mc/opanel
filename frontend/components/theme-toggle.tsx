"use client";

import { useState, useEffect } from "react";
import { useTheme } from "next-themes";
import { Sun, Moon } from "lucide-react";
import { Button } from "./ui/button";
import { cn } from "@/lib/utils";

export function ThemeToggle({ className }: { className?: string }) {
  const [mounted, setMounted] = useState(false);
  const { theme, setTheme } = useTheme();

  const handleClick = () => {
    if(theme === "dark") {
      setTheme("light");
    } else {
      setTheme("dark");
    }
  };

  useEffect(() => {
    setMounted(true);

    if(theme === "system") {
      const isDarkMode = window.matchMedia("(prefers-color-scheme: dark)").matches;
      setTheme(isDarkMode ? "dark" : "light");
    }
  }, [theme, setTheme]);

  if(!mounted) return null;

  return (
    <Button
      variant="ghost"
      size="icon"
      className={cn("cursor-pointer group-data-[state=collapsed]:size-8", className)}
      onClick={() => handleClick()}>
      {
        theme === "dark"
        ? <Moon />
        : <Sun />
      }
    </Button>
  );
}
