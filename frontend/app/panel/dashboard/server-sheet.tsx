import type { z } from "zod";
import type { ServerPropertiesResponse } from "@/lib/types";
import Link from "next/link";
import { Fragment, useId, useMemo, useState, type PropsWithChildren } from "react";
import { toast } from "sonner";
import { Properties } from "properties-file";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
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
import { Button } from "@/components/ui/button";
import {
  generateFormSchema,
  type ServerProperties
} from "@/lib/server-config";
import { sendGetRequest, sendPostRequest, toastError } from "@/lib/api";
import { base64ToString, isNumeric, objectToMap, stringToBase64 } from "@/lib/utils";
import {
  Field,
  FieldContent,
  FieldDescription,
  FieldError,
  FieldLabel
} from "@/components/ui/field";
import serverPropertiesPresets from "@/lib/server-config/presets";
import { Switch } from "@/components/ui/switch";
import { Input } from "@/components/ui/input";
import { $ } from "@/lib/i18n";
import { Text } from "@/components/i18n-text";
import { useRestartAlert } from "@/hooks/use-restart-alert";

export function ServerSheet({
  children,
  asChild
}: PropsWithChildren & {
  asChild?: boolean
}) {
  const { restartAlert, openRestartAlert } = useRestartAlert();
  const [properties, setProperties] = useState<ServerProperties>({});
  const [hasChanged, setChanged] = useState<boolean>(false);
  const propertiesMap = useMemo(() => objectToMap(properties), [properties]);
  const formSchema = useMemo(() => generateFormSchema(properties), [properties]);
  const formId = useId();
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    values: properties
  });

  const fetchConfigFile = async () => {
    try {
      const res = await sendGetRequest<ServerPropertiesResponse>(`/api/control/properties`);
      const rawProperties = base64ToString(res.properties);
      const propertiesObj: ServerProperties = new Properties(rawProperties).toObject();
      for(const key in propertiesObj) {
        const value = propertiesObj[key] as string;
        if(value === "true" || value === "false") {
          propertiesObj[key] = value === "true";
        } else if(isNumeric(value)) {
          propertiesObj[key] = Number(value);
        } else {
          propertiesObj[key] = value.replaceAll("\n", "\\n");
        }
      }

      setProperties(propertiesObj);
    } catch (e: any) {
      toastError(e, $("dashboard.properties.fetch.error"), [
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    }
  };

  const handleSubmit = async (data: z.infer<typeof formSchema>) => {
    // Transform object to properties string
    let rawProperties = "";
    for(const key in data) {
      const value = data[key];
      if(typeof value === "string") {
        rawProperties += `${key}=${value.replaceAll("\n", "\\n")}\n`;
      } else {
        rawProperties += `${key}=${value}\n`;
      }
    }

    try {
      await sendPostRequest(`/api/control/properties`, stringToBase64(rawProperties));
      toast.success($("dashboard.properties.save.success"));
      setChanged(false);
      setProperties({});
      openRestartAlert();
    } catch (e: any) {
      toastError(e, $("dashboard.properties.save.error"), [
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    }
  };

  return (
    <Sheet onOpenChange={(open) => open && fetchConfigFile()}>
      <SheetTrigger asChild={asChild}>{children}</SheetTrigger>
      <SheetContent className="min-md:min-w-[450px] max-sm:w-full">
        <form className="min-h-0 flex flex-col gap-4" onSubmit={form.handleSubmit(handleSubmit)} onChange={() => setChanged(true)}>
          <SheetHeader>
            <SheetTitle>{$("dashboard.properties.title")}</SheetTitle>
            <SheetDescription>
              <Text
                id="dashboard.properties.description"
                args={[
                  <code key={0}>server.properties</code>
                ]}/>
            </SheetDescription>
          </SheetHeader>
          <div className="flex flex-col overflow-y-auto o-scrollbar">
            {Array.from(propertiesMap).map(([key, value]) => {
              const preset = serverPropertiesPresets.find(({ id }) => id === key);

              if(key === "motd") {
                return <Fragment key={key}/>;
              }

              return (
                <Controller
                  defaultValue={value}
                  control={form.control}
                  name={key}
                  render={({ field, fieldState }) => (
                    <Field orientation="horizontal" data-invalid={fieldState.invalid} className="w-auto mx-1 p-4 gap-4 flex-wrap">
                      <FieldContent>
                        <FieldLabel htmlFor={`${formId}-${field.name}`}>
                          {(preset && preset.icon) && <preset.icon size={17}/>}
                          {key}
                        </FieldLabel>
                        {preset && <FieldDescription>{preset.description}</FieldDescription>}
                        {fieldState.invalid && (
                          <FieldError id={`${formId}-${field.name}-error`} errors={[fieldState.error]}/>
                        )}
                      </FieldContent>
                      <div className="flex items-center gap-2">
                        {typeof field.value === "boolean" ? (
                          <Switch
                            id={`${formId}-${field.name}`}
                            aria-invalid={fieldState.invalid}
                            aria-describedby={fieldState.invalid ? `${formId}-${field.name}-error` : undefined}
                            name={field.name}
                            ref={field.ref}
                            onBlur={field.onBlur}
                            disabled={field.disabled}
                            checked={field.value}
                            onCheckedChange={field.onChange}
                            className="cursor-pointer"/>
                        ) : (
                          <Input
                            id={`${formId}-${field.name}`}
                            aria-invalid={fieldState.invalid}
                            aria-describedby={fieldState.invalid ? `${formId}-${field.name}-error` : undefined}
                            {...field}
                            value={field.value}
                            type={typeof value === "number" ? "number" : "text"}
                            className="w-28"
                            autoComplete="off"/>
                        )}
                      </div>
                    </Field>
                  )}
                  key={key}/>
              );
            })}
          </div>
          <SheetFooter>
            <span className="text-sm text-muted-foreground">{$("dashboard.properties.hint1")}</span>
            <Text
              id="dashboard.properties.hint2"
              args={[
                <Link
                  href="https://zh.minecraft.wiki/w/服务端配置文件格式"
                  target="_blank"
                  rel="noopener noreferrer"
                  key={0}>
                  Minecraft Wiki
                </Link>,
                <Link
                  href="https://docs.papermc.io/paper/reference/server-properties"
                  target="_blank"
                  rel="noopener noreferrer"
                  key={1}>
                  Paper Docs
                </Link>
              ]}
              className="text-sm text-muted-foreground"/>
            <SheetClose asChild>
              <Button
                type="submit"
                className="cursor-pointer"
                disabled={!hasChanged}>
                {$("dialog.save")}
              </Button>
            </SheetClose>
          </SheetFooter>
        </form>
      </SheetContent>
      {restartAlert}
    </Sheet>
  );
}
