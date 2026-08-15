import { useCallback, useEffect, useRef } from "react";
import MinecraftSkinViewer from "minecraft-skin-viewer";
import axios from "axios";
import { getSettings } from "@/lib/settings";

const CAPES_API_BASE_URL = "https://api.capes.dev";

interface CapesApiResponse {
  imageUrl: string
}

export function SkinViewer({
  name,
  uuid
}: {
  name: string
  uuid: string
}) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const skinViewerRef = useRef<MinecraftSkinViewer | null>(null);

  const loadCape = useCallback(async () => {
    try {
      const { data } = await axios.get<CapesApiResponse>(`${CAPES_API_BASE_URL}/load/${uuid}/minecraft`);
      skinViewerRef.current?.loadCape(data.imageUrl);
    } catch (e) {
      //
    }
  // oxlint-disable-next-line react/exhaustive-deps
  }, [name, uuid]);

  useEffect(() => {
    if(!canvasRef.current) return;

    skinViewerRef.current = new MinecraftSkinViewer({
      canvas: canvasRef.current,
      skin: getSettings("players.skin-provider") + name
    });
  }, [name]);

  useEffect(() => {
    loadCape();
  }, [loadCape]);

  return <canvas ref={canvasRef}/>;
}
