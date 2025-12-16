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
import { Text } from "@/components/i18n-text";
import { $ } from "@/lib/i18n";

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
      toast.error($("coc.create.invalid-lang"));
      return;
    }
    if(excludedLocales.includes(formattedLang)) {
      toast.warning($("coc.create.exist"));
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
          <DialogTitle>{$("coc.create.title")}</DialogTitle>
          <DialogDescription>
            {$("coc.create.description")}
          </DialogDescription>
        </DialogHeader>
        <div>
          <Field>
            <FieldLabel>{$("coc.create.input.label")}</FieldLabel>
            <Input
              value={inputtedLang}
              placeholder={$("coc.create.input.placeholder")}
              onInput={(e) => setInputtedLang((e.target as HTMLInputElement).value)}/>
            <FieldDescription>
              <Text
                id="coc.create.input.description"
                args={[
                  <Link
                    href="https://simplelocalize.io/data/locales"
                    target="_blank"
                    key={0}>
                    https://simplelocalize.io/data/locales
                  </Link>,
                  <Link
                    href="https://iso.org/iso-639-language-code"
                    target="_blank"
                    key={1}>
                    ISO-639-1
                  </Link>,
                  <Link
                    href="https://iso.org/iso-3166-country-codes.html"
                    target="_blank"
                    key={2}>
                    ISO-3166
                  </Link>
                ]}/>
            </FieldDescription>
          </Field>
        </div>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline">{$("dialog.cancel")}</Button>
          </DialogClose>
          <Button
            disabled={inputtedLang === ""}
            onClick={() => handleAction()}>
            {$("dialog.create")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
