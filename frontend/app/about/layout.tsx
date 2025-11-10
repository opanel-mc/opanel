import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "OPanel - 关于"
};

export default function AboutLayout({
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
