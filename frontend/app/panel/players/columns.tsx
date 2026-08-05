import type { ColumnDef } from "@tanstack/react-table";
import type { Player } from "@/lib/types";
import { useEffect } from "react";
import Link from "next/link";
import { Backpack, Ban, BrushCleaning, Check, ShieldOff, UserMinus, UserStar } from "lucide-react";
import { base64ToString, gameModeToString } from "@/lib/utils";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { Button } from "@/components/ui/button";
import { Prompt } from "@/components/prompt";
import { OnlineBadge } from "@/components/online-badge";
import {
  ban,
  giveOp,
  depriveOp,
  kick,
  pardon,
} from "./player-utils";
import { PlayerSheet } from "./player-sheet";
import { emitter } from "@/lib/emitter";
import { getSettings } from "@/lib/settings";
import { $ } from "@/lib/i18n";

import SteveAvatar from "@/assets/images/steve-avatar.png";

export const playerColumns: ColumnDef<Player>[] = [
  {
    accessorKey: "name",
    header: $("players.player-list.columns.name"),
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
                    <img
                      src={getSettings("players.avatar-provider") + name}
                      alt={name}
                      width={17}
                      height={17}
                      onError={(e) => {
                        (e.target as HTMLImageElement).src = SteveAvatar.src;
                      }}/>
                    <span className="font-semibold">{name}</span>
                  </div>
                )
                : (
                  <span className="text-muted-foreground italic cursor-pointer">
                    &lt;{$("players.unnamed")}&gt;
                  </span>
                )
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
    header: () => <div className="text-center">{$("players.player-list.columns.is-online")}</div>,
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
    header: () => <div className="text-center">{$("players.player-list.columns.gamemode")}</div>,
    cell: ({ row }) => {
      const { gamemode } = row.original;
      if(!gamemode) return <></>;
      return <div className="text-center">{gameModeToString(gamemode)}</div>;
    }
  },
  {
    accessorKey: "isWhitelisted",
    header: () => <div className="text-center">{$("players.player-list.columns.is-whitelisted")}</div>,
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
      const { uuid, isOnline, isOp } = row.original;
      return (
        <div className="flex justify-end [&>*]:h-4 [&>*]:cursor-pointer [&>*]:hover:!bg-transparent">
          {
            isOp
            ? (
              <Button
                variant="ghost"
                size="icon"
                title={$("players.action.deop")}
                onClick={async () => {
                  await depriveOp(uuid);
                  emitter.emit("refresh-data");
                }}>
                <UserMinus />
              </Button>
            )
            : (
              <Button
                variant="ghost"
                size="icon"
                title={$("players.action.op")}
                onClick={async () => {
                  await giveOp(uuid);
                  emitter.emit("refresh-data");
                }}>
                <UserStar />
              </Button>
            )
          }
          <Button
            variant="ghost"
            size="icon"
            title={$("players.action.edit-inventory")}
            asChild>
            <Link href={`/panel/players/inventory?uuid=${uuid}`}>
              <Backpack />
            </Link>
          </Button>
          <Prompt
            title={$("players.action.kick.prompt.title")}
            description={$("players.action.kick.prompt.description")}
            label={$("players.action.kick.prompt.label")}
            placeholder={$("players.action.kick.prompt.placeholder")}
            onAction={async (reason) => {
              await kick(uuid, reason);
              emitter.emit("refresh-data");
            }}
            asChild>
            <Button
              variant="ghost"
              size="icon"
              disabled={!isOnline}
              title={$("players.action.kick")}>
              <BrushCleaning />
            </Button>
          </Prompt>
          <Prompt
            title={$("players.action.ban.prompt.title")}
            description={$("players.action.ban.prompt.description")}
            label={$("players.action.ban.prompt.label")}
            placeholder={$("players.action.ban.prompt.placeholder")}
            onAction={async (reason) => {
              await ban(uuid, reason);
              emitter.emit("refresh-data");
            }}
            asChild>
            <Button
              variant="ghost"
              size="icon"
              title={$("players.action.ban")}>
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
    header: $("players.banned-list.columns.name"),
    cell: ({ row }) => {
      const { name, uuid } = row.original;
      return (
        <Tooltip>
          <TooltipTrigger>
            <PlayerSheet player={row.original} asChild>
              <div className="flex items-center gap-2 cursor-pointer">
                <img
                  src={getSettings("players.avatar-provider") + uuid}
                  alt={name}
                  width={17}
                  height={17}
                  onError={(e) => {
                    (e.target as HTMLImageElement).src = SteveAvatar.src;
                  }}/>
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
    header: $("players.banned-list.columns.ban-reason"),
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
        title={$("players.action.pardon")}
        onClick={async () => {
          await pardon(row.original.uuid);
          emitter.emit("refresh-data");
        }}>
        <ShieldOff className="stroke-green-600"/>
      </Button>
    )
  }
];
