"use client";

import type { ConsoleLogLevel } from "@/lib/terminal/log-levels";
import { Settings as SettingsIcon } from "lucide-react";
import { changeSettings, getSettings, resetSettings, type SettingsStorageType } from "@/lib/settings";
import { SubPage } from "../sub-page";
import { Section } from "./section";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "@/components/ui/select";
import { controlWidth, SettingsNumberInput, SettingsSwitch } from "./settings-control";
import { Button } from "@/components/ui/button";
import { SecurityDialog } from "./security-dialog";
import { cn } from "@/lib/utils";
import { googleSansCode } from "@/lib/fonts";
import { AvatarProvider, CapeProvider, SkinProvider } from "@/lib/types";
import { type LanguageCode, languages } from "@/lang";

function SettingsItem<K extends keyof SettingsStorageType>({
  name,
  description,
  control,
  id
}: {
  id: K
  name: string
  description?: string
  control: React.ReactNode
}) {
  return (
    <div id={id} className="flex justify-between items-center px-4 py-3 border-b last:border-b-0">
      <div className="flex flex-col gap-1">
        <span className="text-sm">{name}</span>
        <span className="text-xs text-muted-foreground">{description}</span>
      </div>
      {control}
    </div>
  );
}

export default function Settings() {
  return (
    <SubPage title="设置" icon={<SettingsIcon />} className="px-64 max-xl:px-0">
      <span className="text-sm text-muted-foreground">所有设置选项将保存于本地，不会同步至服务端。</span>
      <div className="flex flex-col gap-7 mt-4">
        <Section title="仪表盘">
          <SettingsItem
            id="dashboard.monitor-interval"
            name="资源监控刷新间隔"
            description="刷新监控数据时间间隔（单位: ms）"
            control={<SettingsNumberInput id="dashboard.monitor-interval" min={1}/>}/>
        </Section>
        <Section title="玩家">
          <SettingsItem
            id="players.avatar-provider"
            name="头像贴图提供方"
            description="头像贴图API提供方的URL地址"
            control={
              <Select
                defaultValue={getSettings("players.avatar-provider")}
                onValueChange={(value) => {
                  value !== "$custom" && changeSettings("players.avatar-provider", value);
                }}>
                <SelectTrigger className={controlWidth}>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={AvatarProvider.CRAFATAR} title={AvatarProvider.CRAFATAR}>Crafatar</SelectItem>
                  <SelectItem value={AvatarProvider.MINEATAR} title={AvatarProvider.MINEATAR}>Mineatar</SelectItem>
                  <SelectItem value={AvatarProvider.MCHEADS} title={AvatarProvider.MCHEADS}>MCHeads</SelectItem>
                </SelectContent>
              </Select>
            }/>
          <SettingsItem
            id="players.skin-provider"
            name="皮肤贴图提供方"
            description="皮肤贴图API提供方的URL地址"
            control={
              <Select
                defaultValue={getSettings("players.skin-provider")}
                onValueChange={(value) => {
                  value !== "$custom" && changeSettings("players.skin-provider", value);
                }}>
                <SelectTrigger className={controlWidth}>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={SkinProvider.CRAFATAR} title={SkinProvider.CRAFATAR}>Crafatar</SelectItem>
                  <SelectItem value={SkinProvider.MINEATAR} title={SkinProvider.MINEATAR}>Mineatar</SelectItem>
                  <SelectItem value={SkinProvider.MCHEADS} title={SkinProvider.MCHEADS}>MCHeads</SelectItem>
                </SelectContent>
              </Select>
            }/>
          <SettingsItem
            id="players.cape-provider"
            name="披风贴图提供方"
            description="披风贴图API提供方的URL地址"
            control={
              <Select
                defaultValue={getSettings("players.cape-provider")}
                onValueChange={(value) => {
                  value !== "$custom" && changeSettings("players.cape-provider", value);
                }}>
                <SelectTrigger className={controlWidth}>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={CapeProvider.CRAFATAR} title={CapeProvider.CRAFATAR}>Crafatar</SelectItem>
                </SelectContent>
              </Select>
            }/>
        </Section>
        <Section title="后台终端">
          <SettingsItem
            id="terminal.autocomplete"
            name="命令补全"
            control={<SettingsSwitch id="terminal.autocomplete"/>}/>
          <SettingsItem
            id="terminal.word-wrap"
            name="自动换行"
            control={<SettingsSwitch id="terminal.word-wrap"/>}/>
          <SettingsItem
            id="terminal.font-size"
            name="字体大小"
            description="终端内字体显示大小（单位: px）"
            control={<SettingsNumberInput id="terminal.font-size" min={1} max={30}/>}/>
          <SettingsItem
            id="terminal.max-log-lines"
            name="日志最大行数"
            description="终端内显示日志的最大行数（最大为20000）"
            control={<SettingsNumberInput id="terminal.max-log-lines" min={100} max={20000}/>}/>
          <SettingsItem
            id="terminal.log-level"
            name="日志等级"
            description="终端所显示的最低日志等级"
            control={
              <Select
                defaultValue={getSettings("terminal.log-level")}
                onValueChange={(value) => changeSettings("terminal.log-level", value as ConsoleLogLevel)}>
                <SelectTrigger className={cn(controlWidth, googleSansCode.className)}>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent className={googleSansCode.className}>
                  <SelectItem value="INFO">INFO</SelectItem>
                  <SelectItem value="WARN">WARN</SelectItem>
                  <SelectItem value="ERROR">ERROR</SelectItem>
                </SelectContent>
              </Select>
            }/>
          <SettingsItem
            id="terminal.log-time"
            name="显示日志时间"
            control={<SettingsSwitch id="terminal.log-time"/>}/>
          <SettingsItem
            id="terminal.thread-name"
            name="显示线程名称"
            control={<SettingsSwitch id="terminal.thread-name"/>}/>
          <SettingsItem
            id="terminal.source-name"
            name="显示日志源"
            control={<SettingsSwitch id="terminal.source-name"/>}/>
          <SettingsItem
            id="terminal.convert-ansi-code"
            name="转换ANSI代码"
            description="将终端内的ANSI代码转换为更易读的HTML"
            control={<SettingsSwitch id="terminal.convert-ansi-code"/>}/>
        </Section>
        <Section title="行为准则编辑器">
          <SettingsItem
            id="code-of-conduct.auto-saving-interval"
            name="自动保存间隔"
            description="编辑器自动保存时间间隔（单位: ms）"
            control={<SettingsNumberInput id="code-of-conduct.auto-saving-interval" min={1000}/>}/>
        </Section>
        <Section title="Monaco 编辑器">
          <SettingsItem
            id="monaco.word-wrap"
            name="自动换行"
            control={<SettingsSwitch id="monaco.word-wrap"/>}/>
          <SettingsItem
            id="monaco.font-size"
            name="字体大小"
            description="编辑器内字体显示大小（单位: px）"
            control={<SettingsNumberInput id="monaco.font-size" min={1} max={30}/>}/>
        </Section>
        <Section title="系统">
          <SettingsItem
            id="system.language"
            name="语言"
            control={
              <Select
                defaultValue={getSettings("system.language")}
                onValueChange={(value) => changeSettings("system.language", value as LanguageCode)}>
                <SelectTrigger className={controlWidth}>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.keys(languages).map((lang, i) => (
                    <SelectItem value={lang} key={i}>{languages[lang]["$lang"]}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            }/>
          <SettingsItem
            id="system.access-key"
            name="访问密钥"
            control={
              <SecurityDialog asChild>
                <Button className="cursor-pointer" size="sm">修改</Button>
              </SecurityDialog>
            }/>
        </Section>
        <div>
          <Button
            variant="outline"
            className="cursor-pointer"
            onClick={() => {
              resetSettings();
              window.location.reload();
            }}>
            恢复默认设置
          </Button>
        </div>
      </div>
    </SubPage>
  );
}
