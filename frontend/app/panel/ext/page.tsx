"use client";

import { useEffect, useState } from "react";
import { SubPage } from "../sub-page";
import { apiUrl } from "@/lib/api";
import { emitter } from "@/lib/emitter";

const EXTENSION_ID_PATTERN = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;

function getExtensionId(pathname: string): string | null {
  const prefix = "/panel/ext/";
  if(!pathname.startsWith(prefix)) return null;

  const path = pathname.substring(prefix.length).replace(/\/$/, "");
  if(path.includes("/")) return null;

  try {
    const extensionId = decodeURIComponent(path);
    return EXTENSION_ID_PATTERN.test(extensionId) ? extensionId : null;
  } catch {
    return null;
  }
}

export default function ExtensionPage() {
  const [extensionId, setExtensionId] = useState<string | null>(null);

  useEffect(() => {
    setExtensionId(getExtensionId(window.location.pathname));
    emitter.emit("loading-done");
  }, []);

  return (
    <SubPage
      title=""
      showHeader={false}
      className="min-h-0 bg-background p-0!">
      {extensionId && (
        <iframe
          className="size-full border-0"
          src={`${apiUrl}/api/extension-res/${encodeURIComponent(extensionId)}/`}
          title={`Extension: ${extensionId}`}/>
      )}
    </SubPage>
  );
}
