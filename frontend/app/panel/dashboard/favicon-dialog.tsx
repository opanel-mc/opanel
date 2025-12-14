import { useContext, useEffect, useState, type PropsWithChildren } from "react";
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
import { Button } from "@/components/ui/button";
import { Field, FieldDescription, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { InfoContext } from "@/contexts/api-context";
import { apiUrl, toastError, uploadFile } from "@/lib/api";
import { fileToDataUrl } from "@/lib/utils";

import PackIcon from "@/assets/images/pack.png";
import { emitter } from "@/lib/emitter";
import { $ } from "@/lib/i18n";

export function FaviconDialog({
  children,
  asChild
}: PropsWithChildren & {
  asChild?: boolean
}) {
  const ctx = useContext(InfoContext);
  const [dialogOpen, setDialogOpen] = useState<boolean>(false);
  const [file, setFile] = useState<File | null>(null);
  const [previewDataUrl, setPreviewDataUrl] = useState<string | null>(null);
  const [isSizeValid, setIsSizeValid] = useState<boolean>(false);

  const handleUpload = async (file: File) => {
    try {
      await uploadFile("/api/icon", file);
      emitter.emit("refresh-data");
      setDialogOpen(false);
    } catch (e: any) {
      toastError(e, $("dashboard.favicon.error"), [
        [400, $("dashboard.favicon.error.400")],
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    }
  };

  useEffect(() => {
    if(!previewDataUrl) return;

    const image = new Image();
    image.src = previewDataUrl;
    image.addEventListener("load", () => {
      setIsSizeValid(image.width === 64 && image.height === 64);
    });

    return () => image.remove();
  }, [previewDataUrl]);

  useEffect(() => {
    if(!dialogOpen) {
      setFile(null);
      setPreviewDataUrl(null);
    }
  }, [dialogOpen]);

  return (
    <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
      <DialogTrigger asChild={asChild}>{children}</DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{$("dashboard.favicon.title")}</DialogTitle>
          <DialogDescription>
            {$("dashboard.favicon.description")}
          </DialogDescription>
        </DialogHeader>
        <div className="flex flex-col items-center gap-4">
          <img
            className="aspect-square w-32 rounded-xs image-pixelated"
            src={
              previewDataUrl === null
              ? ((ctx && ctx.favicon) ? (apiUrl + ctx.favicon) : PackIcon.src)
              : previewDataUrl
            }
            alt="favicon"/>
          <Field>
            <FieldLabel>{$("dashboard.favicon.input.label")}</FieldLabel>
            <Input
              type="file"
              accept="image/png"
              onChange={async (e) => {
                const fileList = (e.target as HTMLInputElement).files;
                if(!fileList) return;
                if(fileList.length === 0) {
                  setFile(null);
                  setPreviewDataUrl(null);
                  return;
                }
                setFile(fileList[0]);
                setPreviewDataUrl(await fileToDataUrl(fileList[0]));
              }}/>
            <FieldDescription>
              <span>{$("dashboard.favicon.input.description")}</span><br />
              {(file !== null && !isSizeValid) && (
                <span className="text-destructive">{$("dashboard.favicon.input.error")}</span>
              )}
            </FieldDescription>
          </Field>
        </div>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline">{$("dialog.cancel")}</Button>
          </DialogClose>
          <Button
            disabled={file === null || !isSizeValid}
            onClick={() => file && handleUpload(file)}>
            {$("dialog.confirm")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
