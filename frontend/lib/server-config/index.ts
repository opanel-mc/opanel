import type { LucideIcon } from "lucide-react";
import type { LanguageCode } from "@/lang";
import { z } from "zod";
import serverPropertiesPresets from "./presets";
import * as zhCN from "./presets-zh-cn";
import * as zhTW from "./presets-zh-tw";
import * as zhHK from "./presets-zh-hk";
import * as jaJP from "./presets-ja-jp";
import * as enUS from "./presets-en-us";
import * as frFR from "./presets-fr-fr";
import * as deDE from "./presets-de-de";
import * as koKR from "./presets-ko-kr";

export interface Property {
  id: string
  description: string
  type: "boolean" | "number" | "string"
  icon?: LucideIcon
}

export type PropertyPreset = Omit<Property, "description">;
export type PropertyTranslations = Record<string, string>;

export type ServerProperties = Record<string, boolean | number | string>;

const translations: Record<LanguageCode, { default: PropertyTranslations; wikiUrl: string }> = {
  "zh-cn": zhCN,
  "zh-tw": zhTW,
  "zh-hk": zhHK,
  "ja-jp": jaJP,
  "en-us": enUS,
  "fr-fr": frFR,
  "de-de": deDE,
  "ko-kr": koKR
};

export function getServerPropertiesPresets(language: LanguageCode): Property[] {
  const texts = translations[language]?.default;
  return serverPropertiesPresets.map(preset => ({
    ...preset,
    description: texts?.[preset.id] ?? ""
  }));
}

export function getServerPropertiesWikiUrl(language: LanguageCode): string {
  return translations[language]?.wikiUrl ?? enUS.wikiUrl;
}

export function generateFormSchema(properties: ServerProperties) {
  return z.object(Object.fromEntries(
    Object.entries(properties).map(([key, value]) => [
      key,
      typeof value === "boolean" ? z.boolean() : z.number().or(z.string())
    ] as const)
  ));
}
