const EXTENSION_ID_PATTERN = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;

export function parseExtensionPagePath(pathname: string) {
  const prefix = "/panel/ext/";
  if(!pathname.startsWith(prefix)) return null;

  const [encodedExtensionId, ...resourceParts] = pathname.substring(prefix.length).split("/");

  try {
    const extensionId = decodeURIComponent(encodedExtensionId);
    if(!EXTENSION_ID_PATTERN.test(extensionId)) return null;

    return {
      extensionId,
      resourcePath: resourceParts.length === 0 ? "/" : `/${resourceParts.join("/")}`
    };
  } catch {
    return null;
  }
}
