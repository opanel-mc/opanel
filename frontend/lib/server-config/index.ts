import type { LucideIcon } from "lucide-react";
import { z } from "zod";

export interface Property {
  id: string
  description: string
  type: "boolean" | "number" | "string"
  icon?: LucideIcon
}

export type ServerProperties = Record<string, boolean | number | string>;

export function generateFormSchema(properties: ServerProperties) {
  return z.object(Object.fromEntries(
    Object.entries(properties).map(([key, value]) => [
      key,
      typeof value === "boolean" ? z.boolean() : z.number().or(z.string())
    ] as const)
  ));
}
