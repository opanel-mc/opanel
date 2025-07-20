"use client";

import { useContext } from "react";
import { Power, RotateCw } from "lucide-react";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { apiUrl, sendPostRequest } from "@/lib/api";
import { InfoContext } from "@/contexts/api-context";
import { MinecraftText } from "@/components/mc-text";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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
} from "@/components/ui/alert-dialog";

import PackIcon from "@/assets/images/pack.png";
import { toast } from "sonner";

export function InfoCard({
  className,
}: Readonly<{
  className?: string
}>) {
  const ctx = useContext(InfoContext);

  return (
    <Card className={cn(className, "flex flex-row rounded-md")}>
      <img
        className="aspect-square h-full rounded-xs"
        src={(ctx && ctx.favicon) ? (apiUrl + ctx.favicon) : PackIcon.src}
        alt="favicon"/>
      
      <div className="flex-1 flex flex-col gap-2">
        <div className="flex gap-4 [&>*]:space-x-2">
          <div>
            <span className="font-semibold">版本:</span>
            <span>{ctx?.version}</span>
          </div>
          <div>
            <span className="font-semibold">端口:</span>
            <span className="text-emerald-500 font-[Consolas]">{ctx ? ctx.port : ""}</span>
          </div>
        </div>
        <div className="h-fit text-sm">
          {ctx && <MinecraftText maxLines={2}>{"§7"+ atob(ctx.motd)}</MinecraftText>}
        </div>
      </div>
      <div className="flex flex-col justify-between">
        <Badge className="self-end" variant="outline">
          <div className={cn("w-2 h-2 rounded-full", ctx ? "bg-green-600" : "bg-red-700")}/>
          {ctx ? "正在运行" : "未运行"}
        </Badge>
        <div className="space-x-1 self-end [&>*]:cursor-pointer">
          <Button
            variant="ghost"
            size="icon"
            title="重载服务器"
            onClick={() => toast.promise(sendPostRequest("/api/control/reload"), {
              loading: "正在重载服务器...",
              success: "重载完毕",
              error: "重载失败"
            })}>
            <RotateCw />
          </Button>
          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button
                variant="outline"
                size="icon"
                title="停止服务器">
                <Power />
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>确定要停止服务器吗？</AlertDialogTitle>
                <AlertDialogDescription>
                  此操作将保存所有服务器信息并关闭服务器，OPanel也将无法访问，之后您可以从后台手动启动服务器。
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>取消</AlertDialogCancel>
                <AlertDialogAction
                  onClick={() => {
                    sendPostRequest("/api/control/stop");
                    toast.loading("正在停止服务器...");
                  }}>
                  确定
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </div>
      </div>
    </Card>
  );
}
