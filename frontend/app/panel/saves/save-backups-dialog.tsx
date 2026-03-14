import type {
  BackupProviderConfig,
  BackupProvidersResponse,
  BackupRecord,
  CreateSaveBackupResponse,
  SaveBackupsResponse
} from "@/lib/types";
import type { PropsWithChildren } from "react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { RotateCcw, Trash2 } from "lucide-react";
import { toast } from "sonner";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { sendDeleteRequest, sendGetRequest, sendPostRequest, toastError } from "@/lib/api";
import { $ } from "@/lib/i18n";
import { formatDataSize } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { Prompt } from "@/components/prompt";
import { Alert } from "@/components/alert";
import { emitter } from "@/lib/emitter";

const statusClassName: Record<string, string> = {
  RUNNING: "border-blue-600 text-blue-600",
  SUCCESS: "border-emerald-600 text-emerald-600",
  FAILED: "border-red-600 text-red-600"
};

const LOCAL_PROVIDER_ID = "__local__";

export function SaveBackupsDialog({
  saveName,
  saveNames,
  defaultSaveName,
  children,
  asChild
}: PropsWithChildren<{
  saveName?: string
  saveNames?: string[]
  defaultSaveName?: string
  asChild?: boolean
}>) {
  const isFixedSave = Boolean(saveName);
  const availableSaveNames = useMemo(() => {
    if(saveName) return [saveName];
    return Array.from(new Set((saveNames ?? []).filter(item => !!item)));
  }, [saveName, saveNames]);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [providers, setProviders] = useState<BackupProviderConfig[]>([]);
  const [backups, setBackups] = useState<BackupRecord[]>([]);
  const [selectedSaveName, setSelectedSaveName] = useState<string>(saveName ?? defaultSaveName ?? "");
  const [providerId, setProviderId] = useState<string>(LOCAL_PROVIDER_ID);
  const [creating, setCreating] = useState(false);
  const [loading, setLoading] = useState(false);

  const providersById = useMemo(() => {
    const map = new Map<string, BackupProviderConfig>();
    for(const provider of providers) {
      map.set(provider.id, provider);
    }
    return map;
  }, [providers]);

  const hasRunningBackup = backups.some(backup => backup.status === "RUNNING");

  const fetchProviders = useCallback(async () => {
    const res = await sendGetRequest<BackupProvidersResponse>("/api/backup/providers");
    setProviders(res.providers);
    setProviderId((currentProviderId) => {
      if(currentProviderId) return currentProviderId;
      if(res.providers.length === 0) return LOCAL_PROVIDER_ID;
      return res.providers[0].id;
    });
  }, []);

  const fetchBackups = useCallback(async () => {
    if(!selectedSaveName) {
      setBackups([]);
      return;
    }

    const res = await sendGetRequest<SaveBackupsResponse>(`/api/saves/${selectedSaveName}/backups`);
    setBackups(res.backups);
  }, [selectedSaveName]);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      await Promise.all([fetchProviders(), fetchBackups()]);
    } catch (e: any) {
      toastError(e, $("saves.backups.fetch.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [404, $("common.error.400")],
        [500, $("common.error.500")]
      ]);
    } finally {
      setLoading(false);
    }
  }, [fetchBackups, fetchProviders]);

  const createBackup = async () => {
    const targetSaveName = selectedSaveName.trim();
    if(!targetSaveName) {
      toast.error($("saves.backups.save.empty"));
      return;
    }

    const targetProviderId = providerId || LOCAL_PROVIDER_ID;
    if(!targetProviderId) {
      toast.error($("saves.backups.provider.empty"));
      return;
    }

    setCreating(true);
    try {
      const res = await sendPostRequest<CreateSaveBackupResponse>(`/api/saves/${targetSaveName}/backups`, {
        providerId: targetProviderId
      });
      toast.success($("saves.backups.create.success", res.backupId));
      await fetchBackups();
    } catch (e: any) {
      toastError(e, $("saves.backups.create.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [404, $("common.error.400")],
        [409, $("saves.backups.create.error.409")],
        [500, $("common.error.500")]
      ]);
    } finally {
      setCreating(false);
    }
  };

  const restoreBackup = async (backupId: string, targetSaveName: string) => {
    const sourceSaveName = selectedSaveName.trim();
    if(!sourceSaveName) {
      toast.error($("saves.backups.save.empty"));
      return;
    }

    try {
      const body = targetSaveName.trim() ? { targetSaveName: targetSaveName.trim() } : {};
      const res = await sendPostRequest<{ restoredSaveName: string }>(`/api/saves/${sourceSaveName}/backups/${backupId}/restore`, body);
      toast.success($("saves.backups.item.restore.success", res.restoredSaveName));
      emitter.emit("refresh-data");
      await fetchBackups();
    } catch (e: any) {
      toastError(e, $("saves.backups.item.restore.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [404, $("common.error.400")],
        [409, $("saves.backups.item.restore.error.409")],
        [500, $("common.error.500")]
      ]);
    }
  };

  const deleteBackup = async (backupId: string) => {
    const sourceSaveName = selectedSaveName.trim();
    if(!sourceSaveName) {
      toast.error($("saves.backups.save.empty"));
      return;
    }

    try {
      await sendDeleteRequest(`/api/saves/${sourceSaveName}/backups/${backupId}`);
      toast.success($("saves.backups.item.delete.success"));
      await fetchBackups();
    } catch (e: any) {
      toastError(e, $("saves.backups.item.delete.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [404, $("common.error.400")],
        [500, $("common.error.500")]
      ]);
    }
  };

  useEffect(() => {
    setSelectedSaveName((current) => {
      if(saveName) return saveName;
      if(current && availableSaveNames.includes(current)) return current;
      if(defaultSaveName && availableSaveNames.includes(defaultSaveName)) return defaultSaveName;
      return availableSaveNames[0] || "";
    });
  }, [availableSaveNames, defaultSaveName, saveName]);

  useEffect(() => {
    if(!dialogOpen) return;
    refresh();
  }, [dialogOpen, refresh]);

  useEffect(() => {
    if(!dialogOpen || !hasRunningBackup) return;

    const timer = setInterval(() => {
      fetchBackups().catch((e) => {
        console.error("Failed to poll backup records", e);
      });
    }, 2500);
    return () => clearInterval(timer);
  }, [dialogOpen, fetchBackups, hasRunningBackup]);

  return (
    <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
      <DialogTrigger asChild={asChild}>{children}</DialogTrigger>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto o-scrollbar">
        <DialogHeader>
          <DialogTitle>{$("saves.backups.title")}</DialogTitle>
          <DialogDescription>
            {
              isFixedSave
              ? $("saves.backups.description", selectedSaveName)
              : $("saves.backups.description.general")
            }
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-4">
          <div className={
            isFixedSave
            ? "grid grid-cols-[1fr_auto] gap-2 max-sm:grid-cols-1"
            : "grid grid-cols-[1fr_1fr_auto] gap-2 max-lg:grid-cols-1"
          }>
            {!isFixedSave && (
              availableSaveNames.length > 0
              ? (
                <Select value={selectedSaveName} onValueChange={setSelectedSaveName}>
                  <SelectTrigger>
                    <SelectValue placeholder={$("saves.backups.save.label")} />
                  </SelectTrigger>
                  <SelectContent>
                    {availableSaveNames.map((name) => (
                      <SelectItem value={name} key={name}>
                        {name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )
              : (
                <div className="text-sm text-muted-foreground border rounded-md px-3 py-2">
                  {$("saves.backups.save.empty-options")}
                </div>
              )
            )}
            <Select value={providerId} onValueChange={setProviderId}>
              <SelectTrigger>
                <SelectValue placeholder={$("saves.backups.provider.label")} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={LOCAL_PROVIDER_ID}>{$("saves.backups.provider.local")}</SelectItem>
                {providers.map(provider => (
                  <SelectItem value={provider.id} key={provider.id}>
                    {provider.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button
              className="cursor-pointer"
              disabled={creating || hasRunningBackup || !selectedSaveName}
              onClick={() => createBackup()}>
              {creating ? $("saves.backups.create.creating") : $("saves.backups.create")}
            </Button>
          </div>

          {loading && (
            <div className="text-sm text-muted-foreground py-4">{$("saves.backups.loading")}</div>
          )}

          {!loading && backups.length === 0 && (
            <div className="text-sm text-muted-foreground py-4">{$("saves.backups.list.empty")}</div>
          )}

          {!loading && backups.length > 0 && (
            <div className="flex flex-col gap-3">
              {backups.map((backup) => (
                <div className="border rounded-md p-3 flex flex-col gap-2" key={backup.id}>
                  <div className="flex justify-between items-center gap-3">
                    <div className="flex flex-col gap-1 min-w-0">
                      <span className="text-sm font-medium truncate">{new Date(backup.createdAt).toLocaleString()}</span>
                      <span className="text-xs text-muted-foreground truncate">
                        {$("saves.backups.item.provider")} {(
                          backup.providerId === LOCAL_PROVIDER_ID
                          ? $("saves.backups.provider.local")
                          : (backup.providerName || providersById.get(backup.providerId)?.name || backup.providerId)
                        )}
                      </span>
                    </div>
                    <Badge variant="outline" className={statusClassName[backup.status] || ""}>
                      {backup.status === "RUNNING" && $("saves.backups.item.status.running")}
                      {backup.status === "SUCCESS" && $("saves.backups.item.status.success")}
                      {backup.status === "FAILED" && $("saves.backups.item.status.failed")}
                    </Badge>
                  </div>

                  <div className="text-xs text-muted-foreground flex flex-wrap gap-x-4 gap-y-1">
                    <span>{$("saves.backups.item.size")} {backup.sizeBytes > 0 ? formatDataSize(backup.sizeBytes) : "-"}</span>
                    <span className="truncate">sha256: {backup.sha256 || "-"}</span>
                  </div>

                  {backup.status === "FAILED" && backup.error && (
                    <span className="text-xs text-red-500 wrap-anywhere">{$("saves.backups.item.error", backup.error)}</span>
                  )}

                  {backup.status === "SUCCESS" && (
                    <div className="flex justify-end gap-2">
                      <Prompt
                        title={$("saves.backups.item.restore.prompt.title")}
                        description={$("saves.backups.item.restore.prompt.description")}
                        label={$("saves.backups.item.restore.prompt.label")}
                        placeholder={$("saves.backups.item.restore.prompt.placeholder")}
                        onAction={(targetSaveName) => restoreBackup(backup.id, targetSaveName)}
                        asChild>
                        <Button variant="outline" size="sm" className="cursor-pointer">
                          <RotateCcw />
                          {$("saves.backups.item.restore")}
                        </Button>
                      </Prompt>
                      <Alert
                        title={$("saves.backups.item.delete.alert.title")}
                        description={$("saves.backups.item.delete.alert.description")}
                        onAction={() => deleteBackup(backup.id)}
                        asChild>
                        <Button variant="outline" size="sm" className="cursor-pointer">
                          <Trash2 />
                          {$("saves.backups.item.delete")}
                        </Button>
                      </Alert>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline">{$("dialog.close")}</Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
