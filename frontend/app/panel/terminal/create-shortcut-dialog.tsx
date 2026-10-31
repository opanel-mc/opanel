import type { CommandShortcut } from "@/lib/types";
import { type PropsWithChildren, useEffect, useId, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { Controller, useForm } from "react-hook-form";
import { z } from "zod";
import { Field, FieldLabel, FieldError } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
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
import { $ } from "@/lib/i18n";
import { googleSansCode } from "@/lib/fonts";

const SHORTCUT_NAME_MAX_LENGTH = 30;

const formSchema = z.object({
  name: z
    .string()
    .trim()
    .nonempty($("terminal.shortcut.form.name.empty"))
    .max(
      SHORTCUT_NAME_MAX_LENGTH,
      $("terminal.shortcut.form.name.max-length", SHORTCUT_NAME_MAX_LENGTH.toString())
    ),
  command: z.string().trim().nonempty($("terminal.shortcut.form.command.empty"))
});

export function CreateShortcutDialog({
  children,
  asChild,
  onCreate
}: PropsWithChildren & {
  asChild?: boolean
  onCreate?: (shortcut: CommandShortcut) => void
}) {
  const [dialogOpen, setDialogOpen] = useState(false);
  const formId = useId();
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    mode: "onChange",
    defaultValues: {
      name: "",
      command: ""
    }
  });

  const handleSubmit = (values: z.infer<typeof formSchema>) => {
    const shortcut: CommandShortcut = {
      name: values.name.trim(),
      command: values.command.trim()
    };
    onCreate && onCreate(shortcut);

    form.reset();
    setDialogOpen(false);
  };

  useEffect(() => {
    if(!dialogOpen) {
      form.reset();
    }
  }, [dialogOpen, form]);

  return (
    <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
      <DialogTrigger asChild={asChild}>{children}</DialogTrigger>
      <DialogContent>
        <form className="flex flex-col gap-4" onSubmit={form.handleSubmit(handleSubmit)}>
          <DialogHeader>
            <DialogTitle>{$("terminal.shortcut.create.title")}</DialogTitle>
            <DialogDescription>
              {$("terminal.shortcut.create.description")}
            </DialogDescription>
          </DialogHeader>
          <Controller
            control={form.control}
            name="name"
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor={`${formId}-${field.name}`}>{$("terminal.shortcut.form.name.label")}</FieldLabel>
                <Input
                  id={`${formId}-${field.name}`}
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? `${formId}-${field.name}-error` : undefined}
                  autoComplete="off"
                  placeholder={$("terminal.shortcut.form.name.placeholder")}
                  {...field}/>
                {fieldState.invalid && (
                  <FieldError id={`${formId}-${field.name}-error`} errors={[fieldState.error]}/>
                )}
              </Field>
            )}/>
          <Controller
            control={form.control}
            name="command"
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor={`${formId}-${field.name}`}>{$("terminal.shortcut.form.command.label")}</FieldLabel>
                <Input
                  id={`${formId}-${field.name}`}
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? `${formId}-${field.name}-error` : undefined}
                  className={googleSansCode.className}
                  autoComplete="off"
                  placeholder={$("terminal.shortcut.form.command.placeholder")}
                  {...field}/>
                {fieldState.invalid && (
                  <FieldError id={`${formId}-${field.name}-error`} errors={[fieldState.error]}/>
                )}
              </Field>
            )}/>
          <DialogFooter className="flex flex-row [&>*]:flex-1 [&_button]:cursor-pointer">
            <DialogClose asChild>
              <Button type="button" variant="outline">{$("dialog.cancel")}</Button>
            </DialogClose>
            <Button type="submit">{$("dialog.create")}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
