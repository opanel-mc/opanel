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
import { AlertDialog, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle, AlertDialogTrigger } from "@/components/ui/alert-dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

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
      toastError(e, "无法获取存档列表", [
        [400, "请求参数错误"],
        [401, "未登录"]
      ]);
    }
  };

  const handleUpload = async (file: File) => {
    setUploadVisible(false);

    if(!file.name.endsWith(".zip")) {
      toast.error("无法上传存档", { description: "存档格式不正确，请上传一个zip文件" });
      return;
    }

    setUploadName(file.name);
    try {
      await uploadFile("/api/saves", file, (progress) => {
        setUploadProgress(progress < 1 ? progress : null);
      });
      fetchServerWorlds();
    } catch (e: any) {
      toastError(e, "上传失败", [
        [400, "存档格式不正确，请上传一个包含地图存档的zip文件"],
        [401, "未登录"],
        [403, "检测到非法压缩包"],
        [409, "存档名称冲突"],
        [500, "服务器内部错误"]
      ]);
    }
  };

  useEffect(() => {
    fetchServerWorlds();

    emitter.on("refresh-data", () => fetchServerWorlds());
  }, []);

  return (
    <SubPage
      title="存档"
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
        <span className="text-muted-foreground">将地图文件拖拽至此处以上传</span>
      </div>

      <div className="flex flex-col gap-4">
        <div className="flex justify-between max-lg:flex-col max-lg:gap-4">
          <div className="flex flex-col gap-3">
            <span className="text-sm text-muted-foreground max-md:hidden">将存档压缩包 (zip格式) 拖入页面以进行导入。若存档压缩包内包含单个存档文件夹，则压缩包名称需与文件夹名称一致，否则拒绝导入。</span>
            <span className="text-sm text-muted-foreground">点击存档名以切换当前存档，切换后需重启服务器使改动生效。</span>
          </div>
          {uploadProgress && (
            <div className="w-72 self-end max-md:w-full flex flex-col justify-end items-end gap-2">
              <span>正在上传 {uploadName}...</span>
              <Progress value={uploadProgress * 100} className="h-1"/>
            </div>
          )}
        </div>
        <div className="flex justify-between items-end">
          <h2 className="text-lg font-semibold">所有存档</h2>
          <AlertDialog open={uploadDialogOpen} onOpenChange={setUploadDialogOpen}>
            <AlertDialogTrigger asChild>
              <Button className="cursor-pointer">
                <Upload />
                上传
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>上传存档</AlertDialogTitle>
                <AlertDialogDescription>在此上传存档。若存档压缩包内包含单个存档文件夹，则压缩包名称需与文件夹名称一致，否则拒绝导入。</AlertDialogDescription>
              </AlertDialogHeader>
              <Label>存档 (zip格式)</Label>
              <Input
                type="file"
                onChange={(e) => {
                  const fileList = (e.target as HTMLInputElement).files;
                  fileList && handleUpload(fileList[0]);
                  setUploadDialogOpen(false);
                }}/>
              <AlertDialogFooter>
                <AlertDialogCancel>关闭</AlertDialogCancel>
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
