"use client";

import type { Plugin } from "@/lib/types";
import { type PropsWithChildren, useEffect, useState } from "react";
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
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "@/components/ui/select";
import { toastError } from "@/lib/api";
import { $ } from "@/lib/i18n";
import { base64ToString } from "@/lib/utils";
import { fetchPluginUpdateBindings, removePluginUpdateBinding, savePluginUpdateBinding } from "./plugin-utils";

const CHANNELS = ["release", "beta", "alpha"] as const;

export function BindingDialog({
  plugin,
  children,
  asChild
}: PropsWithChildren & {
  plugin: Plugin
  asChild?: boolean
}) {
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [source, setSource] = useState("hangar");
  const [projectId, setProjectId] = useState("");
  const [owner, setOwner] = useState("");
  const [repo, setRepo] = useState("");
  const [assetPattern, setAssetPattern] = useState(".*\\.jar$");
  const [channels, setChannels] = useState<string[]>(["release"]);

  const fileName = base64ToString(plugin.fileName);

  const load = async () => {
    try {
      const bindings = await fetchPluginUpdateBindings();
      const binding = bindings.find((item) => base64ToString(item.fileName) === fileName);
      if(binding) {
        setSource(binding.source);
        setProjectId(binding.projectId ?? "");
        setOwner(binding.owner ?? "");
        setRepo(binding.repo ?? "");
        setAssetPattern(binding.assetPattern ?? ".*\\.jar$");
        setChannels(binding.channels.length > 0 ? binding.channels : ["release"]);
      } else {
        setSource("hangar");
        setProjectId("");
        setOwner("");
        setRepo("");
        setAssetPattern(".*\\.jar$");
        setChannels(["release"]);
      }
    } catch {
      // Keep the current form state when the binding list cannot be loaded.
    }
  };

  useEffect(() => {
    if(open) load();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const toggleChannel = (channel: string) => {
    setChannels((prev) => (
      prev.includes(channel) ? prev.filter((c) => c !== channel) : [...prev, channel]
    ));
  };

  const handleSave = async () => {
    if(source === "hangar" && !projectId.trim()) {
      toast.error($("plugins.bind.error.required"));
      return;
    }
    if(source === "github" && (!owner.trim() || !repo.trim())) {
      toast.error($("plugins.bind.error.required"));
      return;
    }

    setSaving(true);
    try {
      await savePluginUpdateBinding({
        fileName,
        source,
        projectId: source === "hangar" ? projectId.trim() : null,
        owner: source === "github" ? owner.trim() : null,
        repo: source === "github" ? repo.trim() : null,
        assetPattern: source === "github" ? (assetPattern.trim() || ".*\\.jar$") : null,
        channels: channels.length > 0 ? channels : ["release"]
      });
      toast.success($("plugins.bind.save.success"));
      setOpen(false);
    } catch (e: any) {
      toastError(e, $("plugins.bind.error.save"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    } finally {
      setSaving(false);
    }
  };

  const handleRemove = async () => {
    setSaving(true);
    try {
      await removePluginUpdateBinding(fileName);
      toast.success($("plugins.bind.remove.success"));
      setOpen(false);
    } catch (e: any) {
      toastError(e, $("plugins.bind.error.remove"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    } finally {
      setSaving(false);
    }
  };

  const hasBinding = plugin.source != null && plugin.source !== "unbound";

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild={asChild}>{children}</DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{$("plugins.bind.title")}</DialogTitle>
          <DialogDescription>{$("plugins.bind.description", plugin.name)}</DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-3">
          <div className="flex flex-col gap-1.5">
            <Label>{$("plugins.bind.source")}</Label>
            <Select value={source} onValueChange={setSource}>
              <SelectTrigger><SelectValue/></SelectTrigger>
              <SelectContent>
                <SelectItem value="hangar">Hangar</SelectItem>
                <SelectItem value="github">GitHub</SelectItem>
              </SelectContent>
            </Select>
          </div>
          {
            source === "hangar"
            ? (
              <div className="flex flex-col gap-1.5">
                <Label>{$("plugins.bind.project-id")}</Label>
                <Input
                  value={projectId}
                  placeholder="ViaVersion"
                  onChange={(e) => setProjectId(e.target.value)}/>
              </div>
            )
            : (
              <>
                <div className="flex flex-col gap-1.5">
                  <Label>{$("plugins.bind.owner")}</Label>
                  <Input
                    value={owner}
                    placeholder="opanel-mc"
                    onChange={(e) => setOwner(e.target.value)}/>
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label>{$("plugins.bind.repo")}</Label>
                  <Input
                    value={repo}
                    placeholder="opanel"
                    onChange={(e) => setRepo(e.target.value)}/>
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label>{$("plugins.bind.asset-pattern")}</Label>
                  <Input
                    value={assetPattern}
                    placeholder=".*\.jar$"
                    onChange={(e) => setAssetPattern(e.target.value)}/>
                </div>
              </>
            )
          }
          <div className="flex flex-col gap-1.5">
            <Label>{$("plugins.bind.channels")}</Label>
            <div className="flex gap-2">
              {CHANNELS.map((channel) => (
                <Button
                  key={channel}
                  type="button"
                  size="sm"
                  className="cursor-pointer"
                  variant={channels.includes(channel) ? "default" : "outline"}
                  onClick={() => toggleChannel(channel)}>
                  {$(`plugins.update.channel.${channel}` as never)}
                </Button>
              ))}
            </div>
          </div>
        </div>
        <DialogFooter className="justify-between!">
          {
            hasBinding && (
              <Button
                variant="ghost"
                className="cursor-pointer text-destructive"
                disabled={saving}
                onClick={handleRemove}>
                {$("plugins.bind.remove")}
              </Button>
            )
          }
          <div className="flex gap-2">
            <DialogClose asChild>
              <Button variant="outline">{$("dialog.close")}</Button>
            </DialogClose>
            <Button className="cursor-pointer" disabled={saving} onClick={handleSave}>
              {$("plugins.bind.save")}
            </Button>
          </div>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
