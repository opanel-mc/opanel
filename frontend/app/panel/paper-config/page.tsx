"use client";

import type { PaperServerConfigResponse } from "@/lib/types";
import dynamic from "next/dynamic";
import { useCallback, useContext, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useTheme } from "next-themes";
import { PaintBucket, RotateCw, Save } from "lucide-react";
import { SubPage } from "../sub-page";
import {
  FilesEditor,
  FilesEditorContent,
  FilesEditorSidebar,
  FilesEditorSidebarList,
  FilesEditorStatusBar,
  FilesEditorStatusBarItem
} from "@/components/ui/files-editor";
import { changeSettings, getSettings, monacoSettingsOptions } from "@/lib/settings";
import { VersionContext } from "@/contexts/api-context";
import { emitter } from "@/lib/emitter";
import { base64ToString, isPaperSeries, stringToBase64 } from "@/lib/utils";
import { sendGetRequest, sendPostRequest, toastError } from "@/lib/api";
import { $ } from "@/lib/i18n";
import { Button } from "@/components/ui/button";
import { ConfigItem } from "./config-item";
import { useKeydown } from "@/hooks/use-keydown";
import { Spinner } from "@/components/ui/spinner";
import { toastRestartAlert } from "@/components/restart-alert";

const MonacoEditor = dynamic(() => import("@/components/monaco-editor"), { ssr: false });

export type ConfigFile = "bukkit" | "spigot" | "paper" | "leaves";

export default function PaperConfig() {
  const { push } = useRouter();
  const { theme } = useTheme();
  const versionCtx = useContext(VersionContext);
  const [configs, setConfigs] = useState<Map<string, string> | null>(null);
  const [currentEditing, setCurrentEditing] = useState<ConfigFile>("bukkit");
  const [editorValue, setEditorValue] = useState<string>("");
  const [saved, setSaved] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  const fetchServerConfigs = useCallback(async () => {
    if(versionCtx && !isPaperSeries(versionCtx.serverType)) {
      push("/panel/dashboard");
      return;
    }

    try {
      const res = await sendGetRequest<PaperServerConfigResponse>("/api/control/paper-config");
      const parsedMap: Map<string, string> = new Map();
      for(const [type, value] of Object.entries(res)) {
        parsedMap.set(type, base64ToString(value));
      }
      setConfigs(parsedMap);
      setCurrentEditing(getSettings("state.paper-config.current-editing"));
      setSaved(true);
    } catch (e: any) {
      toastError(e, $("paper-config.fetch.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [500, $("common.error.500")],
        [503, $("paper-config.error.503")]
      ]);
    } finally {
      emitter.emit("loading-done");
    }
  }, [push, versionCtx]);

  const handleSave = useCallback(async () => {
    setIsSaving(true);
    try {
      await sendPostRequest(`/api/control/paper-config?target=${currentEditing}`, stringToBase64(editorValue));
      emitter.emit("refresh-data");
      setIsSaving(false);
      setSaved(true);
      toastRestartAlert();
    } catch (e: any) {
      toastError(e, $("paper-config.save.error", currentEditing), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [500, $("common.error.500")],
        [503, $("paper-config.error.503")]
      ]);
    }
  }, [currentEditing, editorValue]);

  const handleSwitchFile = (file: ConfigFile) => {
    setCurrentEditing(file);
    setSaved(true);
    changeSettings("state.paper-config.current-editing", file);
  };

  useEffect(() => {
    fetchServerConfigs();

    emitter.on("refresh-data", () => fetchServerConfigs());
    return () => {
      emitter.removeAllListeners("refresh-data");
    };
  }, [fetchServerConfigs]);

  // Update the editor value when the current editing file is changed
  useEffect(() => {
    if(!currentEditing || !configs) return;

    setEditorValue(configs.get(currentEditing) ?? "");
  }, [currentEditing, configs]);

  useKeydown("s", { ctrl: true }, () => handleSave());

  return (
    <SubPage
      title={$("paper-config.title")}
      description={$("paper-config.description")}
      category={$("sidebar.config")}
      icon={<PaintBucket />}
      outerClassName="max-h-[100dvh] overflow-y-hidden max-lg:max-h-none max-lg:overflow-y-auto"
      className="flex-1 min-h-0">
      <FilesEditor>
        <FilesEditorSidebar className="h-full justify-between">
          <FilesEditorSidebarList>
            <ConfigItem
              name="bukkit.yml"
              isActive={currentEditing === "bukkit"}
              isSaved={saved}
              onClick={() => handleSwitchFile("bukkit")}/>
            {["Paper", "Folia", "Leaves"].includes(versionCtx?.serverType ?? "") && (
              <ConfigItem
                name="spigot.yml"
                isActive={currentEditing === "spigot"}
                isSaved={saved}
                onClick={() => handleSwitchFile("spigot")}/>
            )}
            {["Paper", "Folia", "Leaves"].includes(versionCtx?.serverType ?? "") && (
              <ConfigItem
                name={versionCtx?.version.startsWith("1.16.") ? "paper.yml" : "paper-global.yml"}
                isActive={currentEditing === "paper"}
                isSaved={saved}
                onClick={() => handleSwitchFile("paper")}/>
            )}
            {["Leaves"].includes(versionCtx?.serverType ?? "") && (
              <ConfigItem
                name="leaves.yml"
                isActive={currentEditing === "leaves"}
                isSaved={saved}
                onClick={() => handleSwitchFile("leaves")}/>
            )}
          </FilesEditorSidebarList>
          <div className="flex justify-end *:cursor-pointer">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => fetchServerConfigs()}>
              <RotateCw />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              disabled={saved}
              onClick={() => handleSave()}>
              <Save />
            </Button>
          </div>
        </FilesEditorSidebar>
        <FilesEditorContent>
          <MonacoEditor
            defaultLanguage="yaml"
            value={editorValue}
            theme={theme === "dark" ? "opanel-theme-dark" : "opanel-theme"}
            options={{
              minimap: {
                enabled: false
              },
              ...monacoSettingsOptions
            }}
            onChange={(value) => {
              setSaved(false);
              setEditorValue(value ?? "");
            }}/>
          <FilesEditorStatusBar>
            <FilesEditorStatusBarItem side="right">
              {
                isSaving
                ? (
                  <>
                    <Spinner />
                    <span>{$("paper-config.editor.status-bar.saving")}</span>
                  </>
                )
                : (
                  saved
                  ? $("paper-config.editor.status-bar.saved")
                  : $("paper-config.editor.status-bar.edited")
                )
              }
            </FilesEditorStatusBarItem>
          </FilesEditorStatusBar>
        </FilesEditorContent>
      </FilesEditor>
    </SubPage>
  );
}
