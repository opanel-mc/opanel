"use client";

import type { PropsWithChildren } from "react";
import type { BackupConfigResponse } from "@/lib/types";
import { useCallback, useEffect, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";
import {
    Form,
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
    FormDescription
} from "@/components/ui/form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue
} from "@/components/ui/select";
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
import { sendGetRequest, sendPostRequest, toastError } from "@/lib/api";
import { PasswordInput } from "@/components/password-input";
import { $ } from "@/lib/i18n";
import { BackupProviderType } from "@/lib/types";

const formSchema = z.object({
    enabled: z.boolean(),
    providerType: z.string(),
    maxBackups: z.number().min(0),
    localPath: z.string().optional(),
    s3Endpoint: z.string().optional(),
    s3AccessKey: z.string().optional(),
    s3SecretKey: z.string().optional(),
    s3Bucket: z.string().optional(),
    s3Region: z.string().optional(),
    s3Prefix: z.string().optional(),
    webdavUrl: z.string().optional(),
    webdavUsername: z.string().optional(),
    webdavPassword: z.string().optional()
});

export function BackupConfigDialog({
    children,
    asChild
}: PropsWithChildren & {
    asChild?: boolean
}) {
    const [open, setOpen] = useState(false);
    const [loading, setLoading] = useState(false);

    const form = useForm<z.infer<typeof formSchema>>({
        resolver: zodResolver(formSchema),
        defaultValues: {
            enabled: false,
            providerType: BackupProviderType.LOCAL,
            maxBackups: 5,
            localPath: "",
            s3Endpoint: "",
            s3AccessKey: "",
            s3SecretKey: "",
            s3Bucket: "",
            s3Region: "",
            s3Prefix: "",
            webdavUrl: "",
            webdavUsername: "",
            webdavPassword: ""
        }
    });

    const providerType = form.watch("providerType");

    const fetchConfig = useCallback(async () => {
        setLoading(true);
        try {
            const res = await sendGetRequest<BackupConfigResponse>("/api/backup/config");
            const config = res.config;
            form.reset({
                enabled: config.enabled,
                providerType: config.providerType || BackupProviderType.LOCAL,
                maxBackups: config.maxBackups || 5,
                localPath: config.localPath || "",
                s3Endpoint: config.s3Endpoint || "",
                s3AccessKey: config.s3AccessKey || "",
                s3SecretKey: config.s3SecretKey || "",
                s3Bucket: config.s3Bucket || "",
                s3Region: config.s3Region || "",
                s3Prefix: config.s3Prefix || "",
                webdavUrl: config.webdavUrl || "",
                webdavUsername: config.webdavUsername || "",
                webdavPassword: config.webdavPassword || ""
            });
        } catch (e: any) {
            toastError(e, $("backup.config.fetch.error"), [
                [401, $("common.error.401")]
            ]);
        } finally {
            setLoading(false);
        }
    }, [form]);

    useEffect(() => {
        if (open) {
            fetchConfig();
        }
    }, [open, fetchConfig]);

    const handleSubmit = async (values: z.infer<typeof formSchema>) => {
        try {
            await sendPostRequest("/api/backup/config", values);
            toast.success($("backup.config.save.success"));
            setOpen(false);
        } catch (e: any) {
            toastError(e, $("backup.config.save.error"), [
                [401, $("common.error.401")],
                [500, $("common.error.500")]
            ]);
        }
    };

    return (
        <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild={asChild}>{children}</DialogTrigger>
            <DialogContent className="max-w-lg max-h-[80vh] overflow-y-auto">
                {loading ? (
                    <div className="flex justify-center items-center py-12">
                        <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
                    </div>
                ) : (
                    <Form {...form}>
                        <form className="flex flex-col gap-4" onSubmit={form.handleSubmit(handleSubmit)}>
                            <DialogHeader>
                                <DialogTitle>{$("backup.config.title")}</DialogTitle>
                                <DialogDescription>
                                    {$("backup.config.description")}
                                </DialogDescription>
                            </DialogHeader>

                            {/* Enable Backup */}
                            <FormField
                                control={form.control}
                                name="enabled"
                                render={({ field }) => (
                                    <FormItem className="flex flex-row items-center justify-between rounded-lg border p-3">
                                        <div className="space-y-0.5">
                                            <FormLabel>{$("backup.config.enabled")}</FormLabel>
                                        </div>
                                        <FormControl>
                                            <Switch
                                                checked={field.value}
                                                onCheckedChange={field.onChange}
                                            />
                                        </FormControl>
                                    </FormItem>
                                )}
                            />

                            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                                {/* Provider Type */}
                                <FormField
                                    control={form.control}
                                    name="providerType"
                                    render={({ field }) => (
                                        <FormItem>
                                            <FormLabel>{$("backup.config.provider-type")}</FormLabel>
                                            <Select onValueChange={field.onChange} value={field.value}>
                                                <FormControl>
                                                    <SelectTrigger className="w-full cursor-pointer">
                                                        <SelectValue />
                                                    </SelectTrigger>
                                                </FormControl>
                                                <SelectContent>
                                                    <SelectItem value={BackupProviderType.LOCAL}>
                                                        {$("backup.config.provider-type.local")}
                                                    </SelectItem>
                                                    <SelectItem value={BackupProviderType.S3}>
                                                        {$("backup.config.provider-type.s3")}
                                                    </SelectItem>
                                                    <SelectItem value={BackupProviderType.WEBDAV}>
                                                        {$("backup.config.provider-type.webdav")}
                                                    </SelectItem>
                                                </SelectContent>
                                            </Select>
                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />

                                {/* Max Backups */}
                                <FormField
                                    control={form.control}
                                    name="maxBackups"
                                    render={({ field }) => (
                                        <FormItem>
                                            <FormLabel>{$("backup.config.max-backups")}</FormLabel>
                                            <FormControl>
                                                <Input
                                                    type="number"
                                                    min={0}
                                                    {...field}
                                                    onChange={(e) => field.onChange(parseInt(e.target.value) || 0)}
                                                />
                                            </FormControl>
                                            <FormDescription>
                                                {$("backup.config.max-backups.description")}
                                            </FormDescription>
                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />
                            </div>

                            {/* Local Settings */}
                            {providerType === BackupProviderType.LOCAL && (
                                <FormField
                                    control={form.control}
                                    name="localPath"
                                    render={({ field }) => (
                                        <FormItem>
                                            <FormLabel>{$("backup.config.local-path")}</FormLabel>
                                            <FormControl>
                                                <Input
                                                    placeholder={$("backup.config.local-path.placeholder")}
                                                    {...field}
                                                />
                                            </FormControl>
                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />
                            )}

                            {/* S3 Settings */}
                            {providerType === BackupProviderType.S3 && (
                                <>
                                    <FormField
                                        control={form.control}
                                        name="s3Endpoint"
                                        render={({ field }) => (
                                            <FormItem>
                                                <FormLabel>{$("backup.config.s3-endpoint")}</FormLabel>
                                                <FormControl>
                                                    <Input
                                                        placeholder={$("backup.config.s3-endpoint.placeholder")}
                                                        {...field}
                                                    />
                                                </FormControl>
                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />
                                    <div className="grid grid-cols-2 gap-3">
                                        <FormField
                                            control={form.control}
                                            name="s3AccessKey"
                                            render={({ field }) => (
                                                <FormItem>
                                                    <FormLabel>{$("backup.config.s3-access-key")}</FormLabel>
                                                    <FormControl>
                                                        <Input {...field} />
                                                    </FormControl>
                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />
                                        <FormField
                                            control={form.control}
                                            name="s3SecretKey"
                                            render={({ field }) => (
                                                <FormItem>
                                                    <FormLabel>{$("backup.config.s3-secret-key")}</FormLabel>
                                                    <FormControl>
                                                        <PasswordInput {...field} />
                                                    </FormControl>
                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />
                                    </div>
                                    <div className="grid grid-cols-2 gap-3">
                                        <FormField
                                            control={form.control}
                                            name="s3Bucket"
                                            render={({ field }) => (
                                                <FormItem>
                                                    <FormLabel>{$("backup.config.s3-bucket")}</FormLabel>
                                                    <FormControl>
                                                        <Input {...field} />
                                                    </FormControl>
                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />
                                        <FormField
                                            control={form.control}
                                            name="s3Region"
                                            render={({ field }) => (
                                                <FormItem>
                                                    <FormLabel>{$("backup.config.s3-region")}</FormLabel>
                                                    <FormControl>
                                                        <Input {...field} />
                                                    </FormControl>
                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />
                                    </div>
                                    <FormField
                                        control={form.control}
                                        name="s3Prefix"
                                        render={({ field }) => (
                                            <FormItem>
                                                <FormLabel>{$("backup.config.s3-prefix")}</FormLabel>
                                                <FormControl>
                                                    <Input {...field} />
                                                </FormControl>
                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />
                                </>
                            )}

                            {/* WebDAV Settings */}
                            {providerType === BackupProviderType.WEBDAV && (
                                <>
                                    <FormField
                                        control={form.control}
                                        name="webdavUrl"
                                        render={({ field }) => (
                                            <FormItem>
                                                <FormLabel>{$("backup.config.webdav-url")}</FormLabel>
                                                <FormControl>
                                                    <Input
                                                        placeholder={$("backup.config.webdav-url.placeholder")}
                                                        {...field}
                                                    />
                                                </FormControl>
                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />
                                    <div className="grid grid-cols-2 gap-3">
                                        <FormField
                                            control={form.control}
                                            name="webdavUsername"
                                            render={({ field }) => (
                                                <FormItem>
                                                    <FormLabel>{$("backup.config.webdav-username")}</FormLabel>
                                                    <FormControl>
                                                        <Input {...field} />
                                                    </FormControl>
                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />
                                        <FormField
                                            control={form.control}
                                            name="webdavPassword"
                                            render={({ field }) => (
                                                <FormItem>
                                                    <FormLabel>{$("backup.config.webdav-password")}</FormLabel>
                                                    <FormControl>
                                                        <PasswordInput {...field} />
                                                    </FormControl>
                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />
                                    </div>
                                </>
                            )}

                            <DialogFooter className="flex flex-row [&>*]:flex-1 [&_button]:cursor-pointer">
                                <DialogClose asChild>
                                    <Button
                                        variant="outline"
                                        onClick={() => form.reset()}>
                                        {$("dialog.cancel")}
                                    </Button>
                                </DialogClose>
                                <Button type="submit">{$("dialog.save")}</Button>
                            </DialogFooter>
                        </form>
                    </Form>
                )}
            </DialogContent>
        </Dialog>
    );
}
