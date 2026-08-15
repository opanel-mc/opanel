import { describe, expect, it } from "vitest";
import { patchVinextRscCacheBustingSource } from "./vinext-rsc-cache-busting-fallback";

const VINEXT_MODULE_ID =
  "D:/project/node_modules/vinext/dist/server/app-rsc-cache-busting.js";
const VINEXT_SOURCE = `async function sha256CacheBustingHash(input) {
\tconst digest = await globalThis.crypto.subtle.digest("SHA-256", textEncoder.encode(input));
\treturn encodeBase64Url(new Uint8Array(digest));
}`;

describe("vinext RSC cache-busting fallback", () => {
  it("falls back to vinext's legacy hash when Web Crypto is unavailable", () => {
    const patched = patchVinextRscCacheBustingSource(VINEXT_SOURCE, VINEXT_MODULE_ID);

    expect(patched).toContain("const subtle = globalThis.crypto?.subtle");
    expect(patched).toContain("if(!subtle) return fnv1a64(input)");
    expect(patched).toContain("await subtle.digest");
    expect(patched).not.toContain("globalThis.crypto.subtle.digest");
  });

  it("does not modify unrelated modules", () => {
    expect(patchVinextRscCacheBustingSource(VINEXT_SOURCE, "src/example.ts")).toBeNull();
  });

  it("fails loudly when vinext changes the patched implementation", () => {
    expect(() => patchVinextRscCacheBustingSource("export {};", VINEXT_MODULE_ID)).toThrow(
      "vinext RSC cache-busting implementation changed"
    );
  });
});
