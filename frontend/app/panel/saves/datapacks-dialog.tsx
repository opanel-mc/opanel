import { useRef, useState, type PropsWithChildren } from "react";
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
import { $ } from "@/lib/i18n";
import { Switch } from "@/components/ui/switch";
import { googleSansCode } from "@/lib/fonts";
import { cn } from "@/lib/utils";
import { sendPatchRequest, toastError } from "@/lib/api";
import { emitter } from "@/lib/emitter";

export function DatapacksDialog({
  saveName,
  datapacks,
  children,
  asChild
}: PropsWithChildren & {
  saveName: string
  datapacks: Record<string, boolean>
  asChild?: boolean
}) {
  const [dialogOpen, setDialogOpen] = useState(false);
  const datapackMapRef = useRef(new Map(Object.entries(datapacks)));
  const datapackList = Array.from(datapackMapRef.current);

  const toggleDatapack = async (datapack: string, enabled: boolean) => {
    try {
      await sendPatchRequest(`/api/saves/${saveName}?datapack=${datapack}&enabled=${enabled ? "1" : "0"}`);
      datapackMapRef.current.set(datapack, enabled);
      emitter.emit("refresh-data");
    } catch (e: any) {
      toastError(e, enabled ? $("saves.datapacks.toggle.enable.error", datapack) : $("saves.datapacks.toggle.disable.error", datapack), [
        [401, $("common.error.401")],
        [403, $("saves.datapacks.toggle.error.403")],
        [404, $("saves.datapacks.toggle.error.404", datapack)],
        [500, $("common.error.500")]
      ]);
    }
  };

  return (
    <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
      <DialogTrigger asChild={asChild}>{children}</DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{$("saves.datapacks.title")}</DialogTitle>
          <DialogDescription>
            {$("saves.datapacks.description", saveName)}
          </DialogDescription>
        </DialogHeader>
        <div className="max-h-96 pr-2 overflow-y-auto o-scrollbar flex flex-col gap-4">
          {
            [
              ...datapackList.filter(([datapack]) => datapack.startsWith("file/")), // custom datapacks
              ...datapackList.filter(([datapack]) => datapack.startsWith("mod:")), // mod datapacks
              ...datapackList.filter(([datapack]) => (
                !datapack.startsWith("file/")
                && !datapack.startsWith("mod:")
                && datapack !== "vanilla"
              )), // built-in datapacks
              datapackList.find(([datapack]) => datapack === "vanilla")!, // vanilla datapack
            ].map(([datapack, enabled], i) => (
              <div className="flex justify-between items-center" key={i}>
                <span className={cn("text-sm", googleSansCode.className)}>{datapack}</span>
                <Switch
                  checked={enabled}
                  disabled={datapack === "vanilla"}
                  onCheckedChange={(checked) => toggleDatapack(datapack, checked)}/>
              </div>
            ))
          }
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
