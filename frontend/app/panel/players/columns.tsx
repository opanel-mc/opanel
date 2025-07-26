import type { ColumnDef } from "@tanstack/react-table";
import type { Player } from "@/lib/types";
import { Ban, BrushCleaning, Check, ShieldOff } from "lucide-react";
import { getGameModeText } from "@/lib/utils";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { Button } from "@/components/ui/button";
import { Prompt } from "@/components/prompt";
import { avatarUrl } from "@/lib/api";
import { OnlineBadge } from "@/components/online-badge";
import { ban, kick, pardon } from "./player-utils";
import { PlayerSheet } from "./player-sheet";

export const playerColumns: ColumnDef<Player>[] = [
  {
    accessorKey: "name",
    header: "玩家名",
    cell: ({ row }) => {
      const { name, uuid } = row.original;
      return (
        <Tooltip>
          <TooltipTrigger>
            <PlayerSheet player={row.original} asChild>
              <div className="flex items-center gap-2 cursor-pointer">
                <img src={avatarUrl + uuid} alt={name} width={17} height={17}/>
                <span className="font-semibold">{name}</span>
              </div>
            </PlayerSheet>
          </TooltipTrigger>
          <TooltipContent>{uuid}</TooltipContent>
        </Tooltip>
      );
    }
  },
  {
    accessorKey: "isOnline",
    header: () => <div className="text-center">状态</div>,
    cell: ({ row }) => {
      const { isOnline } = row.original;
      return (
        <div className="text-center">
          <OnlineBadge isOnline={isOnline}/>
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
      const { gamemode } = row.original;
      if(!gamemode) return <></>;
      return <div className="text-center">{getGameModeText(gamemode)}</div>;
    }
  },
  {
    accessorKey: "isOp",
    header: "OP",
    cell: ({ row }) => (
      row.original.isOp
      ? <Check size={18} color="var(--color-muted-foreground)"/>
      : <></>
    )
  },
  {
    header: " ",
    cell: ({ row }) => {
      const { uuid } = row.original;
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
      const { name, uuid } = row.original;
      return (
        <Tooltip>
          <TooltipTrigger>
            <PlayerSheet player={row.original} asChild>
              <div className="flex items-center gap-2 cursor-pointer">
                <img src={avatarUrl + uuid} alt={name} width={17} height={17}/>
                <span className="font-semibold">{name}</span>
              </div>
            </PlayerSheet>
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
    cell: ({ row }) => (
      row.original.isOp
      ? <Check size={18} color="var(--color-muted-foreground)"/>
      : <></>
    )
  },
  {
    header: " ",
    cell: ({ row }) => (
      <Button
        variant="ghost"
        size="icon"
        className="float-right h-4 cursor-pointer hover:!bg-transparent"
        title="解除封禁"
        onClick={async () => {
          await pardon(row.original.uuid);
          window.location.reload();
        }}>
        <ShieldOff className="stroke-green-600"/>
      </Button>
    )
  }
];
