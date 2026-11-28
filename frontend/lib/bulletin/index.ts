import axios from "axios";
import { coerce, satisfies, valid, validRange } from "semver";
import { version as opanelVersion } from "../global";
import { getSettings } from "../settings";
import {
  getBulletinReadKey,
  getValidCachedPayload,
  isBulletinDismissed,
  markBulletinDismissed,
  resetBulletinCheckState,
  saveBulletinCache,
} from "./storage";
import {
  type BulletinContent,
  type BulletinItem,
  type BulletinLocale,
  type BulletinPayload,
  type ResolvedBulletin,
  bulletinPayloadSchema,
  bulletinSignatureEnvelopeSchema,
} from "./schema";
import {
  BULLETIN_ALGORITHM,
  BULLETIN_KEY_ID,
  BULLETIN_PUBLIC_KEY_PEM,
  parseEd25519PublicKey,
  verifyBulletinSignature,
} from "./verify";

export const DEFAULT_BULLETIN_BASE_URL = "https://bulletin.opanel.cn/v1";
export const BULLETIN_FETCH_TIMEOUT_MS = 5000;

export function getBulletinBaseUrl(): string {
  const raw = (import.meta.env.VITE_BULLETIN_BASE_URL ?? "").trim();
  return (raw || DEFAULT_BULLETIN_BASE_URL).replace(/\/+$/, "");
}

export function isSafeBulletinUrl(url: string): boolean {
  const trimmed = url.trim();
  if(trimmed.length === 0) return false;
  if(trimmed.startsWith("/")) {
    return !trimmed.startsWith("//");
  }
  try {
    const parsed = new URL(trimmed);
    return parsed.protocol === "https:";
  } catch {
    return false;
  }
}

export function normalizeOPanelVersion(rawVersion: string): string | null {
  const cleaned = rawVersion.trim().replace(/^v/i, "").replace(/(?<!-)rc/g, "-rc");
  if(!cleaned) return null;

  const exact = valid(cleaned);
  if(exact) return exact;

  return coerce(cleaned)?.version ?? null;
}

export function resolveBulletinLocale(locale: string): BulletinLocale {
  return locale.toLowerCase().startsWith("zh") ? "zh-cn" : "en-us";
}

export function sortBulletinsByPriority<T extends Pick<BulletinItem, "priority">>(items: readonly T[]): T[] {
  return [...items].sort((a, b) => b.priority - a.priority);
}

export function isBulletinActive(
  bulletin: BulletinItem,
  options?: {
    now?: number
    currentVersion?: string
  }
): boolean {
  const now = options?.now ?? Date.now();

  if(bulletin.startAt !== undefined) {
    const startMs = Date.parse(bulletin.startAt);
    if(Number.isNaN(startMs) || now < startMs) return false;
  }

  if(bulletin.endAt !== undefined) {
    const endMs = Date.parse(bulletin.endAt);
    if(Number.isNaN(endMs) || now > endMs) return false;
  }

  if(bulletin.target?.versions) {
    const range = validRange(bulletin.target.versions);
    if(range === null) return false;

    const currentVersion = normalizeOPanelVersion(options?.currentVersion ?? opanelVersion ?? "");
    if(!currentVersion) return false;

    if(!satisfies(currentVersion, range, { includePrerelease: true })) {
      return false;
    }
  }

  return true;
}

export function normalizeBulletinContent(content: BulletinContent): string {
  return Array.isArray(content) ? content.join("\n") : content;
}

export function resolveBulletinContent(
  bulletin: BulletinItem,
  locale: string = getSettings("system.language")
): ResolvedBulletin {
  const localized = bulletin.i18n[resolveBulletinLocale(locale)];
  const safeActions = (localized.actions ?? [])
    .filter((action) => isSafeBulletinUrl(action.url))
    .slice(0, 3);

  return {
    id: bulletin.id,
    revision: bulletin.revision,
    priority: bulletin.priority,
    readKey: getBulletinReadKey(bulletin),
    policy: bulletin.policy,
    level: bulletin.level,
    display: bulletin.display,
    title: localized.title,
    content: normalizeBulletinContent(localized.content),
    actions: safeActions,
  };
}

export function shouldDisplayBulletin(
  bulletin: Pick<BulletinItem, "id" | "revision" | "policy">,
  fetchedFresh: boolean
): boolean {
  switch(bulletin.policy) {
    // Display every time the panel is opened, regardless of read state or cache
    case "always":
      return true;
    // Display only once per `${id}:${revision}` until dismissed by the user
    case "once":
      return !isBulletinDismissed(getBulletinReadKey(bulletin));
    // Display only when the client cache is refreshed with a fresh server fetch
    case "fetched":
      return fetchedFresh === true;
    default:
      return false;
  }
}

export async function fetchVerifiedBulletinPayload(options?: {
  publicKey?: string
  expectedKeyId?: string
}): Promise<BulletinPayload | null> {
  const baseUrl = getBulletinBaseUrl();
  const publicKey = options?.publicKey ?? BULLETIN_PUBLIC_KEY_PEM;
  const expectedKeyId = options?.expectedKeyId ?? BULLETIN_KEY_ID;

  try {
    const res = await axios.get<ArrayBuffer>(`${baseUrl}/bulletin.json`, {
      responseType: "arraybuffer",
      timeout: BULLETIN_FETCH_TIMEOUT_MS,
    });

    const rawBytes = res.data instanceof Uint8Array
      ? res.data
      : new Uint8Array(res.data);
    const headerSignature = res.headers["x-bulletin-signature"] as string | undefined;
    const headerKeyId = res.headers["x-bulletin-key-id"] as string | undefined;
    const headerAlgorithm = res.headers["x-bulletin-algorithm"] as string | undefined;

    let signatureToVerify: string | null = null;

    // Prefer the signature from response headers to verify in a single atomic request.
    // Fallback to `bulletin.json.sig` when custom `X-Bulletin-*` headers are unavailable
    // (e.g. stripped by a proxy/CDN, blocked by missing CORS Access-Control-Expose-Headers,
    // or served from a static mirror without custom `_headers` support).
    if(headerSignature) {
      if(headerAlgorithm && headerAlgorithm.toLowerCase() !== BULLETIN_ALGORITHM) {
        return null;
      }
      if(headerKeyId && headerKeyId !== expectedKeyId) {
        return null;
      }
      signatureToVerify = headerSignature;
    } else {
      const sigRes = await axios.get(`${baseUrl}/bulletin.json.sig`, {
        timeout: BULLETIN_FETCH_TIMEOUT_MS,
      });
      const sigParsed = bulletinSignatureEnvelopeSchema.safeParse(sigRes.data);
      if(!sigParsed.success || sigParsed.data.keyId !== expectedKeyId) {
        return null;
      }
      signatureToVerify = sigParsed.data.signature;
    }

    if(!verifyBulletinSignature(rawBytes, signatureToVerify, publicKey)) {
      return null;
    }

    const decodedText = new TextDecoder("utf-8", { fatal: true }).decode(rawBytes);
    const parsedJson = JSON.parse(decodedText);
    const validated = bulletinPayloadSchema.safeParse(parsedJson);
    if(!validated.success) {
      return null;
    }

    return validated.data;
  } catch {
    return null;
  }
}

/**
 * Fetches, verifies, filters, and sorts all currently eligible & unread bulletins
 * in descending priority order (highest priority first).
 */
export async function checkBulletins(options?: {
  force?: boolean
  now?: number
  currentVersion?: string
  locale?: string
  publicKey?: string
  expectedKeyId?: string
}): Promise<ResolvedBulletin[]> {
  const baseUrl = getBulletinBaseUrl();
  const now = options?.now ?? Date.now();
  const bypassCache = options?.force === true;

  // Get cached bulletins
  let payload: BulletinPayload | null = bypassCache
    ? null
    : getValidCachedPayload(baseUrl, now);
  let fetchedFresh = false;

  // No cache or cache outdated
  if(!payload) {
    payload = await fetchVerifiedBulletinPayload({
      publicKey: options?.publicKey,
      expectedKeyId: options?.expectedKeyId,
    });
    if(!payload) return [];
    
    saveBulletinCache(baseUrl, payload, now);
    fetchedFresh = true;
  }

  // No bulletin to show
  if(payload.bulletins.length === 0) return [];

  const filtered = payload.bulletins.filter((item) => (
    isBulletinActive(item, { now, currentVersion: options?.currentVersion })
    && shouldDisplayBulletin(item, fetchedFresh)
  ));

  return sortBulletinsByPriority(filtered).map((item) => resolveBulletinContent(item, options?.locale));
}

export {
  BULLETIN_ALGORITHM,
  BULLETIN_KEY_ID,
  BULLETIN_PUBLIC_KEY_PEM,
  getBulletinReadKey,
  isBulletinDismissed,
  markBulletinDismissed,
  parseEd25519PublicKey,
  resetBulletinCheckState,
  verifyBulletinSignature,
};

export type {
  BulletinAction,
  BulletinContent,
  BulletinDisplay,
  BulletinItem,
  BulletinLevel,
  BulletinLocale,
  BulletinLocaleContent,
  BulletinPayload,
  BulletinPolicy,
  BulletinSignatureEnvelope,
  ResolvedBulletin,
} from "./schema";
