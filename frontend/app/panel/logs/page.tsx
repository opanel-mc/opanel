"use client";

import { toast } from "sonner";
import { ScrollText, Trash2 } from "lucide-react";
import { SubPage } from "../sub-page";
import { DataTable } from "@/components/data-table";
import { columns } from "./columns";
import { useEffect, useState } from "react";
import { sendDeleteRequest, sendGetRequest } from "@/lib/api";
import { LogsResponse } from "@/lib/types";
import { Button } from "@/components/ui/button";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger
} from "@/components/ui/alert-dialog";

export default function Logs() {
  const [logs, setLogs] = useState<string[]>([]);

  const fetchServerLogs = async () => {
    try {
      const res = await sendGetRequest<LogsResponse>("/api/logs");
      setLogs(res.logs);
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    } catch (e) {
      toast.error("无法获取日志列表");
    }
  };

  const handleClearLogs = async () => {
    try {
      await sendDeleteRequest("/api/logs");
      toast.success("已清空除当前日志外的所有日志");
      window.location.reload();
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    } catch (e) {
      toast.error("清空日志失败");
    }
  };

  useEffect(() => {
    fetchServerLogs();
  }, []);

  return (
    <SubPage title="日志" icon={<ScrollText />} className="flex flex-col gap-5">
      <div className="flex justify-end">
        <AlertDialog>
          <AlertDialogTrigger asChild>
            <Button
              variant="destructive"
              className="cursor-pointer">
              <Trash2 />
              清空日志
            </Button>
          </AlertDialogTrigger>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>确定要清空所有日志文件吗？</AlertDialogTitle>
              <AlertDialogDescription>此操作不会清除当前的服务器日志，但被清空的日志文件将不可恢复。</AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>取消</AlertDialogCancel>
              <AlertDialogAction onClick={() => handleClearLogs()}>确定</AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
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
        className="max-h-[500px] overflow-y-auto"/>
    </SubPage>
  );
}
