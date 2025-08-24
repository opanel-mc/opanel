"use client";

import { useEffect, useState } from "react";
import { useTheme } from "next-themes";
import { cn } from "@/lib/utils";

import LogoLight from "@/assets/images/logo-light.png";
import LogoDark from "@/assets/images/logo-dark.png";

export function Logo({
  size = 144,
  className
}: {
  size?: number
  className?: string
}) {
  const { theme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  if(!mounted) return <></>;

  return (
    <img
      src={theme === "light" ? LogoLight.src : LogoDark.src}
      alt="logo"
      width={size}
      className={cn("aspect-square", className)}
      style={{ imageRendering: "pixelated" }}/>
  );
}
