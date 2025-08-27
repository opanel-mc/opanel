import type { ChangeEvent } from "react";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { changeSettings, getSettings, type SettingsStorageType } from "@/lib/settings";

export function SettingsInput<K extends keyof SettingsStorageType>({
  id,
  ...props
}: {
  id: keyof SettingsStorageType
} & React.ComponentProps<typeof Input>) {
  return (
    <Input
      {...props}
      className="w-36"
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
      className="w-36"
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
