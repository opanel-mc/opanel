"use client";

import type { APIResponse, VersionResponse } from "@/lib/types";
import { useEffect, useState } from "react";
import { AppSidebar } from "@/components/app-sidebar";
import {
  SidebarInset,
  SidebarProvider
} from "@/components/ui/sidebar";
import { VersionContext } from "@/contexts/api-context";
import { sendGetRequest } from "@/lib/api";
import { useKeydown } from "@/hooks/use-keydown";
import { useCheckAuth } from "@/hooks/use-check-auth";

export default function PanelLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const [mounted, setMounted] = useState(false);
  const [versionInfo, setVersionInfo] = useState<APIResponse<VersionResponse>>();

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

  useEffect(() => {
    setMounted(true);
  }, []);

  useCheckAuth(() => fetchVersionInfo());

  useKeydown("a", { ctrl: true }, (e) => e.preventDefault());
  useKeydown("p", { ctrl: true }, (e) => e.preventDefault());
  useKeydown("s", { ctrl: true }, (e) => e.preventDefault());

  if(!mounted) return <></>;

  return (
    <SidebarProvider className="overflow-hidden">
      <VersionContext value={versionInfo}>
        <AppSidebar />
        <SidebarInset className="min-w-0">
          {children}
        </SidebarInset>
      </VersionContext>
    </SidebarProvider>
  );
}
