import type { InventoryType, ItemStack } from "@/lib/types";
import dynamic from "next/dynamic";
import Link from "next/link";
import {
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren
} from "react";
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
import {
  parseContainerNBT,
  serializeContainerNBT
} from "@/lib/nbt/container";
import { InventoryContext } from "@/contexts/inventory-context";
import { Text } from "@/components/i18n-text";
import { cn } from "@/lib/utils";
import { ContainerEditor } from "./container-editor";

const MonacoEditor = dynamic(() => import("@/components/monaco-editor"), { ssr: false });

export function ItemDialog({
  itemStack,
  inventoryType,
  children,
  disabled = false,
  asChild
}: PropsWithChildren & {
  itemStack: ItemStack
  inventoryType?: InventoryType
  disabled?: boolean
  asChild?: boolean
}) {
  const ctx = useContext(InventoryContext);
  const { updateItemNBT } = ctx;
  const [dialogOpen, setDialogOpen] = useState(false);
  const [value, setValue] = useState("");
  const [heldItem, setHeldItem] = useState<ItemStack | null>(null);
  const { theme } = useTheme();
  const parsedContainer = useMemo(
    () => parseContainerNBT(value, itemStack.id),
    [itemStack.id, value]
  );

  const handleSave = () => {
    if(inventoryType && !heldItem) updateItemNBT(inventoryType, itemStack, value);
  };

  useEffect(() => {
    setValue(itemStack.snbt ? prettyFormatNBT(itemStack.snbt) : "{}");
    setHeldItem(null);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dialogOpen]);

  if(!ctx) return <></>;

  return (
    <Dialog
      open={dialogOpen}
      onOpenChange={(open) => setDialogOpen(disabled ? false : open)}>
      <DialogTrigger asChild={asChild}>{children}</DialogTrigger>
      <DialogContent
        className={cn(
          parsedContainer && [
            "max-h-[calc(100vh-2rem)] overflow-x-hidden overflow-y-auto o-scrollbar",
            "sm:max-w-[calc(100%-2rem)] lg:max-w-6xl"
          ]
        )}>
        <DialogHeader>
          <DialogTitle>{$("players.inventory.nbt-editor.title")}</DialogTitle>
          <DialogDescription>
            {$("players.inventory.nbt-editor.description")}
          </DialogDescription>
        </DialogHeader>
        <div className={cn(
          "grid gap-4",
          parsedContainer && "lg:grid-cols-[minmax(0,1fr)_auto]"
        )}>
          <div className="min-w-0 flex flex-col gap-2">
            <div className="h-[500px] border rounded-md flex overflow-hidden">
              <MonacoEditor
                language="python"
                value={value}
                theme={theme === "dark" ? "opanel-theme-dark" : "opanel-theme"}
                options={{
                  minimap: { enabled: false },
                  automaticLayout: true,
                  tabSize: 2,
                  ...monacoSettingsOptions,
                  readOnly: heldItem !== null
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
          {parsedContainer && (
            <ContainerEditor
              container={parsedContainer}
              heldItem={heldItem}
              setHeldItem={setHeldItem}
              onItemsChange={(items) => {
                setValue(serializeContainerNBT(value, parsedContainer.format, items));
              }}/>
          )}
        </div>
        <DialogFooter>
          <DialogClose asChild>
            <Button
              className="cursor-pointer"
              disabled={heldItem !== null}
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
