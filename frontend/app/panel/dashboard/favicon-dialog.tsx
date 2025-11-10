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
      toastError(e, "无法上传图片", [
        [400, "图片格式错误，请上传一个64*64大小的png文件"],
        [401, "未登录"],
        [500, "服务器内部错误"]
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
          <DialogTitle>修改服务器图标</DialogTitle>
          <DialogDescription>
            在此上传图片作为服务器的新图标。
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
            <FieldLabel>上传图片</FieldLabel>
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
              <span>请选择一个64*64大小的png格式图片作为服务器图标。</span><br />
              {(file !== null && !isSizeValid) && (
                <span className="text-destructive">图片大小错误，尺寸应为64*64</span>
              )}
            </FieldDescription>
          </Field>
        </div>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline">取消</Button>
          </DialogClose>
          <Button
            disabled={file === null || !isSizeValid}
            onClick={() => file && handleUpload(file)}>
            确认
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
