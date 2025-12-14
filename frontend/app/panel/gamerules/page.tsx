"use client";

import type { z } from "zod";
import type { GamerulesResponse } from "@/lib/types";
import Link from "next/link";
import { useEffect, useMemo, useState, Fragment } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { PencilRuler, Search } from "lucide-react";
import {
  Form,
  FormField,
  FormMessage
} from "@/components/ui/form";
import {
  generateFormSchema,
  type ServerGamerules
} from "@/lib/gamerules";
import { sendGetRequest, sendPostRequest, toastError } from "@/lib/api";
import { getCurrentState, isNumeric, objectToMap } from "@/lib/utils";
import { Switch } from "@/components/ui/switch";
import { Input } from "@/components/ui/input";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { Button } from "@/components/ui/button";
import {
  Item,
  ItemActions,
  ItemContent,
  ItemDescription,
  ItemTitle
} from "@/components/ui/item";
import gamerulePresets from "@/lib/gamerules/presets";
import { SubPage } from "../sub-page";
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group";

export default function Gamerules() {
  const [serverGamerules, setServerGamerules] = useState<ServerGamerules>({});
  const [searchString, setSearchString] = useState<string>("");
  const [hasChanged, setChanged] = useState<boolean>(false);
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
    if(!(await getCurrentState(setChanged))) return;

    // Transform strings to numbers
    for(const key in data) {
      const value = data[key];
      if(typeof value === "string" && isNumeric(value)) {
        data[key] = parseFloat(value);
      }
    }
    
    try {
      await sendPostRequest("/api/gamerules", { gamerules: data });
      toast.success("保存成功");
      setChanged(false);
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
    <SubPage
      title="游戏规则"
      icon={<PencilRuler />}
      outerClassName="max-h-screen overflow-y-hidden"
      className="flex-1 min-h-0 flex flex-col gap-3"
      onKeyDown={(e) => (e.ctrlKey && e.key === "s") && form.handleSubmit(handleSubmit)()}>
      <div className="flex justify-between items-center gap-2 max-sm:flex-col max-sm:items-start">
        <span className="text-sm text-muted-foreground">编辑游戏规则后需保存以生效。</span>
        <InputGroup className="w-fit">
          <InputGroupAddon>
            <Search />
          </InputGroupAddon>
          <InputGroupInput
            value={searchString}
            placeholder="搜索游戏规则..."
            autoFocus
            onChange={(e) => setSearchString(e.target.value)}/>
        </InputGroup>
      </div>
      <Form {...form}>
        <form className="min-h-0 flex flex-col gap-4" onSubmit={form.handleSubmit(handleSubmit)} onChange={() => setChanged(true)}>
          <div className="flex-1 overflow-y-auto space-y-5 pr-2">
            {Array.from(gamerulesMap).map(([key, value]) => {
              const preset = gamerulePresets.find(({ id, type }) => (id === key && typeof value === type));

              // if(!preset) {
              //   toast.error("游戏规则预设错误", { description: "游戏规则预设与实际服务器游戏规则无法匹配："+ key });
              //   return <></>;
              // }
              if(searchString && !key.toLowerCase().includes(searchString.toLowerCase())) {
                return <Fragment key={key}/>;
              }

              return (
                <FormField
                  /** @see https://github.com/react-hook-form/react-hook-form/issues/10977#issuecomment-1737917718 */
                  defaultValue=""
                  control={form.control}
                  name={key}
                  render={({ field }) => (
                    <Item variant="outline" className="p-3">
                      <ItemContent className="max-w-full">
                        <ItemTitle
                          className="gap-2 max-w-full"
                          /** prevent default here, because if not, clicking on labels will trigger submission */
                          onClick={(e) => e.preventDefault()}>
                          {(preset && preset.icon) && <preset.icon size={17}/>}
                          <Tooltip>
                            <TooltipTrigger className="text-ellipsis overflow-hidden">{key}</TooltipTrigger>
                            <TooltipContent>{preset ? preset.name : key}</TooltipContent>
                          </Tooltip>
                        </ItemTitle>
                        {(preset && preset.description) && <ItemDescription>{preset.description}</ItemDescription>}
                        <FormMessage />
                      </ItemContent>
                      <ItemActions>
                        {(() => {
                          if(typeof value === "boolean") {
                            return (
                              <Switch
                                {...field}
                                defaultChecked={value as boolean}
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
          <div className="flex max-lg:flex-col justify-between items-center max-lg:items-start max-lg:gap-4">
            <div className="flex items-center gap-2 [&>*]:cursor-pointer">
              <Button type="submit" disabled={!hasChanged}>保存</Button>
              <Button
                type="reset"
                variant="outline"
                onClick={() => window.location.reload()}>重置</Button>
              <span className="text-sm text-muted-foreground max-sm:hidden"><kbd>ctrl</kbd>+<kbd>S</kbd> 以保存更改</span>
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
