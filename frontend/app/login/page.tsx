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
import { sendGetRequestWithoutToken, sendPostRequestWithoutToken } from "@/lib/api";
import { Brand } from "@/components/logo";
import { PasswordInput } from "@/components/password-input";
import { Alert } from "@/components/alert";
import { generateRandomString } from "@/lib/utils";
import { Spinner } from "@/components/ui/spinner";

const formSchema = z.object({
  accessKey: z.string().nonempty("此项不可为空"),
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
    setLoading(true);
    const accessKey = form.getValues("accessKey"); // hashed 0
    const hashedKey = md5(md5(accessKey)); // hashed 2
    
    try {
      const id = generateRandomString(5);
      const { cram } = await sendGetRequestWithoutToken<{ cram: string }>(`/api/auth?id=${id}`);
      const challengeResult = md5(hashedKey + cram); // hashed 3

      const res = await sendPostRequestWithoutToken<{ token: string }>("/api/auth", { id, result: challengeResult });
      setCookie("token", res.token);
      router.push("/panel");
    } catch (e: any) {
      setLoading(false);
      switch(e.status) {
        case 401:
          form.setError("accessKey", { message: "访问密钥错误" });
          break;
        case 403:
          setBanned(true);
          form.setError("accessKey", { message: "验证失败次数过多，请稍后再试" });
          break;
      }
    }
  };

  useEffect(() => {
    if(hasCookie("token")) {
      router.push("/panel");
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
        <p className="text-lg text-muted-foreground">Minecraft 服务器管理面板</p>
      </div>
      <Card className="w-96">
        <CardHeader>
          <CardTitle className="flex gap-2 items-center mb-1">
            <KeyRound />
            <span>登录到 OPanel</span>
          </CardTitle>
          <CardDescription>
            你需要访问密钥才可以登录到OPanel面板
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
                      <FormLabel>访问密钥</FormLabel>
                      <Alert
                        title="重置密钥"
                        description={
                          <>
                            <span>如果你是服务器管理员，且密钥已丢失，可通过<b>删除配置文件</b>并<b>重启服务器</b>来进行密钥重置。</span><br /><br />
                            <span>具体步骤请参见文档：<Link href="https://opanel.cn/docs/quick-start.html#%E4%BD%BF%E7%94%A8" target="_blank">https://opanel.cn/docs/quick-start.html#使用</Link></span><br />
                            <span>配置文件路径请参见：<Link href="https://opanel.cn/docs/configuration.html" target="_blank">https://opanel.cn/docs/configuration.html</Link></span>
                          </>
                        }
                        asChild
                        cancellable={false}>
                        <span className="text-right text-sm text-muted-foreground cursor-pointer">忘记密钥？</span>
                      </Alert>
                    </div>
                    <PasswordInput
                      placeholder="请输入访问密钥..."
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
            登录
          </Button>
        </CardFooter>
      </Card>
      <div className="flex justify-between items-center mt-2 px-2 text-sm">
        <span className="text-muted-foreground">Copyright &copy; OPanel Project 2025</span>
        <Button variant="link" size="sm" asChild>
          <Link href="/about">
            <Info />
            关于
          </Link>
        </Button>
      </div>
    </div>
  );
}
