"use client";

import { useEffect } from "react";
import { hasCookie } from "cookies-next/client";

export default function Home() {
  useEffect(() => {
    if(hasCookie("token")) {
      window.location.href = "/panel";
    } else {
      window.location.href = "/login";
    }
  }, []);

  return <></>;
}
