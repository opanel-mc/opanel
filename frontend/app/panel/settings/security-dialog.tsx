import type { PropsWithChildren } from "react";
import { useId } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { Controller, useForm } from "react-hook-form";
import { z } from "zod";
import md5 from "md5";
import { Field, FieldLabel, FieldError } from "@/components/ui/field";
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
import { sendPostRequest, toastError } from "@/lib/api";
import { PasswordInput } from "@/components/password-input";
import { $ } from "@/lib/i18n";

const formSchema = z.object({
  currentKey: z.string().nonempty($("settings.security.form.empty")),
  newKey: z.string().nonempty($("settings.security.form.empty")).min(6, $("settings.security.form.new-key.min"))
});

export function SecurityDialog({
  children,
  asChild
}: PropsWithChildren & {
  asChild?: boolean
}) {
  const formId = useId();
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      currentKey: "",
      newKey: ""
    }
  });

  const handleSubmit = async (values: z.infer<typeof formSchema>) => {
    try {
      await sendPostRequest("/api/security", {
        currentKey: md5(values.currentKey), // hashed 1
        newKey: md5(values.newKey) // hashed 1
      });
      window.location.reload();
    } catch (e: any) {
      if(e.status === 403) {
        form.setError("currentKey", { message: $("settings.security.error.400") });
        return;
      }
      toastError(e, $("settings.security.error.401"), [
        [401, $("common.error.401")]
      ]);
    }
  };

  return (
    <Dialog>
      <DialogTrigger asChild={asChild}>{children}</DialogTrigger>
      <DialogContent>
        <form className="flex flex-col gap-4" onSubmit={form.handleSubmit(handleSubmit)}>
          <DialogHeader>
            <DialogTitle>{$("settings.security.title")}</DialogTitle>
            <DialogDescription>
              {$("settings.security.description")}
            </DialogDescription>
          </DialogHeader>
          <Controller
            control={form.control}
            name="currentKey"
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor={`${formId}-${field.name}`}>{$("settings.security.form.current-key.label")}</FieldLabel>
                <PasswordInput
                  id={`${formId}-${field.name}`}
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? `${formId}-${field.name}-error` : undefined}
                  placeholder={$("settings.security.form.current-key.placeholder")}
                  {...field}/>
                {fieldState.invalid && (
                  <FieldError id={`${formId}-${field.name}-error`} errors={[fieldState.error]}/>
                )}
              </Field>
            )}/>
          <Controller
            control={form.control}
            name="newKey"
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor={`${formId}-${field.name}`}>{$("settings.security.form.new-key.label")}</FieldLabel>
                <PasswordInput
                  id={`${formId}-${field.name}`}
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? `${formId}-${field.name}-error` : undefined}
                  placeholder={$("settings.security.form.new-key.placeholder")}
                  {...field}/>
                {fieldState.invalid && (
                  <FieldError id={`${formId}-${field.name}-error`} errors={[fieldState.error]}/>
                )}
              </Field>
            )}/>
          <DialogFooter className="flex flex-row [&>*]:flex-1 [&_button]:cursor-pointer">
            <DialogClose asChild>
              <Button
                variant="outline"
                onClick={() => form.reset()}>
                {$("dialog.cancel")}
              </Button>
            </DialogClose>
            <Button type="submit">{$("dialog.confirm")}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
