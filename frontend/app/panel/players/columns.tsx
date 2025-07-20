import type { ColumnDef } from "@tanstack/react-table";
import type { GameMode } from "@/lib/types";
import { Ban, BrushCleaning, Check, ShieldBan, ShieldOff, ShieldUser } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { cn, getGameModeText } from "@/lib/utils";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { Button } from "@/components/ui/button";
import {
  ContextMenu,
  ContextMenuContent,
  ContextMenuItem,
  ContextMenuTrigger
} from "@/components/ui/context-menu";
import { ban, depriveOp, giveOp, kick, pardon } from "./player-utils";
import { Prompt } from "@/components/prompt";
import { skinUrl } from "@/lib/api";

export interface Player {
  name: string
  uuid: string
  isOnline: boolean
  isOp: boolean
  isBanned: boolean
  gamemode?: GameMode
  banReason?: string
}

export const playerColumns: ColumnDef<Player>[] = [
  {
    accessorKey: "name",
    header: "玩家名",
    cell: ({ row }) => {
      const name = row.getValue<string>("name");
      const uuid = row.getValue<string>("uuid");
      return (
        <ContextMenu>
          <ContextMenuTrigger>
            <Tooltip>
              <TooltipTrigger>
                <div className="flex items-center gap-2">
                  <img src={skinUrl + name} alt={name} width={17} height={17}/>
                  <span className="font-semibold">{name}</span>
                </div>
              </TooltipTrigger>
              <TooltipContent>{uuid}</TooltipContent>
            </Tooltip>
          </ContextMenuTrigger>
          <ContextMenuContent>
            {
              row.getValue<boolean>("isOp")
              ? (
                <ContextMenuItem
                  onClick={async () => {
                    await depriveOp(uuid);
                    window.location.reload();
                  }}>
                  <ShieldBan />
                  解除OP
                </ContextMenuItem>
              )
              : (
                <ContextMenuItem
                  onClick={async () => {
                    await giveOp(uuid);
                    window.location.reload();
                  }}>
                  <ShieldUser />
                  给予OP
                </ContextMenuItem>
              )
            }
          </ContextMenuContent>
        </ContextMenu>
      );
    }
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
    accessorKey: "uuid",
    header: "",
    cell: ""
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
    cell: ({ row }) => {
      const uuid = row.getValue<string>("uuid");
      return (
        <div className="flex justify-end [&>*]:h-4 [&>*]:cursor-pointer [&>*]:hover:!bg-transparent">
          {row.getValue<boolean>("isOnline") && (
            <Prompt
              title="踢出玩家"
              description="将玩家踢出服务器，之后玩家可重新加入服务器"
              label="原因"
              placeholder="请输入踢出原因..."
              onAction={async (reason) => {
                await kick(uuid, reason);
                window.location.reload();
              }}
              asChild>
              <Button
                variant="ghost"
                size="icon"
                title="踢出服务器">
                <BrushCleaning />
              </Button>
            </Prompt>
          )}
          <Prompt
            title="封禁玩家"
            description="将玩家踢出服务器并加入封禁列表，之后玩家将不可重新加入服务器"
            label="原因"
            placeholder="请输入封禁原因..."
            onAction={async (reason) => {
              await ban(uuid, reason);
              window.location.reload();
            }}
            asChild>
            <Button
              variant="ghost"
              size="icon"
              title="封禁玩家">
              <Ban className="stroke-red-400"/>
            </Button>
          </Prompt>
        </div>
      );
    }
  }
];

export const bannedColumns: ColumnDef<Player>[] = [
  {
    accessorKey: "name",
    header: "玩家名",
    cell: ({ row }) => {
      const name = row.getValue<string>("name");
      const uuid = row.getValue<string>("uuid");
      return (
        <Tooltip>
          <TooltipTrigger>
            <div className="flex items-center gap-2">
              <img src={skinUrl + name} alt={name} width={17} height={17}/>
              <span className="font-semibold">{name}</span>
            </div>
          </TooltipTrigger>
          <TooltipContent>{uuid}</TooltipContent>
        </Tooltip>
      );
    }
  },
  {
    accessorKey: "banReason",
    header: "封禁原因"
  },
  {
    accessorKey: "uuid",
    header: "",
    cell: ""
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
    cell: ({ row }) => {
      const uuid = row.getValue<string>("uuid");
      return (
        <Button
          variant="ghost"
          size="icon"
          className="float-right h-4 cursor-pointer hover:!bg-transparent"
          title="解除封禁"
          onClick={async () => {
            await pardon(uuid);
            window.location.reload();
          }}>
          <ShieldOff className="stroke-green-600"/>
        </Button>
      );
    }
  }
];
