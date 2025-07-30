import type { PropsWithChildren } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Ban, BrushCleaning, ShieldOff } from "lucide-react";
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
import { ban, depriveOp, giveOp, kick, pardon, setGameMode } from "./player-utils";

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
    window.location.reload();
  };

  return (
    <Sheet>
      <SheetTrigger asChild={asChild}>{children}</SheetTrigger>
      <SheetContent>
        <Form {...form}>
          <form className="flex-1 flex flex-col" onSubmit={form.handleSubmit(handleSubmit)}>
            <SheetHeader>
              <SheetTitle>编辑玩家</SheetTitle>
              <SheetDescription>
                在此设置和管理该玩家的状态和角色。
              </SheetDescription>
            </SheetHeader>
            <div className="flex-1 px-4 flex flex-col gap-5">
              <SkinViewer uuid={player.uuid}/>
              <div className="flex justify-center items-center gap-2">
                <OnlineBadge isOnline={player.isOnline}/>
                <h2 className="inline-block text-lg font-semibold">{player.name}</h2>
              </div>
              <FormField
                control={form.control}
                name="gamemode"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>游戏模式</FormLabel>
                    <FormControl>
                      <Select {...field} onValueChange={field.onChange}>
                        <SelectTrigger className="w-full">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="adventure">冒险</SelectItem>
                          <SelectItem value="survival">生存</SelectItem>
                          <SelectItem value="creative">创造</SelectItem>
                          <SelectItem value="spectator">旁观</SelectItem>
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
                    <FormLabel>OP权限</FormLabel>
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
                <Label>管理</Label>
                <div className="flex gap-2 [&>*]:flex-1 [&>*]:cursor-pointer">
                  <Prompt
                    title="踢出玩家"
                    description="将玩家踢出服务器，之后玩家可重新加入服务器"
                    label="原因"
                    placeholder="请输入踢出原因..."
                    onAction={async (reason) => {
                      await kick(player.uuid, reason);
                      window.location.reload();
                    }}
                    asChild>
                    <Button
                      variant="outline"
                      disabled={!player.isOnline}>
                      <BrushCleaning />
                      踢出服务器
                    </Button>
                  </Prompt>
                  {
                    !player.isBanned
                    ? (
                      <Prompt
                        title="封禁玩家"
                        description="将玩家踢出服务器并加入封禁列表，之后玩家将不可重新加入服务器"
                        label="原因"
                        placeholder="请输入封禁原因..."
                        onAction={async (reason) => {
                          await ban(player.uuid, reason);
                          window.location.reload();
                        }}
                        asChild>
                        <Button variant="destructive">
                          <Ban />
                          封禁玩家
                        </Button>
                      </Prompt>
                    )
                    : (
                      <Button
                        variant="outline"
                        onClick={async () => {
                          await pardon(player.uuid);
                          window.location.reload();
                        }}>
                        <ShieldOff />
                        解除封禁
                      </Button>
                    )
                  }
                </div>
              </div>
            </div>
            <SheetFooter>
              <Button
                type="submit"
                className="cursor-pointer">
                确定
              </Button>
              <SheetClose asChild>
                <Button
                  variant="outline"
                  className="cursor-pointer">
                  取消
                </Button>
              </SheetClose>
            </SheetFooter>
          </form>
        </Form>
      </SheetContent>
    </Sheet>
  );
}
