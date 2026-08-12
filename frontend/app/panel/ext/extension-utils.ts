import { apiUrl } from "@/lib/api";

const EXTENSION_ID_PATTERN = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;

export function getExtensionIframeSrc(location: Pick<Location, "pathname" | "search" | "hash">): string | null {
  const prefix = "/panel/ext/";
  if(!location.pathname.startsWith(prefix)) return null;

  const [encodedExtensionId, ...resourceParts] = location.pathname.substring(prefix.length).split("/");

  try {
    const extensionId = decodeURIComponent(encodedExtensionId);
    if(!EXTENSION_ID_PATTERN.test(extensionId)) return null;

    const resourcePath = resourceParts.length === 0 ? "/" : `/${resourceParts.join("/")}`;
    return `${apiUrl}/api/extension-res/${encodeURIComponent(extensionId)}${resourcePath}${location.search}${location.hash}`;
  } catch {
    return null;
  }
}
