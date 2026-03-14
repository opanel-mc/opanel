"use client";

import type { BackupProviderConfig, BackupProvidersResponse } from "@/lib/types";
import { useEffect, useMemo, useState } from "react";
import { CloudUpload, Pencil, FlaskConical, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { SubPage } from "../sub-page";
import { Button } from "@/components/ui/button";
import { ConfigItem, ConfigSection } from "@/components/config-item";
import { sendDeleteRequest, sendGetRequest, sendPostRequest, toastError } from "@/lib/api";
import { $ } from "@/lib/i18n";
import { Alert } from "@/components/alert";
import { ProviderDialog } from "./provider-dialog";

function providerSummary(provider: BackupProviderConfig): string {
  if(provider.type === "S3") {
    return `${provider.s3.bucket} · ${provider.s3.endpoint || provider.s3.region}`;
  }
  return `${provider.webdav.baseUrl} · ${provider.webdav.rootPath || "/"}`;
}

export default function CloudBackup() {
  const [providers, setProviders] = useState<BackupProviderConfig[]>([]);
  const [loading, setLoading] = useState(true);

  const providersById = useMemo(() => {
    const map = new Map<string, BackupProviderConfig>();
    for(const provider of providers) {
      map.set(provider.id, provider);
    }
    return map;
  }, [providers]);

  const fetchProviders = async () => {
    setLoading(true);
    try {
      const res = await sendGetRequest<BackupProvidersResponse>("/api/backup/providers");
      setProviders(res.providers);
    } catch (e: any) {
      toastError(e, $("cloud-backup.fetch.error"), [
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    } finally {
      setLoading(false);
    }
  };

  const testProvider = async (providerId: string) => {
    try {
      await sendPostRequest(`/api/backup/providers/${providerId}/test`);
      toast.success($("cloud-backup.test.success"));
    } catch (e: any) {
      toastError(e, $("cloud-backup.test.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [404, $("cloud-backup.error.404")],
        [500, $("common.error.500")]
      ]);
    }
  };

  const deleteProvider = async (providerId: string) => {
    try {
      await sendDeleteRequest(`/api/backup/providers/${providerId}`);
      await fetchProviders();
    } catch (e: any) {
      toastError(e, $("cloud-backup.delete.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [404, $("cloud-backup.error.404")],
        [409, $("cloud-backup.error.409")],
        [500, $("common.error.500")]
      ]);
    }
  };

  useEffect(() => {
    fetchProviders();
  }, []);

  return (
    <SubPage
      title={$("cloud-backup.title")}
      description={$("cloud-backup.description")}
      category={$("sidebar.config")}
      icon={<CloudUpload />}
      pageClassName="min-xl:px-64!">
      <div className="flex flex-col gap-4">
        <div className="flex justify-between items-end max-sm:flex-col max-sm:items-stretch max-sm:gap-2">
          <p className="text-sm text-muted-foreground">{$("cloud-backup.hint")}</p>
          <ProviderDialog mode="create" onSaved={() => fetchProviders()} asChild>
            <Button className="cursor-pointer">{$("cloud-backup.create")}</Button>
          </ProviderDialog>
        </div>

        <ConfigSection>
          {loading && (
            <div className="px-4 py-8 text-sm text-muted-foreground">{$("cloud-backup.loading")}</div>
          )}
          {!loading && providers.length === 0 && (
            <div className="px-4 py-8 text-sm text-muted-foreground">{$("cloud-backup.empty")}</div>
          )}
          {!loading && providers.map((provider) => (
            <ConfigItem
              key={provider.id}
              name={provider.name}
              description={`${provider.type === "S3" ? $("cloud-backup.provider.type.s3") : $("cloud-backup.provider.type.webdav")} · ${providerSummary(provider)}`}>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  className="cursor-pointer"
                  onClick={() => testProvider(provider.id)}>
                  <FlaskConical />
                  {$("cloud-backup.test")}
                </Button>
                <ProviderDialog
                  mode="edit"
                  provider={providersById.get(provider.id)}
                  onSaved={() => fetchProviders()}
                  asChild>
                  <Button variant="outline" size="icon-sm" className="cursor-pointer" title={$("cloud-backup.edit")}>
                    <Pencil />
                  </Button>
                </ProviderDialog>
                <Alert
                  title={$("cloud-backup.delete.alert.title", provider.name)}
                  description={$("cloud-backup.delete.alert.description")}
                  onAction={() => deleteProvider(provider.id)}
                  asChild>
                  <Button variant="outline" size="icon-sm" className="cursor-pointer" title={$("cloud-backup.delete")}>
                    <Trash2 />
                  </Button>
                </Alert>
              </div>
            </ConfigItem>
          ))}
        </ConfigSection>
      </div>
    </SubPage>
  );
}
