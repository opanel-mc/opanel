import localFont from "next/font/local";

export const notoSansSC = localFont({
  src: [
    { path: "../assets/fonts/NotoSansSC-VariableFont_wght.ttf", style: "normal" },
    { path: "../assets/fonts/NotoSans-VariableFont_wdth,wght.ttf", style: "normal" },
  ]
});

export const minecraftAE = localFont({
  src: [{ path: "../assets/fonts/MinecraftAE.ttf", style: "normal" }]
});
