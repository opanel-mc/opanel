import type { ColumnDef } from "@tanstack/react-table"
import type { GameMode } from "@/lib/types"
import { Check } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { cn, getGameModeText } from "@/lib/utils"
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip"

export interface Player {
  name: string
  uuid: string
  isOnline: boolean
  isOp: boolean
  isBanned: boolean
  gamemode?: GameMode
}

export const playerColumns: ColumnDef<Player>[] = [
  {
    accessorKey: "name",
    header: "玩家名",
    cell: ({ row }) => (
      <Tooltip>
        <TooltipTrigger>
          <span className="font-semibold">{row.getValue("name")}</span>
        </TooltipTrigger>
        <TooltipContent>{row.getValue("uuid")}</TooltipContent>
      </Tooltip>
    )
  },
  {
    accessorKey: "uuid",
    header: "",
    cell: ""
  },
  {
    accessorKey: "isOnline",
    header: () => <div className="text-center">状态</div>,
    cell: ({ row }) => {
      const isOnline = row.getValue<boolean>("isOnline");
      return (
        <div className="text-center">
          <Badge variant="outline">
            <div className={cn("w-2 h-2 rounded-full", isOnline ? "bg-green-600" : "bg-muted-foreground")}/>
            {isOnline ? "在线" : "离线"}
          </Badge>
        </div>
      );
    }
  },
  {
    accessorKey: "gamemode",
    header: () => <div className="text-center">游戏模式</div>,
    cell: ({ row }) => {
      const gamemode = row.getValue<GameMode>("gamemode");
      if(!gamemode) return <></>;
      return <div className="text-center">{getGameModeText(gamemode)}</div>;
    }
  },
  {
    accessorKey: "isOp",
    header: "OP",
    cell: ({ row }) => {
      const isOp = row.getValue<boolean>("isOp");
      return (
        isOp
        ? <Check size={18} color="var(--color-muted-foreground)"/>
        : <></>
      );
    }
  },
  {
    header: " ",
    cell: () => {
      /** @todo */
    }
  }
];

export const bannedColumns: ColumnDef<Player>[] = [
  {
    accessorKey: "name",
    header: "玩家名",
    cell: ({ row }) => (
      <span className="font-semibold">{row.getValue("name")}</span>
    )
  },
  {
    accessorKey: "isOp",
    header: "OP",
    cell: ({ row }) => {
      const isOp = row.getValue<boolean>("isOp");
      return (
        isOp
        ? <Check size={18} color="var(--color-muted-foreground)"/>
        : <></>
      );
    }
  },
  {
    header: " ",
    cell: () => {
      /** @todo */
    }
  }
];
