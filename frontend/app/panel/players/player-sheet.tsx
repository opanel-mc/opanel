import type { PropsWithChildren } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Ban, BrushCleaning, ShieldOff, UserMinus, UserPlus } from "lucide-react";
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
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage
} from "@/components/ui/form";
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
  depriveOp,
  giveOp,
  kick,
  pardon,
  removeFromWhitelist,
  setGameMode
} from "./player-utils";
import { emitter } from "@/lib/emitter";
import { $ } from "@/lib/i18n";

const formSchema = z.object({
  gamemode: z.enum(Object.values(GameMode) as [string, ...string[]]),
  isOp: z.boolean()
});

export function PlayerSheet({
  player,
  children,
  asChild
}: PropsWithChildren & {
  player: Player
  asChild?: boolean
}) {
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    values: {
      gamemode: player.gamemode ?? "adventure",
      isOp: player.isOp
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
    emitter.emit("refresh-data");
  };

  return (
    <Sheet>
      <SheetTrigger asChild={asChild}>{children}</SheetTrigger>
      <SheetContent>
        <Form {...form}>
          <form className="flex-1 flex flex-col" onSubmit={form.handleSubmit(handleSubmit)}>
            <SheetHeader>
              <SheetTitle>{$("players.edit.title")}</SheetTitle>
              <SheetDescription>
                {$("players.edit.description")}
              </SheetDescription>
            </SheetHeader>
            <div className="flex-1 px-4 flex flex-col gap-5">
              <SkinViewer uuid={player.uuid}/>
              <div className="flex justify-center items-center gap-2">
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
              </div>
              <FormField
                control={form.control}
                name="gamemode"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{$("players.edit.form.gamemode.label")}</FormLabel>
                    <FormControl>
                      <Select {...field} onValueChange={field.onChange}>
                        <SelectTrigger className="w-full">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="adventure">{$("common.gamemode.adventure")}</SelectItem>
                          <SelectItem value="survival">{$("common.gamemode.survival")}</SelectItem>
                          <SelectItem value="creative">{$("common.gamemode.creative")}</SelectItem>
                          <SelectItem value="spectator">{$("common.gamemode.spectator")}</SelectItem>
                        </SelectContent>
                      </Select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}/>
              <FormField
                control={form.control}
                name="isOp"
                render={({ field }) => (
                  <FormItem className="flex justify-between">
                    <FormLabel>{$("players.edit.form.op.label")}</FormLabel>
                    <FormControl>
                      <Switch
                        {...field}
                        value=""
                        defaultChecked={player.isOp}
                        onCheckedChange={field.onChange}/>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}/>
              <div className="space-y-3">
                <Label>{$("players.edit.form.manage")}</Label>
                <div className="grid grid-rows-2 grid-cols-2 gap-2 [&>*]:cursor-pointer">
                  {(player.name && player.isWhitelisted !== undefined) && (
                    player.isWhitelisted
                    ? (
                      <Button
                        variant="outline"
                        onClick={async () => {
                          await removeFromWhitelist(player.name, player.uuid);
                          emitter.emit("refresh-data");
                        }}>
                        <UserMinus />
                        {$("players.action.remove-from-whitelist")}
                      </Button>
                    )
                    : (
                      <Button
                        variant="outline"
                        onClick={async () => {
                          await addToWhitelist(player.name, player.uuid);
                          emitter.emit("refresh-data");
                        }}>
                        <UserPlus />
                        {$("players.action.add-to-whitelist")}
                      </Button>
                    )
                  )}
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
                      className={player.isWhitelisted === undefined ? "col-start-1" : "col-start-2"}
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
                        <Button
                          variant="destructive"
                          className={player.isWhitelisted === undefined ? "col-start-2" : "col-span-2"}>
                          <Ban />
                          {$("players.action.ban")}
                        </Button>
                      </Prompt>
                    )
                    : (
                      <Button
                        variant="outline"
                        className={player.isWhitelisted === undefined ? "col-start-2" : "col-span-2"}
                        onClick={async () => {
                          await pardon(player.uuid);
                          emitter.emit("refresh-data");
                        }}>
                        <ShieldOff />
                        {$("players.action.pardon")}
                      </Button>
                    )
                  }
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
        </Form>
      </SheetContent>
    </Sheet>
  );
}
