"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { hasCookie } from "cookies-next/client";
import { AppSidebar } from "@/components/app-sidebar";
import {
  SidebarInset,
  SidebarProvider
} from "@/components/ui/sidebar";

export default function PanelLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const [mounted, setMounted] = useState(false);
  const { push } = useRouter();

  useEffect(() => {
    setMounted(true);

    if(!hasCookie("token")) {
      push("/login");
    }
  }, [push]);

  if(!mounted) return <></>;

  return (
    <SidebarProvider className="overflow-x-hidden">
      <AppSidebar />
      <SidebarInset className="min-w-0">
        {children}
      </SidebarInset>
    </SidebarProvider>
  );
}
