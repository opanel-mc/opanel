import type { Metadata } from "next";
import "@/style/globals.css";
import "@/style/formatting-codes.css";
import { cn } from "@/lib/utils";
import { ThemeProvider } from "@/components/theme-provider";
import { Toaster } from "@/components/ui/sonner";
import { googleSansCode, notoColorEmoji, notoSansSC } from "@/lib/fonts";
import { BrowserInit } from "./browser-init";
import { LoadingBar } from "@/components/loading-bar";

import LogoIcon from "@/assets/images/logo.png";

export const metadata: Metadata = {
  authors: [{ name: "Norcleeh", url: "https://nocp.space" }],
  icons: LogoIcon.src
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-cn" suppressHydrationWarning>
      <body
        className={cn(notoSansSC.className, notoColorEmoji.variable, googleSansCode.variable, "antialiased overflow-hidden relative")}>
        <LoadingBar />
        <ThemeProvider
          attribute="class"
          defaultTheme="system"
          enableSystem
          disableTransitionOnChange>
          <Toaster
            position="bottom-right"
            expand
            richColors/>
          {children}
        </ThemeProvider>
        <BrowserInit />
      </body>
    </html>
  );
}
