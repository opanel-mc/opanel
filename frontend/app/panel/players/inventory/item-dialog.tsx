import type { ItemStack } from "@/lib/types";
import dynamic from "next/dynamic";
import Link from "next/link";
import { useContext, useEffect, useState, type PropsWithChildren } from "react";
import { useTheme } from "next-themes";
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
import { monacoSettingsOptions } from "@/lib/settings";
import { prettyFormatNBT } from "@/lib/nbt/snbt-format";
import { InventoryContext } from "@/contexts/inventory-context";
import { Text } from "@/components/i18n-text";

const MonacoEditor = dynamic(() => import("@/components/monaco-editor"), { ssr: false });

export function ItemDialog({
  itemStack,
  children,
  disabled = false,
  asChild
}: PropsWithChildren & {
  itemStack: ItemStack
  disabled?: boolean
  asChild?: boolean
}) {
  const ctx = useContext(InventoryContext);
  const { updateItemNBT } = ctx;
  const [dialogOpen, setDialogOpen] = useState(false);
  const [value, setValue] = useState("");
  const { theme } = useTheme();

  const handleSave = () => {
    updateItemNBT(itemStack, value);
  };

  useEffect(() => {
    setValue(itemStack.snbt ? prettyFormatNBT(itemStack.snbt) : "{}");
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dialogOpen]);

  if(!ctx) return <></>;

  return (
    <Dialog
      open={dialogOpen}
      onOpenChange={(open) => setDialogOpen(disabled ? false : open)}>
      <DialogTrigger asChild={asChild}>{children}</DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{$("players.inventory.nbt-editor.title")}</DialogTitle>
          <DialogDescription>
            {$("players.inventory.nbt-editor.description")}
          </DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-2">
          <div className="h-[500px] border rounded-md flex overflow-hidden">
            <MonacoEditor
              language="python"
              value={value}
              theme={theme === "dark" ? "vs-dark" : "vs"}
              options={{
                minimap: { enabled: false },
                automaticLayout: true,
                tabSize: 2,
                ...monacoSettingsOptions
              }}
              onChange={(newValue) => setValue(newValue ?? "")}/>
          </div>
          <Text
            id="players.inventory.nbt-editor.hint"
            args={[
              <Link
                href="https://zh.minecraft.wiki/w/SNBT格式"
                target="_blank"
                rel="noopener noreferrer"
                key={0}>
                SNBT
              </Link>
            ]}
            className="text-sm text-muted-foreground"/>
        </div>
        <DialogFooter>
          <DialogClose asChild>
            <Button
              className="cursor-pointer"
              onClick={() => handleSave()}>
              {$("dialog.save")}
            </Button>
          </DialogClose>
          <DialogClose asChild>
            <Button
              variant="outline"
              className="cursor-pointer">
              {$("dialog.cancel")}
            </Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
