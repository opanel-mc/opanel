import type { LucideIcon } from "lucide-react";
import { z } from "zod";

export interface Gamerule {
  id: string
  name: string
  description?: string
  type: "boolean" | "number"
  icon?: LucideIcon
}

export type ServerGamerules = Record<string, boolean | number>;

export function generateFormSchema(gamerules: ServerGamerules) {
  return z.object(Object.fromEntries(
    Object.entries(gamerules).map(([key, value]) => [
      key,
      typeof value === "boolean" ? z.boolean() : z.number().or(z.string())
    ] as const)
  ));
}
