import { type BulletinItem, type BulletinPayload, bulletinPayloadSchema } from "./schema";
import { getLocalStorage } from "../utils";

const storageKey = "opanel.bulletin";
const maxDismissedKeys = 50;

export interface BulletinStorageState {
  baseUrl?: string
  lastCheckedAt?: number
  pollIntervalSec?: number
  dismissedKeys: string[]
  cachedPayload?: BulletinPayload
}

const defaultStorageState: BulletinStorageState = {
  dismissedKeys: [],
};

export function getBulletinReadKey(bulletin: Pick<BulletinItem, "id" | "revision">): string {
  return `${bulletin.id}:${bulletin.revision}`;
}

export function getBulletinStorage(): BulletinStorageState {
  let storage: Storage;
  try {
    storage = getLocalStorage();
  } catch {
    return { ...defaultStorageState, dismissedKeys: [] };
  }

  try {
    const raw = storage.getItem(storageKey);
    if(!raw) return { ...defaultStorageState, dismissedKeys: [] };

    const parsed = JSON.parse(raw) as Partial<BulletinStorageState>;
    return {
      baseUrl: typeof parsed.baseUrl === "string" ? parsed.baseUrl : undefined,
      lastCheckedAt: typeof parsed.lastCheckedAt === "number" ? parsed.lastCheckedAt : undefined,
      pollIntervalSec: typeof parsed.pollIntervalSec === "number" ? parsed.pollIntervalSec : undefined,
      dismissedKeys: Array.isArray(parsed.dismissedKeys)
        ? parsed.dismissedKeys.filter((item): item is string => typeof item === "string")
        : [],
      cachedPayload: parsed.cachedPayload,
    };
  } catch {
    return { ...defaultStorageState, dismissedKeys: [] };
  }
}

function writeBulletinStorage(state: BulletinStorageState) {
  try {
    getLocalStorage().setItem(storageKey, JSON.stringify(state));
  } catch {
    // Ignore storage errors (e.g. private browsing / quota exceeded)
  }
}

export function isBulletinDismissed(readKey: string): boolean {
  return getBulletinStorage().dismissedKeys.includes(readKey);
}

export function markBulletinDismissed(readKey: string) {
  const state = getBulletinStorage();
  const nextKeys = state.dismissedKeys.filter((key) => key !== readKey);
  nextKeys.push(readKey);
  if(nextKeys.length > maxDismissedKeys) {
    nextKeys.splice(0, nextKeys.length - maxDismissedKeys);
  }
  writeBulletinStorage({
    ...state,
    dismissedKeys: nextKeys,
  });
}

export function saveBulletinCache(baseUrl: string, payload: BulletinPayload, now: number = Date.now()) {
  const state = getBulletinStorage();
  writeBulletinStorage({
    ...state,
    baseUrl,
    lastCheckedAt: now,
    pollIntervalSec: payload.pollIntervalSec,
    cachedPayload: payload,
  });
}

export function getValidCachedPayload(baseUrl: string, now: number = Date.now()): BulletinPayload | null {
  const state = getBulletinStorage();
  if(
    !state.cachedPayload
    || state.baseUrl !== baseUrl
    || state.lastCheckedAt === undefined
    || state.pollIntervalSec === undefined
  ) {
    return null;
  }

  const elapsedMs = now - state.lastCheckedAt;
  if(elapsedMs < 0 || elapsedMs >= state.pollIntervalSec * 1000) {
    return null;
  }

  const validated = bulletinPayloadSchema.safeParse(state.cachedPayload);
  return validated.success ? validated.data : null;
}

/**
 * Resets cached poll timing (e.g. after a fresh login) while keeping dismissed read records intact.
 */
export function resetBulletinCheckState() {
  const state = getBulletinStorage();
  writeBulletinStorage({
    dismissedKeys: state.dismissedKeys,
  });
}
