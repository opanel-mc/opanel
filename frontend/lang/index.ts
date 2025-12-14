import zhCN from "@/lang/zh-cn.json";

export type TranslationKey = keyof typeof zhCN;
export type Translations = Record<TranslationKey, string>;

export const languages: Record<string, Translations> = {
  "zh-cn": zhCN
};

export type LanguageCode = keyof typeof languages;
