"use client";

import type { PropsWithChildren } from "react";
import type { BackupInfo, BackupListResponse, BackupRestoreResponse } from "@/lib/types";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Archive, Clock, Loader2, RotateCcw, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    DialogTrigger
} from "@/components/ui/dialog";
import { sendDeleteRequest, sendGetRequest, sendPostRequest, toastError } from "@/lib/api";
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
import { cn, formatDataSize } from "@/lib/utils";
import { googleSansCode } from "@/lib/fonts";
import { $ } from "@/lib/i18n";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { emitter } from "@/lib/emitter";
import {
    Tooltip,
    TooltipContent,
    TooltipTrigger
} from "@/components/ui/tooltip";

export function BackupSheet({
    children,
    asChild
}: PropsWithChildren & {
    asChild?: boolean
}) {
    const [open, setOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const [backups, setBackups] = useState<BackupInfo[]>([]);
    const [configured, setConfigured] = useState(false);

    useEffect(() => {
        if (open) {
            fetchBackups();
        }
    }, [open]);

    const fetchBackups = async () => {
        setLoading(true);
        try {
            const res = await sendGetRequest<BackupListResponse>("/api/backup/list");
            setBackups(res.backups);
            setConfigured(res.configured);
        } catch (e: any) {
            toastError(e, $("backup.list.fetch.error"), [
                [401, $("common.error.401")]
            ]);
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (fileName: string) => {
        try {
            await sendDeleteRequest(`/api/backup/${encodeURIComponent(fileName)}`);
            toast.success($("backup.list.delete.success"));
            fetchBackups();
        } catch (e: any) {
            toastError(e, $("backup.list.delete.error"), [
                [401, $("common.error.401")],
                [404, $("backup.list.delete.error.404")]
            ]);
        }
    };

    const handleRestore = async (fileName: string) => {
        try {
            const res = await sendPostRequest<BackupRestoreResponse>(`/api/backup/restore/${encodeURIComponent(fileName)}`);
            toast.success($("backup.list.restore.success", res.saveName));
            emitter.emit("refresh-data");
        } catch (e: any) {
            toastError(e, $("backup.list.restore.error"), [
                [400, $("backup.list.restore.error.not-configured")],
                [401, $("common.error.401")],
                [404, $("backup.list.restore.error.404")],
                [500, $("common.error.500")]
            ]);
        }
    };

    const formatTimestamp = (timestamp: number) => {
        const date = new Date(timestamp);
        return date.toLocaleString();
    };

    return (
        <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild={asChild}>{children}</DialogTrigger>
            <DialogContent className="max-w-lg max-h-[80vh] overflow-y-auto">
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-2">
                        <Archive className="w-5 h-5" />
                        {$("backup.list.title")}
                    </DialogTitle>
                    <DialogDescription>
                        {configured
                            ? $("backup.list.description")
                            : $("backup.not-configured.description")}
                    </DialogDescription>
                </DialogHeader>

                <div className="mt-4 flex flex-col gap-3">
                    {loading ? (
                        <div className="flex justify-center items-center py-12">
                            <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
                        </div>
                    ) : !configured ? (
                        <div className="flex flex-col items-center justify-center py-12 text-center">
                            <Archive className="w-12 h-12 text-muted-foreground mb-4" />
                            <p className="text-muted-foreground">{$("backup.not-configured")}</p>
                            <p className="text-sm text-muted-foreground mt-2">{$("backup.not-configured.description")}</p>
                        </div>
                    ) : backups.length === 0 ? (
                        <div className="flex flex-col items-center justify-center py-12 text-center">
                            <Archive className="w-12 h-12 text-muted-foreground mb-4" />
                            <p className="text-muted-foreground">{$("backup.list.empty")}</p>
                        </div>
                    ) : (
                        backups.map((backup, i) => (
                            <Card key={i} className="p-3 dark:bg-transparent hover:bg-muted transition-colors">
                                <div className="flex flex-col gap-2">
                                    <div className="flex justify-between items-start">
                                        <div className="flex flex-col gap-1 overflow-hidden">
                                            <span className="font-medium text-sm truncate" title={backup.fileName}>
                                                {backup.fileName}
                                            </span>
                                            <div className="flex items-center gap-2 text-xs text-muted-foreground">
                                                <Badge variant="outline" className="text-xs">
                                                    {backup.saveName}
                                                </Badge>
                                                <span className={cn("text-xs", googleSansCode.className)}>
                                                    {formatDataSize(backup.fileSize)}
                                                </span>
                                            </div>
                                        </div>
                                        <div className="flex items-center gap-1 [&_button]:cursor-pointer">
                                            <AlertDialog>
                                                <Tooltip>
                                                    <AlertDialogTrigger asChild>
                                                        <TooltipTrigger asChild>
                                                            <Button
                                                                variant="ghost"
                                                                size="icon">
                                                                <RotateCcw className="w-4 h-4" />
                                                            </Button>
                                                        </TooltipTrigger>
                                                    </AlertDialogTrigger>
                                                    <TooltipContent>
                                                        {$("backup.list.restore")}
                                                    </TooltipContent>
                                                </Tooltip>
                                                <AlertDialogContent>
                                                    <AlertDialogHeader>
                                                        <AlertDialogTitle className="wrap-anywhere">
                                                            {$("backup.list.restore.alert.title", backup.fileName)}
                                                        </AlertDialogTitle>
                                                        <AlertDialogDescription>
                                                            {$("backup.list.restore.alert.description")}
                                                        </AlertDialogDescription>
                                                    </AlertDialogHeader>
                                                    <AlertDialogFooter>
                                                        <AlertDialogCancel>{$("dialog.cancel")}</AlertDialogCancel>
                                                        <AlertDialogAction onClick={() => handleRestore(backup.fileName)}>
                                                            {$("dialog.confirm")}
                                                        </AlertDialogAction>
                                                    </AlertDialogFooter>
                                                </AlertDialogContent>
                                            </AlertDialog>
                                            <AlertDialog>
                                                <Tooltip>
                                                    <AlertDialogTrigger asChild>
                                                        <TooltipTrigger asChild>
                                                            <Button
                                                                variant="ghost"
                                                                size="icon">
                                                                <Trash2 className="w-4 h-4" />
                                                            </Button>
                                                        </TooltipTrigger>
                                                    </AlertDialogTrigger>
                                                    <TooltipContent>
                                                        {$("backup.list.delete")}
                                                    </TooltipContent>
                                                </Tooltip>
                                                <AlertDialogContent>
                                                    <AlertDialogHeader>
                                                        <AlertDialogTitle className="wrap-anywhere">
                                                            {$("backup.list.delete.alert.title", backup.fileName)}
                                                        </AlertDialogTitle>
                                                        <AlertDialogDescription>
                                                            {$("backup.list.delete.alert.description")}
                                                        </AlertDialogDescription>
                                                    </AlertDialogHeader>
                                                    <AlertDialogFooter>
                                                        <AlertDialogCancel>{$("dialog.cancel")}</AlertDialogCancel>
                                                        <AlertDialogAction onClick={() => handleDelete(backup.fileName)}>
                                                            {$("dialog.confirm")}
                                                        </AlertDialogAction>
                                                    </AlertDialogFooter>
                                                </AlertDialogContent>
                                            </AlertDialog>
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-1 text-xs text-muted-foreground">
                                        <Clock className="w-3 h-3" />
                                        <span>{formatTimestamp(backup.timestamp)}</span>
                                    </div>
                                </div>
                            </Card>
                        ))
                    )}
                </div>
            </DialogContent>
        </Dialog>
    );
}
