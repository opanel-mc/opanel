"use client";

import { useEffect } from "react";
import Link from "next/link";
import { Info, KeyRound } from "lucide-react";
import md5 from "md5";
import { hasCookie, setCookie } from "cookies-next/client";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
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
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { sendPostRequest } from "@/lib/utils";

const formSchema = z.object({
  accessKey: z.string(),
});

export default function Login() {
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      accessKey: ""
    }
  });

  const handleLogin = async () => {
    const accessKey = form.getValues("accessKey"); // hashed 0
    const hashedKey = md5(accessKey); // hashed 1
    
    try {
      const res = await sendPostRequest<{ token: string }>("/api/auth", { accessKey: hashedKey });
      setCookie("token", res.token);
      toast.success("登录成功");
      window.location.href = "/panel";
    } catch (e: any) {
      if(e.status === 401) {
        form.setError("accessKey", { message: "访问密钥错误" });
      }
    }
  };

  useEffect(() => {
    if(hasCookie("token")) {
      window.location.href = "/panel";
    }

    document.body.addEventListener("keydown", (e) => {
      if(e.key === "Enter") handleLogin();
    });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="flex flex-col">
      <div className="text-center space-y-4 mb-8">
        <h1 className="text-4xl font-bold text-theme">OPanel</h1>
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
                    <FormLabel>访问密钥</FormLabel>
                    <FormControl>
                      <Input
                        type="password"
                        placeholder="请输入访问密钥..."
                        autoFocus
                        {...field}/>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}/>
            </form>
          </Form>
        </CardContent>
        <CardFooter>
          <Button
            className="w-full cursor-pointer"
            onClick={() => handleLogin()}>
            登录
          </Button>
        </CardFooter>
      </Card>
      <div className="flex justify-between items-center mt-2 px-2 text-sm">
        <span className="text-muted-foreground">Copyright &copy; NriotHrreion 2025</span>
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
