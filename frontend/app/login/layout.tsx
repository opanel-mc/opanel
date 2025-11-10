import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "OPanel - 登录"
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
