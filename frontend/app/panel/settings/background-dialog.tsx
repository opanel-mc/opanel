"use client";

import { useEffect, useState, type PropsWithChildren } from "react";
import { toast } from "sonner";
import { Check, Image, Trash2, Upload } from "lucide-react";
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
import { apiUrl, sendDeleteRequest, sendGetRequest, toastError, uploadFile } from "@/lib/api";
import type { APIResponse } from "@/lib/types";
import { Field, FieldDescription, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { changeSettings, getSettings } from "@/lib/settings";
import { emitter } from "@/lib/emitter";

interface BackgroundItem {
  name: string;
  url: string;
  size: number;
}

export function BackgroundDialog({
  children,
  asChild
}: PropsWithChildren & {
  asChild?: boolean
}) {
  const [dialogOpen, setDialogOpen] = useState(false);
  const [backgrounds, setBackgrounds] = useState<BackgroundItem[]>([]);
  const [selected, setSelected] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const fetchBackgrounds = async () => {
    try {
      setLoading(true);
      const res = await sendGetRequest<{ files: BackgroundItem[] }>("/backgrounds/");
      setBackgrounds(res.files);
    } catch(e: any) {
      toastError(e, "Failed to load backgrounds", [
        [401, "Unauthorized"],
        [500, "Server error"]
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleUpload = async (file: File) => {
    try {
      await uploadFile("/backgrounds/upload", file);
      await fetchBackgrounds();
    } catch(e: any) {
      toastError(e, "Failed to upload background", [
        [400, "Invalid image format"],
        [409, "File with this name already exists"],
        [401, "Unauthorized"],
        [500, "Server error"]
      ]);
    }
  };

  const handleDelete = async (name: string) => {
    try {
      await sendDeleteRequest("/backgrounds/" + name);
      if(getSettings("panel.background") === "/backgrounds/" + name) {
        changeSettings("panel.background", undefined);
        setSelected(null);
        emitter.emit("background-change");
      }
      await fetchBackgrounds();
    } catch(e: any) {
      toastError(e, "Failed to delete background", [
        [401, "Unauthorized"],
        [500, "Server error"]
      ]);
    }
  };

  const handleSelect = (name: string) => {
    if(selected === name) {
      changeSettings("panel.background", undefined);
      setSelected(null);
    } else {
      changeSettings("panel.background", "/backgrounds/" + name);
      setSelected(name);
    }
    emitter.emit("background-change");
  };

  useEffect(() => {
    if(dialogOpen) {
      fetchBackgrounds();
      const currentBg = getSettings("panel.background");
      if(currentBg) {
        const name = currentBg.replace("/backgrounds/", "");
        setSelected(name);
      }
    }
  }, [dialogOpen]);

  const formatSize = (bytes: number) => {
    if(bytes < 1024) return bytes + " B";
    if(bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
    return (bytes / (1024 * 1024)).toFixed(1) + " MB";
  };

  return (
    <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
      <DialogTrigger asChild={asChild}>{children}</DialogTrigger>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Panel Background</DialogTitle>
          <DialogDescription>
            Upload images to use as the panel background. Supported formats: png, jpg, jpeg, webp.
          </DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-4">
          <Field>
            <FieldLabel>Upload New Background</FieldLabel>
            <Input
              type="file"
              accept=".png,.jpg,.jpeg,.webp"
              onChange={async (e) => {
                const files = (e.target as HTMLInputElement).files;
                if(!files || files.length === 0) return;
                await handleUpload(files[0]);
                (e.target as HTMLInputElement).value = "";
              }}/>
            <FieldDescription>
              Drop images into the opanel/backgrounds/ folder on the server to add them directly.
            </FieldDescription>
          </Field>

          <div className="border rounded-md">
            <div className="px-4 py-2 border-b text-sm font-medium text-muted-foreground">
              Backgrounds ({backgrounds.length})
            </div>
            <div className="max-h-64 o-scrollbar overflow-y-auto">
              {loading ? (
                <div className="p-8 text-center text-muted-foreground text-sm">Loading...</div>
              ) : backgrounds.length === 0 ? (
                <div className="p-8 text-center text-muted-foreground text-sm flex flex-col items-center gap-2">
                  <Image className="w-8 h-8 opacity-30" />
                  No backgrounds yet. Upload one or place images in the backgrounds folder.
                </div>
              ) : (
                backgrounds.map((bg) => (
                  <div
                    key={bg.name}
                    className={cn(
                      "flex items-center gap-3 px-4 py-2 border-b last:border-b-0 hover:bg-accent/50 transition-colors",
                      selected === bg.name && "bg-accent"
                    )}>
                    <div
                      className="w-12 h-8 rounded-sm bg-cover bg-center flex-shrink-0 border"
                      style={{ backgroundImage: `url(${bg.url}?t=${Date.now()})` }}
                    />
                    <div className="flex-1 min-w-0">
                      <div className="text-sm truncate">{bg.name}</div>
                      <div className="text-xs text-muted-foreground">{formatSize(bg.size)}</div>
                    </div>
                    <Button
                      variant={selected === bg.name ? "default" : "outline"}
                      size="icon-xs"
                      className="flex-shrink-0"
                      onClick={() => handleSelect(bg.name)}>
                      <Check className={cn(selected === bg.name ? "opacity-100" : "opacity-0 group-hover:opacity-100")} />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon-xs"
                      className="flex-shrink-0 text-muted-foreground hover:text-destructive"
                      onClick={() => handleDelete(bg.name)}>
                      <Trash2 />
                    </Button>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline">Close</Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}