import type { ColumnDef } from "@tanstack/react-table"
import Link from "next/link";
import { Button } from "@/components/ui/button";

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
          className="h-4"
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
  }
];
