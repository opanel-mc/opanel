import type { PropsWithChildren } from "react";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger
} from "./ui/alert-dialog";
import { $ } from "@/lib/i18n";

export function Alert({
  title,
  description,
  cancellable = true,
  onAction,
  asChild,
  children
}: PropsWithChildren<{
  title: string
  description?: string | React.ReactNode
  cancellable?: boolean
  onAction?: () => void
  asChild?: boolean
}>) {
  return (
    <AlertDialog>
      <AlertDialogTrigger asChild={asChild}>
        {children}
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle className="wrap-anywhere">{title}</AlertDialogTitle>
          {description && (
            <AlertDialogDescription>{description}</AlertDialogDescription>
          )}
        </AlertDialogHeader>
        <AlertDialogFooter>
          {cancellable && <AlertDialogCancel>{$("dialog.cancel")}</AlertDialogCancel>}
          <AlertDialogAction
            onClick={() => {
              onAction && onAction();
            }}>
            {$("dialog.confirm")}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
