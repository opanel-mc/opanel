import Link from "next/link";
import { useCallback, useState, type PropsWithChildren } from "react";
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
import { Button } from "@/components/ui/button";
import { Field, FieldDescription, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { validateLocaleCode } from "@/lib/utils";

export function CreateCodeOfConductDialog({
  excludedLocales,
  children,
  asChild,
  onAction
}: PropsWithChildren & {
  excludedLocales: string[]
  asChild?: boolean,
  onAction?: (lang: string) => void
}) {
  const [dialogOpen, setDialogOpen] = useState<boolean>(false);
  const [inputtedLang, setInputtedLang] = useState<string>("");

  const handleAction = useCallback(() => {
    const formattedLang = inputtedLang.toLowerCase().replaceAll("-", "_");
    if(formattedLang === "" || !validateLocaleCode(formattedLang)) {
      toast.error("请输入有效的语言代码，如：zh_cn");
      return;
    }
    if(excludedLocales.includes(formattedLang)) {
      toast.warning("你所要创建的语言版本已存在");
      return;
    }
    
    onAction && onAction(formattedLang);
    setInputtedLang("");
    setDialogOpen(false);
  }, [excludedLocales, inputtedLang, onAction]);

  return (
    <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
      <DialogTrigger asChild={asChild}>{children}</DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>新建行为准则文档</DialogTitle>
          <DialogDescription>
            在此新建一个特定语言的行为准则文档。
          </DialogDescription>
        </DialogHeader>
        <div>
          <Field>
            <FieldLabel>语言</FieldLabel>
            <Input
              value={inputtedLang}
              placeholder="请输入语言代码...（如：zh_cn）"
              onInput={(e) => setInputtedLang((e.target as HTMLInputElement).value)}/>
            <FieldDescription>
              语言代码可参考 <Link href="https://simplelocalize.io/data/locales" target="_blank">https://simplelocalize.io/data/locales</Link><br />
              遵循<Link href="https://iso.org/iso-639-language-code" target="_blank">ISO-639-1</Link>与<Link href="https://iso.org/iso-3166-country-codes.html" target="_blank">ISO-3166</Link>国际标准。
            </FieldDescription>
          </Field>
        </div>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline">取消</Button>
          </DialogClose>
          <Button
            disabled={inputtedLang === ""}
            onClick={() => handleAction()}>
            新建
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
