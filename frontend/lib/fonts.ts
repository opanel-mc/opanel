import localFont from "next/font/local";

export const notoSansSC = localFont({
  src: [
    { path: "../assets/fonts/NotoSansSC-VariableFont_wght.ttf", style: "normal" },
    { path: "../assets/fonts/NotoSans-VariableFont_wdth,wght.ttf", style: "normal" },
  ]
});

/**
 * GNU Unifont
 * 
 * @see https://unifoundry.com/unifont/index.html
 */
export const unifont = localFont({
  src: [{ path: "../assets/fonts/unifont-16.0.04.otf", style: "normal" }]
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
    { path: "../assets/fonts/Mojangles-Regular.otf", style: "normal", weight: "400" },
    { path: "../assets/fonts/Mojangles-Bold.otf", style: "normal", weight: "600" },
    { path: "../assets/fonts/Mojangles-Italic.otf", style: "italic", weight: "400" },
    { path: "../assets/fonts/Mojangles-BoldItalic.otf", style: "italic", weight: "600" },
  ],
  fallback: ["unifont", "notoSansSC"]
});

/** Old Minecraft AE font (for obfuscated text) */
export const minecraftAEOld = localFont({
  src: [{ path: "../assets/fonts/MinecraftAE.ttf", style: "normal" }],
  variable: "--font-minecraft-ae-old"
});
