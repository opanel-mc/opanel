import type { ColumnDef } from "@tanstack/react-table";
import type { Player } from "@/lib/types";
import Link from "next/link";
import { Backpack, Ban, BrushCleaning, Check, ShieldHalf, ShieldOff, UserMinus, UserStar } from "lucide-react";
import { base64ToString, cn, gameModeToString } from "@/lib/utils";
import { HoverCard, HoverCardContent, HoverCardTrigger } from "@/components/ui/hover-card";
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

function PlayerAvatar({
  name,
  className
}: {
  name: string
  className?: string
}) {
  return (
    <img
      src={getSettings("players.avatar-provider") + name}
      alt={name}
      width={17}
      height={17}
      className={cn("image-pixelated", className)}
      onError={(e) => {
        (e.target as HTMLImageElement).src = SteveAvatar.src;
      }}/>
  );
}

function PlayerHoverInfo({
  name,
  uuid,
  isOp
}: Player) {
  return (
    <div className="flex gap-3" onClick={(e) => e.stopPropagation()}>
      <PlayerAvatar
        name={name}
        className="w-14 h-14 aspect-square"/>
      <div className="flex flex-col gap-2">
        <span className="font-semibold flex items-center gap-1.5">
          {name}
          {isOp && <ShieldHalf size={13} className="stroke-muted-foreground"/>}
        </span>
        <span className="text-sm text-muted-foreground">{uuid}</span>
      </div>
    </div>
  );
}

export const playerColumns: ColumnDef<Player>[] = [
  {
    accessorKey: "name",
    header: $("players.player-list.columns.name"),
    cell: ({ row }) => {
      const { name } = row.original;
      return (
        <PlayerSheet player={row.original} asChild>
          {
            name
            ? (
              <div className="flex items-center gap-2 cursor-pointer">
                <HoverCard closeDelay={100}>
                  <HoverCardTrigger>
                    <PlayerAvatar name={name}/>
                  </HoverCardTrigger>
                  <HoverCardContent side="top">
                    <PlayerHoverInfo {...row.original}/>
                  </HoverCardContent>
                </HoverCard>
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
    cell: ({ row }) => (
      <div className="flex justify-center">
        {
          row.original.isWhitelisted
          ? <Check size={18} color="var(--color-muted-foreground)"/>
          : <></>
        }
      </div>
    ),
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
      const { name } = row.original;
      return (
        <PlayerSheet player={row.original} asChild>
          <div className="flex items-center gap-2 cursor-pointer">
            <HoverCard closeDelay={100}>
              <HoverCardTrigger>
                <PlayerAvatar name={name}/>
              </HoverCardTrigger>
              <HoverCardContent side="top">
                <PlayerHoverInfo {...row.original}/>
              </HoverCardContent>
            </HoverCard>
            <span className="font-semibold">{name}</span>
          </div>
        </PlayerSheet>
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
