import type { CreateTaskResponse, ScheduledTask } from "@/lib/types";
import { useId, useState } from "react";
import dynamic from "next/dynamic";
import Link from "next/link";
import { useTheme } from "next-themes";
import { LayoutTemplate } from "lucide-react";
import * as z from "zod";
import { Controller, useForm } from "react-hook-form";
import { toast } from "sonner";
import { zodResolver } from "@hookform/resolvers/zod";
import { FieldDescription, Field, FieldLabel, FieldError, FieldTitle } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger
} from "@/components/ui/tabs";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "@/components/ui/select";
import { base64ToString, stringToBase64 } from "@/lib/utils";
import { googleSansCode } from "@/lib/fonts";
import { Button } from "@/components/ui/button";
import { monacoSettingsOptions } from "@/lib/settings";
import { sendPostRequest, toastError } from "@/lib/api";
import { $ } from "@/lib/i18n";
import { Text } from "@/components/i18n-text";
import { Spinner } from "@/components/ui/spinner";
import { TaskTemplateDialog } from "./task-template-dialog";

const MonacoEditor = dynamic(() => import("@/components/monaco-editor"), { ssr: false });

export type TaskFormMode = "create" | "edit";

function setCronPart(originalCron: string, index: number, newValue: string): string {
  const parts = originalCron.split(" ");
  parts[index] = newValue;
  return parts.join(" ");
}

function cronWeekToValue(week: string): "0" | "1" | "2" | "3" | "4" | "5" | "6" | "*" {
  switch(week.toLowerCase()) {
    case "*":
    case "0":
    case "1":
    case "2":
    case "3":
    case "4":
    case "5":
    case "6":
      return week as any;
    case "7":
      return "0";
    case "mon":
      return "1";
    case "tue":
      return "2";
    case "wed":
      return "3";
    case "thu":
      return "4";
    case "fri":
      return "5";
    case "sat":
      return "6";
    case "sun":
      return "0";
    default:
      return "*";
  }
}

export const formSchema = z.object({
  name: z.string().min(1, $("tasks.form.name.empty")),
  cron: z.string().min(1),
  commands: z
    .array(z.string())
    .refine(
      (commands) => commands.some((command) => command.trim().length > 0),
      $("tasks.form.commands.empty2")
    ),
});

export function TaskForm({
  task,
  mode = "create",
  ready = true,
  onCreate,
  onEdit
}: {
  task: ScheduledTask
  mode: TaskFormMode
  ready: boolean
  onCreate?: (id: string) => void
  onEdit?: () => void
}) {
  const { theme } = useTheme();
  const [cronInputType, setCronInputType] = useState<"simple" | "advanced">("simple");
  const [loading, setLoading] = useState(false);

  const formId = useId();
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    values: {
      name: base64ToString(task.name),
      cron: task.cron,
      commands: task.commands
    }
  });

  const handleCreate = async (data: z.infer<typeof formSchema>) => {
    try {
      setLoading(true);
      const res = await sendPostRequest<CreateTaskResponse>("/api/tasks", {
        name: stringToBase64(data.name),
        cron: data.cron,
        commands: data.commands
      });
      onCreate && onCreate(res.taskId);
      toast.success($("tasks.create.success"));
    } catch (e: any) {
      toastError(e, $("tasks.create.error"), [
        [400, $("tasks.error.400")],
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = async (data: z.infer<typeof formSchema>) => {
    try {
      setLoading(true);
      await sendPostRequest(`/api/tasks/${task.id}`, data);
      onEdit && onEdit();
      toast.success($("tasks.edit.success"));
    } catch (e: any) {
      toastError(e, $("tasks.edit.error"), [
        [400, $("tasks.error.400")],
        [401, $("common.error.401")],
        [404, $("tasks.error.404")],
        [500, $("common.error.500")]
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (data: z.infer<typeof formSchema>) => {
    mode === "create"
    ? handleCreate(data)
    : handleEdit(data);
  };

  return (
    <form
      className="flex-1 min-w-0 p-4 flex flex-col gap-4 overflow-x-hidden overflow-y-auto o-scrollbar"
      data-taskid={task.id}
      onSubmit={form.handleSubmit(handleSubmit)}>
      <Controller
        name="name"
        control={form.control}
        render={({ field, fieldState }) => (
          <Field data-invalid={fieldState.invalid}>
            <FieldLabel htmlFor={`${formId}-${field.name}`}>{$("tasks.form.name.label")}</FieldLabel>
            <Input
              id={`${formId}-${field.name}`}
              aria-invalid={fieldState.invalid}
              aria-describedby={fieldState.invalid ? `${formId}-${field.name}-error` : undefined}
              placeholder={$("tasks.form.name.placeholder")}
              autoComplete="off"
              {...field}/>
            {fieldState.invalid && (
              <FieldError id={`${formId}-${field.name}-error`} errors={[fieldState.error]}/>
            )}
          </Field>
        )}/>

      <Controller
        name="cron"
        control={form.control}
        render={({ field, fieldState }) => {
          const [minutes = "", hours = "", days = "", months = "", weekdays = "*"] = field.value.split(" ");

          return (
            <div className="h-max flex flex-col gap-3">
              <Label>{$("tasks.form.cron.label")}</Label>
              <Tabs
                value={cronInputType}
                onValueChange={(value) => setCronInputType(value as any)}>
                <TabsList>
                  <TabsTrigger value="simple">{$("tasks.form.cron.simple")}</TabsTrigger>
                  <TabsTrigger value="advanced">{$("tasks.form.cron.advanced")}</TabsTrigger>
                </TabsList>
                <div
                  className="*:data-[slot=tabs-content]:p-2 w-[calc(200%+16px)] flex gap-4 overflow-hidden"
                  style={{ transform: cronInputType === "simple" ? "translateX(0%)" : "translateX(calc(-50% - 8px))" }}>
                  <TabsContent value="simple" forceMount>
                    <div className="w-full grid grid-cols-3 max-sm:grid-cols-2 gap-4">
                      <Field data-invalid={fieldState.invalid} className="flex-1">
                        <FieldLabel htmlFor={`${formId}-${field.name}-minutes`}>{$("tasks.form.cron.simple.minutes")}</FieldLabel>
                        <Input
                          id={`${formId}-${field.name}-minutes`}
                          aria-invalid={fieldState.invalid}
                          aria-describedby={fieldState.invalid ? `${formId}-${field.name}-minutes-error` : undefined}
                          onBlur={field.onBlur}
                          ref={cronInputType === "simple" ? field.ref : undefined}
                          value={minutes}
                          onChange={(e) => field.onChange(setCronPart(field.value, 0, e.target.value))}
                          autoComplete="off"/>
                        {fieldState.invalid && (
                          <FieldError id={`${formId}-${field.name}-minutes-error`} errors={[fieldState.error]}/>
                        )}
                      </Field>
                      <Field data-invalid={fieldState.invalid} className="flex-1">
                        <FieldLabel htmlFor={`${formId}-${field.name}-hours`}>{$("tasks.form.cron.simple.hours")}</FieldLabel>
                        <Input
                          id={`${formId}-${field.name}-hours`}
                          aria-invalid={fieldState.invalid}
                          aria-describedby={fieldState.invalid ? `${formId}-${field.name}-hours-error` : undefined}
                          onBlur={field.onBlur}
                          value={hours}
                          onChange={(e) => field.onChange(setCronPart(field.value, 1, e.target.value))}
                          autoComplete="off"/>
                        {fieldState.invalid && (
                          <FieldError id={`${formId}-${field.name}-hours-error`} errors={[fieldState.error]}/>
                        )}
                      </Field>
                      <Field data-invalid={fieldState.invalid} className="flex-1">
                        <FieldLabel htmlFor={`${formId}-${field.name}-days`}>{$("tasks.form.cron.simple.days")}</FieldLabel>
                        <Input
                          id={`${formId}-${field.name}-days`}
                          aria-invalid={fieldState.invalid}
                          aria-describedby={fieldState.invalid ? `${formId}-${field.name}-days-error` : undefined}
                          onBlur={field.onBlur}
                          value={days}
                          onChange={(e) => field.onChange(setCronPart(field.value, 2, e.target.value))}
                          autoComplete="off"/>
                        {fieldState.invalid && (
                          <FieldError id={`${formId}-${field.name}-days-error`} errors={[fieldState.error]}/>
                        )}
                      </Field>
                      <Field data-invalid={fieldState.invalid} className="flex-1">
                        <FieldLabel htmlFor={`${formId}-${field.name}-months`}>{$("tasks.form.cron.simple.months")}</FieldLabel>
                        <Input
                          id={`${formId}-${field.name}-months`}
                          aria-invalid={fieldState.invalid}
                          aria-describedby={fieldState.invalid ? `${formId}-${field.name}-months-error` : undefined}
                          onBlur={field.onBlur}
                          value={months}
                          onChange={(e) => field.onChange(setCronPart(field.value, 3, e.target.value))}
                          autoComplete="off"/>
                        {fieldState.invalid && (
                          <FieldError id={`${formId}-${field.name}-months-error`} errors={[fieldState.error]}/>
                        )}
                      </Field>
                      <Field data-invalid={fieldState.invalid} className="flex-1">
                        <FieldLabel htmlFor={`${formId}-${field.name}-weekdays`}>{$("tasks.form.cron.simple.weekdays")}</FieldLabel>
                        <Select
                          value={cronWeekToValue(weekdays)}
                          onValueChange={(value) => field.onChange(setCronPart(field.value, 4, value))}>
                          <SelectTrigger
                            id={`${formId}-${field.name}-weekdays`}
                            aria-invalid={fieldState.invalid}
                            aria-describedby={fieldState.invalid ? `${formId}-${field.name}-weekdays-error` : undefined}
                            onBlur={field.onBlur}
                            className="w-full">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="*">{$("tasks.form.cron.simple.weekdays.every")}</SelectItem>
                            <SelectItem value="1">{$("tasks.form.cron.simple.weekdays.mon")}</SelectItem>
                            <SelectItem value="2">{$("tasks.form.cron.simple.weekdays.tue")}</SelectItem>
                            <SelectItem value="3">{$("tasks.form.cron.simple.weekdays.wed")}</SelectItem>
                            <SelectItem value="4">{$("tasks.form.cron.simple.weekdays.thu")}</SelectItem>
                            <SelectItem value="5">{$("tasks.form.cron.simple.weekdays.fri")}</SelectItem>
                            <SelectItem value="6">{$("tasks.form.cron.simple.weekdays.sat")}</SelectItem>
                            <SelectItem value="0">{$("tasks.form.cron.simple.weekdays.sun")}</SelectItem>
                          </SelectContent>
                        </Select>
                        {fieldState.invalid && (
                          <FieldError id={`${formId}-${field.name}-weekdays-error`} errors={[fieldState.error]}/>
                        )}
                      </Field>
                    </div>
                  </TabsContent>
                  <TabsContent value="advanced" forceMount>
                    <Field data-invalid={fieldState.invalid} className="flex-1">
                      <FieldLabel htmlFor={`${formId}-${field.name}`}>{$("tasks.form.cron.advanced.input.label")}</FieldLabel>
                      <Input
                        id={`${formId}-${field.name}`}
                        aria-invalid={fieldState.invalid}
                        aria-describedby={`${formId}-${field.name}-description${fieldState.invalid ? ` ${formId}-${field.name}-error` : ""}`}
                        placeholder={$("tasks.form.cron.advanced.input.placeholder")}
                        autoComplete="off"
                        className={googleSansCode.className}
                        {...field}
                        ref={cronInputType === "advanced" ? field.ref : undefined}/>
                      {fieldState.invalid && (
                        <FieldError id={`${formId}-${field.name}-error`} errors={[fieldState.error]}/>
                      )}
                      <FieldDescription id={`${formId}-${field.name}-description`}>
                        <Text
                          id="tasks.form.cron.advanced.input.description"
                          args={[
                            <Link
                              href="https://ibm.com/docs/en/db2/latest?topic=task-unix-cron-format"
                              target="_blank"
                              rel="noopener noreferrer"
                              key={0}>
                              UNIX cron format
                            </Link>
                          ]}/>
                      </FieldDescription>
                    </Field>
                  </TabsContent>
                </div>
              </Tabs>
            </div>
          );
        }}/>

      <Controller
        name="commands"
        control={form.control}
        render={({ field, fieldState }) => (
          <Field
            data-invalid={fieldState.invalid}
            aria-labelledby={`${formId}-${field.name}-label`}
            aria-describedby={`${formId}-${field.name}-description${fieldState.invalid ? ` ${formId}-${field.name}-error` : ""}`}
            className="flex-1 min-h-40">
            <FieldTitle id={`${formId}-${field.name}-label`}>{$("tasks.form.commands.label")}</FieldTitle>
            <div className="flex-1 min-h-0 border rounded-md overflow-hidden">
              <MonacoEditor
                defaultLanguage="task-command"
                value={field.value.join("\n")}
                theme={theme === "dark" ? "task-command-theme-dark" : "task-command-theme"}
                options={{
                  minimap: {
                    enabled: false
                  },
                  ...monacoSettingsOptions,
                  ariaLabel: $("tasks.form.commands.label")
                }}
                onChange={(value) => field.onChange(value ? value.split("\n") : [])}/>
            </div>
            {fieldState.invalid && (
              <FieldError id={`${formId}-${field.name}-error`} errors={[fieldState.error]}/>
            )}
            <FieldDescription id={`${formId}-${field.name}-description`} className="flex justify-between gap-2">
              {$("tasks.form.commands.description1")}
              <Link href="https://opanel.cn/wiki/task-command" target="_blank">
                {$("tasks.form.commands.description2")}
              </Link>
            </FieldDescription>
          </Field>
        )}/>

      <div className="w-fit mt-auto flex items-center gap-2">
        <Button
          type="submit"
          className="cursor-pointer"
          disabled={!ready || loading}>
          {loading && <Spinner />}
          {mode === "create" ? $("dialog.create") : $("dialog.save")}
        </Button>
        {mode === "create" && (
          <TaskTemplateDialog
            onSelect={(commands) => form.setValue("commands", commands, {
              shouldDirty: true,
              shouldTouch: true,
              shouldValidate: true
            })}>
            <Button type="button" variant="outline" className="cursor-pointer">
              <LayoutTemplate />
              {$("tasks.templates.title")}
            </Button>
          </TaskTemplateDialog>
        )}
      </div>
    </form>
  );
}
