import type { PropsWithChildren } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { setCookie } from "cookies-next/client";
import md5 from "md5";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { sendPostRequest, toastError } from "@/lib/api";

const formSchema = z.object({
  currentKey: z.string().nonempty("此项不可为空"),
  newKey: z.string().nonempty("此项不可为空").min(6, "访问密钥长度不应小于6")
});

export function SecurityDialog({
  children,
  asChild
}: PropsWithChildren & {
  asChild?: boolean
}) {
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      currentKey: "",
      newKey: ""
    }
  });

  const handleSubmit = async (values: z.infer<typeof formSchema>) => {
    try {
      const res = await sendPostRequest<{ token: string }>("/api/security", {
        currentKey: md5(values.currentKey), // hashed 1
        newKey: md5(values.newKey) // hashed 1
      });
      setCookie("token", res.token);
      window.location.reload();
    } catch (e: any) {
      if(e.status === 403) {
        form.setError("currentKey", { message: "访问密钥错误" });
        return;
      }
      toastError(e, "无法修改访问密钥", [
        [401, "未登录"]
      ]);
    }
  };

  return (
    <Dialog>
      <DialogTrigger asChild={asChild}>{children}</DialogTrigger>
      <DialogContent>
        <Form {...form}>
          <form className="flex flex-col gap-4" onSubmit={form.handleSubmit(handleSubmit)}>
            <DialogHeader>
              <DialogTitle>修改访问密钥</DialogTitle>
              <DialogDescription>
                在此修改OPanel面板的访问密钥。
              </DialogDescription>
            </DialogHeader>
            <FormField
              control={form.control}
              name="currentKey"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>当前密钥</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="请输入当前访问密钥..."
                      autoComplete="off"
                      type="password"
                      {...field}/>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}/>
            <FormField
              control={form.control}
              name="newKey"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>新密钥</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="请输入新访问密钥..."
                      autoComplete="off"
                      type="password"
                      {...field}/>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}/>
            <DialogFooter className="flex flex-row [&>*]:flex-1 [&_button]:cursor-pointer">
              <DialogClose asChild>
                <Button
                  variant="outline"
                  onClick={() => form.reset()}>
                  取消
                </Button>
              </DialogClose>
              <Button type="submit">确认</Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
