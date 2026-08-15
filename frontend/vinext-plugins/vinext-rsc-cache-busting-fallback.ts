import type { Plugin } from "vite";

const VINEXT_RSC_CACHE_BUSTING_MODULE =
  /[/\\]vinext[/\\]dist[/\\]server[/\\]app-rsc-cache-busting\.js(?:\?.*)?$/;
const WEB_CRYPTO_DIGEST =
  'const digest = await globalThis.crypto.subtle.digest("SHA-256", textEncoder.encode(input));';
const DIGEST_WITH_INSECURE_CONTEXT_FALLBACK = `const subtle = globalThis.crypto?.subtle;
\tif(!subtle) return fnv1a64(input);
\tconst digest = await subtle.digest("SHA-256", textEncoder.encode(input));`;

export function patchVinextRscCacheBustingSource(code: string, id: string): string | null {
  if(!VINEXT_RSC_CACHE_BUSTING_MODULE.test(id)) return null;
  if(code.includes(DIGEST_WITH_INSECURE_CONTEXT_FALLBACK)) return code;
  if(!code.includes(WEB_CRYPTO_DIGEST)) {
    throw new Error(
      "vinext RSC cache-busting implementation changed; review the insecure-context fallback"
    );
  }

  return code.replace(WEB_CRYPTO_DIGEST, DIGEST_WITH_INSECURE_CONTEXT_FALLBACK);
}

export function vinextRscCacheBustingFallback(): Plugin {
  return {
    name: "vinext-rsc-cache-busting-fallback",
    enforce: "pre",
    transform(code, id) {
      const patched = patchVinextRscCacheBustingSource(code, id);
      return patched === null ? null : { code: patched, map: null };
    },
  };
}
