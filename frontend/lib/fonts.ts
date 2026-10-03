/**
 * Original font files were converted to WOFF2 without subsetting to reduce bundle size.
 */

import localFont from "next/font/local";

export const notoSansSC = localFont({
  src: [{ path: "../assets/fonts/NotoSansSC-VariableFont_wght.woff2", style: "normal" }],
  fallback: ["notoColorEmoji"]
});

export const notoSansTC = localFont({
  src: [{ path: "../assets/fonts/NotoSansTC-VariableFont_wght.woff2", style: "normal" }],
  fallback: ["notoColorEmoji"]
});

export const notoSansHK = localFont({
  src: [{ path: "../assets/fonts/NotoSansHK-VariableFont_wght.woff2", style: "normal" }],
  fallback: ["notoColorEmoji"]
});

export const notoSansJP = localFont({
  src: [{ path: "../assets/fonts/NotoSansJP-VariableFont_wght.woff2", style: "normal" }],
  fallback: ["notoColorEmoji"]
});

export const notoSansKR = localFont({
  src: [{ path: "../assets/fonts/NotoSansKR-VariableFont_wght.woff2", style: "normal" }],
  fallback: ["notoColorEmoji"]
});

export const notoColorEmoji = localFont({
  src: [{ path: "../assets/fonts/NotoColorEmoji-Regular.woff2", style: "normal" }],
  variable: "--font-noto-color-emoji"
});

export const googleSansCode = localFont({
  src: [
    { path: "../assets/fonts/GoogleSansCode-VariableFont_wght.woff2", style: "normal" },
    { path: "../assets/fonts/GoogleSansCode-Italic-VariableFont_wght.woff2", style: "italic" },
  ],
  variable: "--font-google-sans-code"
});

/**
 * GNU Unifont
 * 
 * @see https://unifoundry.com/unifont/index.html
 */
export const unifont = localFont({
  src: [{ path: "../assets/fonts/unifont-16.0.04.woff2", style: "normal" }]
});

/**
 * New Minecraft AE font
 * - Mojangles (Minecraft Seven)
 * - GNU Unifont
 * - Noto Sans SC
 * 
 * @see https://minecraft.wiki/w/Font#Fonts_available
 */
export const minecraftAE = localFont({
  src: [
    { path: "../assets/fonts/Mojangles-Regular.woff2", style: "normal", weight: "400" },
    { path: "../assets/fonts/Mojangles-Bold.woff2", style: "normal", weight: "600" },
    { path: "../assets/fonts/Mojangles-Italic.woff2", style: "italic", weight: "400" },
    { path: "../assets/fonts/Mojangles-BoldItalic.woff2", style: "italic", weight: "600" },
  ],
  fallback: ["unifont", "notoSansSC"]
});

/** Old Minecraft AE font (for obfuscated text) */
export const minecraftAEOld = localFont({
  src: [{ path: "../assets/fonts/MinecraftAE.woff2", style: "normal" }],
  variable: "--font-minecraft-ae-old"
});
