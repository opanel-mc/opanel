import type { PropsWithChildren } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { MinecraftText } from "@/components/mc-text";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormMessage
} from "@/components/ui/form";
import { Textarea } from "@/components/ui/textarea";
import { sendPostRequest, toastError } from "@/lib/api";
import { purify, transformText } from "@/lib/formatting-codes/text";
import { emitter } from "@/lib/emitter";

const formSchema = z.object({
  motd: z.string()
    .nonempty("Motd不可为空")
    .refine((str) => str.split("\n").length <= 2, "Motd行数最大为2")
});

export function MotdEditor({
  motd,
  asChild,
  children
}: PropsWithChildren<{
  motd: string
  asChild?: boolean
}>) {
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    values: { motd: purify(motd) }
  });

  const handleSubmit = async (values: z.infer<typeof formSchema>) => {
    try {
      await sendPostRequest("/api/info/motd", btoa(transformText(values.motd)));
      emitter.emit("refresh-data");
    } catch (e: any) {
      toastError(e, "修改Motd失败", [
        [400, "请求参数错误"],
        [401, "未登录"],
        [500, "服务器内部错误"]
      ]);
    }
  };
  
  return (
    <Dialog>
      <DialogTrigger asChild={asChild}>
        {children}
      </DialogTrigger>
      <DialogContent>
        <Form {...form}>
          <form className="flex flex-col gap-4" onSubmit={form.handleSubmit(handleSubmit)}>
            <DialogHeader>
              <DialogTitle>编辑 Motd</DialogTitle>
              <DialogDescription>
                在此编辑和预览服务器 Motd。<span className="text-red-700 dark:text-red-400">注：此功能在Fabric不稳定，原因未知。</span>
              </DialogDescription>
            </DialogHeader>
            <FormField
              control={form.control}
              name="motd"
              render={({ field }) => (
                <FormItem>
                  <FormControl>
                    <div className="flex flex-col gap-3 min-w-0">
                      <MinecraftText maxLines={2} className="wrap-anywhere">{"§7"+ field.value}</MinecraftText>
                      <Textarea
                        {...field}
                        rows={2}
                        placeholder="请输入文本..."/>
                    </div>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}/>
            <DialogFooter className="flex flex-row !justify-between">
              <Button
                type="button"
                variant="outline"
                size="icon"
                className="cursor-pointer"
                onClick={() => form.setValue("motd", form.getValues().motd + "§")}>
                §
              </Button>
              <div className="space-x-2">
                <DialogClose asChild>
                  <Button
                    variant="outline"
                    onClick={() => form.reset()}>
                    取消
                  </Button>
                </DialogClose>
                <DialogClose asChild>
                  <Button type="submit">保存</Button>
                </DialogClose>
              </div>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
