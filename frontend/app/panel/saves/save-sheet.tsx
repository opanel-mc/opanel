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
import { emitter } from "@/lib/emitter";
import { base64ToString, stringToBase64 } from "@/lib/utils";
import { $ } from "@/lib/i18n";

const formSchema = z.object({
  displayName: z.string().nonempty($("saves.edit.form.name.empty")),
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
      displayName: base64ToString(save.displayName),
      defaultGameMode: save.defaultGameMode
    }
  });

  const handleSubmit = async (values: z.infer<typeof formSchema>) => {
    try {
      values.displayName = stringToBase64(values.displayName); // Use base64 to avoid encoding issue
      await sendPostRequest(`/api/saves/${save.name}`, values);
      emitter.emit("refresh-data");
    } catch (e: any) {
      toastError(e, $("saves.edit.error"), [
        [401, $("common.error.401")],
        [500, $("common.error.500")]
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
              <SheetTitle>{$("saves.edit.title")}</SheetTitle>
              <SheetDescription>
                {$("saves.edit.description")}
              </SheetDescription>
            </SheetHeader>
            <div className="flex-1 px-4 flex flex-col gap-5">
              <FormField
                control={form.control}
                name="displayName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{$("saves.edit.form.name.label")}</FormLabel>
                    <MinecraftText className="text-center [&_*]:wrap-anywhere">{field.value}</MinecraftText>
                    <div className="flex gap-2">
                      <FormControl>
                        <Input
                          {...field}
                          placeholder={$("saves.edit.form.name.placeholder")}
                          autoFocus
                          autoComplete="off"/>
                      </FormControl>
                      <Button
                        variant="outline"
                        size="icon"
                        className="cursor-pointer"
                        onClick={(e) => {
                          e.preventDefault();
                          form.setValue("displayName", field.value + "§");
                        }}>
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
                    <FormLabel>{$("saves.edit.form.gamemode.label")}</FormLabel>
                    <FormControl>
                      <Select {...field} onValueChange={field.onChange}>
                        <SelectTrigger className="w-full">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value={GameMode.ADVENTURE}>{$("common.gamemode.adventure")}</SelectItem>
                          <SelectItem value={GameMode.SURVIVAL}>{$("common.gamemode.survival")}</SelectItem>
                          <SelectItem value={GameMode.CREATIVE}>{$("common.gamemode.creative")}</SelectItem>
                          <SelectItem value={GameMode.SPECTATOR}>{$("common.gamemode.spectator")}</SelectItem>
                        </SelectContent>
                      </Select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}/>
            </div>
            <SheetFooter>
              <SheetClose asChild>
                <Button
                  type="submit"
                  className="cursor-pointer">
                  {$("dialog.save")}
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
