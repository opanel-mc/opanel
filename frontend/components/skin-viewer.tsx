import { useCallback, useEffect, useRef, useState } from "react";
import MinecraftSkinViewer from "minecraft-skin-viewer";
import axios from "axios";
import { Pause, Play } from "lucide-react";
import { getSettings } from "@/lib/settings";
import { localize as $ } from "@/lib/i18n";
import { Button } from "./ui/button";

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
  const [isAnimated, setIsAnimated] = useState(true);

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

    const viewer = new MinecraftSkinViewer({
      canvas: canvasRef.current,
      skin: getSettings("players.skin-provider") + name,
      animate: true
    });
    
    // The library exposes its camera at runtime but omits it from its types
    // see https://github.com/MinecraftCapes/minecraft-skin-viewer/pull/12
    const { camera } = viewer as MinecraftSkinViewer & {
      camera: {
        position: { set(x: number, y: number, z: number): void }
        lookAt(x: number, y: number, z: number): void
      }
    };
    camera.position.set(40.5, 27, 64.8);
    camera.lookAt(0, 0, 0);
    skinViewerRef.current = viewer;

    return () => {
      viewer.dispose();
      skinViewerRef.current = null;
    };
  }, [name]);

  useEffect(() => {
    skinViewerRef.current?.setAnimated(isAnimated);
  }, [isAnimated, name]);

  useEffect(() => {
    loadCape();
  }, [loadCape]);

  return (
    <div className="group/skin-viewer relative">
      <canvas ref={canvasRef} className="block h-[150px] w-full"/>
      <Button
        type="button"
        variant="ghost"
        size="icon"
        className="cursor-pointer absolute top-1 right-1 *:fill-foreground *:stroke-transparent opacity-0 duration-100 pointer-events-none group-hover/skin-viewer:opacity-100 group-hover/skin-viewer:pointer-events-auto focus-visible:opacity-100 focus-visible:pointer-events-auto"
        title={$(isAnimated ? "skin-viewer.pause" : "skin-viewer.play")}
        onClick={() => setIsAnimated((current) => !current)}>
        {isAnimated ? <Pause /> : <Play />}
      </Button>
    </div>
  );
}
