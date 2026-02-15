"use client";

import { useRouter } from "next/navigation";
import { useCheckAuth } from "@/hooks/use-check-auth";

export default function Home() {
  const { push } = useRouter();

  useCheckAuth(() => push("/panel/dashboard"));

  return <></>;
}
