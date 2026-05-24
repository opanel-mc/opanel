"use client";

import type { Save, SavesResponse } from "@/lib/types";
import type { MapCanvasHandle } from "./map-canvas";
import type { RenderSettings } from "@/lib/map/tile-worker-protocol";
import dynamic from "next/dynamic";
import { type PropsWithChildren, useEffect, useRef, useState } from "react";
import { Minus, Plus, Settings } from "lucide-react";
import { SubPage } from "../sub-page";
import { ButtonGroup } from "@/components/ui/button-group";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "@/components/ui/select";
import { cn } from "@/lib/utils";
import { googleSansCode } from "@/lib/fonts";
import {
  Popover,
  PopoverContent,
  PopoverDescription,
  PopoverHeader,
  PopoverTitle,
  PopoverTrigger
} from "@/components/ui/popover";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Switch } from "@/components/ui/switch";
import { sendGetRequest, toastError } from "@/lib/api";
import { $ } from "@/lib/i18n";
import { changeSettings, getSettings } from "@/lib/settings";
import { DEFAULT_ZOOM } from "@/hooks/use-map-tiles";
import { emitter } from "@/lib/emitter";

const MapCanvas = dynamic(() => import("./map-canvas"), { ssr: false });

function MapSettingsPopover({
  children,
  asChild,
  settings,
  onSettingsChange
}: PropsWithChildren<{
  asChild?: boolean
  settings: RenderSettings
  onSettingsChange: (newSettings: RenderSettings) => void
}>) {
  return (
    <Popover>
      <PopoverTrigger asChild={asChild}>{children}</PopoverTrigger>
      <PopoverContent align="end" className="w-xs mb-1 bg-accent">
        <PopoverHeader className="mb-4">
          <PopoverTitle>{$("map.settings.title")}</PopoverTitle>
          <PopoverDescription>
            {$("map.settings.description")}
          </PopoverDescription>
        </PopoverHeader>
        <FieldGroup className="gap-4">
          <Field orientation="horizontal">
            <FieldLabel>{$("map.settings.biome-coloring")}</FieldLabel>
            <Switch
              checked={settings.biomeColoring}
              onCheckedChange={(checked) => onSettingsChange({ ...settings, biomeColoring: checked })}/>
          </Field>
          <Field orientation="horizontal">
            <FieldLabel>{$("map.settings.render-shadows")}</FieldLabel>
            <Switch
              checked={settings.renderShadows}
              onCheckedChange={(checked) => onSettingsChange({ ...settings, renderShadows: checked })}/>
          </Field>
          <Field orientation="horizontal">
            <FieldLabel>{$("map.settings.debug-mode")}</FieldLabel>
            <Switch
              checked={settings.debugMode}
              onCheckedChange={(checked) => onSettingsChange({ ...settings, debugMode: checked })}/>
          </Field>
        </FieldGroup>
      </PopoverContent>
    </Popover>
  );
}

export default function ServerMap() {
  const [saves, setSaves] = useState<Save[]>([]);
  const [save, setSave] = useState<string>("");
  const [coord, setCoord] = useState<{ x: number, z: number } | null>(null);
  const [settings, setSettings] = useState<RenderSettings>(getSettings("map.render-settings"));
  const [fps, setFps] = useState(0);
  const [tilesLoaded, setTilesLoaded] = useState(0);
  const [zoom, setZoom] = useState(DEFAULT_ZOOM);
  const [viewportSize, setViewportSize] = useState({ width: 0, height: 0 });
  const mapRef = useRef<MapCanvasHandle | null>(null);

  const fetchServerWorlds = async () => {
    try {
      const { saves } = await sendGetRequest<SavesResponse>("/api/saves");
      const filteredSaves = saves.filter((item) => item.isRunning);
      setSaves(filteredSaves);
      setSave(filteredSaves[0].name ?? "");
    } catch (e: any) {
      toastError(e, $("saves.fetch.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")]
      ]);
    }
  };

  const handleSettingsChange = (newSettings: RenderSettings) => {
    setSettings(newSettings);
    changeSettings("map.render-settings", newSettings);
  };

  useEffect(() => {
    fetchServerWorlds();
  }, []);

  useEffect(() => {
    if(!settings.debugMode) setFps(0);
  }, [settings.debugMode]);

  return (
    <SubPage
      title={$("map.title")}
      showHeader={false}
      className="bg-background p-0">
      <div className="relative w-full h-full shadow-[inset_0px_0px_20px_4px_rgba(0,0,0,0.14)]">
        <MapCanvas
          key={save}
          ref={mapRef}
          save={save}
          settings={settings}
          onCoordChange={setCoord}
          onFpsChange={setFps}
          onTilesLoadedChange={setTilesLoaded}
          onZoomChange={setZoom}
          onResize={(width, height) => setViewportSize({ width, height })}
          onLoad={() => emitter.emit("loading-done")}/>

        <div className="absolute bottom-6 left-6 bg-accent border w-xs h-10 rounded-lg shadow-xl flex items-center">
          <Select value={save} onValueChange={setSave}>
            <SelectTrigger className="bg-transparent! border-0 ring-0! outline-none px-4 cursor-pointer">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {saves.map(({ name }, i) => (
                <SelectItem value={name} key={i}>{name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
          <span className={cn("ml-auto px-2 text-xs", googleSansCode.className)}>
            {coord ? `${coord.x.toFixed(0)} ${coord.z.toFixed(0)}` : ""}
          </span>
          <MapSettingsPopover
            settings={settings}
            onSettingsChange={handleSettingsChange}
            asChild>
            <Button variant="ghost" size="icon" className="cursor-pointer">
              <Settings />
            </Button>
          </MapSettingsPopover>
        </div>

        <ButtonGroup
          orientation="vertical"
          className="absolute bottom-6 right-6 *:shadow-xl *:bg-accent! *:cursor-pointer">
          <Button variant="outline" size="icon" onClick={() => mapRef.current?.zoomIn()}>
            <Plus />
          </Button>
          <Button variant="outline" size="icon" onClick={() => mapRef.current?.zoomOut()}>
            <Minus />
          </Button>
        </ButtonGroup>

        {settings.debugMode && (
          <div className={cn("absolute top-2 right-2 min-w-24 px-1 py-0.5 bg-accent/50 text-xs flex flex-col items-end gap-1", googleSansCode.className)}>
            <span>W: {viewportSize.width.toFixed(2)}</span>
            <span>H: {viewportSize.height.toFixed(2)}</span>
            <span>{fps} FPS</span>
            <span>Zoom: {zoom.toFixed(3)}</span>
            <span>X: {coord?.x.toFixed(2)}</span>
            <span>Z: {coord?.z.toFixed(2)}</span>
            <span>{tilesLoaded} tiles</span>
          </div>
        )}
      </div>
    </SubPage>
  );
}
