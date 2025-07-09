import type { Metadata } from "next";
import localFont from "next/font/local";
import "./globals.css";
import "./formatting-codes.css";
import { cn } from "@/lib/utils";
import { ThemeProvider } from "@/components/theme-provider";

const notoSansSC = localFont({
  src: [
    { path: "../assets/fonts/NotoSansSC-VariableFont_wght.ttf", style: "normal" },
    { path: "../assets/fonts/NotoSans-VariableFont_wdth,wght.ttf", style: "normal" },
  ]
});

export const metadata: Metadata = {
  title: "OPanel",
  description: "A Minecraft server management panel",
  authors: [{ name: "NriotHrreion", url: "https://nocp.space" }],
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-cn" suppressHydrationWarning>
      <body
        className={cn(notoSansSC.className, "antialiased")}>
        <ThemeProvider
          attribute="class"
          defaultTheme="system"
          enableSystem
          disableTransitionOnChange>
          {children}
        </ThemeProvider>
      </body>
    </html>
  );
}
