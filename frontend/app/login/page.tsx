"use client";

import type { OidcConfigResponse } from "@/lib/types";
import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Info, KeyRound } from "lucide-react";
import md5 from "md5";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle
} from "@/components/ui/card";
import {
  Form,
  FormField,
  FormItem,
  FormLabel,
  FormMessage
} from "@/components/ui/form";
import { apiUrl, sendGetRequest, sendPostRequest } from "@/lib/api";
import { Brand } from "@/components/logo";
import { PasswordInput } from "@/components/password-input";
import { Alert } from "@/components/alert";
import { generateRandomString } from "@/lib/utils";
import { Spinner } from "@/components/ui/spinner";
import { copyrightInfo } from "@/lib/global";
import { $ } from "@/lib/i18n";
import { Text } from "@/components/i18n-text";
import { useKeydown } from "@/hooks/use-keydown";
import { useCheckAuth } from "@/hooks/use-check-auth";
import { doAutoUpdateCheck, resetUpdateCheckInfo } from "@/lib/update";
import { OidcBindDialog } from "./oidc-bind-dialog";
import { emitter } from "@/lib/emitter";

function LoginContent() {
  return (
    <Suspense>
      <LoginForm />
    </Suspense>
  );
}

const formSchema = z.object({
  accessKey: z.string().nonempty($("login.form.input.empty")),
});

export default function Login() {
  return <LoginContent />;
}

function LoginForm() {
  const { push } = useRouter();
  const searchParams = useSearchParams();
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      accessKey: ""
    }
  });
  const [loading, setLoading] = useState<boolean>(false);
  const [banned, setBanned] = useState<boolean>(false);

  // OIDC state
  const [oidcConfig, setOidcConfig] = useState<OidcConfigResponse | null>(null);
  const [oidcBindOpen, setOidcBindOpen] = useState<boolean>(false);
  const [oidcError, setOidcError] = useState<boolean>(false);

  // Fetch OIDC config
  const fetchOidcConfig = async () => {
    try {
      const res = await sendGetRequest<OidcConfigResponse>("/api/auth/oidc/config", false);
      setOidcConfig({ enabled: res.enabled, displayName: res.displayName });
    } catch {
      setOidcConfig({ enabled: false });
    }
  };

  const handleLogin = async () => {
    const accessKey = form.getValues("accessKey"); // hashed 0
    if(accessKey.length === 0) {
      form.setError("accessKey", { message: $("login.form.input.empty") });
      return;
    }
    
    setLoading(true);
    const hashedKey = md5(md5(accessKey)); // hashed 2
    
    try {
      const id = generateRandomString(5);
      const { cram } = await sendGetRequest<{ cram: string }>(`/api/auth?id=${id}`, false);
      const challengeResult = md5(hashedKey + cram); // hashed 3

      await sendPostRequest("/api/auth", { id, result: challengeResult });
      resetUpdateCheckInfo();
      doAutoUpdateCheck();
      push("/panel/dashboard");
    } catch (e: any) {
      setLoading(false);
      switch(e.status) {
        case 401:
          form.setError("accessKey", { message: $("login.form.input.incorrect") });
          break;
        case 403:
          setBanned(true);
          form.setError("accessKey", { message: $("login.form.input.temporarily-banned") });
          break;
      }
    }
  };

  const handleOidcLogin = () => {
    window.location.href = apiUrl + "/api/auth/oidc/login";
  };

  // Fetch OIDC config on mount
  useEffect(() => {
    fetchOidcConfig();
  }, []);

  // Handle OIDC callback query params
  useEffect(() => {
    if(searchParams.get("oidc-error") === "true") {
      setOidcError(true);
    }
    if(searchParams.get("oidc-bind") === "true") {
      setOidcBindOpen(true);
    }
  }, [searchParams]);

  useCheckAuth(
    () => push("/panel/dashboard"),
    () => emitter.emit("loading-done")
  );

  useKeydown("Enter", {}, () => {
    if(!oidcConfig?.enabled) handleLogin();
  });

  const isOidc = oidcConfig?.enabled;

  return (
    <div className="flex flex-col">
      <div className="flex flex-col items-center gap-8 mb-8">
        <Brand className="[&_svg]:w-72"/>
        <p className="text-lg text-muted-foreground">{$("login.title")}</p>
      </div>
      <Card className="w-96">
        <CardHeader>
          <CardTitle className="flex gap-2 items-center mb-1">
            <KeyRound />
            <span>{$("login.form.title")}</span>
          </CardTitle>
          {isOidc ? (
            <CardDescription>
              {$("login.form.oidc-description", oidcConfig.displayName || "OIDC")}
            </CardDescription>
          ) : (
            <CardDescription>
              {$("login.form.description")}
            </CardDescription>
          )}
        </CardHeader>
        <CardContent>
          {oidcConfig === null ? (
            <div className="flex justify-center py-4">
              <Spinner />
            </div>
          ) : isOidc ? (
            <div className="flex flex-col gap-3">
              {oidcError && (
                <p className="text-sm text-destructive text-center">
                  {$("login.oidc.error")}
                </p>
              )}
              <Button
                className="w-full cursor-pointer"
                variant="outline"
                onClick={handleOidcLogin}>
                {$("login.oidc.button", oidcConfig.displayName || "OIDC")}
              </Button>
            </div>
          ) : (
            <Form {...form}>
              <form onSubmit={(e) => e.preventDefault()} suppressHydrationWarning>
                <FormField
                  control={form.control}
                  name="accessKey"
                  render={({ field }) => (
                    <FormItem>
                      <div className="flex justify-between">
                        <FormLabel>{$("login.form.input.label")}</FormLabel>
                        <Alert
                          title={$("login.reset.title")}
                          description={
                            <>
                              <Text id="login.reset.content.line1"/>
                              <br /><br />
                              <span>
                                <Text id="login.reset.content.line2"/>
                                <Link href="https://opanel.cn/docs/quick-start#%E4%BD%BF%E7%94%A8" target="_blank" rel="noopener noreferrer">https://opanel.cn/docs/quick-start#使用</Link>
                              </span>
                              <br />
                              <span>
                                <Text id="login.reset.content.line3"/>
                                <Link href="https://opanel.cn/docs/configuration" target="_blank" rel="noopener noreferrer">https://opanel.cn/docs/configuration</Link>
                              </span>
                            </>
                          }
                          asChild
                          cancellable={false}>
                          <span className="text-right text-sm text-muted-foreground cursor-pointer">
                            {$("login.form.input.forgot-access-key")}
                          </span>
                        </Alert>
                      </div>
                      <PasswordInput
                        placeholder={$("login.form.input.placeholder")}
                        autoFocus
                        {...field}/>
                      <FormMessage />
                    </FormItem>
                  )}/>
              </form>
            </Form>
          )}
        </CardContent>
        {!isOidc && oidcConfig !== null && (
          <CardFooter>
            <Button
              className="w-full cursor-pointer"
              disabled={loading || banned}
              onClick={() => handleLogin()}>
              {loading && <Spinner />}
              {$("login.form.enter")}
            </Button>
          </CardFooter>
        )}
      </Card>
      <div className="flex justify-between items-center mt-2 px-2 text-sm">
        <span className="text-muted-foreground">{copyrightInfo}</span>
        <Button variant="link" size="sm" asChild>
          <Link href="/about">
            <Info />
            {$("login.footer.about")}
          </Link>
        </Button>
      </div>
      <OidcBindDialog
        open={oidcBindOpen}
        onOpenChange={setOidcBindOpen}
        onSuccess={() => push("/panel/dashboard")} />
    </div>
  );
}
