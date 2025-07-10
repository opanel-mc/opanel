import { useEffect, useRef } from "react";
import { parseText } from "@/lib/formatting-codes/text";
import { enableObfuscate } from "@/lib/formatting-codes/obfuscate";

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

  return <div className={className} ref={containerRef}/>;
}
