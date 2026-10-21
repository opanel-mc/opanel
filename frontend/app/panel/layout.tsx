"use client";

import type { APIResponse, ExtensionPage, ExtensionPagesResponse, VersionResponse } from "@/lib/types";
import { useEffect, useState } from "react";
import { AppSidebar } from "@/components/app-sidebar";
import {
  SidebarInset,
  SidebarProvider
} from "@/components/ui/sidebar";
import { ExtensionsContext, VersionContext } from "@/contexts/api-context";
import { sendGetRequest } from "@/lib/api";
import { useKeydown } from "@/hooks/use-keydown";
import { useCheckAuth } from "@/hooks/use-check-auth";
import { changeSettings, getSettings } from "@/lib/settings";
import { emitter } from "@/lib/emitter";

const setCssBg = (url: string | null) => {
  if (typeof document === "undefined") return;
  document.documentElement.style.setProperty(
    "--panel-bg",
    url ? `url(${url})` : "none"
  );
};

export default function PanelLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  const [mounted, setMounted] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [versionInfo, setVersionInfo] = useState<APIResponse<VersionResponse>>();
  const [extensionPages, setExtensionPages] = useState<ExtensionPage[]>([]);

  const fetchVersionInfo = async () => {
    try {
      const res = await sendGetRequest<VersionResponse>("/api/version");
      if(res.version.split(".").length === 2) {
        res.version += ".0";
      }
      setVersionInfo(res);
    } catch (error) {
      console.error("Error fetching version info:", error);
    }
  };

  const fetchExtensionPages = async () => {
    try {
      const res = await sendGetRequest<ExtensionPagesResponse>("/api/extension-res");
      setExtensionPages(res.pages);
    } catch (error) {
      console.error("Error fetching extension pages:", error);
    }
  };

  useEffect(() => {
    const initialBg = getSettings("panel.background") || "/backgrounds/background.jpg";
    setCssBg(initialBg);

    setMounted(true);
    setSidebarOpen(getSettings("state.sidebar.open"));

    const handleBackgroundChange = () => {
      setCssBg(getSettings("panel.background") || null);
    };
    emitter.on("background-change", handleBackgroundChange);
    return () => {
      emitter.off("background-change", handleBackgroundChange);
    };
  }, []);

  useEffect(() => {
    changeSettings("state.sidebar.open", sidebarOpen);
  }, [sidebarOpen]);

  useCheckAuth(() => {
    fetchVersionInfo();
    fetchExtensionPages();
  });

  useKeydown("a", { ctrl: true }, (e) => e.preventDefault());
  useKeydown("p", { ctrl: true }, (e) => e.preventDefault());
  useKeydown("s", { ctrl: true }, (e) => e.preventDefault());

  return (
    <SidebarProvider
      open={sidebarOpen}
      onOpenChange={setSidebarOpen}
      className="h-[100dvh] min-h-0 overflow-hidden">
      <VersionContext value={versionInfo}>
        <ExtensionsContext value={extensionPages}>
          <AppSidebar />
          <SidebarInset className="min-w-0" suppressHydrationWarning>
            {mounted ? children : <></>}
          </SidebarInset>
        </ExtensionsContext>
      </VersionContext>
    </SidebarProvider>
  );
}