import { useEffect, useRef } from "react";
import localFont from "next/font/local";
import { parseText } from "@/lib/formatting-codes/text";
import { enableObfuscate } from "@/lib/formatting-codes/obfuscate";
import { cn } from "@/lib/utils";

const minecraftAE = localFont({
  src: [{ path: "../assets/fonts/MinecraftAE.ttf", style: "normal" }]
});

export function MinecraftText({
  children,
  className
}: {
  children: string
  className?: string
}) {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if(!containerRef.current) return;
    containerRef.current.innerHTML = "";
    containerRef.current.appendChild(parseText(children));

    enableObfuscate(containerRef.current);
  }, [children]);

  return <div className={cn(className, minecraftAE.className)} ref={containerRef}/>;
}
