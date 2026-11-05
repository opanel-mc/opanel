import type { LucideIcon } from "lucide-react";
import type { LanguageCode } from "@/lang";
import { z } from "zod";
import gamerulePresets from "./presets";
import gamerulePresetsOld from "./presets-old";
import * as zhCN from "./presets-zh-cn";
import * as zhTW from "./presets-zh-tw";
import * as zhHK from "./presets-zh-hk";
import * as jaJP from "./presets-ja-jp";
import * as enUS from "./presets-en-us";
import * as frFR from "./presets-fr-fr";
import * as deDE from "./presets-de-de";
import * as koKR from "./presets-ko-kr";

export interface Gamerule {
  id: string
  name: string
  description?: string
  type: "boolean" | "number"
  icon?: LucideIcon
}

export type GamerulePreset = Omit<Gamerule, "name" | "description">;
export type GameruleTranslations = Record<string, Pick<Gamerule, "name" | "description">>;

export type ServerGamerules = Record<string, boolean | number>;

const translations: Record<LanguageCode, { default: GameruleTranslations; wikiUrl: string }> = {
  "zh-cn": zhCN,
  "zh-tw": zhTW,
  "zh-hk": zhHK,
  "ja-jp": jaJP,
  "en-us": enUS,
  "fr-fr": frFR,
  "de-de": deDE,
  "ko-kr": koKR
};

export function getGamerulePresets(language: LanguageCode, legacy = false): Gamerule[] {
  const texts = translations[language]?.default;
  return (legacy ? gamerulePresetsOld : gamerulePresets).map(preset => ({
    ...preset,
    name: texts?.[preset.id]?.name ?? "",
    description: texts?.[preset.id]?.description ?? ""
  }));
}

export function getGameruleWikiUrl(language: LanguageCode): string {
  return translations[language]?.wikiUrl ?? enUS.wikiUrl;
}

export function generateFormSchema(gamerules: ServerGamerules) {
  return z.object(Object.fromEntries(
    Object.entries(gamerules).map(([key, value]) => [
      key,
      typeof value === "boolean" ? z.boolean() : z.number().or(z.string())
    ] as const)
  ));
}
