import zhCN from "@/lang/zh-cn.json";
import zhTW from "@/lang/zh-tw.json";
import zhHK from "@/lang/zh-hk.json";
import enUS from "@/lang/en-us.json";

import minecraftZhCN from "@/assets/minecraft/zh_cn.json";
import minecraftZhTW from "@/assets/minecraft/zh_tw.json";
import minecraftZhHK from "@/assets/minecraft/zh_hk.json";
import minecraftEnUS from "@/assets/minecraft/en_us.json";

export const languages: Record<string, Translations> = {
  "zh-cn": { ...zhCN, ...minecraftZhCN },
  "zh-tw": { ...zhTW, ...minecraftZhTW },
  "zh-hk": { ...zhHK, ...minecraftZhHK },
  "en-us": { ...enUS, ...minecraftEnUS },
};

export type TranslationKey = keyof (typeof zhCN & typeof minecraftZhCN);
export type Translations = Record<TranslationKey, string>;

export type LanguageCode = keyof typeof languages;
