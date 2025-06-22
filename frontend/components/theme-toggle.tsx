"use client";

import { useState, useEffect } from "react";
import { useTheme } from "next-themes";
import { Sun, Moon } from "lucide-react";
import { Button } from "./ui/button";

export function ThemeToggle() {
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
  }, []);

  if(!mounted) return null;

  return (
    <Button
      variant="ghost"
      size="icon"
      className="cursor-pointer group-data-[state=collapsed]:size-8"
      onClick={() => handleClick()}>
      {
        theme === "dark"
        ? <Moon />
        : <Sun />
      }
    </Button>
  );
}
