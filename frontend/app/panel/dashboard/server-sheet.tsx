import type { z } from "zod";
import type { ServerPropertiesResponse } from "@/lib/types";
import Link from "next/link";
import { Fragment, useMemo, useState, type PropsWithChildren } from "react";
import { toast } from "sonner";
import { Properties } from "properties-file";
import { useForm } from "react-hook-form";
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
import { transformText } from "@/lib/formatting-codes/text";
import { base64ToString, isNumeric, objectToMap, stringToBase64 } from "@/lib/utils";
import { Form, FormField, FormMessage } from "@/components/ui/form";
import {
  Item,
  ItemActions,
  ItemContent,
  ItemDescription,
  ItemTitle
} from "@/components/ui/item";
import serverPropertiesPresets from "@/lib/server-config/presets";
import { Switch } from "@/components/ui/switch";
import { Input } from "@/components/ui/input";

export function ServerSheet({
  children,
  asChild
}: PropsWithChildren & {
  asChild?: boolean
}) {
  const [properties, setProperties] = useState<ServerProperties>({});
  const [hasChanged, setChanged] = useState<boolean>(false);
  const propertiesMap = useMemo(() => objectToMap(properties), [properties]);
  const formSchema = useMemo(() => generateFormSchema(properties), [properties]);
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
      toastError(e, "无法获取server.properties", [
        [401, "未登录"],
        [500, "服务器内部错误"]
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
      await sendPostRequest(`/api/control/properties`, transformText(stringToBase64(rawProperties)));
      toast.success("保存成功", { description: "重启服务器以使改动生效" });
      setChanged(false);
    } catch (e: any) {
      toastError(e, "无法保存server.properties", [
        [401, "未登录"],
        [500, "服务器内部错误"]
      ]);
    }
  };
  
  return (
    <Sheet onOpenChange={(open) => open && fetchConfigFile()}>
      <SheetTrigger asChild={asChild}>{children}</SheetTrigger>
      <SheetContent className="min-md:min-w-[450px] max-sm:w-full">
        <Form {...form}>
          <form className="min-h-0 flex flex-col gap-4" onSubmit={form.handleSubmit(handleSubmit)} onChange={() => setChanged(true)}>
            <SheetHeader>
              <SheetTitle>编辑 server.properties</SheetTitle>
              <SheetDescription>
                在此编辑服务器配置文件，你也可以直接编辑服务器目录下的<code>server.properties</code>文件。
              </SheetDescription>
            </SheetHeader>
            <div className="flex flex-col overflow-y-auto">
              {Array.from(propertiesMap).map(([key, value]) => {
                const preset = serverPropertiesPresets.find(({ id }) => id === key);

                if(key === "motd") {
                  return <Fragment key={key}/>;
                }

                return (
                  <FormField
                    defaultValue=""
                    control={form.control}
                    name={key}
                    render={({ field }) => (
                      <Item className="mx-1">
                        <ItemContent>
                          <ItemTitle>
                            {(preset && preset.icon) && <preset.icon size={17}/>}
                            {key}
                          </ItemTitle>
                          {preset && <ItemDescription>{preset.description}</ItemDescription>}
                          <FormMessage />
                        </ItemContent>
                        <ItemActions>
                          {(() => {
                            if(typeof value === "boolean") {
                              return (
                                <Switch
                                  {...field}
                                  defaultChecked={value}
                                  onCheckedChange={field.onChange}
                                  className="cursor-pointer"/>
                              );
                            } else if(typeof value === "number") {
                              return (
                                <Input
                                  {...field}
                                  type="number"
                                  className="w-28"
                                  autoComplete="off"/>
                              );
                            } else {
                              return (
                                <Input
                                  {...field}
                                  className="w-28"
                                  autoComplete="off"/>
                              );
                            }
                          })()}
                        </ItemActions>
                      </Item>
                    )}
                    key={key}/>
                );
              })}
            </div>
            <SheetFooter>
              <span className="text-sm text-muted-foreground">需重启服务器以使改动生效。</span>
              <span className="text-sm text-muted-foreground">
                服务器配置属性描述信息均来自<Link href="https://zh.minecraft.wiki/w/%E6%9C%8D%E5%8A%A1%E7%AB%AF%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6%E6%A0%BC%E5%BC%8F" target="_blank">Minecraft Wiki</Link>与<Link href="https://docs.papermc.io/paper/reference/server-properties">Paper文档</Link>。
              </span>
              <SheetClose asChild>
                <Button
                  type="submit"
                  className="cursor-pointer"
                  disabled={!hasChanged}>
                  保存设置
                </Button>
              </SheetClose>
            </SheetFooter>
          </form>
        </Form>
      </SheetContent>
    </Sheet>
  );
}
