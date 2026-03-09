"use client";

import type { Plugin, PluginsResponse } from "@/lib/types";
import { type DragEvent, useContext, useEffect, useRef, useState } from "react";
import { Blocks, PackageCheck, PackageX, RotateCw, Search, Upload } from "lucide-react";
import { toast } from "sonner";
import { SubPage } from "../sub-page";
import { changeSettings, getSettings, type SettingsStorageType } from "@/lib/settings";
import { $ } from "@/lib/i18n";
import { useKeydown } from "@/hooks/use-keydown";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group";
import { Button } from "@/components/ui/button";
import { emitter } from "@/lib/emitter";
import { sendGetRequest, toastError, uploadFile } from "@/lib/api";
import { VersionContext } from "@/contexts/api-context";
import { DataTable } from "@/components/data-table";
import { disabledPluginColumns, enabledPluginColumns } from "./columns";
import { base64ToString, cn } from "@/lib/utils";
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger
} from "@/components/ui/alert-dialog";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Progress } from "@/components/ui/progress";

const DISABLED_SUFFIX = ".disabled";

export default function Plugins() {
  type TabValueType = SettingsStorageType["state.plugins.tab"];

  const versionCtx = useContext(VersionContext);
  const [plugins, setPlugins] = useState<Plugin[]>([]);
  const [folderPath, setFolderPath] = useState("");
  const [currentTab, setCurrentTab] = useState<TabValueType>(getSettings("state.plugins.tab"));
  const [searchString, setSearchString] = useState("");
  const [uploadVisible, setUploadVisible] = useState(false);
  const [uploadName, setUploadName] = useState<string | null>(null);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const dragDepthRef = useRef(0);

  const hasDraggedFiles = (event: DragEvent<HTMLElement>) => Array.from(event.dataTransfer.types).includes("Files");

  const resetDragState = () => {
    dragDepthRef.current = 0;
    setUploadVisible(false);
  };

  const fetchPluginList = async () => {
    if(!versionCtx) return;

    try {
      const res = await sendGetRequest<PluginsResponse>("/api/plugins");
      setPlugins(res.plugins.sort((a, b) => a.name.localeCompare(b.name)));
      setFolderPath(res.folderPath);
    } catch (e: any) {
      toastError(e, $("plugins.fetch.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")]
      ]);
    }
  };

  const handleUpload = async (file: File) => {
    setUploadVisible(false);

    if(!file.name.endsWith(".jar")) {
      toast.error($("plugins.action.upload.error"), { description: $("plugins.action.upload.error.description") });
      return;
    }

    setUploadName(file.name);
    try {
      await uploadFile("/api/plugins", file, (progress) => {
        setUploadProgress(progress < 1 ? progress : null);
      });
      fetchPluginList();
    } catch (e: any) {
      toastError(e, $("plugins.action.upload.error"), [
        [400, $("plugins.action.upload.error.400")],
        [401, $("common.error.401")],
        [409, $("plugins.action.upload.error.409")],
        [500, $("common.error.500")]
      ]);
    }
  };

  useEffect(() => {
    fetchPluginList();

    emitter.on("refresh-data", () => fetchPluginList());
    return () => {
      emitter.removeAllListeners("refresh-data");
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [versionCtx]);

  useKeydown("ArrowRight", { ctrl: true }, () => setCurrentTab("enabled-list"));
  useKeydown("ArrowLeft", { ctrl: true }, () => setCurrentTab("disabled-list"));
  
  return (
    <SubPage
      title={$("plugins.title")}
      description={$("plugins.description")}
      category={$("sidebar.management")}
      icon={<Blocks />}
      className="relative h-full"
      onDragEnter={(e) => {
        if(!hasDraggedFiles(e)) return;
        e.preventDefault();
        dragDepthRef.current += 1;
        setUploadVisible(true);
      }}
      onDragOver={(e) => {
        if(!hasDraggedFiles(e)) return;
        e.preventDefault();
      }}
      onDragLeave={(e) => {
        if(!hasDraggedFiles(e)) return;
        e.preventDefault();
        dragDepthRef.current = Math.max(dragDepthRef.current - 1, 0);
        if(dragDepthRef.current === 0) {
          setUploadVisible(false);
        }
      }}
      onDrop={(e) => {
        if(!hasDraggedFiles(e)) return;
        e.preventDefault();
        resetDragState();
        if(e.dataTransfer.files.length === 0) return;
        handleUpload(e.dataTransfer.files[0]);
      }}>
      {/* Drag and Drop Area */}
      <div className={cn("absolute top-0 left-0 right-0 bottom-0 z-50 flex flex-col justify-center items-center gap-4 pointer-events-none", uploadVisible ? "" : "hidden")}>
        <div
          className="absolute w-full h-full border-4 rounded-sm border-dashed"/>
        <Upload size={60} stroke="var(--color-muted-foreground)"/>
        <span className="text-muted-foreground">{$("plugins.dnd.label")}</span>
      </div>

      <div className="flex flex-col gap-3">
        <div className="flex justify-between items-end max-lg:flex-col max-lg:gap-4">
          <span className="text-sm text-muted-foreground">{$("plugins.hint", folderPath)}</span>
          {uploadProgress && (
            <div className="w-72 self-end max-md:w-full flex flex-col justify-end items-end gap-2">
              {$("plugins.progress.label", uploadName)}
              <Progress value={uploadProgress * 100} className="h-1"/>
            </div>
          )}
        </div>
        <Tabs
          value={currentTab}
          onValueChange={(value) => {
            setCurrentTab(value as TabValueType);
            changeSettings("state.plugins.tab", value as TabValueType);
          }}>
          <div className="flex justify-between items-end max-lg:flex-col-reverse max-lg:items-start">
            <TabsList>
              <TabsTrigger value="enabled-list">
                <PackageCheck />
                {$("plugins.enabled-list.title")}
              </TabsTrigger>
              <TabsTrigger value="disabled-list">
                <PackageX />
                {$("plugins.disabled-list.title")}
              </TabsTrigger>
            </TabsList>
            <div className="min-w-fit border-b border-b-sidebar-border max-lg:border-b-transparent pb-1 flex gap-2 max-sm:flex-col max-sm:items-start *:cursor-pointer">
              <Button
                variant="ghost"
                title={$("plugins.action.refresh")}
                onClick={() => emitter.emit("refresh-data")}>
                <RotateCw />
              </Button>
              <InputGroup>
                <InputGroupAddon>
                  <Search />
                </InputGroupAddon>
                <InputGroupInput
                  value={searchString}
                  placeholder={$("plugins.search.placeholder")}
                  onChange={(e) => setSearchString(e.target.value)}/>
              </InputGroup>
              <AlertDialog open={uploadDialogOpen} onOpenChange={setUploadDialogOpen}>
                <AlertDialogTrigger asChild>
                  <Button className="cursor-pointer">
                    <Upload />
                    {$("plugins.action.upload")}
                  </Button>
                </AlertDialogTrigger>
                <AlertDialogContent>
                  <AlertDialogHeader>
                    <AlertDialogTitle>{$("plugins.action.upload.title")}</AlertDialogTitle>
                    <AlertDialogDescription>{$("plugins.action.upload.description")}</AlertDialogDescription>
                  </AlertDialogHeader>
                  <Label>{$("plugins.action.upload.input.label")}</Label>
                  <Input
                    type="file"
                    accept=".jar"
                    onChange={(e) => {
                      const fileList = (e.target as HTMLInputElement).files;
                      fileList && handleUpload(fileList[0]);
                      setUploadDialogOpen(false);
                    }}/>
                  <AlertDialogFooter>
                    <AlertDialogCancel>{$("dialog.close")}</AlertDialogCancel>
                  </AlertDialogFooter>
                </AlertDialogContent>
              </AlertDialog>
            </div>
          </div>
          <TabsContent value="enabled-list">
            <DataTable
              columns={enabledPluginColumns}
              data={[
                ...plugins.filter(({ name, fileName, enabled }) => (
                  (
                    name.toLowerCase().includes(searchString.toLowerCase())
                    || decodeURIComponent(base64ToString(fileName)).toLowerCase().includes(searchString.toLowerCase())
                  )
                  && (enabled && !fileName.endsWith(DISABLED_SUFFIX))
                )),
              ]}
              pagination
              paginationQueryKey="page"
              fallbackMessage={$("plugins.empty")}/>
          </TabsContent>
          <TabsContent value="disabled-list">
            <DataTable
              columns={disabledPluginColumns}
              data={[
                ...plugins.filter(({ name, fileName, enabled }) => (
                  (
                    name.toLowerCase().includes(searchString.toLowerCase())
                    || decodeURIComponent(base64ToString(fileName)).toLowerCase().includes(searchString.toLowerCase())
                  )
                  && (!enabled || fileName.endsWith(DISABLED_SUFFIX))
                )),
              ]}
              pagination
              paginationQueryKey="disabled-page"
              fallbackMessage={$("plugins.empty")}/>
          </TabsContent>
        </Tabs>
      </div>
    </SubPage>
  );
}
