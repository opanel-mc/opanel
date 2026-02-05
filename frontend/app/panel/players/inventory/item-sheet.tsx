import type { ItemStack } from "@/lib/types"
import { useState, type PropsWithChildren } from "react"
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
  SheetTrigger
} from "@/components/ui/sheet"
import { Button } from "@/components/ui/button"
import { $ } from "@/lib/i18n"

export function ItemSheet({
  itemStack,
  children,
  disabled = false,
  asChild
}: PropsWithChildren & {
  itemStack: ItemStack
  disabled?: boolean
  asChild?: boolean
}) {
  const [sheetOpen, setSheetOpen] = useState(false);

  return (
    <Sheet open={sheetOpen} onOpenChange={(open) => setSheetOpen(disabled ? false : open)}>
      <SheetTrigger asChild={asChild}>{children}</SheetTrigger>
      <SheetContent>
        <SheetHeader>
          <SheetTitle>编辑物品 NBT</SheetTitle>
          <SheetDescription>
            在此编辑物品的NBT标签数据。
          </SheetDescription>
        </SheetHeader>
        <div>

        </div>
        <SheetFooter>
          <SheetClose asChild>
            <Button
              type="submit"
              className="cursor-pointer">
              {$("dialog.save")}
            </Button>
          </SheetClose>
          <SheetClose asChild>
            <Button
              variant="outline"
              className="cursor-pointer">
              {$("dialog.cancel")}
            </Button>
          </SheetClose>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}
