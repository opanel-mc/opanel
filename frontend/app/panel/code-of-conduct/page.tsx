"use client";

import type { CodeOfConductResponse } from "@/lib/types";
import dynamic from "next/dynamic";
import { useRouter } from "next/navigation";
import { useCallback, useContext, useEffect, useState } from "react";
import { useTheme } from "next-themes";
import { FilePlus2, HeartHandshake, Plus } from "lucide-react";
import { compare } from "semver";
import locale from "locale-codes";
import { VersionContext } from "@/contexts/api-context";
import { SubPage } from "../sub-page";
import { sendGetRequest, sendPostRequest, toastError } from "@/lib/api";
import { base64ToString, getCurrentState, stringToBase64, validateLocaleCode } from "@/lib/utils";
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty";
import { Button } from "@/components/ui/button";
import { emitter } from "@/lib/emitter";
import { changeSettings, getSettings, monacoSettingsOptions } from "@/lib/settings";
import { CodeOfConductItem } from "./coc-item";
import { CreateCodeOfConductDialog } from "./create-coc-dialog";
import { Spinner } from "@/components/ui/spinner";

const MonacoEditor = dynamic(() => import("@/components/monaco-editor"), { ssr: false });

export default function CodeOfConduct() {
  const { push } = useRouter();
  const { theme } = useTheme();
  const versionCtx = useContext(VersionContext);
  const [codeOfConducts, setCodeOfConducts] = useState<Map<string, string> | null>(null);
  const [currentEditing, setCurrentEditing] = useState<string>();
  const [editorValue, setEditorValue] = useState<string>("");
  const [, setOriginalValue] = useState<string>(""); // to determine if the value is changed
  const [isSaving, setIsSaving] = useState<boolean>(false);

  const fetchCodeOfConducts = useCallback(async () => {
    if(versionCtx && compare(versionCtx.version, "1.21.9") < 0) {
      push("/panel/dashboard");
      return;
    }

    try {
      const res = await sendGetRequest<CodeOfConductResponse>("/api/control/code-of-conduct");
      const parsedMap: Map<string, string> = new Map();
      for(const [lang, value] of Object.entries(res.codeOfConducts)) {
        if(validateLocaleCode(lang)) {
          parsedMap.set(lang, base64ToString(value));
        }
      }
      setCodeOfConducts(parsedMap);

      if(parsedMap.size === 0) {
        setCurrentEditing(undefined);
        changeSettings("state.code-of-conduct.current-editing", undefined);
        return;
      }
      
      const storedCurrentEditing = getSettings("state.code-of-conduct.current-editing");
      if(!storedCurrentEditing || !parsedMap.has(storedCurrentEditing)) {
        const firstOne = parsedMap.keys().next().value;
        setCurrentEditing(firstOne);
        changeSettings("state.code-of-conduct.current-editing", firstOne);
        return;
      }

      setCurrentEditing(storedCurrentEditing);
    } catch (e: any) {
      toastError(e, "无法获取行为准则信息", [
        [400, "请求参数错误"],
        [401, "未登录"],
        [500, "服务器内部错误"],
        [503, "该服务端版本不支持行为准则"]
      ]);
    }
  }, [push, versionCtx]);

  const handleCreateCodeOfConduct = async (lang: string) => {
    try {
      await sendPostRequest(`/api/control/code-of-conduct?lang=${lang}`);
      emitter.emit("refresh-data");
    } catch (e: any) {
      toastError(e, "无法创建行为准则", [
        [400, "请求参数错误"],
        [401, "未登录"],
        [500, "服务器内部错误"],
        [503, "该服务端版本不支持行为准则"]
      ]);
    }
  };

  const handleSave = async () => {
    const original = await getCurrentState(setOriginalValue);
    const current = await getCurrentState(setEditorValue);
    if(original === current) return;

    setIsSaving(true);
    const lang = await getCurrentState(setCurrentEditing);
    try {
      await sendPostRequest(`/api/control/code-of-conduct?lang=${lang}`, stringToBase64(current));
      emitter.emit("refresh-data");
    } catch (e: any) {
      toastError(e, "无法修改行为准则", [
        [400, "请求参数错误"],
        [401, "未登录"],
        [500, "服务器内部错误"],
        [503, "该服务端版本不支持行为准则"]
      ]);
    } finally {
      setTimeout(() => setIsSaving(false), 300);
    }
  };

  useEffect(() => {
    fetchCodeOfConducts();

    emitter.on("refresh-data", () => fetchCodeOfConducts());
  }, [fetchCodeOfConducts]);

  // Update the editor value when the current editing file is changed
  useEffect(() => {
    if(!currentEditing || !codeOfConducts || !codeOfConducts.has(currentEditing)) return;

    setEditorValue(codeOfConducts.get(currentEditing) ?? "");
    setOriginalValue(codeOfConducts.get(currentEditing) ?? "");
  }, [currentEditing, codeOfConducts]);

  // Auto-save the code-of-conduct content
  useEffect(() => {
    const timer = setInterval(() => {
      handleSave();
    }, getSettings("code-of-conduct.auto-saving-interval"));

    return () => clearInterval(timer);
  }, []);

  /**
   * To avoid the <Empty> content appearing for a very shord period time
   * when the fetching request is awaiting response
   */
  if(!codeOfConducts) return <></>;

  return (
    <SubPage
      title="行为准则"
      icon={<HeartHandshake />}
      outerClassName="max-h-screen overflow-y-hidden max-lg:max-h-none max-lg:overflow-y-auto"
      className="flex-1 min-h-0">
      <div className="h-full border rounded-md flex max-lg:flex-col">
        {
          codeOfConducts.size !== 0 && currentEditing
          ? (
            <>
              <div className="flex-1/4 h-fit max-h-full max-lg:max-h-none flex flex-col gap-3 p-2">
                <div className="flex-1 flex flex-col overflow-y-auto">
                  {Array.from(codeOfConducts).map(([lang]) => (
                    <CodeOfConductItem
                      lang={lang}
                      isActive={lang === currentEditing}
                      onClick={() => {
                        setCurrentEditing(lang);
                        changeSettings("state.code-of-conduct.current-editing", lang);
                      }}
                      key={lang}/>
                  ))}
                </div>
                <CreateCodeOfConductDialog
                  excludedLocales={Array.from(codeOfConducts.keys())}
                  asChild
                  onAction={(lang) => handleCreateCodeOfConduct(lang)}>
                  <Button
                    size="sm"
                    className="w-full mb-1 cursor-pointer">
                    <Plus />
                  </Button>
                </CreateCodeOfConductDialog>
              </div>
              <div className="flex-3/4 border-l max-lg:border-l-0 max-lg:border-t max-lg:min-h-96 flex flex-col justify-end max-lg:flex-col-reverse">
                <MonacoEditor
                  defaultLanguage="txt"
                  value={editorValue}
                  theme={theme === "dark" ? "server-log-theme-dark" : "server-log-theme"}
                  options={{
                    minimap: {
                      enabled: false
                    },
                    ...monacoSettingsOptions
                  }}
                  onChange={(value) => setEditorValue(value ?? "")}/>
                <div className="h-6 px-2 border-t max-lg:border-t-0 max-lg:border-b flex justify-between items-center [&>*]:text-xs [&>*]:text-muted-foreground cursor-default">
                  <div className="flex items-center gap-3">
                    <span>{locale.getByTag(currentEditing.toLowerCase().replaceAll("_", "-")).name}</span>
                    <span>{editorValue.length +" 个字"}</span>
                  </div>
                  <div className="flex items-center gap-1">
                    {
                      isSaving
                      ? (
                        <>
                          <Spinner />
                          <span>正在保存...</span>
                        </>
                      )
                      : (
                        <span>已保存</span>
                      )
                    }
                  </div>
                </div>
              </div>
            </>
          )
          : (
            <Empty>
              <EmptyHeader>
                <EmptyMedia variant="icon">
                  <HeartHandshake />
                </EmptyMedia>
                <EmptyTitle>暂无行为准则文档</EmptyTitle>
                <EmptyDescription>
                  此服务器上还没有编写任何行为准则，你可以在此创建第一个行为准则文档。
                </EmptyDescription>
              </EmptyHeader>
              <EmptyContent>
                <Button
                  className="cursor-pointer"
                  onClick={() => handleCreateCodeOfConduct("zh_cn")/* zh_cn by default */}>
                  <FilePlus2 />
                  新建行为准则
                </Button>
              </EmptyContent>
            </Empty>
          )
        }
      </div>
    </SubPage>
  );
}
