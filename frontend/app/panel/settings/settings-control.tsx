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
  ...props
}: {
  id: keyof SettingsStorageType
} & React.ComponentProps<typeof Input>) {
  return (
    <Input
      {...props}
      className="w-36"
      type="number"
      defaultValue={getSettings(id) as number}
      onChange={(e) => changeSettings(id, parseInt((e.target as HTMLInputElement).value))}/>
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
