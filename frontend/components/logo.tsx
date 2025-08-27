"use client";

import { useEffect, useState } from "react";
import { useTheme } from "next-themes";
import { ReactSVG } from "react-svg";
import { cn } from "@/lib/utils";

import LogoIcon from "@/assets/images/logo.png"; // 32x32
import BrandLight from "@/assets/images/brand-light.svg";
import BrandDark from "@/assets/images/brand-dark.svg";

export function Logo({
  size = 144,
  className
}: {
  size?: number
  className?: string
}) {
  return (
    <img
      src={LogoIcon.src}
      alt="logo"
      width={size}
      className={cn("aspect-square", className)}
      style={{ imageRendering: size >= 32 ? "pixelated" : "crisp-edges" }}/>
  );
}

export function Brand({
  className
}: {
  className?: string
}) {
  const { theme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  if(!mounted) return <></>;

  return (
    <ReactSVG
      src={theme === "light" ? BrandLight.src : BrandDark.src}
      className={cn("[&_svg]:h-fit", className)}/>
  );
}
