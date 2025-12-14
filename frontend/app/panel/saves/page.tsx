"use client";

import type { Save, SavesResponse } from "@/lib/types";
import { useEffect, useState } from "react";
import { Earth, Upload } from "lucide-react";
import { toast } from "sonner";
import { SubPage } from "../sub-page";
import { SaveCard } from "./save-card";
import { sendGetRequest, toastError, uploadFile } from "@/lib/api";
import { cn } from "@/lib/utils";
import { Progress } from "@/components/ui/progress";
import { emitter } from "@/lib/emitter";
import { Button } from "@/components/ui/button";
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
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { $ } from "@/lib/i18n";
import { Text } from "@/components/i18n-text";

export default function Saves() {
  const [saves, setSaves] = useState<Save[]>([]);
  const [uploadVisible, setUploadVisible] = useState(false);
  const [uploadName, setUploadName] = useState<string | null>(null);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);

  const fetchServerWorlds = async () => {
    try {
      const res = await sendGetRequest<SavesResponse>("/api/saves");
      setSaves(res.saves);
    } catch (e: any) {
      toastError(e, $("saves.fetch.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")]
      ]);
    }
  };

  const handleUpload = async (file: File) => {
    setUploadVisible(false);

    if(!file.name.endsWith(".zip")) {
      toast.error($("saves.upload.error"), { description: $("saves.upload.error.description") });
      return;
    }

    setUploadName(file.name);
    try {
      await uploadFile("/api/saves", file, (progress) => {
        setUploadProgress(progress < 1 ? progress : null);
      });
      fetchServerWorlds();
    } catch (e: any) {
      toastError(e, $("saves.upload.error"), [
        [400, $("saves.upload.error.400")],
        [401, $("common.error.401")],
        [403, $("saves.upload.error.403")],
        [409, $("saves.upload.error.409")],
        [500, $("common.error.500")]
      ]);
    }
  };

  useEffect(() => {
    fetchServerWorlds();

    emitter.on("refresh-data", () => fetchServerWorlds());
  }, []);

  return (
    <SubPage
      title={$("saves.title")}
      icon={<Earth />}
      className="relative h-full z-20"
      onDragEnter={() => setUploadVisible(true)}>
      {/* Drag and Drop Area */}
      <div className={cn("absolute top-0 left-0 right-0 bottom-16 flex flex-col justify-center items-center gap-4", uploadVisible ? "" : "hidden")}>
        <div
          className="absolute w-full h-full border-4 rounded-sm border-dashed"
          onDrop={(e) => {
            e.preventDefault();
            handleUpload(e.dataTransfer.files[0]);
          }}
          onDragOver={(e) => e.preventDefault()}
          onDragLeave={() => setUploadVisible(false)}/>
        <Upload size={60} stroke="var(--color-muted-foreground)"/>
        <span className="text-muted-foreground">{$("saves.dnd.label")}</span>
      </div>

      <div className="flex flex-col gap-4">
        <div className="flex justify-between max-lg:flex-col max-lg:gap-4">
          <div className="flex flex-col gap-3">
            <span className="text-sm text-muted-foreground max-md:hidden">{$("saves.description.line1")}</span>
            <span className="text-sm text-muted-foreground">{$("saves.description.line2")}</span>
          </div>
          {uploadProgress && (
            <div className="w-72 self-end max-md:w-full flex flex-col justify-end items-end gap-2">
              <Text
                id="saves.progress.label"
                args={[uploadName]}/>
              <Progress value={uploadProgress * 100} className="h-1"/>
            </div>
          )}
        </div>
        <div className="flex justify-between items-end">
          <h2 className="text-lg font-semibold">{$("saves.list.title")}</h2>
          <AlertDialog open={uploadDialogOpen} onOpenChange={setUploadDialogOpen}>
            <AlertDialogTrigger asChild>
              <Button className="cursor-pointer">
                <Upload />
                {$("saves.list.upload")}
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>{$("saves.list.upload.title")}</AlertDialogTitle>
                <AlertDialogDescription>{$("saves.list.upload.description")}</AlertDialogDescription>
              </AlertDialogHeader>
              <Label>{$("saves.list.upload.input.label")}</Label>
              <Input
                type="file"
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
        <div className="pt-2 grid grid-cols-3 max-xl:grid-cols-2 max-lg:flex flex-col gap-4">
          {saves.map((save, i) => <SaveCard save={save} key={i}/>)}
        </div>
      </div>
    </SubPage>
  );
}
