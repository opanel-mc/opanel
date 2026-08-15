import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { checkAuth } from "@/lib/api";

export function useCheckAuth(success?: () => void, done?: () => void) {
  const { push } = useRouter();

  useEffect(() => {
    checkAuth()
      .then((res) => {
        if(!res && window.location.pathname !== "/login" && window.location.pathname !== "/login/") {
          push("/login");
          return;
        }
        if(res) {
          success && success();
        }
      })
      .finally(() => done && done());
  // oxlint-disable-next-line react/exhaustive-deps
  }, []);
}
