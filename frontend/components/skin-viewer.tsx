import MinecraftSkinViewer from "minecraft-skin-viewer";
import { capeUrl, skinUrl } from "@/lib/api";
import { useEffect, useRef } from "react";

export function SkinViewer({
  uuid
}: {
  uuid: string
}) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    if(!canvasRef.current) return;

    new MinecraftSkinViewer({
      canvas: canvasRef.current,
      skin: skinUrl + uuid,
      cape: capeUrl + uuid
    });
  }, [uuid]);

  return <canvas ref={canvasRef}/>;
}
