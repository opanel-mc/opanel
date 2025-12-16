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
import { $ } from "@/lib/i18n";

export default function Logs() {
  const [logs, setLogs] = useState<string[]>([]);

  const fetchServerLogs = async () => {
    try {
      const res = await sendGetRequest<LogsResponse>("/api/logs");
      setLogs(res.logs);
    } catch (e: any) {
      toastError(e, $("logs.fetch.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    }
  };

  const handleClearLogs = async () => {
    try {
      await sendDeleteRequest("/api/logs");
      toast.success($("logs.clear.success"));
      fetchServerLogs();
    } catch (e: any) {
      toastError(e, $("logs.clear.error"), [
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    }
  };

  useEffect(() => {
    fetchServerLogs();

    emitter.on("refresh-data", () => fetchServerLogs());
  }, []);

  return (
    <SubPage
      title={$("logs.title")}
      icon={<ScrollText />}
      className="flex-1 flex flex-col gap-5">
      <div className="flex justify-end">
        <Alert
          title={$("logs.clear.alert.title")}
          description={$("logs.clear.alert.description")}
          onAction={() => handleClearLogs()}
          asChild>
          <Button
            variant="destructive"
            className="cursor-pointer">
            <Trash2 />
            {$("logs.clear")}
          </Button>
        </Alert>
      </div>
      <DataTable
        columns={columns}
        data={
          (() => {
            const dataList = logs.map((name) => ({
              name,
              type: (name.substring(name.lastIndexOf(".")) === ".gz" ? "gzip" : "log") as "gzip" | "log"
            }));
            return [
              ...dataList.filter((item) => item.type === "log"),
              ...dataList.filter((item) => item.type === "gzip")
            ];
          })()
        }
        pagination
        fallbackMessage={$("logs.empty")}
        className="overflow-y-auto"/>
    </SubPage>
  );
}
