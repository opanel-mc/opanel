import axios from "axios";
import nacl from "tweetnacl";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  type BulletinItem,
  type BulletinPayload,
  BULLETIN_PUBLIC_KEY_PEM,
  checkBulletins,
  fetchVerifiedBulletinPayload,
  getBulletinBaseUrl,
  getBulletinReadKey,
  isBulletinActive,
  isBulletinDismissed,
  isSafeBulletinUrl,
  markBulletinDismissed,
  normalizeOPanelVersion,
  parseEd25519PublicKey,
  resetBulletinCheckState,
  resolveBulletinContent,
  resolveBulletinLocale,
  sortBulletinsByPriority,
  verifyBulletinSignature,
} from "../bulletin";

const NaclUint8Array = nacl.randomBytes(0).constructor as Uint8ArrayConstructor;

function bytesToBase64(bytes: Uint8Array): string {
  return Buffer.from(bytes).toString("base64");
}

function createBulletinItem(overrides?: Partial<BulletinItem>): BulletinItem {
  return {
    id: "2026-11-notice",
    revision: 1,
    priority: 100,
    policy: "once",
    level: "warning",
    display: "modal",
    startAt: "2026-11-01T00:00:00+08:00",
    endAt: "2026-12-31T23:59:59+08:00",
    target: {
      versions: ">=2.0.0",
    },
    i18n: {
      "zh-cn": {
        title: "中文标题",
        content: "中文内容 **加粗**",
        actions: [
          { label: "更新日志", url: "https://opanel.cn/changelog" },
          { label: "设置", url: "/panel/settings" },
        ],
      },
      "en-us": {
        title: "English Title",
        content: "English content **bold**",
        actions: [
          { label: "Changelog", url: "https://opanel.cn/changelog" },
        ],
      },
    },
    ...overrides,
  };
}

function createSignedFixture(
  bulletins: BulletinItem[] = [createBulletinItem()],
  payloadOverrides?: Partial<BulletinPayload>
) {
  const keyPair = nacl.sign.keyPair();
  const publicKeyBase64 = bytesToBase64(keyPair.publicKey);
  const keyId = "testkeyid1234567";

  const payload: BulletinPayload = {
    schemaVersion: 1,
    revision: "abc1234",
    updatedAt: "2026-11-27T00:00:00.000Z",
    pollIntervalSec: 3600,
    bulletins,
    ...payloadOverrides,
  };

  const rawText = JSON.stringify(payload);
  const rawBytes = new TextEncoder().encode(rawText);
  const naclBytes = new NaclUint8Array(rawBytes);
  const signature = bytesToBase64(nacl.sign.detached(naclBytes, keyPair.secretKey));

  return {
    keyPair,
    publicKeyBase64,
    keyId,
    payload,
    rawText,
    rawBytes,
    signature,
  };
}

describe("lib/bulletin", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("parses bundled SPKI PEM public key from frontend/assets/keys", () => {
    const parsed = parseEd25519PublicKey(BULLETIN_PUBLIC_KEY_PEM);
    expect(parsed).not.toBeNull();
    expect(parsed?.length).toBe(32);
    expect(bytesToBase64(parsed!)).toBe("qH7BrkiKTLCBMvs6ORij7eFe3a6ZVDENmd1X/ggNQjg=");
  });

  it("verifies valid Ed25519 signatures and rejects tampered or malformed inputs", () => {
    const { rawBytes, signature, publicKeyBase64 } = createSignedFixture();

    expect(verifyBulletinSignature(rawBytes, signature, publicKeyBase64)).toBe(true);

    const tampered = new Uint8Array(rawBytes);
    tampered[0] ^= 0xff;
    expect(verifyBulletinSignature(tampered, signature, publicKeyBase64)).toBe(false);

    const otherKey = bytesToBase64(nacl.sign.keyPair().publicKey);
    expect(verifyBulletinSignature(rawBytes, signature, otherKey)).toBe(false);
    expect(verifyBulletinSignature(rawBytes, "not-valid-base64!!!", publicKeyBase64)).toBe(false);
    expect(verifyBulletinSignature(rawBytes, bytesToBase64(new Uint8Array(10)), publicKeyBase64)).toBe(false);
  });

  it("resolves base URL, locale fallback, version normalization, and safe URLs", () => {
    expect(getBulletinBaseUrl()).toBe("https://bulletin.opanel.cn/v1");

    expect(resolveBulletinLocale("zh-cn")).toBe("zh-cn");
    expect(resolveBulletinLocale("zh-tw")).toBe("zh-cn");
    expect(resolveBulletinLocale("zh-HK")).toBe("zh-cn");
    expect(resolveBulletinLocale("en-us")).toBe("en-us");
    expect(resolveBulletinLocale("ja-jp")).toBe("en-us");

    expect(normalizeOPanelVersion("2.2.5")).toBe("2.2.5");
    expect(normalizeOPanelVersion("v2.2.5rc1")).toBe("2.2.5-rc1");
    expect(normalizeOPanelVersion("")).toBeNull();

    expect(isSafeBulletinUrl("https://opanel.cn/docs")).toBe(true);
    expect(isSafeBulletinUrl("/panel/settings")).toBe(true);
    expect(isSafeBulletinUrl("http://opanel.cn/docs")).toBe(false);
    expect(isSafeBulletinUrl("javascript:alert(1)")).toBe(false);
    expect(isSafeBulletinUrl("//evil.example/path")).toBe(false);
    expect(isSafeBulletinUrl("data:text/html,hi")).toBe(false);
  });

  it("stably sorts bulletins by priority descending", () => {
    const items = [
      { id: "low", priority: 10 },
      { id: "high-first", priority: 100 },
      { id: "mid", priority: 50 },
      { id: "high-second", priority: 100 },
    ];

    expect(sortBulletinsByPriority(items).map((item) => item.id)).toEqual([
      "high-first",
      "high-second",
      "mid",
      "low",
    ]);
  });

  it("normalizes string[] multi-line content and filters out unsafe action URLs in resolveBulletinContent", () => {
    const bulletin = createBulletinItem({
      priority: 80,
      i18n: {
        "zh-cn": {
          title: "测试",
          content: ["第一行 **加粗**", "第二行"],
          actions: [
            { label: "安全外链", url: "https://opanel.cn" },
            { label: "不安全", url: "javascript:alert(1)" },
            { label: "安全内链", url: "/panel/settings" },
          ],
        },
        "en-us": {
          title: "Test",
          content: "Body",
        },
      },
    });

    const resolved = resolveBulletinContent(bulletin, "zh-tw");
    expect(resolved.title).toBe("测试");
    expect(resolved.content).toBe("第一行 **加粗**\n第二行");
    expect(resolved.priority).toBe(80);
    expect(resolved.actions).toEqual([
      { label: "安全外链", url: "https://opanel.cn" },
      { label: "安全内链", url: "/panel/settings" },
    ]);
  });

  it("validates string[] content payloads during fetchVerifiedBulletinPayload", async () => {
    const validArrayItem = createBulletinItem({
      i18n: {
        "zh-cn": {
          title: "多行公告",
          content: ["Hello **World**", "111"],
        },
        "en-us": {
          title: "Multi-line",
          content: ["Line 1", "Line 2"],
        },
      },
    });
    const validFixture = createSignedFixture([validArrayItem]);
    vi.spyOn(axios, "get").mockResolvedValueOnce({
      data: validFixture.rawBytes.buffer,
      headers: {
        "x-bulletin-signature": validFixture.signature,
        "x-bulletin-key-id": validFixture.keyId,
      },
    } as any);

    const validResult = await fetchVerifiedBulletinPayload({
      publicKey: validFixture.publicKeyBase64,
      expectedKeyId: validFixture.keyId,
    });
    expect(validResult).toEqual(validFixture.payload);

    // Reject array containing only whitespace lines
    const blankArrayItem = createBulletinItem({
      i18n: {
        "zh-cn": {
          title: "空公告",
          content: ["   ", ""],
        },
        "en-us": {
          title: "Empty",
          content: "Valid",
        },
      },
    });
    const invalidFixture = createSignedFixture([blankArrayItem]);
    vi.spyOn(axios, "get").mockResolvedValueOnce({
      data: invalidFixture.rawBytes.buffer,
      headers: {
        "x-bulletin-signature": invalidFixture.signature,
        "x-bulletin-key-id": invalidFixture.keyId,
      },
    } as any);

    const invalidResult = await fetchVerifiedBulletinPayload({
      publicKey: invalidFixture.publicKeyBase64,
      expectedKeyId: invalidFixture.keyId,
    });
    expect(invalidResult).toBeNull();
  });

  it("evaluates startAt/endAt time windows and SemVer version ranges", () => {
    const bulletin = createBulletinItem({
      startAt: "2026-11-10T00:00:00+08:00",
      endAt: "2026-11-20T00:00:00+08:00",
      target: { versions: "<2.0.0" },
    });

    const insideMs = Date.parse("2026-11-15T12:00:00+08:00");
    const beforeMs = Date.parse("2026-11-09T23:59:59+08:00");
    const afterMs = Date.parse("2026-11-20T00:00:01+08:00");

    expect(isBulletinActive(bulletin, { now: insideMs, currentVersion: "1.5.0" })).toBe(true);
    expect(isBulletinActive(bulletin, { now: beforeMs, currentVersion: "1.5.0" })).toBe(false);
    expect(isBulletinActive(bulletin, { now: afterMs, currentVersion: "1.5.0" })).toBe(false);
    expect(isBulletinActive(bulletin, { now: insideMs, currentVersion: "2.2.5" })).toBe(false);
  });

  it("fetches and verifies payload from X-Bulletin-Signature header", async () => {
    const { rawBytes, signature, publicKeyBase64, keyId, payload } = createSignedFixture();
    const getSpy = vi.spyOn(axios, "get").mockResolvedValue({
      data: rawBytes.buffer,
      headers: {
        "content-type": "application/json",
        "x-bulletin-signature": signature,
        "x-bulletin-key-id": keyId,
        "x-bulletin-algorithm": "ed25519",
      },
    } as any);

    const result = await fetchVerifiedBulletinPayload({
      publicKey: publicKeyBase64,
      expectedKeyId: keyId,
    });

    expect(result).toEqual(payload);
    expect(getSpy).toHaveBeenCalledTimes(1);
    expect(getSpy.mock.calls[0]?.[0]).toBe("https://bulletin.opanel.cn/v1/bulletin.json");
  });

  it("falls back to /v1/bulletin.json.sig when signature header is stripped", async () => {
    const { rawBytes, signature, publicKeyBase64, keyId, payload } = createSignedFixture();
    const getSpy = vi.spyOn(axios, "get").mockImplementation(async (url: string) => {
      if(url.endsWith(".sig")) {
        return {
          data: {
            algorithm: "ed25519",
            keyId,
            signature,
          },
          headers: {},
        } as any;
      }
      return {
        data: rawBytes.buffer,
        headers: {},
      } as any;
    });

    const result = await fetchVerifiedBulletinPayload({
      publicKey: publicKeyBase64,
      expectedKeyId: keyId,
    });

    expect(result).toEqual(payload);
    expect(getSpy).toHaveBeenCalledTimes(2);
    expect(getSpy.mock.calls[1]?.[0]).toBe("https://bulletin.opanel.cn/v1/bulletin.json.sig");
  });

  it("silently returns null when signature or keyId fails verification", async () => {
    const { rawBytes, signature, publicKeyBase64, keyId } = createSignedFixture();
    vi.spyOn(axios, "get").mockResolvedValue({
      data: rawBytes.buffer,
      headers: {
        "x-bulletin-signature": signature,
        "x-bulletin-key-id": "wrong-key-id",
      },
    } as any);

    const result = await fetchVerifiedBulletinPayload({
      publicKey: publicKeyBase64,
      expectedKeyId: keyId,
    });

    expect(result).toBeNull();
  });

  it("enforces always, once, and fetched display policies across cache hits and refreshes", async () => {
    const alwaysModal = createBulletinItem({
      id: "always-modal",
      priority: 300,
      policy: "always",
      display: "modal",
    });
    const onceModal = createBulletinItem({
      id: "once-modal",
      priority: 200,
      policy: "once",
      display: "modal",
    });
    const fetchedToast = createBulletinItem({
      id: "fetched-toast",
      priority: 100,
      policy: "fetched",
      display: "toast",
    });

    const { rawBytes, signature, publicKeyBase64, keyId } = createSignedFixture(
      [alwaysModal, onceModal, fetchedToast],
      { pollIntervalSec: 3600 }
    );
    const now = Date.parse("2026-11-27T12:00:00+08:00");
    const getSpy = vi.spyOn(axios, "get").mockResolvedValue({
      data: rawBytes.buffer,
      headers: {
        "x-bulletin-signature": signature,
        "x-bulletin-key-id": keyId,
      },
    } as any);

    // 1. Fresh fetch: always + unread once + fetched are all returned
    const initial = await checkBulletins({
      now,
      currentVersion: "2.2.5",
      locale: "zh-cn",
      publicKey: publicKeyBase64,
      expectedKeyId: keyId,
    });
    expect(initial.map((b) => b.id)).toEqual(["always-modal", "once-modal", "fetched-toast"]);
    expect(getSpy).toHaveBeenCalledTimes(1);

    // Dismiss once-modal (and even if always-modal were somehow dismissed, always still displays)
    markBulletinDismissed(getBulletinReadKey(onceModal));
    markBulletinDismissed(getBulletinReadKey(alwaysModal));

    // 2. Cache hit within pollIntervalSec: always displays, dismissed once & fetched do NOT display
    const cachedHit = await checkBulletins({
      now: now + 60_000,
      currentVersion: "2.2.5",
      locale: "zh-cn",
      publicKey: publicKeyBase64,
      expectedKeyId: keyId,
    });
    expect(cachedHit.map((b) => b.id)).toEqual(["always-modal"]);
    expect(getSpy).toHaveBeenCalledTimes(1);

    // 3. Cache expires after pollIntervalSec and refreshes: always + fetched display again, dismissed once stays hidden
    const refreshed = await checkBulletins({
      now: now + 3600_000,
      currentVersion: "2.2.5",
      locale: "zh-cn",
      publicKey: publicKeyBase64,
      expectedKeyId: keyId,
    });
    expect(refreshed.map((b) => b.id)).toEqual(["always-modal", "fetched-toast"]);
    expect(getSpy).toHaveBeenCalledTimes(2);
  });

  it("returns multiple matched bulletins sorted by priority, filters inactive/dismissed, and respects cache rules", async () => {
    const lowModal = createBulletinItem({
      id: "low-modal",
      priority: 10,
      display: "modal",
    });
    const highModal = createBulletinItem({
      id: "high-modal",
      priority: 200,
      display: "modal",
    });
    const toastNotice = createBulletinItem({
      id: "mid-toast",
      priority: 50,
      display: "toast",
    });
    const unmatchedVersion = createBulletinItem({
      id: "old-only",
      priority: 500,
      target: { versions: "<1.5.0" },
    });

    const { rawBytes, signature, publicKeyBase64, keyId } = createSignedFixture([
      lowModal,
      toastNotice,
      unmatchedVersion,
      highModal,
    ]);
    const now = Date.parse("2026-11-27T12:00:00+08:00");
    const getSpy = vi.spyOn(axios, "get").mockResolvedValue({
      data: rawBytes.buffer,
      headers: {
        "x-bulletin-signature": signature,
        "x-bulletin-key-id": keyId,
      },
    } as any);

    // First fetch hits network and returns active items sorted by priority desc
    const first = await checkBulletins({
      now,
      currentVersion: "2.2.5",
      locale: "zh-cn",
      publicKey: publicKeyBase64,
      expectedKeyId: keyId,
    });
    expect(first.map((b) => b.id)).toEqual(["high-modal", "mid-toast", "low-modal"]);
    expect(getSpy).toHaveBeenCalledTimes(1);

    // Second check within pollIntervalSec reuses cache
    const second = await checkBulletins({
      now: now + 60_000,
      currentVersion: "2.2.5",
      locale: "en-us",
      publicKey: publicKeyBase64,
      expectedKeyId: keyId,
    });
    expect(second.map((b) => b.id)).toEqual(["high-modal", "mid-toast", "low-modal"]);
    expect(second[0]?.title).toBe("English Title");
    expect(getSpy).toHaveBeenCalledTimes(1);

    // force: true bypasses cache even within pollIntervalSec
    await checkBulletins({
      force: true,
      now: now + 61_000,
      currentVersion: "2.2.5",
      locale: "zh-cn",
      publicKey: publicKeyBase64,
      expectedKeyId: keyId,
    });
    expect(getSpy).toHaveBeenCalledTimes(2);

    // Dismissing high-modal keeps remaining unread bulletins and survives resetBulletinCheckState()
    const readKey = getBulletinReadKey(highModal);
    markBulletinDismissed(readKey);
    expect(isBulletinDismissed(readKey)).toBe(true);
    resetBulletinCheckState();
    expect(isBulletinDismissed(readKey)).toBe(true);

    const afterDismiss = await checkBulletins({
      now: now + 120_000,
      currentVersion: "2.2.5",
      locale: "zh-cn",
      publicKey: publicKeyBase64,
      expectedKeyId: keyId,
    });
    expect(afterDismiss.map((b) => b.id)).toEqual(["mid-toast", "low-modal"]);
    expect(getSpy).toHaveBeenCalledTimes(3);
  });
});
