import { z } from "zod";

export const bulletinActionSchema = z.object({
  label: z.string().min(1).max(64),
  url: z.string().min(1).max(512),
});

export const bulletinContentSchema = z.union([
  z.string().min(1).max(4000),
  z.array(z.string().max(1000))
    .min(1)
    .max(50)
    .refine((lines) => lines.some((line) => line.trim().length > 0))
    .refine((lines) => lines.join("\n").length <= 4000),
]);

export const bulletinLocaleContentSchema = z.object({
  title: z.string().min(1).max(120),
  content: bulletinContentSchema,
  actions: z.array(bulletinActionSchema).max(3).optional(),
});

export const bulletinPolicySchema = z.enum(["always", "once", "fetched"]);
export const bulletinLevelSchema = z.enum(["info", "warning", "critical"]);
export const bulletinDisplaySchema = z.enum(["toast", "modal"]);

export const bulletinItemSchema = z.object({
  id: z.string().regex(/^[a-z0-9][a-z0-9._-]{1,63}$/),
  revision: z.number().int().min(1),
  priority: z.number().int().min(0).max(1000),
  policy: bulletinPolicySchema,
  level: bulletinLevelSchema,
  display: bulletinDisplaySchema,
  startAt: z.string().optional(),
  endAt: z.string().optional(),
  target: z.object({
    versions: z.string().min(1).max(128).optional(),
  }).optional(),
  i18n: z.object({
    "zh-cn": bulletinLocaleContentSchema,
    "en-us": bulletinLocaleContentSchema,
  }),
});

export const bulletinPayloadSchema = z.object({
  schemaVersion: z.literal(1),
  revision: z.string().min(1).max(64),
  updatedAt: z.string(),
  pollIntervalSec: z.number().int().min(60).max(86400),
  bulletins: z.array(bulletinItemSchema).max(50),
});

export const bulletinSignatureEnvelopeSchema = z.object({
  algorithm: z.literal("ed25519"),
  keyId: z.string().min(1),
  signature: z.string().min(1),
});

export type BulletinAction = z.infer<typeof bulletinActionSchema>;
export type BulletinContent = z.infer<typeof bulletinContentSchema>;
export type BulletinLocaleContent = z.infer<typeof bulletinLocaleContentSchema>;
export type BulletinPolicy = z.infer<typeof bulletinPolicySchema>;
export type BulletinLevel = z.infer<typeof bulletinLevelSchema>;
export type BulletinDisplay = z.infer<typeof bulletinDisplaySchema>;
export type BulletinItem = z.infer<typeof bulletinItemSchema>;
export type BulletinPayload = z.infer<typeof bulletinPayloadSchema>;
export type BulletinSignatureEnvelope = z.infer<typeof bulletinSignatureEnvelopeSchema>;
export type BulletinLocale = "zh-cn" | "en-us";

export interface ResolvedBulletin {
  id: string
  revision: number
  priority: number
  readKey: string
  policy: BulletinPolicy
  level: BulletinLevel
  display: BulletinDisplay
  title: string
  content: string
  actions: BulletinAction[]
}
