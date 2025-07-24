import type { ColumnDef } from "@tanstack/react-table"
import Link from "next/link";
import { Download, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { deleteLog, downloadLog } from "./log-utils";

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
          onClick={() => {
            const name = row.getValue<string>("name") ?? "";
            downloadLog(name);
          }}>
          <Download />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          title="删除日志"
          onClick={async () => {
            const name = row.getValue<string>("name") ?? "";
            await deleteLog(name);
            window.location.reload();
          }}>
          <Trash2 />
        </Button>
      </div>
    )
  }
];
