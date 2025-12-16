import type { WhitelistResponse } from "@/lib/types";
import dynamic from "next/dynamic";
import { useState, type PropsWithChildren } from "react";
import { useTheme } from "next-themes";
import { toast } from "sonner";
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
  SheetTrigger
} from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { sendGetRequest, sendPostRequest, toastError } from "@/lib/api";
import { setWhitelistEnabled } from "./player-utils";
import { monacoSettingsOptions } from "@/lib/settings";
import { $ } from "@/lib/i18n";
import { Text } from "@/components/i18n-text";

const MonacoEditor = dynamic(() => import("@/components/monaco-editor"), { ssr: false });

export function WhitelistSheet({
  onDisableWhitelist,
  children,
  asChild
}: PropsWithChildren & {
  onDisableWhitelist?: () => void
  asChild?: boolean
}) {
  const [value, setValue] = useState<string>("");
  const { theme } = useTheme();

  const fetchServerWhitelist = async () => {
    try {
      const res = await sendGetRequest<WhitelistResponse>("/api/whitelist");
      setValue(JSON.stringify(res.whitelist, undefined, 2));
    } catch (e: any) {
      toastError(e, "无法获取白名单", [
        [401, "未登录"],
        [500, "服务器内部错误"]
      ]);
    }
  };

  const saveServerWhitelist = async () => {
    try {
      await sendPostRequest("/api/whitelist/write", JSON.parse(value));
      toast.success("保存成功");
    } catch (e: any) {
      toastError(e, "无法保存白名单", [
        [401, "未登录"],
        [500, "服务器内部错误"]
      ]);
    }
  };

  return (
    <Sheet onOpenChange={(open) => open && fetchServerWhitelist()}>
      <SheetTrigger asChild={asChild}>{children}</SheetTrigger>
      <SheetContent>
        <SheetHeader>
          <SheetTitle>{$("players.edit-whitelist.title")}</SheetTitle>
          <SheetDescription>
            <Text
              id="players.edit-whitelist.description"
              args={[
                <code key={0}>whitelist.json</code>
              ]}/>
          </SheetDescription>
        </SheetHeader>
        <div className="flex flex-col h-full">
          {value && <MonacoEditor
            defaultLanguage="json"
            value={value}
            theme={theme === "dark" ? "vs-dark" : "vs"}
            options={{
              minimap: { enabled: false },
              automaticLayout: true,
              tabSize: 2,
              ...monacoSettingsOptions
            }}
            onChange={(newValue) => setValue(newValue ?? "")}/>}
        </div>
        <SheetFooter>
          <Button
            variant="destructive"
            className="cursor-pointer"
            onClick={async () => {
              await setWhitelistEnabled(false);
              onDisableWhitelist && onDisableWhitelist();
            }}>
            {$("players.disable-whitelist")}
          </Button>
          <SheetClose asChild>
            <Button
              className="cursor-pointer"
              onClick={() => saveServerWhitelist()}>{$("dialog.save")}</Button>
          </SheetClose>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}
