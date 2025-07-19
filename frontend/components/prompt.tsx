import { PropsWithChildren, useRef } from "react";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger
} from "./ui/dialog";
import { Label } from "./ui/label";
import { Input } from "./ui/input";
import { Button } from "./ui/button";

export function Prompt({
  title,
  description,
  label,
  placeholder,
  onAction,
  asChild,
  children
}: PropsWithChildren<{
  title: string
  description?: string
  label: string
  placeholder?: string
  onAction?: (result: string) => void
  asChild?: boolean
}>) {
  const inputRef = useRef<HTMLInputElement>(null);

  return (
    <Dialog>
      <DialogTrigger asChild={asChild}>
        {children}
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          {description && (
            <DialogDescription>{description}</DialogDescription>
          )}
        </DialogHeader>
        <div className="flex flex-col gap-3">
          <Label>{label}</Label>
          <Input placeholder={placeholder} ref={inputRef}/>
        </div>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline">取消</Button>
          </DialogClose>
          <DialogClose asChild>
            <Button onClick={() => {
              if(!inputRef.current) return;
              onAction && onAction(inputRef.current.value);
            }}>
              确定
            </Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
