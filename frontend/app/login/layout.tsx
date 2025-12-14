import type { Metadata } from "next";
import { $ } from "@/lib/i18n";

export const metadata: Metadata = {
  title: "OPanel - "+ $("login")
};

export default function LoginLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <div className="w-[100vw] h-[100vh] flex justify-center items-center">
      {children}
    </div>
  );
}
