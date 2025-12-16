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
import { $ } from "@/lib/i18n";

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
      toastError(e, $("players.banned-ips.fetch.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")]
      ]);
    }
  };

  const banIp = async (ip: string) => {
    if(ip === "" || !validateIpv4Address(ip)) {
      toast.error($("players.banned-ips.add.error"), { description: $("players.banned-ips.add.error.400") });
      return;
    }
    if(bannedIps.includes(ip)) {
      toast.warning($("players.banned-ips.add.exist"));
      return;
    }
    
    try {
      await sendPostRequest(`/api/banned-ips/add?ip=${ip}`);
      setInputtedIp("");
      emitter.emit("refresh-data");
      toast.success($("players.banned-ips.add.success"));
    } catch (e: any) {
      toastError(e, $("players.banned-ips.add.error"), [
        [400, $("players.banned-ips.add.error.400")],
        [401, $("common.error.401")]
      ]);
    }
  };

  const pardonIp = async (ip: string) => {
    try {
      await sendPostRequest(`/api/banned-ips/remove?ip=${ip}`);
      emitter.emit("refresh-data");
      toast.success($("players.banned-ips.pardon.success"));
    } catch (e: any) {
      toastError(e, $("players.banned-ips.pardon.error"), [
        [400, $("players.banned-ips.pardon.error.400")],
        [401, $("common.error.401")]
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
          <DialogTitle>{$("players.banned-ips.title")}</DialogTitle>
          <DialogDescription>
            {$("players.banned-ips.description")}
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
                        title={$("players.banned-ips.pardon")}
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
              placeholder={$("players.banned-ips.input.placeholder")}
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
            <Button variant="outline">{$("dialog.close")}</Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
