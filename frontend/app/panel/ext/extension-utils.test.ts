import { describe, expect, it } from "vitest";
import { getExtensionIframeSrc } from "./extension-utils";

describe("getExtensionIframeSrc", () => {
  it.each([
    ["/panel/ext/example-extension", "/api/extension-res/example-extension/"],
    ["/panel/ext/example-extension/", "/api/extension-res/example-extension/"],
    ["/panel/ext/example-extension/test", "/api/extension-res/example-extension/test"],
    ["/panel/ext/example-extension/test/nested", "/api/extension-res/example-extension/test/nested"],
    ["/panel/ext/example-extension/test%20page", "/api/extension-res/example-extension/test%20page"],
    ["/panel/ext/example-extension/test?key=value", "/api/extension-res/example-extension/test?key=value"],
    ["/panel/ext/example-extension/test?key=value#test", "/api/extension-res/example-extension/test?key=value#test"],
  ])("converts %s to the extension iframe src", (path, expected) => {
    expect(getExtensionIframeSrc(new URL(path, "https://opanel.test"))).toBe(expected);
  });

  it.each([
    "/panel/ext",
    "/panel/ext/",
    "/panel/ext//test",
    "/panel/ext/Example-Extension/test",
    "/panel/ext/example%2Fextension/test",
    "/panel/ext/%/test"
  ])("rejects a missing or invalid extension id in %s", (path) => {
    expect(getExtensionIframeSrc(new URL(path, "https://opanel.test"))).toBeNull();
  });
});
