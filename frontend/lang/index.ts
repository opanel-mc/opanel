import zhCN from "@/lang/zh-cn.json";
import zhTW from "@/lang/zh-tw.json";
import zhHK from "@/lang/zh-hk.json";
import jaJP from "@/lang/ja-jp.json";
import enUS from "@/lang/en-us.json";
import frFR from "@/lang/fr-fr.json";
import deDE from "@/lang/de-de.json";
import koKR from "@/lang/ko-kr.json";

import minecraftZhCN from "@/assets/minecraft/zh_cn.json";
import minecraftZhTW from "@/assets/minecraft/zh_tw.json";
import minecraftZhHK from "@/assets/minecraft/zh_hk.json";
import minecraftJaJP from "@/assets/minecraft/ja_jp.json";
import minecraftEnUS from "@/assets/minecraft/en_us.json";
import minecraftFrFR from "@/assets/minecraft/fr_fr.json";
import minecraftDeDE from "@/assets/minecraft/de_de.json";
import minecraftKoKR from "@/assets/minecraft/ko_kr.json";

export const languages: Record<string, Translations> = {
  "zh-cn": { ...zhCN, ...minecraftZhCN },
  "zh-tw": { ...zhTW, ...minecraftZhTW },
  "zh-hk": { ...zhHK, ...minecraftZhHK },
  "ja-jp": { ...jaJP, ...minecraftJaJP },
  "en-us": { ...enUS, ...minecraftEnUS },
  "fr-fr": { ...frFR, ...minecraftFrFR },
  "de-de": { ...deDE, ...minecraftDeDE },
  "ko-kr": { ...koKR, ...minecraftKoKR },
};

export type TranslationKey = keyof (typeof zhCN & typeof minecraftZhCN);
export type Translations = Record<TranslationKey, string>;

export type LanguageCode = keyof typeof languages;
