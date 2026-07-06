"use client";

import { Activity } from "lucide-react";
import { $ } from "@/lib/i18n";
import { SubPage } from "../sub-page";
import { useLoadingDone } from "@/hooks/use-loading-done";

export default function Monitor() {
  useLoadingDone();

  return (
    <SubPage
      title="监控"
      category={$("sidebar.server")}
      icon={<Activity />}>
      {/** @todo */}
    </SubPage>
  );
}
