"use client";

import type { z } from "zod";
import type { GamerulesResponse } from "@/lib/types";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { PencilRuler } from "lucide-react";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage
} from "@/components/ui/form";
import {
  generateFormSchema,
  type ServerGamerules
} from "@/lib/gamerules/gamerule";
import { sendGetRequest, sendPostRequest, toastError } from "@/lib/api";
import { objectToMap } from "@/lib/utils";
import { Switch } from "@/components/ui/switch";
import { Input } from "@/components/ui/input";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { Button } from "@/components/ui/button";
import gamerulePresets from "@/lib/gamerules/presets";
import { SubPage } from "../sub-page";

export default function Gamerules() {
  const [serverGamerules, setServerGamerules] = useState<ServerGamerules>({});
  const gamerulesMap = useMemo(() => objectToMap(serverGamerules), [serverGamerules]);
  const formSchema = useMemo(() => generateFormSchema(serverGamerules), [serverGamerules]);
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    values: serverGamerules
  });

  const fetchServerGamerules = async () => {
    try {
      const res = await sendGetRequest<GamerulesResponse>("/api/gamerules");
      setServerGamerules(res.gamerules);
    } catch (e: any) {
      toastError(e, "无法获取服务器游戏规则信息", [
        [401, "未登录"]
      ]);
    }
  };

  const handleSubmit = async (data: z.infer<typeof formSchema>) => {
    // Transform strings to numbers
    for(const key in data) {
      const value = data[key];
      if(typeof value === "string") {
        data[key] = parseInt(value);
      }
    }
    
    try {
      await sendPostRequest("/api/gamerules", { gamerules: data });
      toast.success("保存成功");
    } catch (e: any) {
      toastError(e, "无法保存游戏规则", [
        [400, "请求参数错误"],
        [401, "未登录"]
      ]);
    }
  };

  useEffect(() => {
    fetchServerGamerules();
  }, []);
  
  return (
    <SubPage title="游戏规则" icon={<PencilRuler />} className="flex flex-col gap-3">
      <span className="text-sm text-muted-foreground">编辑游戏规则后需保存以生效。</span>
      <Form {...form}>
        <form className="space-y-5" onSubmit={form.handleSubmit(handleSubmit)}>
          {Array.from(gamerulesMap).map(([key, value]) => {
            const preset = gamerulePresets.find(({ id, type }) => (id === key && typeof value === type));

            if(!preset) {
              toast.error("游戏规则预设错误", { description: "游戏规则预设与实际服务器游戏规则无法匹配："+ key });
              return <></>;
            }

            return (
              <FormField
                /** @see https://github.com/react-hook-form/react-hook-form/issues/10977#issuecomment-1737917718 */
                defaultValue=""
                control={form.control}
                name={key}
                render={({ field }) => (
                  <FormItem className="p-3 border rounded-md flex flex-row max-sm:flex-col justify-between items-center max-sm:items-start max-sm:gap-4">
                    <div className="space-y-2">
                      <Tooltip>
                        <TooltipTrigger>
                          <FormLabel
                            className="gap-2"
                            /** prevent default here, because if not, clicking on labels will trigger submission */
                            onClick={(e) => e.preventDefault()}>
                            {preset.icon && <preset.icon size={17}/>}
                            {key}
                          </FormLabel>
                        </TooltipTrigger>
                        <TooltipContent>{preset.name}</TooltipContent>
                      </Tooltip>
                      {preset.description && <FormDescription>{preset.description}</FormDescription>}
                      <FormMessage />
                    </div>
                    <FormControl className="max-sm:self-end">
                      {
                        preset.type === "boolean"
                        ? (
                          <Switch
                            {...field}
                            defaultChecked={value as boolean}
                            onCheckedChange={field.onChange}
                            className="cursor-pointer"/>
                        )
                        : (
                          <Input
                            {...field}
                            type="number"
                            className="w-28"
                            autoComplete="off"/>
                        )
                      }
                    </FormControl>
                  </FormItem>
                )}
                key={key}/>
            );
          })}
          <div className="flex max-lg:flex-col justify-between items-center max-lg:items-start max-lg:gap-4">
            <div className="flex gap-2 [&>*]:cursor-pointer">
              <Button type="submit">保存</Button>
              <Button
                type="reset"
                variant="outline"
                onClick={() => window.location.reload()}>重置</Button>
            </div>
            <span className="text-sm text-muted-foreground">
              游戏规则名称与描述信息均来自<Link href="https://zh.minecraft.wiki/w/%E6%B8%B8%E6%88%8F%E8%A7%84%E5%88%99#%E6%B8%B8%E6%88%8F%E8%A7%84%E5%88%99%E5%88%97%E8%A1%A8" target="_blank">Minecraft Wiki</Link>
            </span>
          </div>
        </form>
      </Form>
    </SubPage>
  );
}
