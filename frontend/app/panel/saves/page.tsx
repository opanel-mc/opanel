"use client";

import type { Save, SavesResponse } from "@/lib/types";
import { type DragEvent, useEffect, useState } from "react";
import { Earth, Upload } from "lucide-react";
import { toast } from "sonner";
import { SubPage } from "../sub-page";
import { SaveCard } from "./save-card";
import { sendGetRequest, toastError, uploadFile } from "@/lib/api";
import { cn } from "@/lib/utils";
import { Progress } from "@/components/ui/progress";

export default function Saves() {
  const [saves, setSaves] = useState<Save[]>([]);
  const [uploadVisible, setUploadVisible] = useState(false);
  const [uploadName, setUploadName] = useState<string | null>(null);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);

  const fetchServerWorlds = async () => {
    try {
      const res = await sendGetRequest<SavesResponse>("/api/saves");
      const list = res.saves;

      // Move the current save to the first
      for(let i = 0; i < list.length; i++) {
        if(list[i].isCurrent && i > 0) {
          const tmp = list[i - 1];
          list[i - 1] = list[i];
          list[i] = tmp;
        }
      }
      setSaves(list);
    } catch (e: any) {
      toastError(e, "无法获取存档列表", [
        [400, "请求参数错误"],
        [401, "未登录"]
      ]);
    }
  };

  const handleUpload = async (e: DragEvent) => {
    e.preventDefault();

    setUploadVisible(false);

    const file = e.dataTransfer.files[0];
    if(!file.name.endsWith(".zip")) {
      toast.error("无法上传存档", { description: "存档格式不正确，请上传一个zip文件" });
      return;
    }

    setUploadName(file.name);
    try {
      await uploadFile("/api/saves", file, (progress) => {
        setUploadProgress(progress < 1 ? progress : null);
      });
      window.location.reload();
    } catch (e: any) {
      toastError(e, "上传失败", [
        [400, "存档格式不正确，请上传一个zip文件"],
        [401, "未登录"],
        [409, "存档名称冲突"],
        [500, "服务器内部错误"]
      ]);
    }
  };

  useEffect(() => {
    fetchServerWorlds();
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
          onDrop={(e) => handleUpload(e)}
          onDragOver={(e) => e.preventDefault()}
          onDragLeave={() => setUploadVisible(false)}/>
        <Upload size={60} stroke="var(--color-muted-foreground)"/>
        <span className="text-muted-foreground">将地图文件拖拽至此处以上传</span>
      </div>

      <div className="flex flex-col gap-3">
        <div className="flex justify-between">
          <div className="flex flex-col gap-3">
            <span className="text-sm text-muted-foreground">将地图文件 (zip格式) 拖入页面以进行导入。</span>
            <h2 className="text-lg font-semibold">所有存档</h2>
          </div>
          {uploadProgress && (
            <div className="w-72 flex flex-col justify-end items-end gap-2">
              <span>正在上传 {uploadName}...</span>
              <Progress value={uploadProgress * 100} className="h-1"/>
            </div>
          )}
        </div>
        <div className="pt-2 grid grid-cols-3 max-lg:flex flex-col gap-4">
          {saves.map((save, i) => <SaveCard save={save} key={i}/>)}
        </div>
      </div>
    </SubPage>
  );
}
