import Link from "next/link";
import { useId, useEffect, useRef, useState, type PropsWithChildren } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Backpack, Ban, BrushCleaning, Copy, ShieldOff, Trash } from "lucide-react";
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
  SheetTrigger
} from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { Field, FieldContent, FieldLabel, FieldError, FieldTitle } from "@/components/ui/field";
import { GameMode, type Player } from "@/lib/types";
import { SkinViewer } from "@/components/skin-viewer";
import { OnlineBadge } from "@/components/online-badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Prompt } from "@/components/prompt";
import {
  addToWhitelist,
  ban,
  banIp,
  depriveOp,
  giveOp,
  kick,
  pardon,
  removeFromWhitelist,
  removePlayerData,
  setGameMode
} from "./player-utils";
import { emitter } from "@/lib/emitter";
import { millisToTime } from "@/lib/time";
import { $ } from "@/lib/i18n";
import { Alert } from "@/components/alert";
import { cn, copyToClipboard, sleep } from "@/lib/utils";
import { googleSansCode } from "@/lib/fonts";

const formSchema = z.object({
  gamemode: z.enum(Object.values(GameMode) as [string, ...string[]]),
  isOp: z.boolean(),
  isWhitelisted: z.boolean()
});

export function PlayerSheet({
  player,
  children,
  asChild
}: PropsWithChildren & {
  player: Player
  asChild?: boolean
}) {
  const [onlineTime, setOnlineTime] = useState<number | null>(
    player.joinTime ? Date.now() - player.joinTime : null
  ); // ms
  const positionStr = (
    `${player.position.x.toFixed(0)} ${player.position.y.toFixed(0)} ${player.position.z.toFixed(0)}`
  );
  const timerRef = useRef<NodeJS.Timeout | null>(null);
  const formId = useId();
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    values: {
      gamemode: player.gamemode ?? "adventure",
      isOp: player.isOp,
      isWhitelisted: player.isWhitelisted ?? false
    }
  });

  const handleSubmit = async (values: z.infer<typeof formSchema>) => {
    if(values.gamemode !== player.gamemode) {
      await setGameMode(player.uuid, values.gamemode as GameMode, false);
    }
    if(values.isOp !== player.isOp) {
      player.isOp
      ? await depriveOp(player.uuid, false)
      : await giveOp(player.uuid, false);
    }
    if(
      player.name
      && player.isWhitelisted !== undefined
      && values.isWhitelisted !== player.isWhitelisted
    ) {
      values.isWhitelisted
      ? await addToWhitelist(player.name, player.uuid, false)
      : await removeFromWhitelist(player.name, player.uuid, false);
    }
    emitter.emit("refresh-data");
  };

  const handleBanIp = async () => {
    if(player.isBanned || !player.ip) return;

    await banIp(player.ip);
    await kick(player.uuid, undefined, false);
  };

  useEffect(() => {
    if(!player.isOnline) return;

    timerRef.current = setInterval(() => {
      setOnlineTime((prev) => {
        if(!player.joinTime || !prev) return null;
        return prev + 1000;
      });
    }, 1000);

    return () => {
      if(timerRef.current) {
        clearInterval(timerRef.current);
      }
    };
  }, [player]);

  return (
    <Sheet>
      <SheetTrigger asChild={asChild}>{children}</SheetTrigger>
      <SheetContent>
        <form className="flex-1 flex flex-col" onSubmit={form.handleSubmit(handleSubmit)}>
          <SheetHeader>
            <SheetTitle>{$("players.edit.title")}</SheetTitle>
            <SheetDescription>
              {$("players.edit.description")}
            </SheetDescription>
          </SheetHeader>
          <div className="flex-1 px-4 flex flex-col gap-5">
            <SkinViewer
              name={player.name}
              uuid={player.uuid}/>

            <div className="flex flex-col items-center gap-1">
              <div className="flex items-center gap-2">
                <OnlineBadge isOnline={player.isOnline}/>
                {
                  player.name
                  ? <h2 className="inline-block text-lg font-semibold">{player.name}</h2>
                  : (
                    <span className="text-muted-foreground italic">
                      &lt;{$("players.unnamed")}&gt;
                    </span>
                  )
                }
                {player.name && (
                  <Button
                    variant="ghost"
                    size="icon-xs"
                    className="cursor-pointer text-muted-foreground! hover:bg-transparent! *:size-3!"
                    onClick={() => copyToClipboard(player.name)}>
                    <Copy />
                  </Button>
                )}
              </div>

              <div className="flex items-center">
                <span className={cn("text-xs text-muted-foreground", googleSansCode.className)}>
                  {player.uuid}
                </span>
                <Button
                  variant="ghost"
                  size="icon-xs"
                  className="cursor-pointer text-muted-foreground! hover:bg-transparent! *:size-3!"
                  onClick={() => copyToClipboard(player.uuid)}>
                  <Copy />
                </Button>
              </div>
            </div>

            <div className="flex flex-col gap-3">
              {player.isOnline && (
                <>
                  {player.ip && (
                    <Field orientation="horizontal">
                      <FieldTitle className="flex-1">{$("players.edit.form.ip")}</FieldTitle>
                      <span className={cn("text-xs", googleSansCode.className)}>
                        <Alert
                          title={$("players.edit.form.ip.alert.title", player.ip)}
                          description={$("players.edit.form.ip.alert.description")}
                          onAction={() => handleBanIp()}
                          asChild>
                          <button
                            type="button"
                            className="cursor-pointer mr-1.5 text-destructive">
                            <Ban size={12}/>
                          </button>
                        </Alert>
                        {player.ip}
                      </span>
                    </Field>
                  )}
                  <Field orientation="horizontal">
                    <FieldTitle className="flex-1">{$("players.edit.form.online-time")}</FieldTitle>
                    <span className="text-sm">{millisToTime(onlineTime ?? 0)}</span>
                  </Field>
                </>
              )}
              <Field orientation="horizontal">
                <FieldTitle className="flex-1">{$("players.edit.form.position")}</FieldTitle>
                <span className={cn("text-xs", googleSansCode.className)}>
                  <button
                    type="button"
                    className="cursor-pointer mr-1.5 text-muted-foreground! hover:bg-transparent! *:size-3!"
                    onClick={() => copyToClipboard(positionStr)}>
                    <Copy size={12}/>
                  </button>
                  {positionStr}
                </span>
              </Field>
            </div>
            <Controller
              control={form.control}
              name="gamemode"
              render={({ field, fieldState }) => (
                <Field data-invalid={fieldState.invalid}>
                  <FieldLabel htmlFor={`${formId}-${field.name}`}>{$("players.edit.form.gamemode.label")}</FieldLabel>
                  <Select name={field.name} value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger
                      id={`${formId}-${field.name}`}
                      aria-invalid={fieldState.invalid}
                      aria-describedby={fieldState.invalid ? `${formId}-${field.name}-error` : undefined}
                      ref={field.ref}
                      onBlur={field.onBlur}
                      className="w-full">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="adventure">{$("common.gamemode.adventure")}</SelectItem>
                      <SelectItem value="survival">{$("common.gamemode.survival")}</SelectItem>
                      <SelectItem value="creative">{$("common.gamemode.creative")}</SelectItem>
                      <SelectItem value="spectator">{$("common.gamemode.spectator")}</SelectItem>
                    </SelectContent>
                  </Select>
                  {fieldState.invalid && (
                    <FieldError id={`${formId}-${field.name}-error`} errors={[fieldState.error]}/>
                  )}
                </Field>
              )}/>
            <Controller
              control={form.control}
              name="isOp"
              render={({ field, fieldState }) => (
                <Field orientation="horizontal" data-invalid={fieldState.invalid}>
                  <FieldContent>
                    <FieldLabel htmlFor={`${formId}-${field.name}`}>{$("players.edit.form.op.label")}</FieldLabel>
                    {fieldState.invalid && (
                      <FieldError id={`${formId}-${field.name}-error`} errors={[fieldState.error]}/>
                    )}
                  </FieldContent>
                  <Switch
                    id={`${formId}-${field.name}`}
                    aria-invalid={fieldState.invalid}
                    aria-describedby={fieldState.invalid ? `${formId}-${field.name}-error` : undefined}
                    ref={field.ref}
                    onBlur={field.onBlur}
                    name={field.name}
                    checked={field.value}
                    onCheckedChange={field.onChange}/>
                </Field>
              )}/>
            {(player.name && player.isWhitelisted !== undefined) && (
              <Controller
                control={form.control}
                name="isWhitelisted"
                render={({ field, fieldState }) => (
                  <Field orientation="horizontal" data-invalid={fieldState.invalid}>
                    <FieldContent>
                      <FieldLabel htmlFor={`${formId}-${field.name}`}>{$("players.edit.form.whitelist.label")}</FieldLabel>
                      {fieldState.invalid && (
                        <FieldError id={`${formId}-${field.name}-error`} errors={[fieldState.error]}/>
                      )}
                    </FieldContent>
                    <Switch
                      id={`${formId}-${field.name}`}
                      aria-invalid={fieldState.invalid}
                      aria-describedby={fieldState.invalid ? `${formId}-${field.name}-error` : undefined}
                      ref={field.ref}
                      onBlur={field.onBlur}
                      name={field.name}
                      checked={field.value}
                      onCheckedChange={field.onChange}/>
                  </Field>
                )}/>
            )}
            <div className="space-y-3">
              <Label>{$("players.edit.form.manage")}</Label>
              <div className="grid grid-cols-2 gap-2 [&>*]:cursor-pointer">
                <Button
                  variant="outline"
                  title={$("players.action.edit-inventory")}
                  className="col-span-2"
                  asChild>
                  <Link href={`/panel/players/inventory?uuid=${player.uuid}`}>
                    <Backpack />
                    {$("players.action.edit-inventory")}
                  </Link>
                </Button>
                <Prompt
                  title={$("players.action.kick.prompt.title")}
                  description={$("players.action.kick.prompt.description")}
                  label={$("players.action.kick.prompt.label")}
                  placeholder={$("players.action.kick.prompt.placeholder")}
                  onAction={async (reason) => {
                    await kick(player.uuid, reason);
                    emitter.emit("refresh-data");
                  }}
                  asChild>
                  <Button
                    variant="outline"
                    className="col-span-2"
                    disabled={!player.isOnline}>
                    <BrushCleaning />
                    {$("players.action.kick")}
                  </Button>
                </Prompt>
                {
                  !player.isBanned
                  ? (
                    <Prompt
                      title={$("players.action.ban.prompt.title")}
                      description={$("players.action.ban.prompt.description")}
                      label={$("players.action.ban.prompt.label")}
                      placeholder={$("players.action.ban.prompt.placeholder")}
                      onAction={async (reason) => {
                        await ban(player.uuid, reason);
                        emitter.emit("refresh-data");
                      }}
                      asChild>
                      <Button variant="destructive">
                        <Ban />
                        {$("players.action.ban")}
                      </Button>
                    </Prompt>
                  )
                  : (
                    <Button
                      variant="outline"
                      onClick={async () => {
                        await pardon(player.uuid);
                        emitter.emit("refresh-data");
                      }}>
                      <ShieldOff />
                      {$("players.action.pardon")}
                    </Button>
                  )
                }
                <Alert
                  title={$("players.action.remove.alert.title", player.name)}
                  description={
                    !player.isOnline
                    ? $("players.action.remove.alert.description1")
                    : $("players.action.remove.alert.description2")
                  }
                  onAction={async () => {
                    if(player.isOnline) {
                      await kick(player.uuid, $("players.action.remove.kick-reason"), false);
                      await sleep(100);
                    }
                    await removePlayerData(player.uuid);
                    emitter.emit("refresh-data");
                  }}
                  asChild>
                  <Button variant="destructive">
                    <Trash />
                    {$("players.action.remove")}
                  </Button>
                </Alert>
              </div>
            </div>
          </div>
          <SheetFooter>
            <SheetClose asChild>
              <Button
                type="submit"
                className="cursor-pointer">
                {$("dialog.confirm")}
              </Button>
            </SheetClose>
            <SheetClose asChild>
              <Button
                variant="outline"
                className="cursor-pointer">
                {$("dialog.cancel")}
              </Button>
            </SheetClose>
          </SheetFooter>
        </form>
      </SheetContent>
    </Sheet>
  );
}
