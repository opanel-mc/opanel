import type { ColumnDef } from "@tanstack/react-table"
import type { LogResponse } from "@/lib/types";
import Link from "next/link";
import download from "downloadjs";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Download, Trash2 } from "lucide-react";
import { sendDeleteRequest, sendGetRequest } from "@/lib/api";

export interface Log {
  name: string
  type: "gzip" | "log"
}

export const columns: ColumnDef<Log>[] = [
  {
    accessorKey: "name",
    header: () => (
      <span className="pl-3">日志</span>
    ),
    cell: ({ row }) => {
      const name = row.getValue<string>("name") ?? "";
      return (
        <Button
          variant="link"
          size="sm"
          className="h-4 font-semibold"
          asChild>
          <Link href={`/panel/logs/view?log=${name}`}>{name}</Link>
        </Button>
      );
    }
  },
  {
    accessorKey: "type",
    header: "类型",
    cell: ({ row }) => (
      <span className="text-muted-foreground">{row.getValue("type")}</span>
    )
  },
  {
    header: " ",
    cell: ({ row }) => (
      <div className="flex justify-end [&>*]:h-4 [&>*]:cursor-pointer [&>*]:hover:!bg-transparent">
        <Button
          variant="ghost"
          size="icon"
          title="下载日志"
          onClick={async () => {
            const name = row.getValue<string>("name") ?? "";
            const fileName = name.endsWith(".log.gz") ? name.replace(".log.gz", ".log") : name;
            try {
              const res = await sendGetRequest<LogResponse>(`/api/logs/${name}`);
              download(res.log, fileName, "text/plain");
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            } catch (e) {
              toast.error("下载失败", { description: `无法下载日志${fileName}` });
            }
          }}>
          <Download />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          title="删除日志"
          onClick={async () => {
            const name = row.getValue<string>("name") ?? "";
            try {
              await sendDeleteRequest(`/api/logs/${name}`);
              toast.success("删除成功");
              window.location.reload();
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            } catch (e) {
              toast.error("删除失败");
            }
          }}>
          <Trash2 />
        </Button>
      </div>
    )
  }
];
