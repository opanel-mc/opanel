"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { isAuth } from "@/lib/utils";

export default function Home() {
  const { push } = useRouter();

  useEffect(() => {
    push("/panel/dashboard");
    isAuth().then((res) => {
      if(!res) {
        push("/login");
      }
    });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return <></>;
}
