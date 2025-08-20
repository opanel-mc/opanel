import type { PropsWithChildren } from "react";

export function Section({
  title,
  children
}: PropsWithChildren<{
  title: string
}>) {
  return (
    <section>
      <h2 className="text-lg font-semibold mb-3">{title}</h2>
      <div className="border rounded-md flex flex-col">{children}</div>
    </section>
  );
}
