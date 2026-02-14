"use client";

import { useEffect } from "react";
import { isAuth } from "@/lib/utils";

export default function Panel() {
  useEffect(() => {
    window.location.href = "/panel/dashboard";
    isAuth().then((res) => {
      if(!res) {
        window.location.href = "/login";
      }
    });
  }, []);

  return <></>;
}
