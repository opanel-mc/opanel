"use client";

import { toast } from "sonner";
import { ScrollText } from "lucide-react";
import { SubPage } from "../sub-page";
import { DataTable } from "@/components/data-table";
import { columns } from "./columns";
import { useEffect, useState } from "react";
import { sendGetRequest } from "@/lib/api";
import { LogsResponse } from "@/lib/types";

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

  useEffect(() => {
    fetchServerLogs();
  }, []);

  return (
    <SubPage title="日志" icon={<ScrollText />}>
      <DataTable
        columns={columns}
        data={
          logs.map((name) => ({
            name,
            type: (name.substring(name.lastIndexOf(".")) === ".gz" ? "gzip" : "log") as "gzip" | "log"
          }))
        }
        pagination
        className="max-h-[500px] overflow-y-auto"/>
    </SubPage>
  );
}
