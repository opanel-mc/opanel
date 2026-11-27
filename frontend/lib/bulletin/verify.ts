import nacl from "tweetnacl";
import bulletinPubKeyPem from "@/assets/keys/bulletin.pub?raw";

/**
 * Built-in Ed25519 production public key (SPKI PEM) from frontend/assets/keys/bulletin.pub.
 * Never fetch this dynamically from the bulletin server.
 */
export const BULLETIN_PUBLIC_KEY_PEM = bulletinPubKeyPem;

/**
 * Expected 16-char hex SHA-256 fingerprint of the SPKI DER public key (X-Bulletin-Key-Id).
 */
export const BULLETIN_KEY_ID = "c4d69c3cab6c4562";

export const BULLETIN_ALGORITHM = "ed25519";

const ED25519_PUBLIC_KEY_LENGTH = 32;
const ED25519_SIGNATURE_LENGTH = 64;
const ED25519_SPKI_DER_PREFIX = [0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00] as const;

const NaclUint8Array = nacl.randomBytes(0).constructor as Uint8ArrayConstructor;

function toNaclBytes(bytes: Uint8Array): Uint8Array {
  return bytes instanceof NaclUint8Array ? bytes : new NaclUint8Array(bytes);
}

export function decodeBase64ToBytes(base64: string): Uint8Array | null {
  try {
    const normalized = base64.replace(/\s+/g, "");
    if(normalized.length === 0) return null;
    const binStr = atob(normalized);
    const bytes = new NaclUint8Array(binStr.length);
    for(let i = 0; i < binStr.length; i++) {
      bytes[i] = binStr.charCodeAt(i);
    }
    return bytes;
  } catch {
    return null;
  }
}

/**
 * Parses an Ed25519 public key from either:
 * - SPKI PEM ("-----BEGIN PUBLIC KEY-----...")
 * - Base64-encoded SPKI DER (44 bytes)
 * - Base64-encoded raw Ed25519 public key (32 bytes)
 */
export function parseEd25519PublicKey(publicKey: string): Uint8Array | null {
  const stripped = publicKey
    .replace(/-----BEGIN PUBLIC KEY-----/g, "")
    .replace(/-----END PUBLIC KEY-----/g, "")
    .replace(/\s+/g, "");

  const decoded = decodeBase64ToBytes(stripped);
  if(!decoded) return null;

  if(decoded.length === ED25519_PUBLIC_KEY_LENGTH) {
    return decoded;
  }

  if(decoded.length === ED25519_SPKI_DER_PREFIX.length + ED25519_PUBLIC_KEY_LENGTH) {
    for(let i = 0; i < ED25519_SPKI_DER_PREFIX.length; i++) {
      if(decoded[i] !== ED25519_SPKI_DER_PREFIX[i]) return null;
    }
    return new NaclUint8Array(decoded.subarray(ED25519_SPKI_DER_PREFIX.length));
  }

  return null;
}

/**
 * Verifies an Ed25519 detached signature over raw UTF-8 response bytes.
 * Uses pure-JS TweetNaCl so verification works reliably on plain HTTP panels
 * where window.crypto.subtle is unavailable.
 */
export function verifyBulletinSignature(
  payloadBytes: Uint8Array,
  signatureBase64: string,
  publicKey: string = BULLETIN_PUBLIC_KEY_PEM
): boolean {
  const signatureBytes = decodeBase64ToBytes(signatureBase64);
  if(!signatureBytes || signatureBytes.length !== ED25519_SIGNATURE_LENGTH) {
    return false;
  }

  const publicKeyBytes = parseEd25519PublicKey(publicKey);
  if(!publicKeyBytes) {
    return false;
  }

  try {
    return nacl.sign.detached.verify(toNaclBytes(payloadBytes), signatureBytes, publicKeyBytes);
  } catch {
    return false;
  }
}
