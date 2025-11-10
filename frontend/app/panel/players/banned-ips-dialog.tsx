import type { BannedIpsResponse } from "@/lib/types";
import { type PropsWithChildren, useEffect, useState } from "react";
import { Plus, ShieldOff } from "lucide-react";
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
import {
  Table,
  TableBody,
  TableCell,
  TableRow
} from "@/components/ui/table";
import { Input } from "@/components/ui/input";
import { sendGetRequest, sendPostRequest, toastError } from "@/lib/api";
import { cn, validateIpv4Address } from "@/lib/utils";
import { emitter } from "@/lib/emitter";

export function BannedIpsDialog({
  children,
  asChild
}: PropsWithChildren & {
  asChild?: boolean
}) {
  const [dialogOpen, setDialogOpen] = useState<boolean>(false);
  const [bannedIps, setBannedIps] = useState<string[]>([]);
  const [inputtedIp, setInputtedIp] = useState<string>("");

  const fetchBannedIps = async () => {
    try {
      const res = await sendGetRequest<BannedIpsResponse>("/api/banned-ips");
      setBannedIps(res.bannedIps);
    } catch (e: any) {
      toastError(e, "无法获取封禁IP列表", [
        [400, "请求参数错误"],
        [401, "未登录"]
      ]);
    }
  };

  const banIp = async (ip: string) => {
    if(ip === "" || !validateIpv4Address(ip)) {
      toast.error("无法添加该IP到封禁列表", { description: "无效的IP地址" });
      return;
    }
    if(bannedIps.includes(ip)) {
      toast.warning("该IP已在封禁列表中");
      return;
    }
    
    try {
      await sendPostRequest(`/api/banned-ips/add?ip=${ip}`);
      setInputtedIp("");
      emitter.emit("refresh-data");
      toast.success("添加成功");
    } catch (e: any) {
      toastError(e, "无法添加该IP到封禁列表", [
        [400, "无效的IP地址"],
        [401, "未登录"]
      ]);
    }
  };

  const pardonIp = async (ip: string) => {
    try {
      await sendPostRequest(`/api/banned-ips/remove?ip=${ip}`);
      emitter.emit("refresh-data");
      toast.success("移除成功");
    } catch (e: any) {
      toastError(e, "无法从封禁列表移除该IP", [
        [400, "无效的IP地址"],
        [401, "未登录"]
      ]);
    }
  };

  useEffect(() => {
    emitter.on("refresh-data", () => {
      fetchBannedIps();
    });
  }, []);

  useEffect(() => {
    if(dialogOpen) {
      fetchBannedIps();
    } else {
      setInputtedIp("");
    }
  }, [dialogOpen]);

  return (
    <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
      <DialogTrigger asChild={asChild}>{children}</DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>管理封禁IP</DialogTitle>
          <DialogDescription>
            在此添加 / 移除被封禁的IP地址。
          </DialogDescription>
        </DialogHeader>
        <div className="border rounded-md">
          <div className="max-h-64 overflow-y-auto">
            <Table>
              <TableBody>
                {bannedIps.map((ip, i) => (
                  <TableRow key={i}>
                    <TableCell>{ip}</TableCell>
                    <TableCell>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="float-right h-4 cursor-pointer hover:!bg-transparent"
                        title="解除封禁"
                        onClick={() => pardonIp(ip)}>
                        <ShieldOff className="stroke-green-600"/>
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
          <div className={cn("flex gap-2 p-2", bannedIps.length > 0 && "border-t")}>
            <Input
              value={inputtedIp}
              placeholder="请输入IP地址以添加..."
              className="h-8"
              onInput={(e) => setInputtedIp((e.target as HTMLInputElement).value)}
              onKeyDown={(e) => (e.key === "Enter" && inputtedIp.length > 0) && banIp(inputtedIp)}/>
            <Button
              variant="ghost"
              size="icon-sm"
              className="cursor-pointer"
              disabled={inputtedIp.length === 0}
              onClick={() => banIp(inputtedIp)}>
              <Plus />
            </Button>
          </div>
        </div>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline">关闭</Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
