"use client";

import { useRouter } from "next/navigation";
import { useCheckAuth } from "@/hooks/use-check-auth";

export default function Panel() {
  const { push } = useRouter();

  useCheckAuth(() => push("/panel/dashboard"));

  return <></>;
}
