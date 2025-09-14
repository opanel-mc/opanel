"use client";

import type { LogsResponse } from "@/lib/types";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { ScrollText, Trash2 } from "lucide-react";
import { DataTable } from "@/components/data-table";
import { sendDeleteRequest, sendGetRequest, toastError } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Alert } from "@/components/alert";
import { columns } from "./columns";
import { SubPage } from "../sub-page";
import { emitter } from "@/lib/emitter";

export default function Logs() {
  const [logs, setLogs] = useState<string[]>([]);

  const fetchServerLogs = async () => {
    try {
      const res = await sendGetRequest<LogsResponse>("/api/logs");
      setLogs(res.logs);
    } catch (e: any) {
      toastError(e, "无法获取日志列表", [
        [400, "请求参数错误"],
        [401, "未登录"],
        [500, "服务器内部错误"]
      ]);
    }
  };

  const handleClearLogs = async () => {
    try {
      await sendDeleteRequest("/api/logs");
      toast.success("已清空除当前日志外的所有日志");
      fetchServerLogs();
    } catch (e: any) {
      toastError(e, "清空日志失败", [
        [401, "未登录"],
        [500, "服务器内部错误"]
      ]);
    }
  };

  useEffect(() => {
    fetchServerLogs();

    emitter.on("refresh-data", () => fetchServerLogs());
  }, []);

  return (
    <SubPage title="日志" icon={<ScrollText />} className="flex-1 flex flex-col gap-5">
      <div className="flex justify-end">
        <Alert
          title="确定要清空所有日志文件吗？"
          description="此操作不会清除当前的服务器日志，但被清空的日志文件将不可恢复。"
          onAction={() => handleClearLogs()}
          asChild>
          <Button
            variant="destructive"
            className="cursor-pointer">
            <Trash2 />
            清空日志
          </Button>
        </Alert>
      </div>
      <DataTable
        columns={columns}
        data={
          logs.map((name) => ({
            name,
            type: (name.substring(name.lastIndexOf(".")) === ".gz" ? "gzip" : "log") as "gzip" | "log"
          }))
        }
        pagination
        fallbackMessage="暂无日志"
        className="overflow-y-auto"/>
    </SubPage>
  );
}
