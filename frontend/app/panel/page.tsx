"use client";

import { hasCookie } from "cookies-next/client";
import { useEffect } from "react";

export default function Panel() {
  useEffect(() => {
    if(hasCookie("token")) {
      window.location.href = "/panel/dashboard";
    } else {
      window.location.href = "/login";
    }
  }, []);

  return <></>;
}
