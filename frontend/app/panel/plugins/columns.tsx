import type { ColumnDef } from "@tanstack/react-table";
import type { Plugin } from "@/lib/types";
import { Ban, Check, Download, Link2, PackagePlus, Trash2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { base64ToString, cn, formatDataSize } from "@/lib/utils";
import { googleSansCode } from "@/lib/fonts";
import { Button } from "@/components/ui/button";
import { deletePlugin, downloadPlugin, togglePlugin } from "./plugin-utils";
import { $ } from "@/lib/i18n";
import { PluginDialog } from "./plugin-dialog";
import { BindingDialog } from "./binding-dialog";

export function PluginSourceBadge({ source }: { source?: string }) {
  const value = source ?? "unbound";
  const className = {
    mcim: "bg-cyan-500/10 text-cyan-700 border-cyan-500/30",
    modrinth: "bg-cyan-500/10 text-cyan-700 border-cyan-500/30",
    curseforge: "bg-cyan-500/10 text-cyan-700 border-cyan-500/30",
    hangar: "bg-sky-500/10 text-sky-600 border-sky-500/30",
    github: "bg-slate-500/10 text-slate-500 border-slate-500/30",
    unbound: "bg-muted/40 text-muted-foreground border-border"
  }[value] ?? "bg-muted/40 text-muted-foreground border-border";
  const labelKey = value === "modrinth" || value === "curseforge" ? "plugins.source.mcim" : `plugins.source.${value}`;
  return (
    <Badge variant="outline" className={className}>
      {$(labelKey as never)}
    </Badge>
  );
}

export const enabledPluginColumns: ColumnDef<Plugin>[] = [
  {
    accessorKey: "name",
    header: $("plugins.columns.name"),
    cell: ({ row }) => {
      const { fileName, name, loaded } = row.original;
      return (
        <Tooltip>
          <TooltipTrigger>
            {
              loaded
              ? (
                <PluginDialog plugin={row.original} asChild>
                  <span className="font-semibold cursor-pointer">{name}</span>
                </PluginDialog>
              )
              : <span className="font-semibold">{name}</span>
            }
          </TooltipTrigger>
          <TooltipContent>{base64ToString(fileName)}</TooltipContent>
        </Tooltip>
      );
    }
  },
  {
    accessorKey: "version",
    header: $("plugins.columns.version"),
    cell: ({ row }) => row.original.version ?? ""
  },
  {
    accessorKey: "source",
    header: $("plugins.columns.source"),
    cell: ({ row }) => <PluginSourceBadge source={row.original.source}/>,
  },
  {
    accessorKey: "description",
    header: $("plugins.columns.description"),
    cell: ({ row }) => {
      const { description } = row.original;
      return (
        <span className="max-w-96 overflow-hidden text-ellipsis block">
          {description ? base64ToString(description) : ""}
        </span>
      );
    }
  },
  {
    accessorKey: "size",
    header: $("plugins.columns.size"),
    cell: ({ row }) => (
      <span className={cn("text-xs", googleSansCode.className)}>
        {formatDataSize(row.original.size)}
      </span>
    )
  },
  {
    accessorKey: "loaded",
    header: () => <div className="text-center">{$("plugins.columns.loaded")}</div>,
    cell: ({ row }) => (
      <div className="flex justify-center">
        {
          row.original.loaded
          ? <Check size={18} color="var(--color-muted-foreground)"/>
          : <></>
        }
      </div>
    )
  },
  {
    header: " ",
    cell: ({ row }) => {
      const { fileName: rawFileName } = row.original;
      const fileName = base64ToString(rawFileName);
      return (
        <div className="flex justify-end [&>*]:h-4 [&>*]:cursor-pointer [&>*]:hover:!bg-transparent">
          <BindingDialog asChild plugin={row.original}>
            <Button
              variant="ghost"
              size="icon"
              title={$("plugins.action.bind")}>
              <Link2 />
            </Button>
          </BindingDialog>
          <Button
            variant="ghost"
            size="icon"
            title={$("plugins.action.toggle.disable")}
            onClick={() => togglePlugin(fileName, false)}>
            <Ban className="stroke-red-400"/>
          </Button>
          <Button
            variant="ghost"
            size="icon"
            title={$("plugins.action.download")}
            onClick={() => downloadPlugin(fileName)}>
            <Download />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            title={$("plugins.action.delete")}
            onClick={() => deletePlugin(fileName)}>
            <Trash2 />
          </Button>
        </div>
      );
    }
  }
];

export const disabledPluginColumns: ColumnDef<Plugin>[] = [
  {
    accessorKey: "name",
    header: $("plugins.columns.name"),
    cell: ({ row }) => (
      <Tooltip>
        <TooltipTrigger>
          <span className="font-semibold">{row.original.name}</span>
        </TooltipTrigger>
        <TooltipContent>{base64ToString(row.original.fileName)}</TooltipContent>
      </Tooltip>
    )
  },
  {
    accessorKey: "source",
    header: $("plugins.columns.source"),
    cell: ({ row }) => <PluginSourceBadge source={row.original.source}/>,
  },
  {
    accessorKey: "size",
    header: $("plugins.columns.size"),
    cell: ({ row }) => (
      <span className={cn("text-xs", googleSansCode.className)}>
        {formatDataSize(row.original.size)}
      </span>
    )
  },
  {
    header: " ",
    cell: ({ row }) => {
      const { fileName: rawFileName } = row.original;
      const fileName = base64ToString(rawFileName);
      return (
        <div className="flex justify-end [&>*]:h-4 [&>*]:cursor-pointer [&>*]:hover:!bg-transparent">
          <Button
            variant="ghost"
            size="icon"
            title={$("plugins.action.toggle.enable")}
            onClick={() => togglePlugin(fileName, true)}>
            <PackagePlus className="stroke-green-600"/>
          </Button>
          <Button
            variant="ghost"
            size="icon"
            title={$("plugins.action.download")}
            onClick={() => downloadPlugin(fileName)}>
            <Download />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            title={$("plugins.action.delete")}
            onClick={() => deletePlugin(fileName)}>
            <Trash2 />
          </Button>
        </div>
      );
    }
  }
];
