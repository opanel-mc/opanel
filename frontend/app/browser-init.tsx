"use client";

import { useEffect } from "react";
import { getSettings } from "@/lib/settings";
import {
  notoSansSC,
  notoSansTC,
  notoSansHK,
  notoSansJP,
  notoSansKR,
} from "@/lib/fonts";
import { doAutoUpdateCheck } from "@/lib/update";

export function BrowserInit() {
  useEffect(() => {
    if(getSettings("system.language") === "zh-tw") {
      document.body.classList.remove(notoSansSC.className);
      document.body.classList.add(notoSansTC.className);
    }
    if(getSettings("system.language") === "zh-hk") {
      document.body.classList.remove(notoSansSC.className);
      document.body.classList.add(notoSansHK.className);
    }
    if(getSettings("system.language") === "ja-jp") {
      document.body.classList.remove(notoSansSC.className);
      document.body.classList.add(notoSansJP.className);
    }
    if(getSettings("system.language") === "ko-kr") {
      document.body.classList.remove(notoSansSC.className);
      document.body.classList.add(notoSansKR.className);
    }

    doAutoUpdateCheck();
  }, []);

  return <></>;
}
