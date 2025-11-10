import type { LucideIcon } from "lucide-react";
import { z } from "zod";

export interface Property {
  id: string
  description: string
  type: "boolean" | "number" | "string"
  icon?: LucideIcon
}

export type ServerProperties = Record<string, boolean | number | string>;

export function generateFormSchema(properties: ServerProperties): z.ZodObject<z.ZodRawShape> {
  const schemeList: z.ZodRawShape = {};
  for(const key in properties) {
    schemeList[key] = (
      typeof properties[key] === "boolean"
      ? z.boolean()
      : z.number().or(z.string())
    );
  }
  return z.object(schemeList);
}
