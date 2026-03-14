import type { PropsWithChildren } from "react";
import type { BackupProviderConfig, BackupProviderType } from "@/lib/types";
import { useEffect, useState } from "react";
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
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "@/components/ui/select";
import { sendPostRequest, toastError } from "@/lib/api";
import { $ } from "@/lib/i18n";
import { Switch } from "@/components/ui/switch";

const emptyProvider = (): BackupProviderConfig => ({
  id: "",
  name: "",
  type: "S3",
  s3: {
    endpoint: "",
    region: "us-east-1",
    bucket: "",
    accessKey: "",
    secretKey: "",
    prefix: "opanel",
    forcePathStyle: true
  },
  webdav: {
    baseUrl: "",
    username: "",
    password: "",
    rootPath: "opanel"
  }
});

export function ProviderDialog({
  mode,
  provider,
  onSaved,
  children,
  asChild
}: PropsWithChildren<{
  mode: "create" | "edit"
  provider?: BackupProviderConfig | null
  onSaved?: () => void
  asChild?: boolean
}>) {
  const [dialogOpen, setDialogOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState<BackupProviderConfig>(emptyProvider());

  useEffect(() => {
    if(mode === "edit" && provider) {
      setForm({
        ...provider,
        s3: {
          ...provider.s3,
          secretKey: ""
        },
        webdav: {
          ...provider.webdav,
          password: ""
        }
      });
      return;
    }
    setForm(emptyProvider());
  }, [mode, provider, dialogOpen]);

  const setProviderType = (type: BackupProviderType) => {
    setForm(prev => ({ ...prev, type }));
  };

  const submit = async () => {
    if(submitting) return;
    if(!form.name.trim()) {
      toast.error($("cloud-backup.provider.form.name.empty"));
      return;
    }

    setSubmitting(true);
    try {
      const body: BackupProviderConfig = {
        ...form,
        name: form.name.trim()
      };

      if(mode === "edit" && provider) {
        await sendPostRequest(`/api/backup/providers/${provider.id}`, body);
      } else {
        await sendPostRequest("/api/backup/providers", body);
      }

      toast.success(mode === "create" ? $("cloud-backup.create.success") : $("cloud-backup.edit.success"));
      setDialogOpen(false);
      onSaved && onSaved();
    } catch (e: any) {
      toastError(e, mode === "create" ? $("cloud-backup.create.error") : $("cloud-backup.edit.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [404, $("cloud-backup.error.404")],
        [409, $("cloud-backup.error.409")],
        [500, $("common.error.500")]
      ]);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
      <DialogTrigger asChild={asChild}>{children}</DialogTrigger>
      <DialogContent className="max-h-[90vh] overflow-y-auto o-scrollbar">
        <DialogHeader>
          <DialogTitle>
            {mode === "create" ? $("cloud-backup.provider.dialog.create.title") : $("cloud-backup.provider.dialog.edit.title")}
          </DialogTitle>
          <DialogDescription>
            {mode === "create" ? $("cloud-backup.provider.dialog.create.description") : $("cloud-backup.provider.dialog.edit.description")}
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-4">
          <div className="grid gap-2">
            <Label>{$("cloud-backup.provider.form.name.label")}</Label>
            <Input
              placeholder={$("cloud-backup.provider.form.name.placeholder")}
              value={form.name}
              onChange={(e) => setForm(prev => ({ ...prev, name: e.target.value }))}/>
          </div>

          <div className="grid gap-2">
            <Label>{$("cloud-backup.provider.form.type.label")}</Label>
            <Select value={form.type} onValueChange={(value) => setProviderType(value as BackupProviderType)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="S3">{$("cloud-backup.provider.type.s3")}</SelectItem>
                <SelectItem value="WEBDAV">{$("cloud-backup.provider.type.webdav")}</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {form.type === "S3" && (
            <>
              <div className="grid gap-2">
                <Label>{$("cloud-backup.provider.form.s3.endpoint")}</Label>
                <Input
                  placeholder={$("cloud-backup.provider.form.s3.endpoint.placeholder")}
                  value={form.s3.endpoint}
                  onChange={(e) => setForm(prev => ({
                    ...prev,
                    s3: { ...prev.s3, endpoint: e.target.value }
                  }))}/>
              </div>
              <div className="grid gap-2">
                <Label>{$("cloud-backup.provider.form.s3.region")}</Label>
                <Input
                  placeholder="us-east-1"
                  value={form.s3.region}
                  onChange={(e) => setForm(prev => ({
                    ...prev,
                    s3: { ...prev.s3, region: e.target.value }
                  }))}/>
              </div>
              <div className="grid gap-2">
                <Label>{$("cloud-backup.provider.form.s3.bucket")}</Label>
                <Input
                  value={form.s3.bucket}
                  onChange={(e) => setForm(prev => ({
                    ...prev,
                    s3: { ...prev.s3, bucket: e.target.value }
                  }))}/>
              </div>
              <div className="grid gap-2">
                <Label>{$("cloud-backup.provider.form.s3.access-key")}</Label>
                <Input
                  value={form.s3.accessKey}
                  onChange={(e) => setForm(prev => ({
                    ...prev,
                    s3: { ...prev.s3, accessKey: e.target.value }
                  }))}/>
              </div>
              <div className="grid gap-2">
                <Label>{$("cloud-backup.provider.form.s3.secret-key")}</Label>
                <Input
                  type="password"
                  placeholder={mode === "edit" ? $("cloud-backup.provider.form.secret.placeholder") : ""}
                  value={form.s3.secretKey}
                  onChange={(e) => setForm(prev => ({
                    ...prev,
                    s3: { ...prev.s3, secretKey: e.target.value }
                  }))}/>
              </div>
              <div className="grid gap-2">
                <Label>{$("cloud-backup.provider.form.s3.prefix")}</Label>
                <Input
                  value={form.s3.prefix}
                  onChange={(e) => setForm(prev => ({
                    ...prev,
                    s3: { ...prev.s3, prefix: e.target.value }
                  }))}/>
              </div>
              <div className="flex justify-between items-center px-1">
                <Label>{$("cloud-backup.provider.form.s3.path-style")}</Label>
                <Switch
                  checked={form.s3.forcePathStyle}
                  onCheckedChange={(checked) => setForm(prev => ({
                    ...prev,
                    s3: { ...prev.s3, forcePathStyle: checked }
                  }))}/>
              </div>
            </>
          )}

          {form.type === "WEBDAV" && (
            <>
              <div className="grid gap-2">
                <Label>{$("cloud-backup.provider.form.webdav.base-url")}</Label>
                <Input
                  placeholder="https://example.com/dav"
                  value={form.webdav.baseUrl}
                  onChange={(e) => setForm(prev => ({
                    ...prev,
                    webdav: { ...prev.webdav, baseUrl: e.target.value }
                  }))}/>
              </div>
              <div className="grid gap-2">
                <Label>{$("cloud-backup.provider.form.webdav.username")}</Label>
                <Input
                  value={form.webdav.username}
                  onChange={(e) => setForm(prev => ({
                    ...prev,
                    webdav: { ...prev.webdav, username: e.target.value }
                  }))}/>
              </div>
              <div className="grid gap-2">
                <Label>{$("cloud-backup.provider.form.webdav.password")}</Label>
                <Input
                  type="password"
                  placeholder={mode === "edit" ? $("cloud-backup.provider.form.secret.placeholder") : ""}
                  value={form.webdav.password}
                  onChange={(e) => setForm(prev => ({
                    ...prev,
                    webdav: { ...prev.webdav, password: e.target.value }
                  }))}/>
              </div>
              <div className="grid gap-2">
                <Label>{$("cloud-backup.provider.form.webdav.root-path")}</Label>
                <Input
                  value={form.webdav.rootPath}
                  onChange={(e) => setForm(prev => ({
                    ...prev,
                    webdav: { ...prev.webdav, rootPath: e.target.value }
                  }))}/>
              </div>
            </>
          )}
        </div>

        <DialogFooter className="flex flex-row [&>*]:flex-1 [&_button]:cursor-pointer">
          <DialogClose asChild>
            <Button variant="outline">{$("dialog.cancel")}</Button>
          </DialogClose>
          <Button disabled={submitting} onClick={() => submit()}>
            {mode === "create" ? $("dialog.create") : $("dialog.save")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
