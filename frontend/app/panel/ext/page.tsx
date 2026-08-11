"use client";

import { useEffect, useMemo } from "react";
import { SubPage } from "../sub-page";
import { apiUrl } from "@/lib/api";
import { emitter } from "@/lib/emitter";
import { parseExtensionPagePath } from "./extension-utils";

export default function ExtensionPage() {
  const pagePath = useMemo(() => parseExtensionPagePath(window.location.pathname), []);

  useEffect(() => {
    emitter.emit("loading-done");
  }, []);

  return (
    <SubPage
      title=""
      showHeader={false}
      className="min-h-0 bg-background p-0!">
      {pagePath && (
        <iframe
          className="size-full border-0"
          src={`${apiUrl}/api/extension-res/${encodeURIComponent(pagePath.extensionId)}${pagePath.resourcePath}`}
          title={`Extension: ${pagePath.extensionId}`}/>
      )}
    </SubPage>
  );
}
