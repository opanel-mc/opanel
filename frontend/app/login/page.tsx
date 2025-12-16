"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Info, KeyRound } from "lucide-react";
import md5 from "md5";
import { hasCookie, setCookie } from "cookies-next/client";
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
import { sendGetRequest, sendPostRequest } from "@/lib/api";
import { Brand } from "@/components/logo";
import { PasswordInput } from "@/components/password-input";
import { Alert } from "@/components/alert";
import { generateRandomString } from "@/lib/utils";
import { Spinner } from "@/components/ui/spinner";
import { copyrightInfo } from "@/lib/global";
import { $ } from "@/lib/i18n";
import { Text } from "@/components/i18n-text";

const formSchema = z.object({
  accessKey: z.string().nonempty($("login.form.input.empty")),
});

export default function Login() {
  const router = useRouter();
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      accessKey: ""
    }
  });
  const [loading, setLoading] = useState<boolean>(false);
  const [banned, setBanned] = useState<boolean>(false);

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

      const res = await sendPostRequest<{ token: string }>("/api/auth", { id, result: challengeResult }, false);
      setCookie("token", res.token);
      router.push("/panel/dashboard");
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

  useEffect(() => {
    if(hasCookie("token")) {
      router.push("/panel/dashboard");
    }

    document.body.addEventListener("keydown", (e) => {
      if(e.key === "Enter") handleLogin();
    });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

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
          <CardDescription>
            {$("login.form.description")}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={(e) => e.preventDefault()}>
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
                              <Link href="https://opanel.cn/docs/quick-start.html#%E4%BD%BF%E7%94%A8" target="_blank">https://opanel.cn/docs/quick-start.html#使用</Link>
                            </span>
                            <br />
                            <span>
                              <Text id="login.reset.content.line3"/>
                              <Link href="https://opanel.cn/docs/configuration.html" target="_blank">https://opanel.cn/docs/configuration.html</Link>
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
        </CardContent>
        <CardFooter>
          <Button
            className="w-full cursor-pointer"
            disabled={loading || banned}
            onClick={() => handleLogin()}>
            {loading && <Spinner />}
            {$("login.form.enter")}
          </Button>
        </CardFooter>
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
    </div>
  );
}
