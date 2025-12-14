import { useState, type PropsWithChildren } from "react";
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
import { stringToBase64 } from "@/lib/utils";
import { $ } from "@/lib/i18n";
import { Text } from "@/components/i18n-text";

const formSchema = z.object({
  motd: z.string()
    .nonempty($("dashboard.motd.textarea.empty"))
    .refine((str) => str.split("\n").length <= 2, $("dashboard.motd.textarea.max-lines"))
});

export function MotdEditor({
  motd,
  asChild,
  children
}: PropsWithChildren<{
  motd: string
  asChild?: boolean
}>) {
  const [dialogOpen, setDialogOpen] = useState(false);

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    values: { motd: purify(motd) }
  });

  const handleSubmit = async (values: z.infer<typeof formSchema>) => {
    try {
      await sendPostRequest("/api/info/motd", transformText(stringToBase64(values.motd)));
      emitter.emit("refresh-data");
      setDialogOpen(false);
    } catch (e: any) {
      toastError(e, $("dashboard.motd.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    }
  };
  
  return (
    <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
      <DialogTrigger asChild={asChild}>
        {children}
      </DialogTrigger>
      <DialogContent>
        <Form {...form}>
          <form className="flex flex-col gap-4" onSubmit={form.handleSubmit(handleSubmit)}>
            <DialogHeader>
              <DialogTitle>{$("dashboard.motd.title")}</DialogTitle>
              <DialogDescription>
                <Text
                  id="dashboard.motd.description"
                  className="[&>span]:text-red-700 dark:[&>span]:text-red-400"/>
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
                        placeholder={$("dashboard.motd.textarea.placeholder")}
                        onKeyDown={(e) => (e.key === "Enter" && e.ctrlKey) && form.handleSubmit(handleSubmit)()}/>
                    </div>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}/>
            <DialogFooter className="flex flex-row !justify-between">
              <div className="space-x-4">
                <Button
                  type="button"
                  variant="outline"
                  size="icon"
                  className="cursor-pointer"
                  onClick={() => form.setValue("motd", form.getValues().motd + "§")}>
                  §
                </Button>
                <Text
                  id="dashboard.motd.hint"
                  args={[
                    <><kbd>ctrl</kbd>+<kbd>Enter</kbd></>
                  ]}
                  className="text-sm text-muted-foreground max-sm:hidden"/>
              </div>
              <div className="space-x-2">
                <DialogClose asChild>
                  <Button
                    variant="outline"
                    onClick={() => form.reset()}>
                    {$("dialog.cancel")}
                  </Button>
                </DialogClose>
                <Button type="submit">{$("dialog.save")}</Button>
              </div>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
