import { useEffect, useRef } from "react";
import { parseText } from "@/lib/formatting-codes/text";
import { enableObfuscate } from "@/lib/formatting-codes/obfuscate";
import { cn } from "@/lib/utils";
import { minecraftAE, minecraftAEOld, unifont } from "@/lib/fonts";

export function MinecraftText({
  maxLines = 1,
  maxCharPerLine = Infinity,
  children,
  className
}: {
  maxLines?: number
  maxCharPerLine?: number
  children: string
  className?: string
}) {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if(!containerRef.current) return;
    containerRef.current.innerHTML = "";
    containerRef.current.appendChild(parseText(children, maxLines, maxCharPerLine));

    enableObfuscate(containerRef.current);
  }, [children, maxLines, maxCharPerLine]);

  return (
    <div
      className={cn("[&_*]:leading-0.5", className, minecraftAE.className, unifont.className, minecraftAEOld.variable)}
      ref={containerRef}/>
  );
}
