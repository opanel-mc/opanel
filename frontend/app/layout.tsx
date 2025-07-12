import type { Metadata } from "next";
import "./globals.css";
import "./formatting-codes.css";
import { cn } from "@/lib/utils";
import { ThemeProvider } from "@/components/theme-provider";
import { Toaster } from "@/components/ui/sonner";
import { notoSansSC } from "@/lib/fonts";

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
          <Toaster
            position="bottom-right"
            expand
            richColors/>
          {children}
        </ThemeProvider>
      </body>
    </html>
  );
}
