"use client";

import type { Save, SavesResponse } from "@/lib/types";
import { useEffect, useState } from "react";
import { Earth } from "lucide-react";
import { toast } from "sonner";
import { SubPage } from "../sub-page";
import { SaveCard } from "./save-card";
import { sendGetRequest } from "@/lib/api";

export default function Saves() {
  const [saves, setSaves] = useState<Save[]>([]);

  const fetchServerWorlds = async () => {
    try {
      const res = await sendGetRequest<SavesResponse>("/api/saves");
      setSaves(res.saves);
    } catch (e) {
      toast.error("无法获取存档列表");
    }
  };

  useEffect(() => {
    fetchServerWorlds();
  }, []);

  return (
    <SubPage title="存档" icon={<Earth />} className="flex flex-col gap-3">
      <span className="text-sm text-muted-foreground">将地图文件 (zip格式) 或文件夹拖入页面以进行导入。</span>
      <h2 className="text-lg font-semibold">所有存档</h2>
      <div className="pt-2 grid grid-cols-3 gap-4">
        {saves.map((save, i) => <SaveCard {...save} key={i}/>)}
      </div>
    </SubPage>
  );
}
