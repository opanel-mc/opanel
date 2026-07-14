import type { CreateTaskResponse, ScheduledTask } from "@/lib/types";
import { useState } from "react";
import dynamic from "next/dynamic";
import Link from "next/link";
import { useTheme } from "next-themes";
import * as z from "zod";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage
} from "@/components/ui/form";
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
  commands: z.array(z.string().min(1, $("tasks.form.commands.empty1"))).min(1, $("tasks.form.commands.empty2")),
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
    <Form {...form}>
      <form
        className="flex-1 min-w-0 p-4 flex flex-col gap-4 overflow-x-hidden overflow-y-auto o-scrollbar"
        data-taskid={task.id}
        onSubmit={form.handleSubmit(handleSubmit)}>
        <FormField
          name="name"
          control={form.control}
          render={({ field }) => (
            <FormItem>
              <FormLabel>{$("tasks.form.name.label")}</FormLabel>
              <FormControl>
                <Input
                  placeholder={$("tasks.form.name.placeholder")}
                  autoComplete="off"
                  {...field}/>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}/>
        
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
                <FormField
                  name="cron"
                  control={form.control}
                  render={({ field }) => {
                    const cronParts = field.value.split(" ");

                    return (
                      <div className="w-full grid grid-cols-3 max-sm:grid-cols-2 gap-4">
                        <FormItem className="flex-1">
                          <FormLabel>{$("tasks.form.cron.simple.minutes")}</FormLabel>
                          <FormControl>
                            <Input
                              value={cronParts[0]}
                              onChange={(e) => field.onChange(setCronPart(field.value, 0, e.target.value))}
                              autoComplete="off"/>
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                        <FormItem className="flex-1">
                          <FormLabel>{$("tasks.form.cron.simple.hours")}</FormLabel>
                          <FormControl>
                            <Input
                              value={cronParts[1]}
                              onChange={(e) => field.onChange(setCronPart(field.value, 1, e.target.value))}
                              autoComplete="off"/>
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                        <FormItem className="flex-1">
                          <FormLabel>{$("tasks.form.cron.simple.days")}</FormLabel>
                          <FormControl>
                            <Input
                              value={cronParts[2]}
                              onChange={(e) => field.onChange(setCronPart(field.value, 2, e.target.value))}
                              autoComplete="off"/>
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                        <FormItem className="flex-1">
                          <FormLabel>{$("tasks.form.cron.simple.months")}</FormLabel>
                          <FormControl>
                            <Input
                              value={cronParts[3]}
                              onChange={(e) => field.onChange(setCronPart(field.value, 3, e.target.value))}
                              autoComplete="off"/>
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                        <FormItem className="flex-1">
                          <FormLabel>{$("tasks.form.cron.simple.weekdays")}</FormLabel>
                          <FormControl>
                            <Select
                              value={cronWeekToValue(cronParts[4])}
                              onValueChange={(value) => field.onChange(setCronPart(field.value, 4, value))}>
                              <SelectTrigger className="w-full">
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
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      </div>
                    );
                  }}/>
              </TabsContent>
              <TabsContent value="advanced" forceMount>
                <FormField
                  name="cron"
                  control={form.control}
                  render={({ field }) => (
                    <FormItem className="flex-1">
                      <FormLabel>{$("tasks.form.cron.advanced.input.label")}</FormLabel>
                      <FormControl>
                        <Input
                          placeholder={$("tasks.form.cron.advanced.input.placeholder")}
                          autoComplete="off"
                          className={googleSansCode.className}
                          {...field}/>
                      </FormControl>
                      <FormMessage />
                      <FormDescription>
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
                      </FormDescription>
                    </FormItem>
                  )}/>
              </TabsContent>
            </div>
          </Tabs>
        </div>

        <FormField
          name="commands"
          control={form.control}
          render={({ field }) => (
            <FormItem className="flex-1 min-h-40">
              <FormLabel>{$("tasks.form.commands.label")}</FormLabel>
              <FormControl className="border rounded-md overflow-hidden">
                <MonacoEditor
                  defaultLanguage="task-command"
                  value={field.value.join("\n")}
                  theme={theme === "dark" ? "task-command-theme-dark" : "task-command-theme"}
                  options={{
                    minimap: {
                      enabled: false
                    },
                    ...monacoSettingsOptions
                  }}
                  onChange={(value) => field.onChange(value ? value.split("\n") : [])}/>
              </FormControl>
              <FormDescription>
                {$("tasks.form.commands.description")}
              </FormDescription>
            </FormItem>
          )}/>

        <Button
          type="submit"
          className="w-fit mt-auto cursor-pointer"
          disabled={!ready || loading}>
          {loading && <Spinner />}
          {mode === "create" ? $("dialog.create") : $("dialog.save")}
        </Button>
      </form>
    </Form>
  );
}
