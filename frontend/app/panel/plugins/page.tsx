"use client";

import { Blocks } from "lucide-react";
import { SubPage } from "../sub-page";
import { $ } from "@/lib/i18n";

export default function Plugins() {
  return (
    <SubPage title={$("plugins.title")} icon={<Blocks />}>
      <div className="flex justify-center">
        <span className="text-muted-foreground">{$("plugins.wip")}</span>
      </div>
    </SubPage>
  );
}
