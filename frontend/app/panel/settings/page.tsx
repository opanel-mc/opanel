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
import { SettingsNumberInput, SettingsSwitch } from "./settings-control";
import { Alert } from "@/components/alert";
import { Button } from "@/components/ui/button";

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
      <div className="flex flex-col">
        <span className="text-sm">{name}</span>
        <span className="text-sm text-muted-foreground">{description}</span>
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
        <Section title="后台终端">
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
            id="terminal.log-level"
            name="日志等级"
            description="终端所显示的最低日志等级"
            control={
              <Select
                defaultValue={getSettings("terminal.log-level")}
                onValueChange={(value) => changeSettings("terminal.log-level", value as ConsoleLogLevel)}>
                <SelectTrigger className="w-36 font-[Consolas]">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent className="font-[Consolas]">
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
        <Section title="安全">
          <SettingsItem
            id="security.access-key"
            name="访问密钥"
            control={
              <Alert
                title="修改访问密钥"
                description="访问密钥需在OPanel的配置文件中手动修改，配置文件中的accessKey项即为访问密钥。"
                cancellable={false}
                asChild>
                <Button className="cursor-pointer" size="sm">修改</Button>
              </Alert>
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
