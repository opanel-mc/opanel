"use client";

import type { z } from "zod";
import type { GamerulesResponse } from "@/lib/types";
import Link from "next/link";
import { useEffect, useMemo, useState, Fragment, useContext } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { compare } from "semver";
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
import { cn, getCurrentState, isNumeric, objectToMap } from "@/lib/utils";
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
import _gamerulePresets from "@/lib/gamerules/presets";
import _gamerulePresetsOld from "@/lib/gamerules/presets-old";
import { SubPage } from "../sub-page";
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group";
import { $ } from "@/lib/i18n";
import { Text } from "@/components/i18n-text";
import { VersionContext } from "@/contexts/api-context";
import { useKeydown } from "@/hooks/use-keydown";
import { Skeleton } from "@/components/ui/skeleton";
import { emitter } from "@/lib/emitter";

export default function Gamerules() {
  const versionCtx = useContext(VersionContext);
  const gamerulePresets = useMemo(() => {
    if(versionCtx && compare(versionCtx.version, "1.21.11") < 0) {
      return _gamerulePresetsOld;
    }
    return _gamerulePresets;
  }, [versionCtx]);
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
      toastError(e, $("gamerules.fetch.error"), [
        [401, $("common.error.401")]
      ]);
    } finally {
      emitter.emit("loading-done");
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
      toast.success($("gamerules.save.success"));
      setChanged(false);
    } catch (e: any) {
      toastError(e, $("gamerules.save.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")]
      ]);
    }
  };

  useEffect(() => {
    fetchServerGamerules();
  }, []);

  useKeydown("s", { ctrl: true }, () => form.handleSubmit(handleSubmit)());
  
  return (
    <SubPage
      title={$("gamerules.title")}
      description={$("gamerules.description")}
      category={$("sidebar.management")}
      icon={<PencilRuler />}
      outerClassName="max-h-screen overflow-y-hidden"
      pageClassName="min-xl:px-64!"
      className="flex-1 min-h-0 flex flex-col gap-3">
      <div className="flex justify-between items-center gap-6 max-sm:flex-col-reverse max-sm:items-start">
        <InputGroup className="flex-1">
          <InputGroupAddon>
            <Search />
          </InputGroupAddon>
          <InputGroupInput
            value={searchString}
            placeholder={$("gamerules.search.placeholder")}
            autoFocus
            onChange={(e) => setSearchString(e.target.value)}/>
        </InputGroup>
        <span className="text-sm text-muted-foreground">{$("gamerules.hint1")}</span>
      </div>
      <Form {...form}>
        <form className="min-h-0 flex flex-col gap-4" onSubmit={form.handleSubmit(handleSubmit)} onChange={() => setChanged(true)}>
          <div className="flex-1 overflow-y-auto o-scrollbar space-y-5 pr-2">
            {
              Object.keys(serverGamerules).length > 0
              ? Array.from(gamerulesMap).map(([key, value]) => {
                  const preset = gamerulePresets.find(({ id, type }) => (id === key && typeof value === type));

                  return (
                    <FormField
                      /** @see https://github.com/react-hook-form/react-hook-form/issues/10977#issuecomment-1737917718 */
                      defaultValue=""
                      control={form.control}
                      name={key}
                      render={({ field }) => (
                        <Item
                          variant="outline"
                          className={cn(
                            "p-3 bg-background dark:bg-transparent",
                            (searchString && !key.toLowerCase().includes(searchString.toLowerCase())) && "hidden"
                          )}>
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
                })
              : new Array(10).fill(null).map((_, i) => (
                <Skeleton className="h-11" key={i}/>
              ))
            }
          </div>
          <div className="flex max-lg:flex-col justify-between items-center max-lg:items-start max-lg:gap-4">
            <Text
              id="gamerules.hint3"
              args={[
                <Link
                  href="https://zh.minecraft.wiki/w/游戏规则#游戏规则列表"
                  target="_blank"
                  rel="noopener noreferrer"
                  key={0}>
                  Minecraft Wiki
                </Link>
              ]}
              className="text-sm text-muted-foreground"/>
            <div className="flex max-lg:flex-row-reverse items-center gap-2 [&>button]:cursor-pointer">
              <Text
                id="gamerules.hint2"
                args={[
                  <><kbd>ctrl</kbd>+<kbd>S</kbd></>
                ]}
                className="mr-2 text-sm text-muted-foreground max-sm:hidden"/>
              <Button
                type="reset"
                variant="outline"
                onClick={() => window.location.reload()}>
                {$("gamerules.reset")}
              </Button>
              <Button type="submit" disabled={!hasChanged}>{$("dialog.save")}</Button>
            </div>
          </div>
        </form>
      </Form>
    </SubPage>
  );
}
