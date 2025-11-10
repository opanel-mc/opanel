import type { ColumnDef } from "@tanstack/react-table";
import type { Player } from "@/lib/types";
import { useEffect } from "react";
import { Ban, BrushCleaning, Check, ShieldOff, Trash, UserMinus, UserPlus } from "lucide-react";
import { base64ToString, gameModeToString, sleep } from "@/lib/utils";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { Button } from "@/components/ui/button";
import { Prompt } from "@/components/prompt";
import { avatarUrl } from "@/lib/api";
import { OnlineBadge } from "@/components/online-badge";
import { addToWhitelist, ban, kick, pardon, removeFromWhitelist, removePlayerData } from "./player-utils";
import { PlayerSheet } from "./player-sheet";
import { emitter } from "@/lib/emitter";
import { Alert } from "@/components/alert";

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
              {
                name
                ? (
                  <div className="flex items-center gap-2 cursor-pointer">
                    <img src={avatarUrl + uuid} alt={name} width={17} height={17}/>
                    <span className="font-semibold">{name}</span>
                  </div>
                )
                : <span className="text-muted-foreground italic cursor-pointer">&lt;无名玩家&gt;</span>
              }
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
      return <div className="text-center">{gameModeToString(gamemode)}</div>;
    }
  },
  {
    accessorKey: "isWhitelisted",
    header: () => <div className="text-center">白名单</div>,
    cell: ({ row, column }) => {
      /* Fuck you react */
      // eslint-disable-next-line react-hooks/rules-of-hooks
      useEffect(() => {
        // this is actually a setState call
        column.toggleVisibility(row.original.isWhitelisted !== undefined);
      }, [row, column]);

      return (
        <div className="flex justify-center">
          {
            row.original.isWhitelisted
            ? <Check size={18} color="var(--color-muted-foreground)"/>
            : <></>
          }
        </div>
      );
    },
  },
  {
    accessorKey: "isOp",
    header: () => <div className="text-center">OP</div>,
    cell: ({ row }) => (
      <div className="flex justify-center">
        {
          row.original.isOp
          ? <Check size={18} color="var(--color-muted-foreground)"/>
          : <></>
        }
      </div>
    )
  },
  {
    header: " ",
    cell: ({ row }) => {
      const { name, uuid, isOnline, isWhitelisted } = row.original;
      return (
        <div className="flex justify-end [&>*]:h-4 [&>*]:cursor-pointer [&>*]:hover:!bg-transparent">
          {isWhitelisted !== undefined && (
            isWhitelisted
            ? (
              <Button
                variant="ghost"
                size="icon"
                title="移出白名单"
                onClick={async () => {
                  await removeFromWhitelist(name, uuid);
                  emitter.emit("refresh-data");
                }}>
                <UserMinus />
              </Button>
            )
            : (
              <Button
                variant="ghost"
                size="icon"
                title="加入白名单"
                onClick={async () => {
                  await addToWhitelist(name, uuid);
                  emitter.emit("refresh-data");
                }}>
                <UserPlus />
              </Button>
            )
          )}
          {isOnline && (
            <Prompt
              title="踢出玩家"
              description="将玩家踢出服务器，之后玩家可重新加入服务器"
              label="原因"
              placeholder="请输入踢出原因..."
              onAction={async (reason) => {
                await kick(uuid, reason);
                emitter.emit("refresh-data");
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
              emitter.emit("refresh-data");
            }}
            asChild>
            <Button
              variant="ghost"
              size="icon"
              title="封禁玩家">
              <Ban className="stroke-red-400"/>
            </Button>
          </Prompt>
          <Alert
            title={`确定要删除 ${name} 的游戏数据吗？`}
            description={`此操作将${isOnline ? "把玩家踢出服务器并" : ""}清空该玩家的所有游戏数据，且被删除的数据将不可恢复。`}
            onAction={async () => {
              if(isOnline) {
                await kick(uuid, "管理员已清空你的玩家数据", false);
                await sleep(100);
              }
              await removePlayerData(uuid);
              emitter.emit("refresh-data");
            }}
            asChild>
            <Button
              variant="ghost"
              size="icon"
              title="删除玩家数据">
              <Trash className="stroke-red-400"/>
            </Button>
          </Alert>
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
    header: "封禁原因",
    cell: ({ row }) => {
      const { banReason } = row.original;
      return banReason && <span>{base64ToString(banReason)}</span>;
    }
  },
  {
    accessorKey: "uuid",
    header: "",
    cell: ""
  },
  {
    accessorKey: "isOp",
    header: () => <div className="text-center">OP</div>,
    cell: ({ row }) => (
      <div className="flex justify-center">
        {
          row.original.isOp
          ? <Check size={18} color="var(--color-muted-foreground)"/>
          : <></>
        }
      </div>
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
          emitter.emit("refresh-data");
        }}>
        <ShieldOff className="stroke-green-600"/>
      </Button>
    )
  }
];
