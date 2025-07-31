import type { PropsWithChildren } from "react";
import { z } from "zod";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { GameMode, type Save } from "@/lib/types";
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
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage
} from "@/components/ui/form";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { MinecraftText } from "@/components/mc-text";
import { sendPostRequest, toastError } from "@/lib/api";

const formSchema = z.object({
  displayName: z.string().nonempty("存档名称不得为空"),
  defaultGameMode: z.enum(Object.values(GameMode) as [string, ...string[]])
});

export function SaveSheet({
  save,
  children,
  asChild
}: PropsWithChildren & {
  save: Save
  asChild?: boolean
}) {
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    values: {
      displayName: save.displayName,
      defaultGameMode: save.defaultGameMode
    }
  });

  const handleSubmit = async (values: z.infer<typeof formSchema>) => {
    try {
      await sendPostRequest(`/api/saves/${save.name}`, values);
      window.location.reload();
    } catch (e: any) {
      toastError(e, "无法保存存档信息", [
        [401, "未登录"],
        [500, "服务器内部错误"]
      ]);
    }
  };

  return (
    <Sheet>
      <SheetTrigger asChild={asChild}>{children}</SheetTrigger>
      <SheetContent>
        <Form {...form}>
          <form className="flex-1 flex flex-col" onSubmit={form.handleSubmit(handleSubmit)}>
            <SheetHeader>
              <SheetTitle>编辑存档</SheetTitle>
              <SheetDescription>
                在此编辑该存档的信息。
              </SheetDescription>
            </SheetHeader>
            <div className="flex-1 px-4 flex flex-col gap-5">
              <FormField
                control={form.control}
                name="displayName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>存档名称</FormLabel>
                    <MinecraftText className="text-center [&_*]:wrap-anywhere">{field.value}</MinecraftText>
                    <div className="flex gap-2">
                      <FormControl>
                        <Input
                          {...field}
                          placeholder="请输入存档名称..."
                          autoFocus
                          autoComplete="off"/>
                      </FormControl>
                      <Button
                        variant="outline"
                        size="icon"
                        className="cursor-pointer"
                        onClick={() => form.setValue("displayName", field.value + "§")}>
                        §
                      </Button>
                    </div>
                    <FormMessage />
                  </FormItem>
                )}/>
              <FormField
                control={form.control}
                name="defaultGameMode"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>默认游戏模式</FormLabel>
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
