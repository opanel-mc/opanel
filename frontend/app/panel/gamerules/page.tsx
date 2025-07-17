"use client";

import type { GamerulesResponse } from "@/lib/types";
import { useEffect, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { PencilRuler } from "lucide-react";
import { SubPage } from "../sub-page";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel
} from "@/components/ui/form";
import {
  generateFormSchema,
  type ServerGamerules
} from "@/lib/gamerules/gamerule";
import { sendGetRequest } from "@/lib/api";
import { objectToMap } from "@/lib/utils";
import { Switch } from "@/components/ui/switch";
import { Input } from "@/components/ui/input";
import { Tooltip, TooltipContent } from "@/components/ui/tooltip";
import { TooltipTrigger } from "@radix-ui/react-tooltip";
import { Button } from "@/components/ui/button";
import gamerulePresets from "@/lib/gamerules/presets";

export default function Gamerules() {
  const [serverGamerules, setServerGamerules] = useState<ServerGamerules>({});
  const gamerulesMap = useMemo(() => objectToMap(serverGamerules), [serverGamerules]);
  const formSchema = useMemo(() => generateFormSchema(serverGamerules), [serverGamerules]);
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: serverGamerules
  });

  const fetchServerGamerules = async () => {
    try {
      const res = await sendGetRequest<GamerulesResponse>("/api/gamerules");
      setServerGamerules(res.gamerules);
    } catch (e) {
      toast.error("无法获取服务器游戏规则信息", { description: `错误：${e}` });
    }
  };

  /** @todo */
  const handleSubmit = () => {

  };

  useEffect(() => {
    fetchServerGamerules();
  }, []);
  
  return (
    <SubPage title="游戏规则" icon={<PencilRuler />} className="pb-16">
      <Form {...form}>
        <form className="space-y-5" onSubmit={() => handleSubmit()}>
          {Array.from(gamerulesMap).map(([key, value]) => {
            const preset = gamerulePresets.find(({ id, type }) => (id === key && typeof value === type));

            if(!preset) {
              toast.error("游戏规则预设错误", { description: "游戏规则预设与实际服务器游戏规则无法匹配："+ key });
              return <></>;
            }

            return (
              <FormField
                control={form.control}
                name={key}
                render={({ field }) => (
                  <FormItem className="p-3 border rounded-md flex flex-row justify-between items-center">
                    <div className="space-y-2">
                      <Tooltip>
                        <TooltipTrigger>
                          <FormLabel className="gap-2">
                            {preset.icon && <preset.icon size={17}/>}
                            {key}
                          </FormLabel>
                        </TooltipTrigger>
                        <TooltipContent>{preset.name}</TooltipContent>
                      </Tooltip>
                      {preset.description && <FormDescription>{preset.description}</FormDescription>}
                    </div>
                    <FormControl>
                      {
                        preset.type === "boolean"
                        ? (
                          <Switch
                            {...field}
                            defaultChecked={value as boolean}
                            className="cursor-pointer"/>
                        )
                        : (
                          <Input
                            {...field}
                            defaultValue={value as number}
                            className="w-28"/>
                        )
                      }
                    </FormControl>
                  </FormItem>
                )}
                key={key}/>
            );
          })}
          <div className="space-x-2 [&>*]:cursor-pointer">
            <Button type="submit">保存</Button>
            <Button variant="outline">重置</Button>
          </div>
        </form>
      </Form>
    </SubPage>
  );
}
