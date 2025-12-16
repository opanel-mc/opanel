import type { ColumnDef } from "@tanstack/react-table"
import Link from "next/link";
import { Download, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { deleteLog, downloadLog } from "./log-utils";
import { emitter } from "@/lib/emitter";
import { $ } from "@/lib/i18n";

export interface Log {
  name: string
  type: "gzip" | "log"
}

export const columns: ColumnDef<Log>[] = [
  {
    accessorKey: "name",
    header: () => (
      <span className="pl-3">{$("logs.columns.name")}</span>
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
    header: $("logs.columns.type"),
    cell: ({ row }) => (
      <span className="text-muted-foreground">{row.getValue("type")}</span>
    )
  },
  {
    header: " ",
    cell: ({ row }) => {
      const name = row.getValue<string>("name") ?? "";
      return (
        <div className="flex justify-end [&>*]:h-4 [&>*]:cursor-pointer [&>*]:hover:!bg-transparent">
          <Button
            variant="ghost"
            size="icon"
            title={$("logs.action.download")}
            onClick={() => downloadLog(name)}>
            <Download />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            title={$("logs.action.delete")}
            disabled={name.endsWith(".log")}
            onClick={async () => {
              await deleteLog(name);
              emitter.emit("refresh-data");
            }}>
            <Trash2 />
          </Button>
        </div>
      );
    }
  }
];
