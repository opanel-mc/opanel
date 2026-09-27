import type { ServerPropertiesResponse } from "@/lib/types";
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
import { Spinner } from "@/components/ui/spinner";
import { sendGetRequest, sendPostRequest, toastError } from "@/lib/api";
import { base64ToString, stringToBase64 } from "@/lib/utils";
import { monacoSettingsOptions } from "@/lib/settings";
import { $ } from "@/lib/i18n";
import { Text } from "@/components/i18n-text";
import { useRestartAlert } from "@/hooks/use-restart-alert";

const MonacoEditor = dynamic(() => import("@/components/monaco-editor"), { ssr: false });

export function PumpkinServerSheet({
  children,
  asChild
}: PropsWithChildren & {
  asChild?: boolean
}) {
  const { theme } = useTheme();
  const { restartAlert, openRestartAlert } = useRestartAlert();
  const [open, setOpen] = useState(false);
  const [configuration, setConfiguration] = useState("");
  const [hasChanged, setChanged] = useState(false);
  const [isLoading, setLoading] = useState(false);
  const [isSaving, setSaving] = useState(false);

  const fetchConfiguration = async () => {
    setLoading(true);
    setChanged(false);
    try {
      const response = await sendGetRequest<ServerPropertiesResponse>("/api/control/properties");
      setConfiguration(base64ToString(response.properties));
    } catch (e: any) {
      setConfiguration("");
      toastError(e, $("dashboard.pumpkin-server.fetch.error"), [
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    } finally {
      setLoading(false);
    }
  };

  const saveConfiguration = async () => {
    setSaving(true);
    try {
      await sendPostRequest("/api/control/properties", stringToBase64(configuration));
      toast.success($("dashboard.pumpkin-server.save.success"));
      setChanged(false);
      setOpen(false);
      openRestartAlert();
    } catch (e: any) {
      toastError(e, $("dashboard.pumpkin-server.save.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <Sheet
        open={open}
        onOpenChange={(nextOpen) => {
          setOpen(nextOpen);
          if(nextOpen) void fetchConfiguration();
        }}>
        <SheetTrigger asChild={asChild}>{children}</SheetTrigger>
        <SheetContent>
          <SheetHeader>
            <SheetTitle>{$("dashboard.pumpkin-server.title")}</SheetTitle>
            <SheetDescription>
              <Text
                id="dashboard.pumpkin-server.description"
                args={[
                  <code key={0}>pumpkin.toml</code>
                ]}/>
            </SheetDescription>
          </SheetHeader>
          <div className="flex flex-col h-full">
            {
              isLoading
                ? (
                  <div className="flex size-full items-center justify-center">
                    <Spinner />
                  </div>
                )
                : (
                  <MonacoEditor
                    defaultLanguage="ini"
                    value={configuration}
                    theme={theme === "dark" ? "opanel-theme-dark" : "opanel-theme"}
                    options={{
                      minimap: { enabled: false },
                      automaticLayout: true,
                      tabSize: 2,
                      ...monacoSettingsOptions
                    }}
                    onChange={(value) => {
                      setConfiguration(value ?? "");
                      setChanged(true);
                    }}/>
                )
            }
          </div>
          <SheetFooter>
            <span className="text-sm text-muted-foreground">
              {$("dashboard.pumpkin-server.hint")}
            </span>
            <SheetClose asChild>
              <Button variant="outline">{$("dialog.cancel")}</Button>
            </SheetClose>
            <Button
              className="cursor-pointer"
              disabled={!hasChanged || isLoading || isSaving}
              onClick={() => saveConfiguration()}>
              {isSaving && <Spinner />}
              {$("dialog.save")}
            </Button>
          </SheetFooter>
        </SheetContent>
      </Sheet>
      {restartAlert}
    </>
  );
}
