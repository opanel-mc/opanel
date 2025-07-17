import { LucideIcon } from "lucide-react";
import { z } from "zod";

export interface Gamerule {
  id: string
  name: string
  description?: string
  type: "boolean" | "number"
  icon?: LucideIcon
}

export interface ServerGamerules {
  [key: string]: boolean | number
}

export function generateFormSchema(gamerules: ServerGamerules): z.ZodObject<z.ZodRawShape> {
  const schemeList: z.ZodRawShape = {};
  for(const key in gamerules) {
    schemeList[key] = (
      typeof gamerules[key] === "boolean"
      ? z.boolean()
      : z.number()
    );
  }
  return z.object(schemeList);
}
