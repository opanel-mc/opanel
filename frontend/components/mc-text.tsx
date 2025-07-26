import { useEffect, useRef } from "react";
import { parseText } from "@/lib/formatting-codes/text";
import { enableObfuscate } from "@/lib/formatting-codes/obfuscate";
import { cn } from "@/lib/utils";
import { minecraftAE, minecraftAEOld, unifont } from "@/lib/fonts";

export function MinecraftText({
  maxLines,
  children,
  className
}: {
  maxLines?: number
  children: string
  className?: string
}) {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if(!containerRef.current) return;
    containerRef.current.innerHTML = "";
    containerRef.current.appendChild(parseText(children, maxLines));

    enableObfuscate(containerRef.current);
  }, [children, maxLines]);

  return <div className={cn(className, minecraftAE.className, unifont.className, minecraftAEOld.variable)} ref={containerRef}/>;
}
