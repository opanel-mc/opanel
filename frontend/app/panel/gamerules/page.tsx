"use client";

import type { z } from "zod";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useId, useEffect, useMemo, useState, useContext, useCallback } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { compare } from "semver";
import { Flame, PencilRuler, Search, TentTree, View } from "lucide-react";
import {
  generateFormSchema,
  type ServerGamerules
} from "@/lib/gamerules";
import { Dimension, type GamerulesResponse } from "@/lib/types";
import { sendGetRequest, sendPostRequest, toastError } from "@/lib/api";
import { cn, getCurrentState, getDimensionByName, isNumeric, objectToMap } from "@/lib/utils";
import { Switch } from "@/components/ui/switch";
import { Input } from "@/components/ui/input";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { Button } from "@/components/ui/button";
import {
  Field,
  FieldContent,
  FieldDescription,
  FieldError,
  FieldLabel
} from "@/components/ui/field";
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
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";

export default function Gamerules() {
  const { replace } = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const dimFromUrl = searchParams.get("dim");
  const versionCtx = useContext(VersionContext);
  const gamerulePresets = useMemo(() => {
    if(versionCtx && compare(versionCtx.version, "1.21.11") < 0) {
      return _gamerulePresetsOld;
    }
    return _gamerulePresets;
  }, [versionCtx]);
  const [dimension, setDimension] = useState<Dimension>(
    dimFromUrl
    ? getDimensionByName(dimFromUrl)
    : Dimension.OVERWORLD
  );
  const [serverGamerules, setServerGamerules] = useState<ServerGamerules>({});
  const [searchString, setSearchString] = useState<string>("");
  const [hasChanged, setChanged] = useState<boolean>(false);
  const gamerulesMap = useMemo(() => objectToMap(serverGamerules), [serverGamerules]);
  const formSchema = useMemo(() => generateFormSchema(serverGamerules), [serverGamerules]);
  const formId = useId();
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    values: serverGamerules
  });

  const fetchServerGamerules = useCallback(async () => {
    try {
      const res = await sendGetRequest<GamerulesResponse>(`/api/gamerules/${dimension}`);
      setServerGamerules(res.gamerules);
      setChanged(false);
    } catch (e: any) {
      toastError(e, $("gamerules.fetch.error"), [
        [401, $("common.error.401")]
      ]);
    } finally {
      emitter.emit("loading-done");
    }
  }, [dimension]);

  const handleSubmit = async (data: z.infer<typeof formSchema>) => {
    if(!(await getCurrentState(setChanged))) return;
    const currentDim = await getCurrentState(setDimension);

    // Transform strings to numbers
    for(const key in data) {
      const value = data[key];
      if(typeof value === "string" && isNumeric(value)) {
        data[key] = parseFloat(value);
      }
    }

    try {
      await sendPostRequest(`/api/gamerules/${currentDim}`, { gamerules: data });
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
    if(
      searchParams.get("dim") === dimension
      || !pathname.startsWith("/panel/gamerules")
    ) return;

    const next = new URLSearchParams(searchParams.toString());
    next.set("dim", dimension);
    replace(`${pathname}?${next.toString()}`);
  }, [dimension, pathname, replace, searchParams, form]);

  useEffect(() => {
    fetchServerGamerules();
  }, [fetchServerGamerules]);

  useKeydown("s", { ctrl: true }, () => form.handleSubmit(handleSubmit)());

  return (
    <SubPage
      title={$("gamerules.title")}
      description={$("gamerules.description")}
      category={$("sidebar.management")}
      icon={<PencilRuler />}
      outerClassName="max-h-[100dvh] overflow-y-hidden"
      pageClassName="min-h-0 min-xl:px-64!"
      className="flex-1 min-h-0 flex flex-col gap-3">
      <div className="flex justify-between items-center gap-3 max-sm:flex-col-reverse max-sm:items-end">
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
        <Select
          value={dimension}
          onValueChange={(value) => setDimension(value as Dimension)}>
          <SelectTrigger className="w-36">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={Dimension.OVERWORLD}>
              <TentTree />
              {$("gamerules.dimension.overworld")}
            </SelectItem>
            <SelectItem value={Dimension.NETHER}>
              <Flame />
              {$("gamerules.dimension.nether")}
            </SelectItem>
            <SelectItem value={Dimension.THE_END}>
              <View />
              {$("gamerules.dimension.the_end")}
            </SelectItem>
          </SelectContent>
        </Select>
      </div>
      <form
        className="min-h-0 flex flex-col gap-4"
        onSubmit={form.handleSubmit(handleSubmit)}>
        <div className="flex-1 overflow-y-auto o-scrollbar space-y-5 pr-2">
          {
            Object.keys(serverGamerules).length > 0
            ? Array.from(gamerulesMap).map(([key, value]) => {
                const preset = gamerulePresets.find(({ id, type }) => (id === key && typeof value === type));

                return (
                  <Controller
                    /** @see https://github.com/react-hook-form/react-hook-form/issues/10977#issuecomment-1737917718 */
                    defaultValue={value}
                    control={form.control}
                    name={key}
                    render={({ field, fieldState }) => (
                      <Field
                        orientation="horizontal"
                        data-invalid={fieldState.invalid}
                        className={cn(
                          "p-3 gap-4 flex-wrap rounded-md border bg-background dark:bg-transparent",
                          (searchString && !key.toLowerCase().includes(searchString.toLowerCase())) && "hidden"
                        )}>
                        <FieldContent className="min-w-0">
                          <FieldLabel htmlFor={`${formId}-${field.name}`} className="max-w-full">
                            {(preset && preset.icon) && <preset.icon size={17}/>}
                            <Tooltip>
                              <TooltipTrigger asChild>
                                <span tabIndex={0} className="text-ellipsis overflow-hidden">{key}</span>
                              </TooltipTrigger>
                              <TooltipContent>{preset ? preset.name : key}</TooltipContent>
                            </Tooltip>
                          </FieldLabel>
                          {(preset && preset.description) && <FieldDescription>{preset.description}</FieldDescription>}
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
                              onCheckedChange={(checked) => {
                                field.onChange(checked);
                                setChanged(true);
                              }}
                              className="cursor-pointer"/>
                          ) : (
                            <Input
                              id={`${formId}-${field.name}`}
                              aria-invalid={fieldState.invalid}
                              aria-describedby={fieldState.invalid ? `${formId}-${field.name}-error` : undefined}
                              {...field}
                              value={field.value}
                              type={typeof value === "number" ? "number" : "text"}
                              onChange={(e) => {
                                field.onChange(e);
                                setChanged(true);
                              }}
                              className="w-28"
                              autoComplete="off"/>
                          )}
                        </div>
                      </Field>
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
            id="gamerules.hint2"
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
              id="gamerules.hint1"
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
    </SubPage>
  );
}
