import type { Metadata } from "next";
import { Suspense } from "react";
import { apiUrl } from "@/lib/api";

export const metadata: Metadata = {
  title: "OPanel"
};

export default function LoginLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <div className="w-[100vw] h-[100vh] flex">
      <div
        className="flex-2/5 h-full bg-cover bg-center shadow-[inset_0px_0px_35px_-13px_rgba(0,0,0,0.85)] max-lg:hidden"
        style={{ backgroundImage: `url(${apiUrl}/assets/login-banner)` }}/>
      <div className="flex-3/5 border-l z-10 flex justify-center items-center">
        <Suspense fallback={null}>{children}</Suspense>
      </div>
    </div>
  );
}
