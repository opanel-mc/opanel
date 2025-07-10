"use client";

import { useEffect } from "react";
import { hasCookie } from "cookies-next/client";
import { useRouter } from "next/navigation";

export default function Home() {
  const { push } = useRouter();

  useEffect(() => {
    if(hasCookie("token")) {
      push("/panel");
    } else {
      push("/login");
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return <></>;
}
