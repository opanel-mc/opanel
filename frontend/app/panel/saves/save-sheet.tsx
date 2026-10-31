import type { PropsWithChildren } from "react";
import { useId } from "react";
import { z } from "zod";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Difficulty, GameMode, type Save } from "@/lib/types";
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
import { Field, FieldContent, FieldLabel, FieldError } from "@/components/ui/field";
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
import { Switch } from "@/components/ui/switch";
import { useTrigger } from "@/hooks/use-trigger";

const formSchema = z.object({
  displayName: z.string().nonempty($("saves.edit.form.name.empty")),
  defaultGameMode: z.enum(Object.values(GameMode) as [string, ...string[]]),
  difficulty: z.enum(Object.values(Difficulty) as [string, ...string[]]),
  isDifficultyLocked: z.boolean(),
  isHardcore: z.boolean()
});

export function SaveSheet({
  save,
  children,
  asChild
}: PropsWithChildren & {
  save: Save
  asChild?: boolean
}) {
  const formId = useId();
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    values: {
      displayName: base64ToString(save.displayName),
      defaultGameMode: save.defaultGameMode,
      difficulty: save.difficulty,
      isDifficultyLocked: save.isDifficultyLocked,
      isHardcore: save.isHardcore
    }
  });
  const { trigger } = useTrigger();

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
        <form className="flex-1 flex flex-col" onSubmit={form.handleSubmit(handleSubmit)}>
          <SheetHeader>
            <SheetTitle>{$("saves.edit.title")}</SheetTitle>
            <SheetDescription>
              {$("saves.edit.description")}
            </SheetDescription>
          </SheetHeader>
          <div className="flex-1 px-4 flex flex-col gap-5">
            <Controller
              control={form.control}
              name="displayName"
              render={({ field, fieldState }) => (
                <Field data-invalid={fieldState.invalid}>
                  <FieldLabel htmlFor={`${formId}-${field.name}`}>{$("saves.edit.form.name.label")}</FieldLabel>
                  <MinecraftText className="text-center [&_*]:wrap-anywhere">{field.value}</MinecraftText>
                  <div className="flex gap-2">
                    <Input
                      id={`${formId}-${field.name}`}
                      aria-invalid={fieldState.invalid}
                      aria-describedby={fieldState.invalid ? `${formId}-${field.name}-error` : undefined}
                      {...field}
                      placeholder={$("saves.edit.form.name.placeholder")}
                      autoFocus
                      autoComplete="off"/>
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
                  {fieldState.invalid && (
                    <FieldError id={`${formId}-${field.name}-error`} errors={[fieldState.error]}/>
                  )}
                </Field>
              )}/>
            <Controller
              control={form.control}
              name="defaultGameMode"
              render={({ field, fieldState }) => (
                <Field data-invalid={fieldState.invalid}>
                  <FieldLabel htmlFor={`${formId}-${field.name}`}>{$("saves.edit.form.gamemode.label")}</FieldLabel>
                  <Select
                    name={field.name}
                    value={field.value}
                    disabled={form.getValues("isHardcore")}
                    onValueChange={field.onChange}>
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
                      <SelectItem value={GameMode.ADVENTURE}>{$("common.gamemode.adventure")}</SelectItem>
                      <SelectItem value={GameMode.SURVIVAL}>{$("common.gamemode.survival")}</SelectItem>
                      <SelectItem value={GameMode.CREATIVE}>{$("common.gamemode.creative")}</SelectItem>
                      <SelectItem value={GameMode.SPECTATOR}>{$("common.gamemode.spectator")}</SelectItem>
                    </SelectContent>
                  </Select>
                  {fieldState.invalid && (
                    <FieldError id={`${formId}-${field.name}-error`} errors={[fieldState.error]}/>
                  )}
                </Field>
              )}/>
            <Controller
              control={form.control}
              name="difficulty"
              render={({ field, fieldState }) => (
                <Field data-invalid={fieldState.invalid}>
                  <FieldLabel htmlFor={`${formId}-${field.name}`}>{$("saves.edit.form.difficulty.label")}</FieldLabel>
                  <Select
                    name={field.name}
                    value={field.value}
                    disabled={form.getValues("isHardcore") || form.getValues("isDifficultyLocked")}
                    onValueChange={field.onChange}>
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
                      <SelectItem value={Difficulty.PEACEFUL}>{$("common.difficulty.peaceful")}</SelectItem>
                      <SelectItem value={Difficulty.EASY}>{$("common.difficulty.easy")}</SelectItem>
                      <SelectItem value={Difficulty.NORMAL}>{$("common.difficulty.normal")}</SelectItem>
                      <SelectItem value={Difficulty.HARD}>{$("common.difficulty.hard")}</SelectItem>
                    </SelectContent>
                  </Select>
                  {fieldState.invalid && (
                    <FieldError id={`${formId}-${field.name}-error`} errors={[fieldState.error]}/>
                  )}
                </Field>
              )}/>
            <Controller
              control={form.control}
              name="isDifficultyLocked"
              render={({ field, fieldState }) => (
                <Field orientation="horizontal" data-invalid={fieldState.invalid}>
                  <FieldContent>
                    <FieldLabel htmlFor={`${formId}-${field.name}`}>{$("saves.edit.form.difficulty-locked.label")}</FieldLabel>
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
                    disabled={form.getValues("isHardcore")}
                    onCheckedChange={(checked) => {
                      field.onChange(checked);
                      trigger();
                    }}/>
                </Field>
              )}/>
            <Controller
              control={form.control}
              name="isHardcore"
              render={({ field, fieldState }) => (
                <Field orientation="horizontal" data-invalid={fieldState.invalid}>
                  <FieldContent>
                    <FieldLabel htmlFor={`${formId}-${field.name}`}>{$("saves.edit.form.hardcore.label")}</FieldLabel>
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
                    onCheckedChange={(checked) => {
                      field.onChange(checked);
                      if(checked) {
                        form.setValue("defaultGameMode", GameMode.SURVIVAL);
                        form.setValue("difficulty", Difficulty.HARD);
                        form.setValue("isDifficultyLocked", true);
                      }
                      trigger();
                    }}/>
                </Field>
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
      </SheetContent>
    </Sheet>
  );
}
