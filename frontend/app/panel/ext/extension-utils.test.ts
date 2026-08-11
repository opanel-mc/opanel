import { describe, expect, it } from "vitest";
import { parseExtensionPagePath } from "./extension-utils";

describe("parseExtensionPagePath", () => {
  it.each([
    ["/panel/ext/example-extension", "/"],
    ["/panel/ext/example-extension/", "/"],
    ["/panel/ext/example-extension/test", "/test"],
    ["/panel/ext/example-extension/test/nested", "/test/nested"],
    ["/panel/ext/example-extension/test%20page", "/test%20page"]
  ])("extracts the extension id and resource path from %s", (pathname, resourcePath) => {
    expect(parseExtensionPagePath(pathname)).toEqual({
      extensionId: "example-extension",
      resourcePath
    });
  });

  it.each([
    "/panel/ext",
    "/panel/ext/",
    "/panel/ext//test",
    "/panel/ext/Example-Extension/test",
    "/panel/ext/example%2Fextension/test",
    "/panel/ext/%/test"
  ])("rejects a missing or invalid extension id in %s", (pathname) => {
    expect(parseExtensionPagePath(pathname)).toBeNull();
  });
});
