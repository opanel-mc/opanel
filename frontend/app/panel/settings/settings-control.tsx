import type { ChangeEvent, PropsWithChildren } from "react";
import React, { useState } from "react";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { changeSettings, getSettings, type SettingsStorageType } from "@/lib/settings";
import { Slider } from "@/components/ui/slider";
import { cn } from "@/lib/utils";
import { $ } from "@/lib/i18n";

export const controlWidth = "w-36";

export function Section({ className, children }: PropsWithChildren & {
  className?: string
}) {
  return (
    <div className={cn("bg-background dark:bg-transparent border rounded-md flex flex-col", className)}>{children}</div>
  );
}

export function SectionGroup({
  title,
  children
}: PropsWithChildren & {
  title: string
}) {
  return (
    <div className="mb-4 flex flex-col gap-3">
      <h3 className="mx-1 font-semibold">{title}</h3>
      {children}
    </div>
  );
}

export function SettingsItem({
  name,
  description,
  control,
  id
}: {
  id: string
  name: string
  description?: string
  control: React.ReactNode
}) {
  return (
    <div id={id} className="flex justify-between items-center flex-wrap gap-2 px-4 py-3 border-b last:border-b-0">
      <div className="flex flex-col gap-1">
        <span className="text-sm">{name}</span>
        <span className="text-xs text-muted-foreground whitespace-pre-line">{description}</span>
      </div>
      {control}
    </div>
  );
}

export function SettingsInput<K extends keyof SettingsStorageType>({
  id,
  ...props
}: {
  id: keyof SettingsStorageType
} & React.ComponentProps<typeof Input>) {
  return (
    <Input
      {...props}
      className={controlWidth}
      defaultValue={getSettings(id) as string}
      onChange={(e) => changeSettings(id, (e.target as HTMLInputElement).value as SettingsStorageType[K])}/>
  );
}

export function SettingsNumberInput({
  id,
  min,
  max,
  ...props
}: {
  id: keyof SettingsStorageType
  min?: number
  max?: number
} & React.ComponentProps<typeof Input>) {
  const handleChange = (e: ChangeEvent) => {
    const value = (e.target as HTMLInputElement).valueAsNumber;
    if(!value || (min && value < min) || (max && value > max)) {
      e.preventDefault();
      return;
    }
    changeSettings(id, value);
  };

  return (
    <Input
      {...props}
      className={controlWidth}
      type="number"
      defaultValue={getSettings(id) as number}
      min={min}
      max={max}
      onChange={(e) => handleChange(e)}/>
  );
}

export function SettingsSwitch({
  id,
  ...props
}: {
  id: keyof SettingsStorageType
} & React.ComponentProps<typeof Switch>) {
  return (
    <Switch
      {...props}
      defaultChecked={getSettings(id) as boolean}
      onCheckedChange={(value) => changeSettings(id, value)}/>
  );
}

export function SettingsSlider({
  id,
  unit = "",
  ...props
}: {
  id: keyof SettingsStorageType
  unit?: string
} & React.ComponentProps<typeof Slider>) {
  const [value, setValue] = useState(getSettings(id) as number);

  const handleChange = ([newValue]: number[]) => {
    changeSettings(id, newValue);
    setValue(newValue);
  };

  return (
    <div className="pb-1.5 flex flex-col gap-2.5">
      <span className="text-xs text-muted-foreground font-semibold self-end">
        {`${value} ${unit}`}
      </span>
      <Slider
        {...props}
        className="w-52"
        defaultValue={[value]}
        onValueChange={handleChange}/>
    </div>
  );
}

export function SettingsFontSizeSlider({
  id,
  ...props
}: {
  id: keyof SettingsStorageType
} & React.ComponentProps<typeof Slider>) {
  return (
    <div className="pb-1.5 flex flex-col gap-1.5">
      <div className="flex items-center text-muted-foreground *:flex-1">
        <span className="text-[10px]">{$("settings.font-size-slider.small")}</span>
        <span className="text-center text-[14px]">{$("settings.font-size-slider.medium")}</span>
        <span className="text-right text-lg">{$("settings.font-size-slider.large")}</span>
      </div>
      <Slider
        {...props}
        min={10}
        max={18}
        className="w-52"
        defaultValue={[getSettings(id) as number]}
        onValueChange={([value]) => changeSettings(id, value)}/>
    </div>
  );
}
