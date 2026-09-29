import type { PropsWithChildren } from "react";
import type { LucideIcon } from "lucide-react";
import type { OpenAPIInterfaceName } from "./interface-data";
import dynamic from "next/dynamic";
import { useTheme } from "next-themes";
import { googleSansCode } from "@/lib/fonts";
import { TextCopy } from "@/components/text-copy";
import { cn } from "@/lib/utils";
import { monacoSettingsOptions } from "@/lib/settings";
import { $ } from "@/lib/i18n";
import { ConfigItem, ConfigSection } from "@/components/config-item";
import { Switch } from "@/components/ui/switch";

const MonacoEditor = dynamic(() => import("@/components/monaco-editor"), { ssr: false });

export function InterfaceSection({
  interfaceName,
  icon,
  enabled,
  disabled,
  onEnabledChange,
  children
}: PropsWithChildren & {
  interfaceName: OpenAPIInterfaceName
  icon: LucideIcon
  enabled: boolean
  disabled?: boolean
  onEnabledChange: (enabled: boolean) => void
}) {
  return (
    <ConfigSection>
      <ConfigItem
        icon={icon}
        name={$(`open-api.interfaces.${interfaceName}`)}>
        <Switch
          disabled={disabled}
          checked={enabled}
          onCheckedChange={onEnabledChange}/>
      </ConfigItem>
      {children}
    </ConfigSection>
  );
}

export function Interface({
  method,
  route,
  children
}: PropsWithChildren & {
  method: "GET" | "POST" | "PATCH" | "DELETE"
  route: string
}) {
  return (
    <div
      data-slot="interface"
      className="p-3 border-b last:border-b-0 flex flex-col gap-2">
      <div className="flex items-center gap-2">
        <span className={cn(
          "px-1 text-sm",
          method === "GET" && "text-emerald-500",
          method === "POST" && "text-blue-500 dark:text-blue-400",
          method === "PATCH" && "text-yellow-700 dark:text-yellow-600",
          method === "DELETE" && "text-destructive",
          googleSansCode.className
        )}>
          {method}
        </span>
        <TextCopy
          text={route}
          className="flex-1"/>
      </div>
      {children}
    </div>
  );
}

export function InterfaceDescription({ children }: PropsWithChildren) {
  return (
    <span className="text-sm text-muted-foreground">{children}</span>
  );
}

export function InterfaceRequest({
  def
}: {
  def: string
}) {
  const { theme } = useTheme();

  return (
    <div className="flex flex-col gap-2">
      <span className="text-sm font-semibold">{$("open-api.interfaces.request")}</span>
      <MonacoEditor
        defaultLanguage="typescript"
        value={def}
        theme={theme === "dark" ? "opanel-theme-dark-default" : "opanel-theme"}
        options={{
          minimap: { enabled: false },
          lineNumbers: "off",
          automaticLayout: true,
          tabSize: 2,
          readOnly: true,
          contextmenu: false,
          showUnused: false,
          showDeprecated: false,
          scrollbar: {
            vertical: "hidden",
            handleMouseWheel: false
          },
          ...monacoSettingsOptions
        }}
        autoFitHeight
        className="border rounded-md overflow-hidden"/>
    </div>
  );
}

export function InterfaceResponse({
  def
}: {
  def: string
}) {
  const { theme } = useTheme();

  return (
    <div className="flex flex-col gap-2">
      <span className="text-sm font-semibold">{$("open-api.interfaces.response")}</span>
      <MonacoEditor
        defaultLanguage="typescript"
        value={def}
        theme={theme === "dark" ? "opanel-theme-dark-default" : "opanel-theme"}
        options={{
          minimap: { enabled: false },
          lineNumbers: "off",
          automaticLayout: true,
          tabSize: 2,
          readOnly: true,
          contextmenu: false,
          showUnused: false,
          showDeprecated: false,
          scrollbar: {
            vertical: "hidden",
            handleMouseWheel: false
          },
          ...monacoSettingsOptions
        }}
        autoFitHeight
        className="border rounded-md overflow-hidden"/>
    </div>
  );
}

