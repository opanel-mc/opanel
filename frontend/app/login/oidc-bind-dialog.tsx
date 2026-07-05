import { useState } from "react";
import md5 from "md5";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Field, FieldLabel } from "@/components/ui/field";
import { PasswordInput } from "@/components/password-input";
import { Spinner } from "@/components/ui/spinner";
import { sendPostRequest } from "@/lib/api";
import { $ } from "@/lib/i18n";
import { doAutoUpdateCheck, resetUpdateCheckInfo } from "@/lib/update";

export function OidcBindDialog({
  open,
  onOpenChange,
  onSuccess
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSuccess: () => void
}) {
  const [accessKey, setAccessKey] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleBind = async () => {
    if(accessKey.length === 0) {
      setError($("login.oidc.bind.input.empty"));
      return;
    }

    setLoading(true);
    setError("");
    const hashedKey = md5(md5(accessKey)); // hashed 2

    try {
      await sendPostRequest("/api/auth/oidc/bind-user", { accessKey: hashedKey });
      resetUpdateCheckInfo();
      doAutoUpdateCheck();
      onOpenChange(false);
      onSuccess();
    } catch (e: any) {
      setLoading(false);
      if(e.status === 403) {
        setError($("login.oidc.bind.input.incorrect"));
      } else {
        setError(e.message);
      }
    }
  };

  return (
    <Dialog open={open} onOpenChange={(v) => { if(!loading) onOpenChange(v); }}>
      <DialogContent onOpenAutoFocus={(e) => e.preventDefault()}>
        <DialogHeader>
          <DialogTitle>{$("login.oidc.bind.title")}</DialogTitle>
          <DialogDescription>
            {$("login.oidc.bind.description")}
          </DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-4">
          <Field>
            <FieldLabel>{$("login.oidc.bind.input.label")}</FieldLabel>
            <PasswordInput
              placeholder={$("login.oidc.bind.input.placeholder")}
              autoFocus
              value={accessKey}
              onChange={(e) => { setAccessKey(e.target.value); setError(""); }}
              onKeyDown={(e) => { if(e.key === "Enter") handleBind(); }}/>
            {error && (
              <span className="text-sm text-destructive">{error}</span>
            )}
          </Field>
        </div>
        <DialogFooter>
          <Button
            className="w-full cursor-pointer"
            disabled={loading}
            onClick={handleBind}>
            {loading && <Spinner />}
            {$("login.oidc.bind.confirm")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
