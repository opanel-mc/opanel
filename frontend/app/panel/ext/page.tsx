"use client";

import { useEffect, useMemo } from "react";
import { SubPage } from "../sub-page";
import { emitter } from "@/lib/emitter";
import { getExtensionIframeSrc } from "./extension-utils";

export default function ExtensionPage() {
  const iframeSrc = useMemo(() => getExtensionIframeSrc(window.location), []);

  useEffect(() => {
    emitter.emit("loading-done");
  }, []);

  return (
    <SubPage
      title=""
      showHeader={false}
      className="min-h-0 bg-background p-0!">
      {iframeSrc && (
        <iframe
          className="size-full border-0"
          src={iframeSrc}
          title="Extension"/>
      )}
    </SubPage>
  );
}
